package io.github.urntt.litematicacreator.creator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

/**
 * Runs a vanilla block item placement against one projection and returns the cells to commit: every cell vanilla
 * wrote, such as both halves of a door or a merged slab, plus one shape update of each directly adjacent projection
 * block so connections such as fences follow. Nothing chains further: no neighbour updates, redstone, fluids or ticks.
 */
final class CreatorPlacementSimulation
{
    private CreatorPlacementSimulation()
    {
    }

    enum Outcome
    {
        PLACED,
        /** Vanilla found no valid state, for example a door whose upper cell is occupied. */
        NO_STATE,
        /** The target cell is occupied or an entity is in the way; like vanilla, this is silent. */
        BLOCKED
    }

    /**
     * @param primaryPos the cell vanilla placed the block into
     * @param writes every cell to commit with its world-oriented state, in write order
     * @param blockEntities block entity data the placed item carried, for cells where it differs from the default
     */
    record Result(
            Outcome outcome,
            @Nullable BlockPos primaryPos,
            Map<BlockPos, BlockState> writes,
            Map<BlockPos, CompoundTag> blockEntities)
    {
        private static Result placed(BlockPos primaryPos, Map<BlockPos, BlockState> writes, Map<BlockPos, CompoundTag> blockEntities)
        {
            return new Result(
                    Outcome.PLACED,
                    primaryPos,
                    Collections.unmodifiableMap(writes),
                    Collections.unmodifiableMap(blockEntities)
            );
        }

        private static Result failed(Outcome outcome)
        {
            return new Result(outcome, null, Map.of(), Map.of());
        }

        boolean placed()
        {
            return this.outcome == Outcome.PLACED;
        }
    }

    /**
     * @param player the player whose view and sneaking drive placement, which is the camera entity when one is active
     * @param placement the projection to place into, or {@code null} for a new draft that is still empty
     */
    static Result simulate(
            Minecraft mc,
            Player player,
            CreatorPlacementHandResolver.Selection selection,
            CreatorEditTarget target,
            @Nullable SchematicPlacement placement)
    {
        Function<BlockPos, BlockState> projectionReader = placement != null ?
                pos -> CreatorSchematicEditor.getBlockState(placement, pos) :
                pos -> Blocks.AIR.defaultBlockState();
        // A clicked real block decides, as in vanilla, whether the block replaces it (grass) or goes next to it (stone).
        Map<BlockPos, BlockState> overrides = target.schematicBlock() || target.airTarget() ?
                Map.of() :
                Map.of(target.clickedBlockPos(), mc.level.getBlockState(target.clickedBlockPos()));
        CreatorPlacementWorld world = CreatorPlacementWorld.begin(mc.level, projectionReader, overrides);

        try
        {
            return simulate(mc, world, player, selection, target);
        }
        finally
        {
            world.end();
        }
    }

    private static Result simulate(
            Minecraft mc,
            CreatorPlacementWorld world,
            Player player,
            CreatorPlacementHandResolver.Selection selection,
            CreatorEditTarget target)
    {
        BlockHitResult hit = new BlockHitResult(target.hitVec(), target.side(), target.clickedBlockPos(), false);
        // Placement consumes the item it is given, so it gets a copy of the virtual stack.
        PlacementContext context = new PlacementContext(world, player, selection.hand(), selection.stack().copy(), hit);

        if (target.airTarget())
        {
            context.useClickedPosition();
        }

        if (!context.canPlace())
        {
            return Result.failed(Outcome.BLOCKED);
        }

        if (!selection.blockItem().place(context).consumesAction())
        {
            return Result.failed(Outcome.NO_STATE);
        }

        BlockPos primaryPos = world.placedPos() != null ? world.placedPos() : context.getClickedPos();
        Map<BlockPos, BlockState> writes = new LinkedHashMap<>();

        for (Map.Entry<BlockPos, BlockState> write : world.writes().entrySet())
        {
            BlockState existing = world.projectionState(write.getKey());

            if (write.getValue().equals(existing))
            {
                continue;
            }

            boolean checkedByVanilla = write.getKey().equals(primaryPos) && !world.isOverridden(write.getKey());

            if (!CreatorPlacementPolicy.canWriteAlongside(checkedByVanilla, existing.isAir(), existing.canBeReplaced()) ||
                !write.getValue().isAir() && !CreatorPlacementEntityCollision.canPlace(mc, world, write.getValue(), write.getKey()))
            {
                return Result.failed(Outcome.BLOCKED);
            }

            writes.put(write.getKey(), write.getValue());
        }

        if (writes.isEmpty())
        {
            return Result.failed(Outcome.BLOCKED);
        }

        Map<BlockPos, CompoundTag> blockEntities = placedBlockEntities(world, writes);
        writes.putAll(updateAdjacentShapes(world, writes));
        return Result.placed(primaryPos, writes, blockEntities);
    }

    // Vanilla applied the item's components and block entity data to the block entities it asked for while placing.
    private static Map<BlockPos, CompoundTag> placedBlockEntities(CreatorPlacementWorld world, Map<BlockPos, BlockState> writes)
    {
        Map<BlockPos, CompoundTag> blockEntities = new LinkedHashMap<>();

        for (Map.Entry<BlockPos, BlockState> write : writes.entrySet())
        {
            @Nullable BlockEntity blockEntity = world.placedBlockEntity(write.getKey());

            if (blockEntity != null && blockEntity.getType().isValid(write.getValue()))
            {
                blockEntity.setBlockState(write.getValue());
                CreatorBlockEntityData.persistent(blockEntity, world.registryAccess())
                        .ifPresent(data -> blockEntities.put(write.getKey(), data));
            }
        }

        return blockEntities;
    }

    private static Map<BlockPos, BlockState> updateAdjacentShapes(CreatorPlacementWorld world, Map<BlockPos, BlockState> writes)
    {
        RandomSource random = world.getRandom();
        Map<BlockPos, BlockState> updates = new LinkedHashMap<>();

        for (Map.Entry<BlockPos, BlockState> write : writes.entrySet())
        {
            for (Direction direction : Direction.values())
            {
                BlockPos neighbourPos = write.getKey().relative(direction);
                BlockState neighbour = updates.getOrDefault(neighbourPos, world.projectionState(neighbourPos));

                if (!CreatorShapeUpdatePolicy.updates(writes.containsKey(neighbourPos), neighbour))
                {
                    continue;
                }

                BlockState updated = neighbour.updateShape(
                        world,
                        world,
                        neighbourPos,
                        direction.getOpposite(),
                        write.getKey(),
                        write.getValue(),
                        random
                );

                if (CreatorShapeUpdatePolicy.keeps(neighbour, updated))
                {
                    updates.put(neighbourPos, updated);
                }
            }
        }

        return updates;
    }

    private static final class PlacementContext extends BlockPlaceContext
    {
        private PlacementContext(Level level, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit)
        {
            super(level, player, hand, stack, hit);
        }

        // An air target has no block to click; the target cell itself is where the block goes.
        private void useClickedPosition()
        {
            this.replaceClicked = true;
        }
    }
}

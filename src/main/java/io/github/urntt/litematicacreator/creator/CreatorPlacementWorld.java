package io.github.urntt.litematicacreator.creator;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.litematica.world.WorldSchematic;

/**
 * A detached level in which vanilla block placement runs against one projection. Reads see that projection plus the
 * real block that was clicked, if any; writes are only recorded, so nothing reaches a projection, Litematica's
 * schematic world or the real world until the caller commits them. Rules that only make sense for real blocks, such as
 * needing support, are relaxed for this level. It is used on the client thread only.
 */
public final class CreatorPlacementWorld extends WorldSchematic
{
    @Nullable private static CreatorPlacementWorld cached;

    private final ClientLevel clientLevel;
    private final Map<BlockPos, BlockState> writes = new LinkedHashMap<>();
    private final Map<BlockPos, BlockState> projection = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private Function<BlockPos, BlockState> projectionReader = CreatorPlacementWorld::air;
    private Map<BlockPos, BlockState> overrides = Map.of();
    @Nullable private BlockPos placedPos;

    private CreatorPlacementWorld(ClientLevel clientLevel)
    {
        super(
                new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, true),
                clientLevel.registryAccess(),
                clientLevel.dimensionTypeRegistration(),
                null
        );
        this.clientLevel = clientLevel;
    }

    /**
     * Starts a placement against a projection, reusing the level created for the current client level.
     *
     * @param projectionReader the projection's world-oriented state at a position
     * @param overrides states that hide the projection, such as the real block a placement clicked on
     */
    static CreatorPlacementWorld begin(
            ClientLevel clientLevel,
            Function<BlockPos, BlockState> projectionReader,
            Map<BlockPos, BlockState> overrides)
    {
        CreatorPlacementWorld world = cached;

        if (world == null || world.clientLevel != clientLevel)
        {
            world = new CreatorPlacementWorld(clientLevel);
            cached = world;
        }

        world.reset(projectionReader, Map.copyOf(overrides));
        return world;
    }

    /** Ends the current placement so the level no longer holds its projection. */
    void end()
    {
        this.reset(CreatorPlacementWorld::air, Map.of());
    }

    /** Drops the cached level, for example when the client leaves its world. */
    static void release()
    {
        cached = null;
    }

    /** Every position written since {@link #begin}, in write order, with its last written state. */
    Map<BlockPos, BlockState> writes()
    {
        return Collections.unmodifiableMap(this.writes);
    }

    /** The position vanilla reported as placed, or {@code null} when nothing reported a placement. */
    @Nullable
    BlockPos placedPos()
    {
        return this.placedPos;
    }

    /** The block entity vanilla placement filled for a written cell, or {@code null} when it never asked for one. */
    @Nullable
    BlockEntity placedBlockEntity(BlockPos pos)
    {
        return this.blockEntities.get(pos);
    }

    BlockState projectionState(BlockPos pos)
    {
        return this.projection.computeIfAbsent(pos.immutable(), this.projectionReader);
    }

    boolean isOverridden(BlockPos pos)
    {
        return this.overrides.containsKey(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        if (!this.isInValidBounds(pos))
        {
            return Blocks.VOID_AIR.defaultBlockState();
        }

        BlockState written = this.writes.get(pos);

        if (written != null)
        {
            return written;
        }

        BlockState override = this.overrides.get(pos);
        return override != null ? override : this.projectionState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags)
    {
        return this.setBlock(pos, state, flags, 512);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int updateLimit)
    {
        if (!this.isInValidBounds(pos))
        {
            return false;
        }

        this.writes.put(pos.immutable(), state);
        return true;
    }

    @Override
    public void neighborShapeChanged(
            Direction direction,
            BlockPos pos,
            BlockPos neighborPos,
            BlockState neighborState,
            int updateFlags,
            int updateLimit)
    {
        // Creator runs a single pass of shape updates over the projection itself once placement has finished.
    }

    // Written cells get a detached block entity so vanilla can apply item data to it; the projection itself has none here.
    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        BlockState state = this.writes.get(pos);

        if (state == null)
        {
            return null;
        }

        BlockEntity existing = this.blockEntities.get(pos);

        if (existing != null && existing.getType().isValid(state))
        {
            existing.setBlockState(state);
            return existing;
        }

        @Nullable BlockEntity created = CreatorBlockEntityData.create(pos.immutable(), state);

        if (created == null)
        {
            this.blockEntities.remove(pos);
            return null;
        }

        created.setLevel(this);
        this.blockEntities.put(pos.immutable(), created);
        return created;
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity)
    {
    }

    @Override
    public void removeBlockEntity(BlockPos pos)
    {
    }

    @Override
    public boolean addFreshEntity(Entity entity)
    {
        return false;
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 position, GameEvent.Context context)
    {
        if (event.is(GameEvent.BLOCK_PLACE))
        {
            this.placedPos = BlockPos.containing(position);
        }
    }

    private void reset(Function<BlockPos, BlockState> projectionReader, Map<BlockPos, BlockState> overrides)
    {
        this.writes.clear();
        this.projection.clear();
        this.blockEntities.clear();
        this.projectionReader = projectionReader;
        this.overrides = overrides;
        this.placedPos = null;
    }

    private static BlockState air(BlockPos pos)
    {
        return Blocks.AIR.defaultBlockState();
    }
}

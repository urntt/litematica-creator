package io.github.urntt.litematicacreator.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.compat.litematica.CreatorLitematicaDataAdapter;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.creator.CreatorEditGestureController;
import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ScheduledTick;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public final class CreatorClientGameTest implements FabricClientGameTest
{
    private static final String CREATOR_MIXIN_CONFIG = "mixins.litematica_creator.json";
    private static final float CAMERA_EYE_HEIGHT = 1.62F;

    @Override
    public void runTest(ClientGameTestContext context)
    {
        loadAllCreatorMixinTargets();

        try (TestSingleplayerContext world = context.worldBuilder().create())
        {
            context.waitFor(mc -> mc.player != null && mc.level != null);
            BlockPos origin = context.computeOnClient(mc -> mc.player.blockPosition().offset(3, 3, 0));
            SchematicPlacement placement = context.computeOnClient(mc ->
            {
                Configs.Generic.ENABLE_CREATOR_CAMERA_WITH_CREATOR_MODE.setBooleanValue(false);
                CreatorManager manager = CreatorManager.getInstance();
                manager.setCreatorModeEnabled(true, false);
                var draft = manager.createBlank(origin);
                check(draft != null, "A blank schematic must have a placement");
                check(manager.getFocus().placement() == draft, "The new draft must own focus");
                check(draft.getSchematic().getMetadata().getRegionCount() == 0, "Blank draft must be sparse");
                return draft;
            });

            context.runOnClient(mc ->
            {
                check(mc.level.getBlockState(origin).isAir(), "Test target must be real air");
                check(CreatorSchematicEditor.setBlockState(placement, origin, Blocks.STONE.defaultBlockState()),
                        "Projection placement must succeed");
                check(CreatorSchematicEditor.setBlockState(placement, origin.east(), Blocks.OAK_PLANKS.defaultBlockState()),
                        "Adjacent sparse cell must be created");
                check(CreatorSchematicEditor.getBlockState(placement, origin).is(Blocks.STONE),
                        "Projection must be readable without a combined world");
                check(placement.getSchematic().getMetadata().getTotalBlocks() == 2, "Block count must increase");
                check(CreatorSchematicEditor.setBlockState(placement, origin, Blocks.AIR.defaultBlockState()),
                        "Projection deletion must succeed");
                check(placement.getSchematic().getMetadata().getRegionCount() == 1, "Empty cell must be removed");
                check(placement.getSchematic().getMetadata().getTotalBlocks() == 1, "Block count must decrease");
                check(CreatorSchematicEditor.getBlockState(placement, origin.east()).is(Blocks.OAK_PLANKS),
                        "Deleting one cell must preserve the other");
                check(mc.level.getBlockState(origin).isAir() && mc.level.getBlockState(origin.east()).isAir(),
                        "Creator edits must not change the real client world");

                // Block entity data and ticks belong to the block in their cell and must not outlive it.
                BlockPos chestPos = origin.above();
                check(CreatorSchematicEditor.setBlockState(placement, chestPos, Blocks.CHEST.defaultBlockState()),
                        "Chest projection must be placed");
                var schematic = placement.getSchematic();
                String chestRegion = CreatorSchematicEditor.findRegionAt(placement, chestPos).regionName();
                attachCellData(schematic, chestRegion, "minecraft:chest");
                check(CreatorSchematicEditor.setBlockState(placement, chestPos,
                                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST)),
                        "Chest state change must succeed");
                check(schematic.getBlockEntityMapForRegion(chestRegion).containsKey(BlockPos.ZERO),
                        "A state change of the same block must keep its block entity data");
                check(CreatorSchematicEditor.setBlockState(placement, chestPos, Blocks.BARREL.defaultBlockState()),
                        "Replacing the chest must succeed");
                check(schematic.getBlockEntityMapForRegion(chestRegion).isEmpty()
                                && schematic.getScheduledBlockTicksForRegion(chestRegion).isEmpty(),
                        "Replacing a block must clear its block entity data and ticks");
                attachCellData(schematic, chestRegion, "minecraft:barrel");
                check(CreatorSchematicEditor.setBlockState(placement, chestPos, Blocks.AIR.defaultBlockState()),
                        "Barrel deletion must succeed");
                check(schematic.getSubRegionContainer(chestRegion) == null
                                && schematic.getMetadata().getRegionCount() == 1,
                        "Deleting a block must clear its cell data so the empty cell is removed");
            });
            world.getServer().runOnServer(server ->
            {
                check(server.overworld().getBlockState(origin).isAir()
                                && server.overworld().getBlockState(origin.east()).isAir(),
                        "Creator edits must not reach the integrated server");
            });

            context.runOnClient(mc ->
            {
                var realHand = mc.player.getMainHandItem().copy();
                var inventory = CreatorInventory.getInstance();
                inventory.runTransaction(() ->
                {
                    for (int slot = 0; slot < CreatorInventory.SLOT_COUNT; ++slot)
                    {
                        inventory.setStack(slot, ItemStack.EMPTY);
                    }
                    inventory.setSelectedHotbarSlot(0);
                    check(inventory.pickBlock(Blocks.STONE.defaultBlockState()), "Virtual pick must succeed");
                    check(inventory.getSelectedStack().getCount() == 1, "First virtual pick must give one item");
                    inventory.setSelectedHotbarSlot(1);
                    inventory.pickBlock(Blocks.STONE.defaultBlockState());
                    check(inventory.getSelectedHotbarSlot() == 0, "Repeated pick must select the existing stack");
                    inventory.setStack(CreatorInventory.OFFHAND_SLOT, new ItemStack(Items.OAK_PLANKS));
                    inventory.swapSelectedWithOffhand();
                    check(inventory.getSelectedStack().is(Items.OAK_PLANKS), "Virtual offhand swap must succeed");
                });
                check(ItemStack.matches(realHand, mc.player.getMainHandItem()), "Real inventory must not change");

                var cameras = CreatorCameraController.getInstance();
                check(cameras.activate(mc), "Creator Camera must initialize in a real client");
                check(mc.getCameraEntity() == cameras.getCamera(), "Camera entity must be the substitute");
                check(cameras.getCamera() != mc.player, "Camera must not replace the actual player");
                cameras.deactivate(mc);
                check(mc.getCameraEntity() == mc.player, "Camera exit must restore the original player view");
                check(!cameras.isActive(), "Camera session must be released");
            });

            context.runOnClient(mc ->
            {
                check(!EntityUtils.shouldPickBlock(mc.player),
                        "Litematica's schematic pick block must not touch the real inventory in Creator mode");

                var inventory = CreatorInventory.getInstance();
                var realMainHand = mc.player.getMainHandItem().copy();
                inventory.runTransaction(() ->
                {
                    for (int slot = 0; slot < CreatorInventory.SLOT_COUNT; ++slot)
                    {
                        inventory.setStack(slot, ItemStack.EMPTY);
                    }
                    inventory.setSelectedHotbarSlot(0);
                    inventory.setStack(0, new ItemStack(Items.STICK));
                });
                check(EntityUtils.hasToolItem(mc.player), "Litematica must see the tool item in the Creator main hand");
                inventory.runTransaction(() ->
                {
                    inventory.setStack(0, ItemStack.EMPTY);
                    inventory.setStack(CreatorInventory.OFFHAND_SLOT, new ItemStack(Items.STICK));
                });
                check(EntityUtils.hasToolItem(mc.player), "Litematica must see the tool item in the Creator offhand");
                inventory.runTransaction(() -> inventory.setStack(CreatorInventory.OFFHAND_SLOT, ItemStack.EMPTY));
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                check(!EntityUtils.hasToolItem(mc.player), "A real tool item must not count while Creator mode is on");
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, realMainHand);

                var cameras = CreatorCameraController.getInstance();
                check(cameras.activate(mc), "Creator Camera must activate for the empty swing");
                cameras.getCamera().setXRot(-90.0F);
                check(!cameras.getCamera().isSwinging() && !mc.player.isSwinging(), "No swing may be in progress yet");
                CreatorEditGestureController.INSTANCE.onBreakInput(true, true);
            });
            context.waitTicks(1);
            context.runOnClient(mc ->
            {
                CreatorEditGestureController.INSTANCE.onBreakInput(false, true);
                var cameras = CreatorCameraController.getInstance();
                check(cameras.getCamera().isSwinging(), "An attack that edits nothing must swing the Creator Camera stand-in");
                check(!mc.player.isSwinging(), "An empty Creator swing must not animate the real player");
                check(CreatorSchematicEditor.getBlockState(placement, origin.east()).is(Blocks.OAK_PLANKS),
                        "An empty swing must not edit the projection");
                cameras.deactivate(mc);
            });

            context.waitFor(mc -> SchematicWorldHandler.getSchematicWorld() != null
                    && SchematicWorldHandler.getSchematicWorld().getBlockState(origin.east()).is(Blocks.OAK_PLANKS));
            context.runOnClient(mc ->
            {
                mc.player.setYRot(-90);
                mc.player.setXRot(-30);
            });
            world.getConnection().waitForChunksRender();
            context.takeScreenshot("creator-projection-smoke");
            context.runOnClient(mc ->
            {
                var cameras = CreatorCameraController.getInstance();
                check(cameras.activate(mc), "Creator Camera must reactivate for the first-person view");
                // Step back so the real player's avatar, the camera's hands and the projection share one frame.
                cameras.getCamera().setPos(cameras.getCamera().position().add(-3.0D, 0.0D, 0.0D));
            });
            context.waitTicks(10);
            context.takeScreenshot("creator-camera-first-person");
            context.runOnClient(mc ->
            {
                CreatorCameraController.getInstance().deactivate(mc);
                check(mc.getCameraEntity() == mc.player, "Camera exit must restore the original player view");
            });

            BlockPos stairsPos = origin.above(2);
            context.runOnClient(mc -> check(
                    CreatorSchematicEditor.setBlockState(placement, stairsPos, Blocks.OAK_STAIRS.defaultBlockState()),
                    "Stairs projection must be placed"));
            context.waitFor(mc -> SchematicWorldHandler.getSchematicWorld() != null
                    && SchematicWorldHandler.getSchematicWorld().getBlockState(stairsPos).is(Blocks.OAK_STAIRS));
            context.runOnClient(mc ->
            {
                var inventory = CreatorInventory.getInstance();
                inventory.runTransaction(() ->
                {
                    for (int slot = 0; slot < CreatorInventory.SLOT_COUNT; ++slot)
                    {
                        inventory.setStack(slot, ItemStack.EMPTY);
                    }
                    inventory.setSelectedHotbarSlot(0);
                    inventory.setStack(0, new ItemStack(Items.DEBUG_STICK));
                });
                var cameras = CreatorCameraController.getInstance();
                check(cameras.activate(mc), "Creator Camera must activate for the debug stick");
                // Look straight down at the stairs so the Creator ray hits the projection deterministically.
                cameras.getCamera().setPos(stairsPos.getX() + 0.5D, stairsPos.getY() + 2.0D, stairsPos.getZ() + 0.5D);
                cameras.getCamera().setXRot(90.0F);
                CreatorEditGestureController.INSTANCE.onBreakInput(true, true);
            });
            context.waitTicks(1);
            context.runOnClient(mc ->
            {
                CreatorEditGestureController.INSTANCE.onBreakInput(false, true);
                var stickState = CreatorInventory.getInstance().getSelectedStack().get(DataComponents.DEBUG_STICK_STATE);
                check(stickState != null
                                && stickState.properties().get(Blocks.OAK_STAIRS.builtInRegistryHolder()) == StairBlock.FACING,
                        "A debug stick attack must select the first stairs property on the virtual stick");
                check(CreatorSchematicEditor.getBlockState(placement, stairsPos).getValue(StairBlock.FACING) == Direction.NORTH,
                        "Selecting a debug stick property must not change the projection");
                CreatorEditGestureController.INSTANCE.onPlaceInput(true, true);
            });
            context.waitTicks(1);
            context.runOnClient(mc ->
            {
                CreatorEditGestureController.INSTANCE.onPlaceInput(false, true);
                Direction expected = Util.findNextInIterable(StairBlock.FACING.getPossibleValues(), Direction.NORTH);
                check(CreatorSchematicEditor.getBlockState(placement, stairsPos).getValue(StairBlock.FACING) == expected,
                        "A debug stick use must cycle the selected projection property");
                check(CreatorSchematicEditor.getBlockState(placement, stairsPos.above()).isAir(),
                        "A debug stick use must not place a projection block");
                check(mc.level.getBlockState(stairsPos).isAir(), "The debug stick must not change the real world");
                CreatorCameraController.getInstance().deactivate(mc);
            });
            runPlacementMatrix(context, world, placement, origin);
            runBlockEntityMatrix(context, world, placement, origin);
            context.runOnClient(mc ->
            {
                var manager = CreatorManager.getInstance();
                var selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
                manager.saveCurrentDraft();
                check(manager.getFocus() == null, "Finish editing must clear focus");
                check(DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement() == selected,
                        "Finish editing must not change Litematica selection");
                manager.focusPlacement(placement);
                manager.discardCurrentDraft();
                check(manager.getFocus() == null, "Discard must release focus");
                check(!DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().contains(placement),
                        "Discard must unload the placement");
                manager.setCreatorModeEnabled(false, false);
                Configs.Generic.ENABLE_CREATOR_CAMERA_WITH_CREATOR_MODE.resetToDefault();

                check(EntityUtils.shouldPickBlock(mc.player), "Litematica's schematic pick block must return outside Creator mode");
                var realMainHand = mc.player.getMainHandItem().copy();
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                check(EntityUtils.hasToolItem(mc.player), "The real hand must hold the Litematica tool outside Creator mode");
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, realMainHand);
            });
            check(!Boolean.getBoolean("litematica.creator.gametest.verifyFailure"),
                    "Intentional failure to verify the client GameTest failure gate");
            LitematicaCreator.LOGGER.info("Creator client GameTest passed: projection edits, virtual inventory, camera, focus, discard and real-world isolation");
        }
    }

    /**
     * Places blocks through the real use-input path with the Creator Camera aimed at fixed targets. Every case runs and
     * all failures are reported together, so one run shows the whole matrix.
     */
    private static void runPlacementMatrix(
            ClientGameTestContext context,
            TestSingleplayerContext world,
            SchematicPlacement placement,
            BlockPos origin)
    {
        BlockPos door = origin.offset(0, 0, 4);
        BlockPos bed = origin.offset(6, 0, 4);
        BlockPos slab = origin.offset(12, 0, 4);
        BlockPos candle = origin.offset(0, 0, 9);
        BlockPos fence = origin.offset(6, 0, 9);
        BlockPos torchSupport = origin.offset(12, 0, 9);
        BlockPos blockedDoor = origin.offset(0, 0, 14);
        BlockPos realGrass = origin.offset(6, 0, 14);
        BlockPos lichenSupport = origin.offset(12, 0, 14);
        BlockPos commandBlock = origin.offset(0, 0, 19);
        BlockPos dripstone = origin.offset(6, 0, 19);
        BlockPos bamboo = origin.offset(12, 0, 19);
        BlockPos scaffolding = origin.offset(0, 0, 24);
        BlockPos seagrass = origin.offset(6, 0, 24);
        BlockPos kelp = origin.offset(12, 0, 24);
        List<BlockPos> projectionOnly = List.of(
                door.above(), door.above(2), bed.above(), bed.above().south(), slab.above(), candle.above(),
                fence.above(), fence.above().west(), torchSupport.east(), blockedDoor.east(), lichenSupport.east(),
                commandBlock.above(), dripstone.above(), bamboo.above(), scaffolding.above(), seagrass.above(), kelp.above()
        );
        List<String> failures = new ArrayList<>();

        world.getServer().runOnServer(server ->
        {
            server.overworld().setBlockAndUpdate(realGrass, Blocks.GRASS_BLOCK.defaultBlockState());
            server.overworld().setBlockAndUpdate(realGrass.above(), Blocks.SHORT_GRASS.defaultBlockState());
        });
        context.runOnClient(mc ->
        {
            check(CreatorSchematicEditor.setBlockState(placement, door, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, bed, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, slab, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, candle, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, fence, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, fence.above().west(), Blocks.OAK_FENCE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, torchSupport, Blocks.OAK_SLAB.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, lichenSupport, Blocks.OAK_SLAB.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, blockedDoor, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, blockedDoor.east().above(), Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, commandBlock, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, dripstone, Blocks.OAK_SLAB.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, bamboo, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, scaffolding, Blocks.OAK_SLAB.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, seagrass, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, kelp, Blocks.STONE.defaultBlockState()),
                    "Placement matrix supports must be written");
            check(CreatorCameraController.getInstance().activate(mc), "Creator Camera must activate for the placement matrix");
        });
        awaitSchematicWorld(context, placement, door, bed, slab, candle, fence, fence.above().west(), torchSupport,
                lichenSupport, blockedDoor, blockedDoor.east().above(), commandBlock, dripstone, bamboo, scaffolding, seagrass, kelp);
        context.waitFor(mc -> mc.level.getBlockState(realGrass.above()).is(Blocks.SHORT_GRASS));

        useFromAbove(context, Items.OAK_DOOR, door.above(3));
        context.runOnClient(mc ->
        {
            BlockState lower = CreatorSchematicEditor.getBlockState(placement, door.above());
            BlockState upper = CreatorSchematicEditor.getBlockState(placement, door.above(2));
            expect(failures, lower.is(Blocks.OAK_DOOR) && lower.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER,
                    "A door must place its lower half on the clicked face");
            expect(failures, upper.is(Blocks.OAK_DOOR) && upper.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                    "A door must also place its upper half");
        });

        useFromAbove(context, Items.STRAW_BED, bed.above(2));
        context.runOnClient(mc ->
        {
            BlockState foot = CreatorSchematicEditor.getBlockState(placement, bed.above());
            BlockState head = CreatorSchematicEditor.getBlockState(placement, bed.above().south());
            expect(failures, foot.is(Blocks.STRAW_BED) && foot.getValue(AbstractBedBlock.PART) == BedPart.FOOT,
                    "A bed must place its foot on the clicked face");
            expect(failures, head.is(Blocks.STRAW_BED) && head.getValue(AbstractBedBlock.PART) == BedPart.HEAD,
                    "A bed must also place its head in the facing direction");
        });

        useFromAbove(context, Items.OAK_SLAB, slab.above(2));
        awaitSchematicWorld(context, placement, slab.above());
        useFromAbove(context, Items.OAK_SLAB, slab.above(2));
        context.runOnClient(mc ->
        {
            BlockState merged = CreatorSchematicEditor.getBlockState(placement, slab.above());
            expect(failures, merged.is(Blocks.OAK_SLAB) && merged.getValue(SlabBlock.TYPE) == SlabType.DOUBLE,
                    "Using a slab on the top of a bottom slab must merge them into a double slab");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, slab.above(2)).isAir(),
                    "Merging slabs must not place a second slab above");
        });

        useFromAbove(context, Items.CANDLE, candle.above(2));
        awaitSchematicWorld(context, placement, candle.above());
        useFromAbove(context, Items.CANDLE, candle.above(2));
        context.runOnClient(mc ->
        {
            BlockState candles = CreatorSchematicEditor.getBlockState(placement, candle.above());
            expect(failures, candles.is(Blocks.CANDLE) && candles.getValue(CandleBlock.CANDLES) == 2,
                    "Using a candle on a candle must add a second candle to the same cell");
        });

        // A fence is 1.5 blocks tall, so the camera stands one block higher to stay clear of it.
        useFromAbove(context, Items.OAK_FENCE, fence.above(3));
        context.runOnClient(mc ->
        {
            BlockState placed = CreatorSchematicEditor.getBlockState(placement, fence.above());
            BlockState neighbour = CreatorSchematicEditor.getBlockState(placement, fence.above().west());
            expect(failures, placed.is(Blocks.OAK_FENCE) && placed.getValue(CrossCollisionBlock.WEST),
                    "A placed fence must connect to the adjacent projection fence");
            expect(failures, neighbour.is(Blocks.OAK_FENCE) && neighbour.getValue(CrossCollisionBlock.EAST),
                    "The adjacent projection fence must update its shape to connect back");
        });

        // The bottom slab's side is not sturdy, so vanilla survival rules would refuse a wall torch here.
        useFromEast(context, Items.TORCH, torchSupport, 0.25D);
        context.runOnClient(mc ->
        {
            BlockState torch = CreatorSchematicEditor.getBlockState(placement, torchSupport.east());
            expect(failures, torch.is(Blocks.WALL_TORCH) && torch.getValue(WallTorchBlock.FACING) == Direction.EAST,
                    "A torch on a projection side must become a wall torch facing out, without needing support");
        });

        // Multiface blocks check their own attachment faces instead of survival; a slab side is not a full face.
        useFromEast(context, Items.GLOW_LICHEN, lichenSupport, 0.25D);
        context.runOnClient(mc ->
        {
            BlockState lichen = CreatorSchematicEditor.getBlockState(placement, lichenSupport.east());
            expect(failures, lichen.is(Blocks.GLOW_LICHEN) && MultifaceBlock.hasFace(lichen, Direction.WEST),
                    "Glow lichen on a projection side must attach to the clicked face, without needing a full face");
        });

        // Game master blocks normally need a creative operator, and the camera stand-in has no permissions at all.
        useFromAbove(context, Items.COMMAND_BLOCK, commandBlock.above(2));
        context.runOnClient(mc -> expect(failures,
                CreatorSchematicEditor.getBlockState(placement, commandBlock.above()).is(Blocks.COMMAND_BLOCK),
                "A command block must be placeable without the real player's permissions"));

        // These blocks check their surroundings directly in their placement rules: support, soil, or water.
        useFromAbove(context, Items.POINTED_DRIPSTONE, dripstone.above(2));
        useFromAbove(context, Items.BAMBOO, bamboo.above(2));
        useFromAbove(context, Items.SCAFFOLDING, scaffolding.above(2));
        useFromAbove(context, Items.SEAGRASS, seagrass.above(2));
        useFromAbove(context, Items.KELP, kelp.above(2));
        context.runOnClient(mc ->
        {
            BlockState stalagmite = CreatorSchematicEditor.getBlockState(placement, dripstone.above());
            expect(failures, stalagmite.is(Blocks.POINTED_DRIPSTONE)
                            && stalagmite.getValue(SpeleothemBlock.TIP_DIRECTION) == Direction.UP,
                    "Pointed dripstone on a slab top must grow up from the clicked face, without a sturdy support");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, bamboo.above()).is(Blocks.BAMBOO_SAPLING),
                    "Bamboo must be placeable without soil below");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, scaffolding.above()).is(Blocks.SCAFFOLDING),
                    "Scaffolding must be placeable without support below");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, seagrass.above()).is(Blocks.SEAGRASS),
                    "Seagrass must be placeable without water");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, kelp.above()).is(Blocks.KELP),
                    "Kelp must be placeable without water");
        });

        context.runOnClient(mc -> CreatorManager.getInstance().clearFocusSilently());
        useFromEast(context, Items.OAK_DOOR, blockedDoor, 0.5D);
        context.runOnClient(mc ->
        {
            expect(failures, CreatorSchematicEditor.getBlockState(placement, blockedDoor.east()).isAir(),
                    "A door whose upper cell is occupied must not be placed");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, blockedDoor.east().above()).is(Blocks.STONE),
                    "A refused door must leave the occupying projection in place");
            expect(failures, CreatorManager.getInstance().getFocus() == null,
                    "A refused placement must not move Focus");
            CreatorManager.getInstance().focusPlacement(placement);
        });

        useFromAbove(context, Items.STONE, realGrass.above(3));
        context.runOnClient(mc ->
        {
            expect(failures, CreatorSchematicEditor.getBlockState(placement, realGrass.above()).is(Blocks.STONE),
                    "Using a block on real short grass must replace the grass cell, as vanilla does");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, realGrass.above(2)).isAir(),
                    "Using a block on real short grass must not place above the grass");
            expect(failures, mc.level.getBlockState(realGrass.above()).is(Blocks.SHORT_GRASS),
                    "Creator placement must not replace the real short grass");

            for (BlockPos pos : projectionOnly)
            {
                expect(failures, mc.level.getBlockState(pos).isAir(), "Creator placement must not change the real world at " + pos);
            }

            // Look down over the whole matrix from its north edge for the screenshot.
            var camera = CreatorCameraController.getInstance().getCamera();
            Vec3 overview = Vec3.atBottomCenterOf(origin.offset(6, 12, -4));
            camera.setPos(overview);
            camera.setYRot(0.0F);
            camera.setXRot(45.0F);
            camera.setOldPosAndRot(overview, 0.0F, 45.0F);
        });
        awaitSchematicWorld(context, placement, door.above(2), bed.above().south(), slab.above(), candle.above(),
                fence.above(), fence.above().west(), torchSupport.east(), lichenSupport.east(), realGrass.above(),
                commandBlock.above(), dripstone.above(), bamboo.above(), scaffolding.above(), seagrass.above(), kelp.above());
        // Let the in-game placement messages fade so they do not cover the matrix.
        context.waitTicks(120);
        context.takeScreenshot("creator-placement-matrix");
        context.runOnClient(mc -> CreatorCameraController.getInstance().deactivate(mc));
        world.getServer().runOnServer(server ->
        {
            for (BlockPos pos : projectionOnly)
            {
                expect(failures, server.overworld().getBlockState(pos).isAir(),
                        "Creator placement must not reach the integrated server at " + pos);
            }
        });

        check(failures.isEmpty(), "Placement matrix failed:\n - " + String.join("\n - ", failures));
    }

    /**
     * Keeps block entity data carried by placed items. Like the placement matrix, every case runs and failures are
     * reported together.
     */
    private static void runBlockEntityMatrix(
            ClientGameTestContext context,
            TestSingleplayerContext world,
            SchematicPlacement placement,
            BlockPos origin)
    {
        BlockPos namedChest = origin.offset(0, 0, -8);
        BlockPos banner = origin.offset(6, 0, -8);
        BlockPos commandData = origin.offset(12, 0, -8);
        BlockPos plainChest = origin.offset(0, 0, -13);
        List<BlockPos> projectionOnly = List.of(
                namedChest.above(), banner.above(), commandData.above(), plainChest.above()
        );
        List<String> failures = new ArrayList<>();
        List<ItemStack> realInventory = context.computeOnClient(mc ->
        {
            List<ItemStack> stacks = new ArrayList<>();

            for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); ++slot)
            {
                stacks.add(mc.player.getInventory().getItem(slot).copy());
            }

            return stacks;
        });

        context.runOnClient(mc ->
        {
            check(CreatorSchematicEditor.setBlockState(placement, namedChest, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, banner, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, commandData, Blocks.STONE.defaultBlockState())
                            && CreatorSchematicEditor.setBlockState(placement, plainChest, Blocks.STONE.defaultBlockState()),
                    "Block entity matrix projections must be written");
            check(CreatorCameraController.getInstance().activate(mc), "Creator Camera must activate for the block entity matrix");
        });
        awaitSchematicWorld(context, placement, namedChest, banner, commandData, plainChest);

        ItemStack named = new ItemStack(Items.CHEST);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Creator Chest"));
        useFromAbove(context, named, namedChest.above(2));
        ItemStack patterned = context.computeOnClient(mc ->
        {
            ItemStack stack = new ItemStack(Items.BANNER.pick(DyeColor.WHITE));
            stack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers.Builder()
                    .add(mc.level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.STRIPE_TOP), DyeColor.RED)
                    .build());
            return stack;
        });
        useFromAbove(context, patterned, banner.above(3));
        CompoundTag command = new CompoundTag();
        command.putString("Command", "say creator");
        ItemStack commandBlock = new ItemStack(Items.COMMAND_BLOCK);
        commandBlock.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityTypes.COMMAND_BLOCK, command));
        useFromAbove(context, commandBlock, commandData.above(2));
        useFromAbove(context, new ItemStack(Items.CHEST), plainChest.above(2));
        awaitSchematicWorld(context, placement, namedChest.above(), banner.above(), commandData.above(), plainChest.above());
        context.waitTicks(5);
        context.runOnClient(mc ->
        {
            CompoundTag chestData = blockEntityData(placement, namedChest.above());
            expect(failures, chestData != null && chestData.contains("CustomName"),
                    "A named chest must keep its custom name in the projection");
            expect(failures, SchematicWorldHandler.getSchematicWorld().getBlockEntity(namedChest.above()) instanceof ChestBlockEntity chest
                            && chest.getCustomName() != null,
                    "The schematic world must load the placed chest's custom name");
            CompoundTag bannerData = blockEntityData(placement, banner.above());
            expect(failures, bannerData != null && bannerData.contains("patterns"),
                    "A banner must keep its patterns in the projection");
            expect(failures, SchematicWorldHandler.getSchematicWorld().getBlockEntity(banner.above()) instanceof BannerBlockEntity flag
                            && flag.getPatterns().layers().size() == 1,
                    "The schematic world must render the placed banner's pattern");
            CompoundTag commandTag = blockEntityData(placement, commandData.above());
            expect(failures, commandTag != null && "say creator".equals(commandTag.getStringOr("Command", "")),
                    "Block entity data on an item must be kept without the real player's permissions");
            expect(failures, CreatorSchematicEditor.getBlockState(placement, plainChest.above()).is(Blocks.CHEST)
                            && blockEntityData(placement, plainChest.above()) == null,
                    "A plain chest must not store default block entity data");
        });

        context.runOnClient(mc ->
        {
            for (int slot = 0; slot < realInventory.size(); ++slot)
            {
                expect(failures, ItemStack.matches(realInventory.get(slot), mc.player.getInventory().getItem(slot)),
                        "Block entity edits must not change the real inventory at slot " + slot);
            }

            for (BlockPos pos : projectionOnly)
            {
                expect(failures, mc.level.getBlockState(pos).isAir(), "Block entity edits must not change the real world at " + pos);
            }

            CreatorCameraController.getInstance().deactivate(mc);
        });
        world.getServer().runOnServer(server ->
        {
            for (BlockPos pos : projectionOnly)
            {
                expect(failures, server.overworld().getBlockState(pos).isAir(),
                        "Block entity edits must not reach the integrated server at " + pos);
            }
        });

        check(failures.isEmpty(), "Block entity matrix failed:\n - " + String.join("\n - ", failures));
    }

    @Nullable
    private static CompoundTag blockEntityData(SchematicPlacement placement, BlockPos pos)
    {
        var target = CreatorSchematicEditor.findRegionAt(placement, pos);

        if (target == null)
        {
            return null;
        }

        // Creator cells are 1x1x1 regions, so the cell's data lives at the region origin.
        CompoundData data = placement.getSchematic().getBlockEntityMapForRegion(target.regionName()).get(BlockPos.ZERO);
        return data != null ? CreatorLitematicaDataAdapter.copyToVanilla(data) : null;
    }

    // Looks straight down at the block below the camera's feet; the camera stays clear of every placed shape.
    private static void useFromAbove(ClientGameTestContext context, Item item, BlockPos cameraFeet)
    {
        useFromAbove(context, new ItemStack(item), cameraFeet);
    }

    private static void useFromAbove(ClientGameTestContext context, ItemStack stack, BlockPos cameraFeet)
    {
        useWithCamera(context, stack, Vec3.atBottomCenterOf(cameraFeet), 0.0F, 90.0F);
    }

    // Looks west at the east face of the target block, at the given height within that face.
    private static void useFromEast(ClientGameTestContext context, Item item, BlockPos target, double faceHeight)
    {
        Vec3 eyes = new Vec3(target.getX() + 3.5D, target.getY() + faceHeight, target.getZ() + 0.5D);
        useWithCamera(context, new ItemStack(item), eyes.subtract(0.0D, CAMERA_EYE_HEIGHT, 0.0D), 90.0F, 0.0F);
    }

    private static void useWithCamera(ClientGameTestContext context, ItemStack stack, Vec3 feet, float yRot, float xRot)
    {
        context.runOnClient(mc ->
        {
            var inventory = CreatorInventory.getInstance();
            inventory.runTransaction(() ->
            {
                for (int slot = 0; slot < CreatorInventory.SLOT_COUNT; ++slot)
                {
                    inventory.setStack(slot, ItemStack.EMPTY);
                }
                inventory.setSelectedHotbarSlot(0);
                inventory.setStack(0, stack.copy());
            });
            var camera = CreatorCameraController.getInstance().getCamera();
            // A flying stand-in has no gravity, so it stays where it is aimed until the use input runs.
            camera.getAbilities().flying = true;
            camera.onUpdateAbilities();
            camera.setDeltaMovement(Vec3.ZERO);
            camera.setPos(feet);
            camera.setYRot(yRot);
            camera.setXRot(xRot);
            camera.setOldPosAndRot(feet, yRot, xRot);
            CreatorEditGestureController.INSTANCE.onPlaceInput(true, true);
        });
        context.waitTicks(1);
        context.runOnClient(mc ->
        {
            CreatorEditGestureController.INSTANCE.onPlaceInput(false, true);
            var camera = CreatorCameraController.getInstance().getCamera();
            check(camera.position().equals(feet) && camera.getEyeHeight() == CAMERA_EYE_HEIGHT,
                    "The Creator Camera must not move while a placement is aimed");
        });
    }

    private static void awaitSchematicWorld(ClientGameTestContext context, SchematicPlacement placement, BlockPos... positions)
    {
        context.waitFor(mc ->
        {
            var schematicWorld = SchematicWorldHandler.getSchematicWorld();

            if (schematicWorld == null)
            {
                return false;
            }

            for (BlockPos pos : positions)
            {
                if (schematicWorld.getBlockState(pos) != CreatorSchematicEditor.getBlockState(placement, pos))
                {
                    return false;
                }
            }

            return true;
        });
    }

    private static void expect(List<String> failures, boolean condition, String message)
    {
        if (!condition)
        {
            failures.add(message);
        }
    }

    private static void attachCellData(LitematicaSchematic schematic, String regionName, String blockEntityId)
    {
        schematic.getBlockEntityMapForRegion(regionName).put(BlockPos.ZERO, new CompoundData().putString("id", blockEntityId));
        schematic.getScheduledBlockTicksForRegion(regionName).put(BlockPos.ZERO,
                new ScheduledTick<>(Blocks.STONE, BlockPos.ZERO, 0L, 0L));
    }

    // Classes untouched by this scenario would otherwise skip Mixin application, hiding broken injections after a port.
    private static void loadAllCreatorMixinTargets()
    {
        ClassLoader loader = CreatorClientGameTest.class.getClassLoader();
        Set<String> targets = creatorMixinTargets(loader);

        for (String className : targets)
        {
            try
            {
                Class.forName(className, false, loader);
            }
            catch (ClassNotFoundException | LinkageError | RuntimeException exception)
            {
                throw new AssertionError("Creator Mixin target failed to load: " + className, exception);
            }
        }

        check(!targets.isEmpty(), "Creator Mixin config must declare targets");
        LitematicaCreator.LOGGER.info("Loaded {} Creator Mixin targets", targets.size());
    }

    private static Set<String> creatorMixinTargets(ClassLoader loader)
    {
        Set<String> targets = new LinkedHashSet<>();
        JsonObject config;

        try (Reader reader = new InputStreamReader(resource(loader, CREATOR_MIXIN_CONFIG), StandardCharsets.UTF_8))
        {
            config = JsonParser.parseReader(reader).getAsJsonObject();
        }
        catch (IOException exception)
        {
            throw new AssertionError("Creator Mixin config is unreadable", exception);
        }

        String mixinPackage = config.get("package").getAsString();

        for (JsonElement mixin : config.getAsJsonArray("client"))
        {
            String path = (mixinPackage + "." + mixin.getAsString()).replace('.', '/') + ".class";
            ClassNode node = new ClassNode();

            try (InputStream input = resource(loader, path))
            {
                new ClassReader(input).accept(node, ClassReader.SKIP_CODE);
            }
            catch (IOException exception)
            {
                throw new AssertionError("Creator Mixin class is unreadable: " + path, exception);
            }

            for (AnnotationNode annotation : node.invisibleAnnotations != null ? node.invisibleAnnotations : List.<AnnotationNode>of())
            {
                if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;") && annotation.values != null)
                {
                    for (int i = 0; i < annotation.values.size(); i += 2)
                    {
                        String key = (String) annotation.values.get(i);

                        if (key.equals("value") || key.equals("targets"))
                        {
                            for (Object value : (List<?>) annotation.values.get(i + 1))
                            {
                                targets.add(value instanceof Type type ? type.getClassName() : ((String) value).replace('/', '.'));
                            }
                        }
                    }
                }
            }
        }

        return targets;
    }

    private static InputStream resource(ClassLoader loader, String path)
    {
        InputStream input = loader.getResourceAsStream(path);

        if (input == null)
        {
            throw new AssertionError("Missing resource " + path);
        }

        return input;
    }

    private static void check(boolean condition, String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }
}

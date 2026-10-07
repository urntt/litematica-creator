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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.ticks.ScheduledTick;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public final class CreatorClientGameTest implements FabricClientGameTest
{
    private static final String CREATOR_MIXIN_CONFIG = "mixins.litematica_creator.json";

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

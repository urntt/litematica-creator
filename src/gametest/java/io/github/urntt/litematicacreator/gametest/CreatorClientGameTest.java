package io.github.urntt.litematicacreator.gametest;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.creator.CreatorInventory;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class CreatorClientGameTest implements FabricClientGameTest
{
    @Override
    public void runTest(ClientGameTestContext context)
    {
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
            });
            check(!Boolean.getBoolean("litematica.creator.gametest.verifyFailure"),
                    "Intentional failure to verify the client GameTest failure gate");
            LitematicaCreator.LOGGER.info("Creator client GameTest passed: projection edits, virtual inventory, camera, focus, discard and real-world isolation");
        }
    }

    private static void check(boolean condition, String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }
}

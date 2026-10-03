package io.github.urntt.litematicacreator.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.config.Configs;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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

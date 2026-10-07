package io.github.urntt.litematicacreator.creator;

import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreatorDebugStickTest
{
    private static final Holder<Block> STAIRS = Blocks.OAK_STAIRS.builtInRegistryHolder();

    @BeforeAll
    static void bootstrapMinecraft()
    {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        DataComponentMap testDefaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        BuiltInRegistries.ITEM.listElements().forEach(holder ->
        {
            if (!holder.areComponentsBound())
            {
                holder.bindComponents(testDefaults);
            }
        });
    }

    @Test
    void selectionWalksThroughEveryPropertyAndWrapsAround()
    {
        List<Property<?>> properties = List.copyOf(Blocks.OAK_STAIRS.getStateDefinition().getProperties());
        DebugStickState stickState = DebugStickState.EMPTY;

        for (Property<?> expected : properties)
        {
            CreatorDebugStick.Action action = CreatorDebugStick.select(Blocks.OAK_STAIRS.defaultBlockState(), stickState, false);
            stickState = action.stickState();
            assertSame(expected, stickState.properties().get(STAIRS));
            assertNull(action.state());
            assertEquals("item.minecraft.debug_stick.select", key(action.message()));
        }

        assertSame(properties.getFirst(), CreatorDebugStick.select(Blocks.OAK_STAIRS.defaultBlockState(), stickState, false)
                .stickState().properties().get(STAIRS));
    }

    @Test
    void sneakingSelectsThePreviousProperty()
    {
        List<Property<?>> properties = List.copyOf(Blocks.OAK_STAIRS.getStateDefinition().getProperties());
        DebugStickState first = DebugStickState.EMPTY.withProperty(STAIRS, properties.getFirst());

        assertSame(properties.getLast(), CreatorDebugStick.select(Blocks.OAK_STAIRS.defaultBlockState(), first, true)
                .stickState().properties().get(STAIRS));
    }

    @Test
    void cyclingChangesOnlyTheSelectedPropertyInEitherDirection()
    {
        BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        DebugStickState facing = DebugStickState.EMPTY.withProperty(STAIRS, StairBlock.FACING);
        List<Direction> directions = List.copyOf(StairBlock.FACING.getPossibleValues());
        int index = directions.indexOf(Direction.NORTH);

        CreatorDebugStick.Action forward = CreatorDebugStick.cycle(north, facing, false);
        CreatorDebugStick.Action backward = CreatorDebugStick.cycle(north, facing, true);

        assertEquals(directions.get((index + 1) % directions.size()), forward.state().getValue(StairBlock.FACING));
        assertEquals(directions.get((index + directions.size() - 1) % directions.size()), backward.state().getValue(StairBlock.FACING));
        assertEquals(north.getValue(StairBlock.HALF), forward.state().getValue(StairBlock.HALF));
        assertNull(forward.stickState());
        assertEquals("item.minecraft.debug_stick.update", key(forward.message()));
    }

    @Test
    void cyclingWithoutASelectionUsesTheFirstProperty()
    {
        Property<?> first = Blocks.OAK_STAIRS.getStateDefinition().getProperties().iterator().next();
        BlockState original = Blocks.OAK_STAIRS.defaultBlockState();

        BlockState cycled = CreatorDebugStick.cycle(original, DebugStickState.EMPTY, false).state();

        assertEquals(first, changedProperty(original, cycled));
    }

    @Test
    void blocksWithoutPropertiesAreLeftAlone()
    {
        CreatorDebugStick.Action select = CreatorDebugStick.select(Blocks.STONE.defaultBlockState(), DebugStickState.EMPTY, false);
        CreatorDebugStick.Action cycle = CreatorDebugStick.cycle(Blocks.STONE.defaultBlockState(), DebugStickState.EMPTY, false);

        assertNull(select.state());
        assertNull(select.stickState());
        assertNull(cycle.state());
        assertEquals("item.minecraft.debug_stick.empty", key(cycle.message()));
    }

    @Test
    void eitherVirtualHandCanHoldTheStick()
    {
        ItemStack stick = new ItemStack(Items.DEBUG_STICK);
        ItemStack block = new ItemStack(Items.STONE);

        assertEquals(InteractionHand.MAIN_HAND, CreatorDebugStick.activeHand(stick, block));
        assertEquals(InteractionHand.OFF_HAND, CreatorDebugStick.activeHand(block, stick));
        assertEquals(InteractionHand.MAIN_HAND, CreatorDebugStick.activeHand(stick, stick));
        assertNull(CreatorDebugStick.activeHand(block, ItemStack.EMPTY));
    }

    private static String key(Component message)
    {
        return ((TranslatableContents) message.getContents()).getKey();
    }

    private static Property<?> changedProperty(BlockState before, BlockState after)
    {
        return before.getProperties().stream()
                .filter(property -> !before.getValue(property).equals(after.getValue(property)))
                .findFirst()
                .orElseThrow();
    }
}

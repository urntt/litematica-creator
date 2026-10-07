package io.github.urntt.litematicacreator.creator;

import java.util.Collection;
import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Vanilla debug stick semantics for projection blocks. {@code DebugStickItem} keeps this logic in private methods that
 * act on the server level, so Creator mirrors it for the virtual stick: attacking selects the block's next property
 * and using cycles the selected property's value, both reversed while sneaking.
 */
final class CreatorDebugStick
{
    private CreatorDebugStick()
    {
    }

    /**
     * @param state the block state after a cycle, or {@code null} when the block was left unchanged
     * @param stickState the stick state after a selection, or {@code null} when the stick was left unchanged
     * @param message the vanilla action bar message for the action
     */
    record Action(@Nullable BlockState state, @Nullable DebugStickState stickState, Component message)
    {
    }

    // Like vanilla right clicks, the main hand is tried first; either hand can hold the stick.
    @Nullable
    static InteractionHand activeHand(ItemStack mainHand, ItemStack offhand)
    {
        if (mainHand.is(Items.DEBUG_STICK))
        {
            return InteractionHand.MAIN_HAND;
        }

        return offhand.is(Items.DEBUG_STICK) ? InteractionHand.OFF_HAND : null;
    }

    static Action select(BlockState state, DebugStickState stickState, boolean backward)
    {
        Holder<Block> block = state.typeHolder();
        Collection<Property<?>> properties = block.value().getStateDefinition().getProperties();

        if (properties.isEmpty())
        {
            return noProperties(block);
        }

        Property<?> property = relative(properties, stickState.properties().get(block), backward);
        return new Action(
                null,
                stickState.withProperty(block, property),
                message("select", property.getName(), valueName(state, property))
        );
    }

    static Action cycle(BlockState state, DebugStickState stickState, boolean backward)
    {
        Holder<Block> block = state.typeHolder();
        Collection<Property<?>> properties = block.value().getStateDefinition().getProperties();

        if (properties.isEmpty())
        {
            return noProperties(block);
        }

        Property<?> property = stickState.properties().get(block);

        if (property == null)
        {
            property = properties.iterator().next();
        }

        BlockState cycled = cycleValue(state, property, backward);
        return new Action(cycled, null, message("update", property.getName(), valueName(cycled, property)));
    }

    private static Action noProperties(Holder<Block> block)
    {
        return new Action(null, null, message("empty", block.getRegisteredName()));
    }

    private static <T extends Comparable<T>> BlockState cycleValue(BlockState state, Property<T> property, boolean backward)
    {
        return state.setValue(property, relative(property.getPossibleValues(), state.getValue(property), backward));
    }

    private static <T> T relative(Iterable<T> values, @Nullable T current, boolean backward)
    {
        return backward ? Util.findPreviousInIterable(values, current) : Util.findNextInIterable(values, current);
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property)
    {
        return property.getName(state.getValue(property));
    }

    private static Component message(String key, Object... arguments)
    {
        return Component.translatable(Items.DEBUG_STICK.getDescriptionId() + "." + key, arguments);
    }
}

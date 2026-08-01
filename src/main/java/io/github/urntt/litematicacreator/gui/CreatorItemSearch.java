package io.github.urntt.litematicacreator.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

final class CreatorItemSearch
{
    private final List<ItemStack> items;
    private final Map<ItemStack, List<String>> textByStack;

    CreatorItemSearch(Collection<ItemStack> creativeItems,
                      Collection<ItemStack> registeredItems,
                      Function<ItemStack, List<String>> textExtractor)
    {
        Set<ItemStack> unique = ItemStackLinkedSet.createTypeAndComponentsSet();
        unique.addAll(creativeItems);
        unique.addAll(registeredItems);
        this.items = List.copyOf(unique);
        this.textByStack = new IdentityHashMap<>();

        for (ItemStack stack : this.items)
        {
            List<String> values = new ArrayList<>();

            for (String value : textExtractor.apply(stack))
            {
                values.add(value.toLowerCase(Locale.ROOT));
            }

            this.textByStack.put(stack, List.copyOf(values));
        }
    }

    List<ItemStack> allItems()
    {
        return this.items;
    }

    List<ItemStack> search(String rawQuery)
    {
        String query = rawQuery.trim().toLowerCase(Locale.ROOT);

        if (query.isEmpty())
        {
            return this.items;
        }

        if (query.startsWith("#"))
        {
            return this.searchTags(query.substring(1));
        }

        List<ItemStack> matches = new ArrayList<>();

        for (ItemStack stack : this.items)
        {
            if (this.textByStack.getOrDefault(stack, List.of()).stream().anyMatch(value -> value.contains(query)))
            {
                matches.add(stack);
            }
        }

        return matches;
    }

    private List<ItemStack> searchTags(String query)
    {
        List<ItemStack> matches = new ArrayList<>();

        for (ItemStack stack : this.items)
        {
            if (stack.typeHolder().tags().map(tag -> tag.location()).anyMatch(id -> matchesIdentifier(id, query)))
            {
                matches.add(stack);
            }
        }

        return matches;
    }

    static boolean matchesIdentifier(Identifier id, String query)
    {
        int separator = query.indexOf(':');

        if (separator < 0)
        {
            return id.getPath().contains(query) || id.getNamespace().contains(query);
        }

        String namespace = query.substring(0, separator).trim();
        String path = query.substring(separator + 1).trim();
        return id.getNamespace().contains(namespace) && id.getPath().contains(path);
    }
}

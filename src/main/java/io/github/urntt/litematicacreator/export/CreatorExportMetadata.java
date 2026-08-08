package io.github.urntt.litematicacreator.export;

import java.util.Objects;

import fi.dy.masa.litematica.schematic.SchematicMetadata;

public record CreatorExportMetadata(String name, String author, String description)
{
    public CreatorExportMetadata
    {
        name = Objects.requireNonNullElse(name, "");
        author = Objects.requireNonNullElse(author, "");
        description = Objects.requireNonNullElse(description, "");
    }

    public static CreatorExportMetadata from(SchematicMetadata metadata)
    {
        return new CreatorExportMetadata(
                metadata.getName(),
                metadata.getAuthor(),
                metadata.getDescription()
        );
    }
}

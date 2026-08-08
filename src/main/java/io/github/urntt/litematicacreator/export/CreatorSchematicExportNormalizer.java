package io.github.urntt.litematicacreator.export;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.dy.masa.malilib.util.FileNameUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import net.minecraft.nbt.CompoundTag;

public final class CreatorSchematicExportNormalizer
{
    private static final Pattern DRAFT_NAME_PATTERN = Pattern.compile("creator-draft-(\\d{8})-(\\d{6})");
    private static final DateTimeFormatter DRAFT_TIME_FORMAT = DateTimeFormatter
            .ofPattern("uuuuMMdd-HHmmss")
            .withResolverStyle(ResolverStyle.STRICT);

    private CreatorSchematicExportNormalizer()
    {
    }

    public static CompoundTag normalizeForExport(CompoundTag original, String finalFileName)
    {
        try
        {
            return normalizeForExport(original, finalFileName, System.currentTimeMillis(), ZoneId.systemDefault());
        }
        catch (Exception exception)
        {
            LitematicaCreator.LOGGER.warn("Failed to normalize Creator schematic export metadata; writing the original data", exception);
            return original;
        }
    }

    static CompoundTag normalizeForExport(CompoundTag original, String finalFileName, long exportTime, ZoneId zoneId)
    {
        CompoundTag metadata = original.getCompoundOrEmpty("Metadata");
        String originalName = metadata.getStringOr("Name", "");
        Matcher matcher = DRAFT_NAME_PATTERN.matcher(originalName);

        if (matcher.matches() == false)
        {
            return original;
        }

        String exportedName = FileNameUtils.getFileNameWithoutExtension(finalFileName);

        if (exportedName.isEmpty())
        {
            return original;
        }

        CompoundTag normalized = original.copy();
        CompoundTag normalizedMetadata = normalized.getCompoundOrEmpty("Metadata");
        normalizedMetadata.putString("Name", exportedName);

        if (normalizedMetadata.getLongOr("TimeCreated", -1L) <= 0L)
        {
            normalizedMetadata.putLong("TimeCreated", recoverCreationTime(matcher, exportTime, zoneId));
        }

        normalized.put("Metadata", normalizedMetadata);
        return normalized;
    }

    private static long recoverCreationTime(Matcher matcher, long exportTime, ZoneId zoneId)
    {
        try
        {
            LocalDateTime draftTime = LocalDateTime.parse(
                    matcher.group(1) + "-" + matcher.group(2),
                    DRAFT_TIME_FORMAT
            );
            return draftTime.atZone(zoneId).toInstant().toEpochMilli();
        }
        catch (DateTimeException exception)
        {
            return exportTime;
        }
    }
}

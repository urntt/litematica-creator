package io.github.urntt.litematicacreator.export;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventHandler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.FileType;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditGuard;
import io.github.urntt.litematicacreator.mixin.LitematicaSchematicAccessor;
import io.github.urntt.litematicacreator.mixin.SchematicPlacementAccessor;
import io.github.urntt.litematicacreator.mixin.SchematicPlacementManagerAccessor;
import io.github.urntt.litematicacreator.mixin.SubRegionPlacementAccessor;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public final class CreatorSchematicBindingService
{
    private static final CreatorSchematicBindingService INSTANCE = new CreatorSchematicBindingService();
    private final CreatorBindingTargetRegistry reservations = new CreatorBindingTargetRegistry();

    private CreatorSchematicBindingService()
    {
    }

    public static CreatorSchematicBindingService getInstance()
    {
        return INSTANCE;
    }

    @Nullable
    public String validateBindingTarget(LitematicaSchematic schematic, Path target)
    {
        return this.validateWriteTarget(schematic, target, CreatorExportOperation.SAVE_AS_AND_BIND);
    }

    @Nullable
    public String validateWriteTarget(
            LitematicaSchematic schematic,
            Path target,
            CreatorExportOperation operation)
    {
        Path normalized = CreatorSchematicExportService.ensureExtension(target.toAbsolutePath().normalize());

        for (LitematicaSchematic loaded : SchematicHolder.getInstance().getAllSchematics())
        {
            if (samePath(loaded.getFile(), normalized) &&
                (loaded != schematic || !operation.bindsFile()))
            {
                return "Target file is already bound to loaded schematic '" + loaded.getMetadata().getName() + "'";
            }
        }

        if (this.reservations.isReserved(normalized))
        {
            return "Another Creator save is already using the target file";
        }

        return null;
    }

    public CreatorBindingReservation reserveBindingTarget(LitematicaSchematic schematic, Path target)
    {
        return this.reserveWriteTarget(schematic, target, CreatorExportOperation.SAVE_AS_AND_BIND);
    }

    public CreatorBindingReservation reserveWriteTarget(
            LitematicaSchematic schematic,
            Path target,
            CreatorExportOperation operation)
    {
        Objects.requireNonNull(schematic, "schematic");
        Objects.requireNonNull(operation, "operation");
        Path normalized = CreatorSchematicExportService.ensureExtension(target.toAbsolutePath().normalize());
        String conflict = this.validateLoadedTarget(schematic, normalized, operation);

        if (conflict != null)
        {
            throw new IllegalStateException(conflict);
        }

        CreatorBindingReservation reservation = new CreatorBindingReservation(this, schematic, normalized);

        if (!this.reservations.reserve(normalized, reservation))
        {
            throw new IllegalStateException("Another Creator save is already using the target file");
        }

        return reservation;
    }

    public CreatorBindingResult commit(
            CreatorPreparedExport prepared,
            CreatorExportWriteResult writeResult,
            @Nullable CreatorBindingReservation reservation)
    {
        try
        {
            if (!writeResult.success())
            {
                String message = writeResult.error() != null ? writeResult.error().getMessage() : "Unknown write failure";
                return CreatorBindingResult.failed(message);
            }

            if (!this.owns(reservation, prepared.source(), prepared.target()))
            {
                return CreatorBindingResult.failed("The write target reservation is no longer valid");
            }

            if (!prepared.operation().bindsFile())
            {
                return CreatorBindingResult.exported();
            }

            if (!SchematicHolder.getInstance().getAllSchematics().contains(prepared.source()))
            {
                return CreatorBindingResult.failed("The source schematic was unloaded while it was being saved");
            }

            LitematicaSchematic schematic = prepared.source();
            boolean unchanged = CreatorSchematicMetadataCopies.writeSchematicToNbt(schematic).equals(prepared.sourceNbt());
            CompoundTag sourceMetadata = prepared.sourceNbt().getCompoundOrEmpty("Metadata");
            CompoundTag outputMetadata = prepared.outputNbt().getCompoundOrEmpty("Metadata");

            try (CreatorSchematicEditGuard.EditTransaction ignored = CreatorSchematicEditGuard.beginEdit())
            {
                LitematicaSchematicAccessor schematicAccessor = (LitematicaSchematicAccessor) schematic;
                schematicAccessor.litematicacreator$setSchematicFile(prepared.target());
                schematicAccessor.litematicacreator$setSchematicType(FileType.LITEMATICA_SCHEMATIC);
                applyIdentityMetadata(schematic.getMetadata(), sourceMetadata, outputMetadata);
                schematic.getMetadata().setFileType(FileType.LITEMATICA_SCHEMATIC);

                List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic);

                for (SchematicPlacement placement : placements)
                {
                    ((SchematicPlacementAccessor) placement).litematicacreator$setSchematicFile(prepared.target());
                    SchematicPlacementEventHandler.getInstance().onPlacementUpdated(placement);
                }

                if (unchanged)
                {
                    schematic.getMetadata().clearModifiedSinceSaved();
                }
                else
                {
                    schematic.getMetadata().setModifiedSinceSaved();
                }
            }

            CreatorRecoveryManager.getInstance().onSchematicSaved(schematic, unchanged);
            return CreatorBindingResult.bound(unchanged);
        }
        finally
        {
            if (reservation != null)
            {
                reservation.close();
            }
        }
    }

    void release(CreatorBindingReservation reservation)
    {
        if (reservation.isActive() && this.reservations.release(reservation.target(), reservation))
        {
            reservation.markReleased();
        }
    }

    private boolean owns(
            @Nullable CreatorBindingReservation reservation,
            LitematicaSchematic schematic,
            Path target)
    {
        if (reservation == null || !reservation.isActive() || reservation.schematic() != schematic)
        {
            return false;
        }

        Path normalized = CreatorSchematicExportService.ensureExtension(target.toAbsolutePath().normalize());

        return reservation.target().equals(normalized) && this.reservations.owns(normalized, reservation);
    }

    @Nullable
    private String validateLoadedTarget(
            LitematicaSchematic schematic,
            Path normalized,
            CreatorExportOperation operation)
    {
        for (LitematicaSchematic loaded : SchematicHolder.getInstance().getAllSchematics())
        {
            if (samePath(loaded.getFile(), normalized) &&
                (loaded != schematic || !operation.bindsFile()))
            {
                return "Target file is already bound to loaded schematic '" + loaded.getMetadata().getName() + "'";
            }
        }

        return null;
    }

    public boolean reloadBoundSchematic(LitematicaSchematic schematic)
    {
        if (schematic.getFile() == null)
        {
            return false;
        }

        Path file = schematic.getFile().toAbsolutePath().normalize();
        Path directory = file.getParent();

        if (directory == null)
        {
            return false;
        }

        LitematicaSchematic loaded = LitematicaSchematic.createFromFile(
                directory,
                file.getFileName().toString(),
                FileType.LITEMATICA_SCHEMATIC
        );

        if (loaded == null)
        {
            return false;
        }

        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        SchematicPlacementManagerAccessor managerAccessor = (SchematicPlacementManagerAccessor) manager;
        List<SchematicPlacement> placements = new ArrayList<>(manager.getAllPlacementsOfSchematic(schematic));
        Map<String, BlockPos> previousAreaPositions = new HashMap<>(schematic.getAreaPositions());
        Map<SchematicPlacement, Set<ChunkPos>> previousChunks = new IdentityHashMap<>();

        try (CreatorSchematicEditGuard.EditTransaction ignored = CreatorSchematicEditGuard.beginEdit())
        {
            for (SchematicPlacement placement : placements)
            {
                managerAccessor.litematicacreator$onPrePlacementChange(placement);
                previousChunks.put(placement, new HashSet<>(managerAccessor.litematicacreator$getChunksPreChange()));
            }

            replaceSchematicData(schematic, loaded, file);

            Map<String, BlockPos> areaPositions = schematic.getAreaPositions();

            for (SchematicPlacement placement : placements)
            {
                SchematicPlacementAccessor accessor = (SchematicPlacementAccessor) placement;
                Map<String, SubRegionPlacement> subRegions = accessor.litematicacreator$getRelativeSubRegionPlacements();
                subRegions.keySet().removeIf(name -> !areaPositions.containsKey(name));

                areaPositions.forEach((name, position) -> {
                    SubRegionPlacement subRegion = subRegions.get(name);

                    if (subRegion == null)
                    {
                        subRegions.put(name, new SubRegionPlacement(position, name));
                        return;
                    }

                    BlockPos previousPosition = previousAreaPositions.get(name);
                    boolean followsSchematic = previousPosition != null && !subRegion.isRegionPlacementModified(previousPosition);
                    SubRegionPlacementAccessor subRegionAccessor = (SubRegionPlacementAccessor) subRegion;
                    subRegionAccessor.litematicacreator$setDefaultPos(position);

                    if (followsSchematic)
                    {
                        subRegionAccessor.litematicacreator$setPos(position);
                    }
                });

                if (placement.getSelectedSubRegionName() != null && !areaPositions.containsKey(placement.getSelectedSubRegionName()))
                {
                    placement.setSelectedSubRegionName(null);
                }

                accessor.litematicacreator$setSubRegionCount(areaPositions.size());
                accessor.litematicacreator$checkAreSubRegionsModified();
                accessor.litematicacreator$updateEnclosingBox();
                managerAccessor.litematicacreator$getChunksPreChange().clear();
                managerAccessor.litematicacreator$getChunksPreChange().addAll(previousChunks.getOrDefault(placement, Set.of()));
                managerAccessor.litematicacreator$onPostPlacementChange(placement);
            }

            schematic.getMetadata().clearModifiedSinceSaved();
        }

        CreatorRecoveryManager.getInstance().onSchematicSaved(schematic, true);
        return true;
    }

    private static void replaceSchematicData(
            LitematicaSchematic target,
            LitematicaSchematic loaded,
            Path file)
    {
        LitematicaSchematicAccessor targetAccessor = (LitematicaSchematicAccessor) target;
        LitematicaSchematicAccessor loadedAccessor = (LitematicaSchematicAccessor) loaded;
        targetAccessor.litematicacreator$getBlockContainers().clear();
        targetAccessor.litematicacreator$getBlockContainers().putAll(loadedAccessor.litematicacreator$getBlockContainers());
        targetAccessor.litematicacreator$getTileEntities().clear();
        targetAccessor.litematicacreator$getTileEntities().putAll(loadedAccessor.litematicacreator$getTileEntities());
        targetAccessor.litematicacreator$getEntities().clear();
        targetAccessor.litematicacreator$getEntities().putAll(loadedAccessor.litematicacreator$getEntities());
        targetAccessor.litematicacreator$getPendingBlockTicks().clear();
        targetAccessor.litematicacreator$getPendingBlockTicks().putAll(loadedAccessor.litematicacreator$getPendingBlockTicks());
        targetAccessor.litematicacreator$getPendingFluidTicks().clear();
        targetAccessor.litematicacreator$getPendingFluidTicks().putAll(loadedAccessor.litematicacreator$getPendingFluidTicks());
        targetAccessor.litematicacreator$getSubRegionPositions().clear();
        targetAccessor.litematicacreator$getSubRegionPositions().putAll(loadedAccessor.litematicacreator$getSubRegionPositions());
        targetAccessor.litematicacreator$getSubRegionSizes().clear();
        targetAccessor.litematicacreator$getSubRegionSizes().putAll(loadedAccessor.litematicacreator$getSubRegionSizes());
        CreatorSchematicMetadataCopies.copy(target.getMetadata(), loaded.getMetadata());
        targetAccessor.litematicacreator$setSchematicFile(file);
        targetAccessor.litematicacreator$setSchematicType(FileType.LITEMATICA_SCHEMATIC);
    }

    static void applyIdentityMetadata(
            SchematicMetadata metadata,
            CompoundTag source,
            CompoundTag output)
    {
        if (metadata.getName().equals(source.getStringOr("Name", "")))
        {
            metadata.setName(output.getStringOr("Name", metadata.getName()));
        }

        if (metadata.getAuthor().equals(source.getStringOr("Author", "")))
        {
            metadata.setAuthor(output.getStringOr("Author", metadata.getAuthor()));
        }

        if (metadata.getDescription().equals(source.getStringOr("Description", "")))
        {
            metadata.setDescription(output.getStringOr("Description", metadata.getDescription()));
        }

        if (metadata.getTimeCreated() == source.getLongOr("TimeCreated", -1L))
        {
            metadata.setTimeCreated(output.getLongOr("TimeCreated", metadata.getTimeCreated()));
        }

        if (metadata.getTimeModified() == source.getLongOr("TimeModified", -1L))
        {
            metadata.setTimeModified(output.getLongOr("TimeModified", metadata.getTimeModified()));
        }

        int[] currentPreview = CreatorSchematicMetadataCopies.snapshotPreview(metadata);
        int[] sourcePreview = source.getIntArray("PreviewImageData").orElse(null);

        if (Arrays.equals(currentPreview, sourcePreview))
        {
            metadata.setPreviewImagePixelData(output.getIntArray("PreviewImageData").orElse(null));
        }
    }

    static boolean samePath(@Nullable Path first, Path second)
    {
        return first != null && first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }
}

package io.github.urntt.litematicacreator.export;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import io.github.urntt.litematicacreator.config.CreatorExportRegionMode;
import io.github.urntt.litematicacreator.creator.CreatorSchematicEditGuard;

public final class CreatorSchematicExportService
{
    private static final CreatorSchematicExportService INSTANCE = new CreatorSchematicExportService();
    private static final long WORLD_SAMPLING_BUDGET_NANOS = 2_000_000L;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("Litematica Creator Export Worker").daemon(true).factory()
    );
    private final ConcurrentLinkedQueue<CreatorSchematicSnapshot.WorldSamplingPlan> samplingQueue = new ConcurrentLinkedQueue<>();
    @Nullable private CreatorSchematicSnapshot.WorldSamplingPlan activeSampling;

    private CreatorSchematicExportService()
    {
    }

    public static CreatorSchematicExportService getInstance()
    {
        return INSTANCE;
    }

    public CreatorPreparedExport prepare(
            LitematicaSchematic schematic,
            Path target,
            CreatorExportOperation operation,
            CreatorExportRegionMode mode,
            @Nullable SchematicPlacement samplingPlacement)
    {
        return this.prepare(
                schematic,
                target,
                operation,
                mode,
                samplingPlacement,
                CreatorExportMetadata.from(schematic.getMetadata())
        );
    }

    public CreatorPreparedExport prepare(
            LitematicaSchematic schematic,
            Path target,
            CreatorExportOperation operation,
            CreatorExportRegionMode mode,
            @Nullable SchematicPlacement samplingPlacement,
            CreatorExportMetadata exportMetadata)
    {
        Objects.requireNonNull(schematic, "schematic");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(exportMetadata, "exportMetadata");
        Path normalizedTarget = ensureExtension(target.toAbsolutePath().normalize());
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel world = mode == CreatorExportRegionMode.ENCLOSING_WITH_WORLD ? minecraft.level : null;

        if (samplingPlacement != null && samplingPlacement.getSchematic() != schematic)
        {
            throw new IllegalArgumentException("Sampling placement belongs to a different schematic");
        }

        return CreatorSchematicEditGuard.readSnapshot(() -> {
            CompoundTag sourceNbt = CreatorSchematicMetadataCopies.writeSchematicToNbt(schematic);
            CreatorSchematicSnapshot source = CreatorSchematicSnapshot.capture(schematic);
            CreatorSchematicSnapshot output = source.normalize(mode, world, samplingPlacement);
            String fileName = normalizedTarget.getFileName().toString();
            CompoundTag outputNbt = output.withExportMetadata(exportMetadata).toNbt(fileName);
            return new CreatorPreparedExport(
                    schematic,
                    normalizedTarget,
                    operation,
                    mode,
                    sourceNbt,
                    outputNbt,
                    output.preview()
            );
        });
    }

    public CompletableFuture<CreatorPreparedExport> prepareAsync(
            LitematicaSchematic schematic,
            Path target,
            CreatorExportOperation operation,
            CreatorExportRegionMode mode,
            @Nullable SchematicPlacement samplingPlacement)
    {
        return this.prepareAsync(
                schematic,
                target,
                operation,
                mode,
                samplingPlacement,
                CreatorExportMetadata.from(schematic.getMetadata())
        );
    }

    public CompletableFuture<CreatorPreparedExport> prepareAsync(
            LitematicaSchematic schematic,
            Path target,
            CreatorExportOperation operation,
            CreatorExportRegionMode mode,
            @Nullable SchematicPlacement samplingPlacement,
            CreatorExportMetadata exportMetadata)
    {
        Objects.requireNonNull(schematic, "schematic");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(exportMetadata, "exportMetadata");
        Path normalizedTarget = ensureExtension(target.toAbsolutePath().normalize());
        CapturedSnapshot captured = this.capture(schematic, mode, samplingPlacement);

        return this.normalizeAsync(captured.snapshot(), mode, captured.world(), captured.transform())
                .thenApplyAsync(output -> new CreatorPreparedExport(
                        schematic,
                        normalizedTarget,
                        operation,
                        mode,
                        captured.sourceNbt(),
                        output.withExportMetadata(exportMetadata).toNbt(normalizedTarget.getFileName().toString()),
                        output.preview()
                ), this.worker);
    }

    public CreatorExportPreview preview(
            LitematicaSchematic schematic,
            CreatorExportRegionMode mode,
            @Nullable SchematicPlacement samplingPlacement)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel world = mode == CreatorExportRegionMode.ENCLOSING_WITH_WORLD ? minecraft.level : null;
        return CreatorSchematicEditGuard.readSnapshot(() -> CreatorSchematicSnapshot.capture(schematic)
                .normalize(mode, world, samplingPlacement)
                .preview());
    }

    public CompletableFuture<CreatorExportPreview> previewAsync(
            LitematicaSchematic schematic,
            CreatorExportRegionMode mode,
            @Nullable SchematicPlacement samplingPlacement)
    {
        Objects.requireNonNull(schematic, "schematic");
        Objects.requireNonNull(mode, "mode");
        CapturedSnapshot captured = this.capture(schematic, mode, samplingPlacement);
        return this.normalizeAsync(captured.snapshot(), mode, captured.world(), captured.transform())
                .thenApplyAsync(CreatorSchematicSnapshot::preview, this.worker);
    }

    public void onClientTick(Minecraft minecraft)
    {
        long deadline = System.nanoTime() + WORLD_SAMPLING_BUDGET_NANOS;

        while (System.nanoTime() < deadline)
        {
            if (this.activeSampling == null)
            {
                this.activeSampling = this.samplingQueue.poll();
            }

            if (this.activeSampling == null)
            {
                return;
            }

            this.activeSampling.runFor(minecraft, deadline);

            if (this.activeSampling.isDone())
            {
                this.activeSampling = null;
            }
            else
            {
                return;
            }
        }
    }

    public CompletableFuture<CreatorExportWriteResult> write(CreatorPreparedExport prepared)
    {
        return CompletableFuture.supplyAsync(
                () -> writeAtomically(prepared.target(), prepared.outputNbt()),
                this.worker
        );
    }

    private CapturedSnapshot capture(
            LitematicaSchematic schematic,
            CreatorExportRegionMode mode,
            @Nullable SchematicPlacement samplingPlacement)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel world = mode == CreatorExportRegionMode.ENCLOSING_WITH_WORLD ? minecraft.level : null;

        if (samplingPlacement != null && samplingPlacement.getSchematic() != schematic)
        {
            throw new IllegalArgumentException("Sampling placement belongs to a different schematic");
        }

        if (mode == CreatorExportRegionMode.ENCLOSING_WITH_WORLD && (world == null || samplingPlacement == null))
        {
            throw new IllegalArgumentException("World-backed export requires a sampling placement");
        }

        CreatorWorldSamplingTransform transform = samplingPlacement != null ?
                CreatorWorldSamplingTransform.capture(samplingPlacement) : null;
        return CreatorSchematicEditGuard.readSnapshot(() -> new CapturedSnapshot(
                CreatorSchematicMetadataCopies.writeSchematicToNbt(schematic),
                CreatorSchematicSnapshot.capture(schematic),
                world,
                transform
        ));
    }

    private CompletableFuture<CreatorSchematicSnapshot> normalizeAsync(
            CreatorSchematicSnapshot snapshot,
            CreatorExportRegionMode mode,
            @Nullable ClientLevel world,
            @Nullable CreatorWorldSamplingTransform transform)
    {
        if (mode != CreatorExportRegionMode.ENCLOSING_WITH_WORLD)
        {
            return CompletableFuture.supplyAsync(() -> snapshot.normalize(mode, null, null), this.worker);
        }

        return CompletableFuture.supplyAsync(() -> snapshot.createWorldSamplingPlan(
                Objects.requireNonNull(world),
                Objects.requireNonNull(transform)
        ), this.worker).thenCompose(plan -> {
            if (!plan.isDone())
            {
                this.samplingQueue.add(plan);
            }

            return plan.future();
        });
    }

    static CreatorExportWriteResult writeAtomically(Path target, CompoundTag outputNbt)
    {
        Path parent = target.getParent();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");

        try
        {
            if (parent != null)
            {
                Files.createDirectories(parent);
            }

            Files.deleteIfExists(temporary);

            if (!NbtUtils.writeCompoundTagToCompressedFile(outputNbt, temporary))
            {
                throw new IOException("NBT writer rejected " + temporary);
            }

            try
            {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException ignored)
            {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return CreatorExportWriteResult.success(target);
        }
        catch (Throwable error)
        {
            try
            {
                Files.deleteIfExists(temporary);
            }
            catch (IOException ignored)
            {
            }

            return CreatorExportWriteResult.failure(target, error);
        }
    }

    public static Path ensureExtension(Path target)
    {
        String name = target.getFileName().toString();

        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".litematic"))
        {
            return target.resolveSibling(name + ".litematic");
        }

        return target;
    }

    private record CapturedSnapshot(
            CompoundTag sourceNbt,
            CreatorSchematicSnapshot snapshot,
            @Nullable ClientLevel world,
            @Nullable CreatorWorldSamplingTransform transform)
    {
    }
}

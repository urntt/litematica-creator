package io.github.urntt.litematicacreator.recovery;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.interfaces.ISchematicPlacementEventListener;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventFlag;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventHandler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.creator.CreatorFocus;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.creator.CreatorPlacementIndex;
import io.github.urntt.litematicacreator.event.CreatorClientTickHandler;
import io.github.urntt.litematicacreator.export.CreatorSchematicMetadataCopies;
import io.github.urntt.litematicacreator.mixin.LitematicaSchematicAccessor;
import io.github.urntt.litematicacreator.mixin.SchematicPlacementAccessor;

public final class CreatorRecoveryManager implements ISchematicPlacementEventListener
{
    private static final CreatorRecoveryManager INSTANCE = new CreatorRecoveryManager();
    private static final long METADATA_SCAN_INTERVAL_TICKS = 20L;

    private final RecoveryStorage storage = new RecoveryStorage(
            FileUtils.getConfigDirectory().resolve("litematica-creator").resolve("recovery")
    );
    private final ExecutorService writer = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("Litematica Creator Recovery Writer").daemon(true).factory()
    );
    private final Map<LitematicaSchematic, TrackedEntry> tracked = new IdentityHashMap<>();
    private final RecoverySuppressionSet<LitematicaSchematic> suppressed = new RecoverySuppressionSet<>();
    private final Map<UUID, Long> entryEpochs = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<IoFailure> ioFailures = new ConcurrentLinkedQueue<>();

    @Nullable
    private RecoveryWorldKey currentWorldKey;
    @Nullable
    private RecoveryWorldKey pendingRestoreKey;
    private long restoreTick = -1L;
    private boolean registered;
    private boolean internalMutation;
    private boolean lifecycleTransition;
    private boolean warnedThisSession;

    private CreatorRecoveryManager()
    {
    }

    public static CreatorRecoveryManager getInstance()
    {
        return INSTANCE;
    }

    public void register()
    {
        if (this.registered)
        {
            return;
        }

        this.registered = true;
        SchematicPlacementEventHandler.getInstance().registerSchematicPlacementEventListener(
                this,
                List.of(SchematicPlacementEventFlag.ALL_EVENTS)
        );
    }

    public void onClientTick(Minecraft mc, long tick)
    {
        this.drainIoFailures(tick);

        if (this.pendingRestoreKey != null && tick >= this.restoreTick && mc.level != null)
        {
            RecoveryWorldKey key = this.pendingRestoreKey;
            this.pendingRestoreKey = null;
            this.restoreTick = -1L;
            this.restoreWorld(key);
        }

        if (this.currentWorldKey == null || this.lifecycleTransition || tick % METADATA_SCAN_INTERVAL_TICKS != 0L)
        {
            return;
        }

        this.scanLoadedSchematics(tick);

        for (Map.Entry<LitematicaSchematic, TrackedEntry> entry : List.copyOf(this.tracked.entrySet()))
        {
            if (entry.getValue().scheduler.shouldWrite(tick))
            {
                this.snapshotAndSubmit(entry.getKey(), entry.getValue());
            }
        }
    }

    public void beforeWorldChange(ClientLevel worldBefore)
    {
        if (this.currentWorldKey == null)
        {
            this.currentWorldKey = worldKey(worldBefore);
        }

        this.flushCurrentWorld();
        this.lifecycleTransition = true;
    }

    public void afterWorldJoin(ClientLevel worldAfter)
    {
        this.clearSessionTracking();
        this.currentWorldKey = worldKey(worldAfter);
        this.pendingRestoreKey = this.currentWorldKey;
        this.restoreTick = CreatorClientTickHandler.getClientTicks() + 1L;
        this.lifecycleTransition = false;
        this.warnedThisSession = false;
    }

    public void afterWorldLeave()
    {
        this.awaitQueuedWrites();
        this.clearSessionTracking();
        this.currentWorldKey = null;
        this.pendingRestoreKey = null;
        this.restoreTick = -1L;
        this.lifecycleTransition = false;
    }

    public void onClientShutdown()
    {
        if (this.currentWorldKey != null && !this.lifecycleTransition)
        {
            this.flushCurrentWorld();
        }
        else
        {
            this.awaitQueuedWrites();
        }
    }

    public void onSchematicChanged(LitematicaSchematic schematic)
    {
        this.markChanged(schematic, CreatorClientTickHandler.getClientTicks());
    }

    public void onFocusChanged(@Nullable CreatorFocus previous, @Nullable CreatorFocus current)
    {
        if (this.internalMutation || this.lifecycleTransition)
        {
            return;
        }

        if (previous != null)
        {
            this.markChanged(previous.schematic(), CreatorClientTickHandler.getClientTicks());
        }

        if (current != null && (previous == null || previous.schematic() != current.schematic()))
        {
            this.markChanged(current.schematic(), CreatorClientTickHandler.getClientTicks());
        }
    }

    public void discardSchematic(LitematicaSchematic schematic)
    {
        this.suppressed.suppress(schematic);
        this.deleteTrackedEntrySynchronously(schematic);
    }

    public void onSchematicSaved(LitematicaSchematic schematic, boolean clean)
    {
        this.suppressed.release(schematic);
        this.deleteTrackedEntrySynchronously(schematic);

        if (!clean)
        {
            this.markChanged(schematic, CreatorClientTickHandler.getClientTicks());
        }
    }

    public boolean hasRecoveryEntry(LitematicaSchematic schematic)
    {
        return this.tracked.containsKey(schematic);
    }

    @Override
    public void onPlacementAdded(SchematicPlacement placement)
    {
        if (this.internalMutation || this.lifecycleTransition)
        {
            return;
        }

        LitematicaSchematic schematic = placement.getSchematic();
        this.suppressed.release(schematic);
        this.markChanged(schematic, CreatorClientTickHandler.getClientTicks());
    }

    @Override
    public void onPlacementRemoved(SchematicPlacement placement)
    {
        if (this.internalMutation || this.lifecycleTransition)
        {
            return;
        }

        LitematicaSchematic schematic = placement.getSchematic();
        boolean schematicStillLoaded = SchematicHolder.getInstance().getAllSchematics().contains(schematic);
        boolean hasRemainingPlacements = !DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic).isEmpty();

        if (!schematicStillLoaded || !hasRemainingPlacements)
        {
            this.suppressed.suppress(schematic);
            this.deleteTrackedEntrySynchronously(schematic);
        }
        else
        {
            this.markChanged(schematic, CreatorClientTickHandler.getClientTicks());
        }
    }

    @Override
    public void onPlacementUpdated(SchematicPlacement placement)
    {
        if (!this.internalMutation && !this.lifecycleTransition)
        {
            this.markChanged(placement.getSchematic(), CreatorClientTickHandler.getClientTicks());
        }
    }

    @Override
    public void onPlacementSelected(@Nullable SchematicPlacement previous, @Nullable SchematicPlacement selected)
    {
        if (this.internalMutation || this.lifecycleTransition)
        {
            return;
        }

        long tick = CreatorClientTickHandler.getClientTicks();

        if (previous != null)
        {
            this.markChanged(previous.getSchematic(), tick);
        }

        if (selected != null && (previous == null || previous.getSchematic() != selected.getSchematic()))
        {
            this.markChanged(selected.getSchematic(), tick);
        }
    }

    private void flushCurrentWorld()
    {
        long tick = CreatorClientTickHandler.getClientTicks();
        this.scanLoadedSchematics(tick);

        for (Map.Entry<LitematicaSchematic, TrackedEntry> entry : List.copyOf(this.tracked.entrySet()))
        {
            this.snapshotAndSubmit(entry.getKey(), entry.getValue());
        }

        this.awaitQueuedWrites();
        this.drainIoFailures(tick);
    }

    private void scanLoadedSchematics(long tick)
    {
        Set<LitematicaSchematic> loaded = Collections.newSetFromMap(new IdentityHashMap<>());
        loaded.addAll(List.copyOf(SchematicHolder.getInstance().getAllSchematics()));

        for (LitematicaSchematic schematic : loaded)
        {
            if (!isEligible(schematic))
            {
                if (this.tracked.containsKey(schematic))
                {
                    this.deleteTrackedEntrySynchronously(schematic);
                }

                continue;
            }

            if (this.suppressed.contains(schematic) ||
                DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic).isEmpty())
            {
                continue;
            }

            RecoverySignature signature = RecoverySignature.from(schematic);
            TrackedEntry trackedEntry = this.tracked.get(schematic);

            if (trackedEntry == null)
            {
                trackedEntry = this.createTrackedEntry(signature);
                trackedEntry.scheduler.markChanged(tick);
            }
            else if (!trackedEntry.signature.equals(signature))
            {
                trackedEntry.signature = signature;
                trackedEntry.scheduler.markChanged(tick);
            }
        }

        for (LitematicaSchematic schematic : List.copyOf(this.tracked.keySet()))
        {
            if (!loaded.contains(schematic) && !this.lifecycleTransition)
            {
                this.suppressed.suppress(schematic);
                this.deleteTrackedEntrySynchronously(schematic);
            }
        }
    }

    private void markChanged(LitematicaSchematic schematic, long tick)
    {
        if (this.currentWorldKey == null || this.internalMutation || this.lifecycleTransition ||
            this.suppressed.contains(schematic) || !isEligible(schematic) ||
            DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic).isEmpty())
        {
            return;
        }

        RecoverySignature signature = RecoverySignature.from(schematic);
        TrackedEntry trackedEntry = this.tracked.computeIfAbsent(
                schematic,
                key -> this.createTrackedEntry(signature)
        );
        trackedEntry.signature = signature;
        trackedEntry.scheduler.markChanged(tick);
    }

    private TrackedEntry createTrackedEntry(RecoverySignature signature)
    {
        UUID entryId = UUID.randomUUID();
        this.entryEpochs.putIfAbsent(entryId, 0L);
        return new TrackedEntry(entryId, 0L, signature);
    }

    private void snapshotAndSubmit(LitematicaSchematic schematic, TrackedEntry trackedEntry)
    {
        if (!isEligible(schematic) || this.suppressed.contains(schematic))
        {
            return;
        }

        try
        {
            RecoverySnapshot snapshot = this.createSnapshot(schematic, trackedEntry);

            if (snapshot != null)
            {
                trackedEntry.scheduler.markSubmitted();
                long epoch = this.entryEpochs.getOrDefault(trackedEntry.entryId, 0L);
                this.submitWrite(snapshot, epoch);
            }
        }
        catch (Exception e)
        {
            trackedEntry.scheduler.markChanged(CreatorClientTickHandler.getClientTicks());
            this.reportFailure("Failed to snapshot schematic '" + schematic.getMetadata().getName() + "'", e);
        }
    }

    @Nullable
    private RecoverySnapshot createSnapshot(LitematicaSchematic schematic, TrackedEntry trackedEntry)
    {
        List<SchematicPlacement> placements = List.copyOf(
                DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic)
        );

        if (placements.isEmpty())
        {
            return null;
        }

        long now = System.currentTimeMillis();
        long generation = Math.max(now, trackedEntry.lastGeneration + 1L);
        Path cachePath = this.storage.generationPath(trackedEntry.entryId, generation);
        Path originalFile = schematic.getFile();
        LitematicaSchematicAccessor schematicAccessor = (LitematicaSchematicAccessor) schematic;
        FileType originalType = schematicAccessor.litematicacreator$getSchematicType();
        Map<SchematicPlacement, Path> originalPlacementFiles = new IdentityHashMap<>();
        List<JsonObject> placementJson = new ArrayList<>(placements.size());

        schematicAccessor.litematicacreator$setSchematicFile(cachePath);

        try
        {
            for (SchematicPlacement placement : placements)
            {
                originalPlacementFiles.put(placement, placement.getSchematicFile());
                ((SchematicPlacementAccessor) placement).litematicacreator$setSchematicFile(cachePath);
            }

            for (SchematicPlacement placement : placements)
            {
                JsonObject json = placement.toJson();

                if (json == null)
                {
                    throw new IllegalStateException("Litematica did not serialize placement " + placement.getHashId());
                }

                json.addProperty("schematic", cachePath.toAbsolutePath().normalize().toString());

                if (!json.has("placements") || !json.get("placements").isJsonArray())
                {
                    json.add("placements", new JsonArray());
                }

                placementJson.add(json);
            }
        }
        finally
        {
            schematicAccessor.litematicacreator$setSchematicFile(originalFile);

            for (Map.Entry<SchematicPlacement, Path> entry : originalPlacementFiles.entrySet())
            {
                ((SchematicPlacementAccessor) entry.getKey()).litematicacreator$setSchematicFile(entry.getValue());
            }
        }

        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        CreatorFocus focus = CreatorManager.getInstance().getFocus();
        UUID selectedHash = selected != null && selected.getSchematic() == schematic ? selected.getHashId() : null;
        UUID focusHash = focus != null && focus.schematic() == schematic ? focus.placement().getHashId() : null;
        SchematicMetadata metadata = schematic.getMetadata();
        RecoveryManifest manifest = new RecoveryManifest(
                RecoveryManifest.CURRENT_FORMAT_VERSION,
                trackedEntry.entryId,
                this.currentWorldKey,
                metadata.getName() != null ? metadata.getName() : "",
                originalFile != null ? originalFile.toAbsolutePath().normalize().toString() : null,
                originalType.name(),
                metadata.wasModifiedSinceSaved(),
                now,
                generation,
                cachePath.getFileName().toString(),
                placementJson,
                selectedHash,
                focusHash
        );

        trackedEntry.lastGeneration = generation;
        trackedEntry.signature = RecoverySignature.from(schematic);
        CompoundTag nbt = CreatorSchematicMetadataCopies.writeSchematicToNbt(schematic);
        return new RecoverySnapshot(manifest, nbt);
    }

    private void submitWrite(RecoverySnapshot snapshot, long epoch)
    {
        try
        {
            this.writer.submit(() -> {
                if (this.entryEpochs.getOrDefault(snapshot.manifest().entryId(), -1L) != epoch)
                {
                    return;
                }

                try
                {
                    this.storage.commit(snapshot);
                }
                catch (Exception e)
                {
                    LitematicaCreator.LOGGER.error(
                            "Failed to write recovery entry {} to {}",
                            snapshot.manifest().entryId(),
                            this.storage.getRoot(),
                            e
                    );
                    this.ioFailures.add(new IoFailure(snapshot.manifest().entryId(), true));
                }
            });
        }
        catch (RejectedExecutionException e)
        {
            this.reportFailure("Recovery writer rejected a schematic snapshot", e);
        }
    }

    private void restoreWorld(RecoveryWorldKey key)
    {
        RecoveryStorage.ScanResult scan = this.storage.scan();

        if (!scan.invalidManifests().isEmpty())
        {
            for (Path invalid : scan.invalidManifests())
            {
                LitematicaCreator.LOGGER.error("Invalid recovery manifest or cache retained at {}", invalid.toAbsolutePath());
            }

            this.warnOnce();
        }

        List<RecoveryStorage.StoredEntry> matching = scan.entries().stream()
                .filter(entry -> entry.manifest().belongsTo(key))
                .toList();

        if (matching.isEmpty())
        {
            return;
        }

        SchematicPlacementManager placementManager = DataManager.getSchematicPlacementManager();
        SchematicPlacement selectedBefore = placementManager.getSelectedSchematicPlacement();
        Map<UUID, SchematicPlacement> restoredByHash = new HashMap<>();
        UUID selectedHash = null;
        UUID focusHash = null;
        int restoredSchematics = 0;
        int restoredPlacements = 0;

        this.internalMutation = true;

        try
        {
            for (RecoveryStorage.StoredEntry stored : matching)
            {
                try
                {
                    RestoredCandidate candidate = this.decodeCandidate(stored);
                    this.applyCandidate(candidate, selectedBefore);

                    for (SchematicPlacement placement : candidate.placements)
                    {
                        restoredByHash.put(placement.getHashId(), placement);
                    }

                    if (candidate.manifest.selectedPlacementHash() != null)
                    {
                        selectedHash = candidate.manifest.selectedPlacementHash();
                    }

                    if (candidate.manifest.creatorFocusHash() != null)
                    {
                        focusHash = candidate.manifest.creatorFocusHash();
                    }

                    ++restoredSchematics;
                    restoredPlacements += candidate.placements.size();
                }
                catch (Exception e)
                {
                    this.reportFailure(
                            "Failed to restore recovery entry '" + stored.manifest().entryId() + "' from " + stored.cacheFile(),
                            e
                    );
                }
            }

            if (selectedHash != null && restoredByHash.containsKey(selectedHash))
            {
                placementManager.setSelectedSchematicPlacement(restoredByHash.get(selectedHash));
            }
            else if (selectedBefore != null && placementManager.getAllSchematicsPlacements().contains(selectedBefore))
            {
                placementManager.setSelectedSchematicPlacement(selectedBefore);
            }

            if (focusHash != null && restoredByHash.containsKey(focusHash))
            {
                CreatorManager.getInstance().restoreFocus(restoredByHash.get(focusHash));
            }
        }
        finally
        {
            this.internalMutation = false;
            CreatorPlacementIndex.INSTANCE.rebuild();
        }

        if (restoredSchematics > 0)
        {
            InfoUtils.showGuiOrInGameMessage(
                    MessageType.SUCCESS,
                    "litematica-creator.message.recovery.restored",
                    restoredSchematics,
                    restoredPlacements
            );
        }
    }

    private RestoredCandidate decodeCandidate(RecoveryStorage.StoredEntry stored)
    {
        RecoveryManifest manifest = stored.manifest();

        if (!stored.schematicNbt().contains("Metadata") || !stored.schematicNbt().contains("Regions") || manifest.placements().isEmpty())
        {
            throw new IllegalArgumentException("Recovery entry is missing schematic or placement data");
        }

        Path originalFile = parseOriginalFile(manifest.originalFile());

        if (!RecoveryEligibility.shouldCache(originalFile, manifest.dirty()))
        {
            throw new IllegalArgumentException("Clean file-backed recovery entries are not eligible");
        }

        FileType originalType = FileType.valueOf(manifest.originalFileType());
        Path expectedCache = stored.cacheFile().toAbsolutePath().normalize();
        LitematicaSchematic schematic = new LitematicaSchematic(
                expectedCache,
                stored.schematicNbt().copy(),
                FileType.LITEMATICA_SCHEMATIC
        );

        if (!Objects.equals(manifest.schematicName(), schematic.getMetadata().getName()))
        {
            throw new IllegalArgumentException("Recovery schematic metadata does not match its manifest");
        }

        SchematicHolder holder = SchematicHolder.getInstance();

        for (LitematicaSchematic loaded : holder.getAllSchematics())
        {
            if (loaded.getFile() != null && loaded.getFile().toAbsolutePath().normalize().equals(expectedCache))
            {
                throw new IllegalStateException("Recovery cache path is already loaded: " + expectedCache);
            }
        }

        holder.addSchematic(schematic, false);
        List<SchematicPlacement> placements = new ArrayList<>();
        Set<UUID> hashes = new java.util.HashSet<>();

        try
        {
            for (JsonObject storedJson : manifest.placements())
            {
                JsonObject json = RecoveryPlacementJson.prepareForRestore(storedJson, expectedCache);

                SchematicPlacement placement = SchematicPlacement.fromJson(json);

                if (placement == null || placement.getSchematic() != schematic || !hashes.add(placement.getHashId()))
                {
                    throw new IllegalArgumentException("Recovery placement could not be decoded or has a duplicate hash");
                }

                placements.add(placement);
            }

            if (manifest.selectedPlacementHash() != null && !hashes.contains(manifest.selectedPlacementHash()) ||
                manifest.creatorFocusHash() != null && !hashes.contains(manifest.creatorFocusHash()))
            {
                throw new IllegalArgumentException("Recovery selection or focus does not reference a cached placement");
            }

            return new RestoredCandidate(
                    manifest,
                    expectedCache,
                    schematic,
                    List.copyOf(placements),
                    originalFile,
                    originalType
            );
        }
        catch (RuntimeException e)
        {
            holder.removeSchematic(schematic);
            throw e;
        }
    }

    private void applyCandidate(RestoredCandidate candidate, @Nullable SchematicPlacement selectedBefore)
    {
        SchematicHolder holder = SchematicHolder.getInstance();
        SchematicPlacementManager placementManager = DataManager.getSchematicPlacementManager();
        List<LitematicaSchematic> nativeSchematics = new ArrayList<>();
        Map<LitematicaSchematic, List<SchematicPlacement>> nativePlacements = new LinkedHashMap<>();

        if (candidate.originalFile != null)
        {
            for (LitematicaSchematic loaded : List.copyOf(holder.getAllSchematics()))
            {
                if (loaded != candidate.schematic && sameFile(loaded.getFile(), candidate.originalFile))
                {
                    nativeSchematics.add(loaded);
                    nativePlacements.put(loaded, List.copyOf(placementManager.getAllPlacementsOfSchematic(loaded)));
                }
            }
        }

        List<SchematicPlacement> addedPlacements = new ArrayList<>();

        try
        {
            for (LitematicaSchematic nativeSchematic : nativeSchematics)
            {
                holder.removeSchematic(nativeSchematic);
            }

            LitematicaSchematicAccessor schematicAccessor = (LitematicaSchematicAccessor) candidate.schematic;
            schematicAccessor.litematicacreator$setSchematicFile(candidate.originalFile);
            schematicAccessor.litematicacreator$setSchematicType(candidate.originalType);

            for (SchematicPlacement placement : candidate.placements)
            {
                ((SchematicPlacementAccessor) placement).litematicacreator$setSchematicFile(candidate.originalFile);
                placementManager.addSchematicPlacement(placement, false);
                addedPlacements.add(placement);
            }

            if (candidate.manifest.dirty())
            {
                candidate.schematic.getMetadata().setModifiedSinceSaved();
            }
            else
            {
                candidate.schematic.getMetadata().clearModifiedSinceSaved();
            }

            TrackedEntry trackedEntry = new TrackedEntry(
                    candidate.manifest.entryId(),
                    candidate.manifest.generation(),
                    RecoverySignature.from(candidate.schematic)
            );
            this.tracked.put(candidate.schematic, trackedEntry);
            this.entryEpochs.putIfAbsent(candidate.manifest.entryId(), 0L);
        }
        catch (RuntimeException e)
        {
            for (SchematicPlacement placement : addedPlacements)
            {
                placementManager.removeSchematicPlacement(placement, false);
            }

            LitematicaSchematicAccessor schematicAccessor = (LitematicaSchematicAccessor) candidate.schematic;
            schematicAccessor.litematicacreator$setSchematicFile(candidate.cacheFile);
            schematicAccessor.litematicacreator$setSchematicType(FileType.LITEMATICA_SCHEMATIC);

            for (SchematicPlacement placement : candidate.placements)
            {
                ((SchematicPlacementAccessor) placement).litematicacreator$setSchematicFile(candidate.cacheFile);
            }

            holder.removeSchematic(candidate.schematic);

            for (LitematicaSchematic nativeSchematic : nativeSchematics)
            {
                holder.addSchematic(nativeSchematic, false);

                for (SchematicPlacement placement : nativePlacements.get(nativeSchematic))
                {
                    placementManager.addSchematicPlacement(placement, false);
                }
            }

            if (selectedBefore != null && placementManager.getAllSchematicsPlacements().contains(selectedBefore))
            {
                placementManager.setSelectedSchematicPlacement(selectedBefore);
            }

            throw e;
        }
    }

    private void deleteTrackedEntrySynchronously(LitematicaSchematic schematic)
    {
        TrackedEntry trackedEntry = this.tracked.remove(schematic);

        if (trackedEntry == null)
        {
            return;
        }

        long epoch = this.entryEpochs.merge(trackedEntry.entryId, 1L, Long::sum);

        try
        {
            Future<?> deletion = this.writer.submit(() -> {
                if (this.entryEpochs.getOrDefault(trackedEntry.entryId, -1L) != epoch)
                {
                    return;
                }

                try
                {
                    this.storage.deleteEntry(trackedEntry.entryId);
                }
                catch (IOException e)
                {
                    throw new RecoveryIoException(e);
                }
            });
            deletion.get();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            this.reportFailure("Interrupted while deleting recovery entry " + trackedEntry.entryId, e);
        }
        catch (ExecutionException | RejectedExecutionException e)
        {
            this.reportFailure("Failed to delete recovery entry " + trackedEntry.entryId, unwrap(e));
        }
        finally
        {
            this.entryEpochs.remove(trackedEntry.entryId);
        }
    }

    private void awaitQueuedWrites()
    {
        try
        {
            this.writer.submit(() -> {}).get();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            this.reportFailure("Interrupted while waiting for recovery writes", e);
        }
        catch (ExecutionException | RejectedExecutionException e)
        {
            this.reportFailure("Failed while waiting for recovery writes", unwrap(e));
        }
    }

    private void drainIoFailures(long tick)
    {
        IoFailure failure;

        while ((failure = this.ioFailures.poll()) != null)
        {
            if (failure.retry)
            {
                for (TrackedEntry trackedEntry : this.tracked.values())
                {
                    if (trackedEntry.entryId.equals(failure.entryId))
                    {
                        trackedEntry.scheduler.markChanged(tick);
                        break;
                    }
                }
            }

            this.warnOnce();
        }
    }

    private void reportFailure(String message, Throwable throwable)
    {
        LitematicaCreator.LOGGER.error(message, throwable);
        this.warnOnce();
    }

    private void warnOnce()
    {
        if (!this.warnedThisSession)
        {
            this.warnedThisSession = true;
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica-creator.message.recovery.failed");
        }
    }

    private void clearSessionTracking()
    {
        this.tracked.clear();
        this.suppressed.clear();
        this.entryEpochs.clear();
        this.ioFailures.clear();
    }

    private static boolean isEligible(LitematicaSchematic schematic)
    {
        return RecoveryEligibility.shouldCache(
                schematic.getFile(),
                schematic.getMetadata().wasModifiedSinceSaved()
        );
    }

    private static RecoveryWorldKey worldKey(ClientLevel world)
    {
        return new RecoveryWorldKey(
                StringUtils.getWorldOrServerNameOrDefault("default"),
                world.dimension().identifier().toString()
        );
    }

    @Nullable
    private static Path parseOriginalFile(@Nullable String file)
    {
        if (file == null)
        {
            return null;
        }

        try
        {
            Path path = Path.of(file);

            if (!path.isAbsolute())
            {
                throw new IllegalArgumentException("Recovery original file path is not absolute");
            }

            return path.toAbsolutePath().normalize();
        }
        catch (InvalidPathException e)
        {
            throw new IllegalArgumentException("Recovery original file path is invalid", e);
        }
    }

    private static boolean sameFile(@Nullable Path first, Path second)
    {
        return first != null && first.toAbsolutePath().normalize().equals(second);
    }

    private static Throwable unwrap(Exception exception)
    {
        Throwable cause = exception.getCause();

        if (cause instanceof RecoveryIoException && cause.getCause() != null)
        {
            return cause.getCause();
        }

        return cause != null ? cause : exception;
    }

    private static final class TrackedEntry
    {
        private final UUID entryId;
        private final RecoveryWriteScheduler scheduler = new RecoveryWriteScheduler();
        private long lastGeneration;
        private RecoverySignature signature;

        private TrackedEntry(UUID entryId, long lastGeneration, RecoverySignature signature)
        {
            this.entryId = entryId;
            this.lastGeneration = lastGeneration;
            this.signature = signature;
        }
    }

    private record RecoverySignature(
            @Nullable String file,
            boolean dirty,
            long modifiedAt,
            int regionCount,
            int totalBlocks)
    {
        private static RecoverySignature from(LitematicaSchematic schematic)
        {
            SchematicMetadata metadata = schematic.getMetadata();
            return new RecoverySignature(
                    schematic.getFile() != null ? schematic.getFile().toAbsolutePath().normalize().toString() : null,
                    metadata.wasModifiedSinceSaved(),
                    metadata.getTimeModified(),
                    metadata.getRegionCount(),
                    metadata.getTotalBlocks()
            );
        }
    }

    private record RestoredCandidate(
            RecoveryManifest manifest,
            Path cacheFile,
            LitematicaSchematic schematic,
            List<SchematicPlacement> placements,
            @Nullable Path originalFile,
            FileType originalType)
    {
    }

    private record IoFailure(UUID entryId, boolean retry)
    {
    }

    private static final class RecoveryIoException extends RuntimeException
    {
        private RecoveryIoException(Throwable cause)
        {
            super(cause);
        }
    }
}

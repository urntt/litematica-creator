package io.github.urntt.litematicacreator.recovery;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import net.minecraft.nbt.CompoundTag;

public final class RecoveryStorage
{
    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private final Path root;
    private final CommitHook commitHook;

    public RecoveryStorage(Path root)
    {
        this(root, (generationFile, manifestFile) -> {});
    }

    RecoveryStorage(Path root, CommitHook commitHook)
    {
        this.root = root.toAbsolutePath().normalize();
        this.commitHook = commitHook;
    }

    public Path getRoot()
    {
        return this.root;
    }

    public Path manifestPath(UUID entryId)
    {
        return this.root.resolve(entryId + ".json");
    }

    public Path generationPath(UUID entryId, long generation)
    {
        return this.root.resolve(generationFileName(entryId, generation));
    }

    public synchronized void commit(RecoverySnapshot snapshot) throws IOException
    {
        RecoveryManifest manifest = snapshot.manifest();
        Files.createDirectories(this.root);

        Path manifestFile = this.manifestPath(manifest.entryId());
        Path generationFile = this.resolveCacheFile(manifest);
        Path generationTemp = this.root.resolve(manifest.cacheFile() + ".tmp");
        Path manifestTemp = this.root.resolve(manifest.entryId() + ".json.tmp");
        Optional<RecoveryManifest> previous = this.readManifest(manifestFile);

        Files.deleteIfExists(generationTemp);
        Files.deleteIfExists(manifestTemp);

        if (!NbtUtils.writeCompoundTagToCompressedFile(snapshot.schematicNbt(), generationTemp))
        {
            throw new IOException("Failed to write recovery schematic: " + generationTemp);
        }

        moveAtomically(generationTemp, generationFile, false);
        writeJson(manifest.toJson(), manifestTemp);
        this.commitHook.beforeManifestMove(generationFile, manifestFile);
        moveAtomically(manifestTemp, manifestFile, true);

        if (previous.isPresent() && !previous.get().cacheFile().equals(manifest.cacheFile()))
        {
            Files.deleteIfExists(this.resolveCacheFile(previous.get()));
        }
    }

    public synchronized ScanResult scan()
    {
        if (!Files.isDirectory(this.root))
        {
            return new ScanResult(List.of(), List.of());
        }

        List<StoredEntry> entries = new ArrayList<>();
        List<Path> invalidManifests = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.root, "*.json"))
        {
            for (Path manifestFile : stream)
            {
                Optional<RecoveryManifest> manifest = this.readManifest(manifestFile);

                if (manifest.isEmpty())
                {
                    invalidManifests.add(manifestFile);
                    continue;
                }

                RecoveryManifest value = manifest.get();

                if (!this.manifestPath(value.entryId()).equals(manifestFile.toAbsolutePath().normalize()))
                {
                    invalidManifests.add(manifestFile);
                    continue;
                }

                Path cacheFile;

                try
                {
                    cacheFile = this.resolveCacheFile(value);
                }
                catch (IllegalArgumentException e)
                {
                    invalidManifests.add(manifestFile);
                    continue;
                }

                CompoundTag nbt = NbtUtils.readNbtFromFile(cacheFile);

                if (nbt == null)
                {
                    invalidManifests.add(manifestFile);
                    continue;
                }

                entries.add(new StoredEntry(manifestFile, cacheFile, value, nbt));
            }
        }
        catch (IOException e)
        {
            invalidManifests.add(this.root);
        }

        entries.sort(Comparator.comparingLong(entry -> entry.manifest().cachedAtEpochMillis()));
        invalidManifests.sort(Comparator.comparing(Path::toString));
        return new ScanResult(entries, invalidManifests);
    }

    public synchronized Optional<RecoveryManifest> readManifest(Path file)
    {
        if (!Files.isRegularFile(file) || !Files.isReadable(file))
        {
            return Optional.empty();
        }

        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            JsonElement element = JsonParser.parseReader(reader);
            return RecoveryManifest.fromJson(element);
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    public synchronized void deleteEntry(UUID entryId) throws IOException
    {
        if (!Files.isDirectory(this.root))
        {
            return;
        }

        String prefix = entryId + "-";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.root))
        {
            for (Path file : stream)
            {
                String name = file.getFileName().toString();

                if (name.equals(entryId + ".json") || name.equals(entryId + ".json.tmp") ||
                    (name.startsWith(prefix) && (name.endsWith(".litematic") || name.endsWith(".litematic.tmp"))))
                {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    private Path resolveCacheFile(RecoveryManifest manifest)
    {
        Path expected = this.generationPath(manifest.entryId(), manifest.generation());
        Path resolved = this.root.resolve(manifest.cacheFile()).toAbsolutePath().normalize();

        if (!resolved.startsWith(this.root) || !resolved.equals(expected))
        {
            throw new IllegalArgumentException("Recovery manifest cache path does not match its generation");
        }

        return resolved;
    }

    private static String generationFileName(UUID entryId, long generation)
    {
        return entryId + "-" + generation + ".litematic";
    }

    private static void writeJson(JsonElement root, Path file) throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        ))
        {
            GSON.toJson(root, writer);
        }
    }

    private static void moveAtomically(Path source, Path target, boolean replace) throws IOException
    {
        try
        {
            if (replace)
            {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            else
            {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        }
        catch (AtomicMoveNotSupportedException e)
        {
            if (replace)
            {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
            else
            {
                Files.move(source, target);
            }
        }
    }

    @FunctionalInterface
    interface CommitHook
    {
        void beforeManifestMove(Path generationFile, Path manifestFile) throws IOException;
    }

    public record StoredEntry(Path manifestFile, Path cacheFile, RecoveryManifest manifest, CompoundTag schematicNbt)
    {
    }

    public record ScanResult(List<StoredEntry> entries, List<Path> invalidManifests)
    {
        public ScanResult
        {
            entries = List.copyOf(entries);
            invalidManifests = List.copyOf(invalidManifests);
        }
    }
}

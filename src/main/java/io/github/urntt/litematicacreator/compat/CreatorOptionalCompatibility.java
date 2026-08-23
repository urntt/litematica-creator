package io.github.urntt.litematicacreator.compat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import io.github.urntt.litematicacreator.LitematicaCreator;
import io.github.urntt.litematicacreator.creator.CreatorCameraCompat;

public final class CreatorOptionalCompatibility
{
    static final CreatorOptionalModSpec TWEAKEROO = new CreatorOptionalModSpec(
            "tweakeroo",
            "Tweakeroo",
            ">=0.29.2- <0.30.0-",
            Set.of("0.29.2-sakura.1", "0.29.3")
    );
    static final CreatorOptionalModSpec SYNCMATICA = new CreatorOptionalModSpec(
            "syncmatica",
            "Syncmatica",
            ">=0.3.18- <0.4.0-",
            Set.of("0.3.18", "0.3.20")
    );
    static final CreatorOptionalModSpec LITHIUM = new CreatorOptionalModSpec(
            "lithium",
            "Lithium",
            ">=0.25.3- <0.26.0-",
            Set.of("0.25.3+mc26.2")
    );
    static final CreatorOptionalModSpec SODIUM = new CreatorOptionalModSpec(
            "sodium",
            "Sodium",
            ">=0.9.2-alpha.4 <0.10.0-",
            Set.of("0.9.2-alpha.4+mc26.2")
    );

    private static final List<CreatorOptionalModSpec> SPECS = List.of(TWEAKEROO, SYNCMATICA, LITHIUM, SODIUM);
    private static final Map<String, RuntimeStatus> STATUSES = new LinkedHashMap<>();
    private static volatile boolean initialized;

    private CreatorOptionalCompatibility()
    {
    }

    public static synchronized void initialize()
    {
        if (initialized)
        {
            return;
        }

        FabricLoader loader = FabricLoader.getInstance();
        STATUSES.clear();

        for (CreatorOptionalModSpec spec : SPECS)
        {
            Optional<ModContainer> container = loader.getModContainer(spec.id());

            if (container.isEmpty())
            {
                STATUSES.put(spec.id(), RuntimeStatus.notInstalled(spec));
                continue;
            }

            String version = container.get().getMetadata().getVersion().getFriendlyString();
            CreatorOptionalModSpec.VersionStatus versionStatus = spec.assess(version);
            boolean integrationEnabled = versionStatus.isSupported();
            String detail = versionStatus == CreatorOptionalModSpec.VersionStatus.TESTED
                    ? "tested"
                    : versionStatus == CreatorOptionalModSpec.VersionStatus.SUPPORTED_UNTESTED
                            ? "supported range, untested version"
                            : "outside supported range " + spec.supportedRange();

            if (spec == TWEAKEROO)
            {
                integrationEnabled = CreatorCameraCompat.initializeTweakerooBridge(integrationEnabled, version);

                if (versionStatus.isSupported() && !integrationEnabled)
                {
                    detail = "reflection contract mismatch; camera bridge disabled";
                }
            }

            RuntimeStatus status = new RuntimeStatus(spec, true, version, versionStatus, integrationEnabled, detail);
            STATUSES.put(spec.id(), status);

            if (versionStatus == CreatorOptionalModSpec.VersionStatus.SUPPORTED_UNTESTED)
            {
                LitematicaCreator.LOGGER.warn(
                        "Detected {} {} inside the supported range {}, but this exact version has not been tested",
                        spec.displayName(),
                        version,
                        spec.supportedRange()
                );
            }
            else if (versionStatus == CreatorOptionalModSpec.VersionStatus.UNSUPPORTED)
            {
                LitematicaCreator.LOGGER.warn(
                        "Detected unsupported {} {}; audited range is {}{}",
                        spec.displayName(),
                        version,
                        spec.supportedRange(),
                        spec == TWEAKEROO ? "; the Creator Camera bridge is disabled" : ""
                );
            }
        }

        if (!loader.isModLoaded(TWEAKEROO.id()))
        {
            CreatorCameraCompat.initializeTweakerooBridge(false, null);
        }

        initialized = true;
        LitematicaCreator.LOGGER.info(
                "Optional compatibility audit: {}",
                STATUSES.values().stream().map(RuntimeStatus::summary).collect(Collectors.joining("; "))
        );
    }

    public static void ensureInitialized()
    {
        if (!initialized)
        {
            initialize();
        }
    }

    private record RuntimeStatus(
            CreatorOptionalModSpec spec,
            boolean installed,
            String version,
            CreatorOptionalModSpec.VersionStatus versionStatus,
            boolean integrationEnabled,
            String detail)
    {
        private static RuntimeStatus notInstalled(CreatorOptionalModSpec spec)
        {
            return new RuntimeStatus(spec, false, "", null, false, "not installed");
        }

        private String summary()
        {
            if (!this.installed)
            {
                return this.spec.displayName() + "=not installed";
            }

            String integration = this.spec == TWEAKEROO
                    ? (this.integrationEnabled ? ", camera bridge active" : ", camera bridge inactive")
                    : "";
            return this.spec.displayName() + "@" + this.version + "=" + this.detail + integration;
        }
    }
}

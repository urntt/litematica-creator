package io.github.urntt.litematicacreator.compat;

import java.util.Objects;
import java.util.Set;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

record CreatorOptionalModSpec(
        String id,
        String displayName,
        String supportedRange,
        Set<String> testedVersions)
{
    CreatorOptionalModSpec
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(supportedRange);
        testedVersions = Set.copyOf(testedVersions);
    }

    VersionStatus assess(String version)
    {
        if (this.testedVersions.contains(version))
        {
            return VersionStatus.TESTED;
        }

        try
        {
            Version parsedVersion = Version.parse(version);
            VersionPredicate predicate = VersionPredicate.parse(this.supportedRange);
            return predicate.test(parsedVersion) ? VersionStatus.SUPPORTED_UNTESTED : VersionStatus.UNSUPPORTED;
        }
        catch (VersionParsingException exception)
        {
            return VersionStatus.UNSUPPORTED;
        }
    }

    enum VersionStatus
    {
        TESTED(true),
        SUPPORTED_UNTESTED(true),
        UNSUPPORTED(false);

        private final boolean supported;

        VersionStatus(boolean supported)
        {
            this.supported = supported;
        }

        boolean isSupported()
        {
            return this.supported;
        }
    }
}

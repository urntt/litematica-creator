package io.github.urntt.litematicacreator.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatorOptionalModSpecTest
{
    @Test
    void recognizesEveryAuditedRuntimeVersion()
    {
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.TESTED,
                CreatorOptionalCompatibility.TWEAKEROO.assess("0.29.2-sakura.1")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.TESTED,
                CreatorOptionalCompatibility.TWEAKEROO.assess("0.29.3")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.TESTED,
                CreatorOptionalCompatibility.SYNCMATICA.assess("0.3.18")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.TESTED,
                CreatorOptionalCompatibility.SYNCMATICA.assess("0.3.20")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.TESTED,
                CreatorOptionalCompatibility.LITHIUM.assess("0.25.3+mc26.2")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.TESTED,
                CreatorOptionalCompatibility.SODIUM.assess("0.9.2-alpha.4+mc26.2")
        );
    }

    @Test
    void distinguishesSupportedUntestedAndUnsupportedVersions()
    {
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.SUPPORTED_UNTESTED,
                CreatorOptionalCompatibility.TWEAKEROO.assess("0.29.4")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.UNSUPPORTED,
                CreatorOptionalCompatibility.TWEAKEROO.assess("0.30.0")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.SUPPORTED_UNTESTED,
                CreatorOptionalCompatibility.SYNCMATICA.assess("0.3.19")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.UNSUPPORTED,
                CreatorOptionalCompatibility.LITHIUM.assess("0.26.0+mc26.2")
        );
        assertEquals(
                CreatorOptionalModSpec.VersionStatus.UNSUPPORTED,
                CreatorOptionalCompatibility.SODIUM.assess("not-a-version")
        );
    }
}

package io.github.urntt.litematicacreator.creator;

final class CreatorProjectionCollisionPolicy
{
    private CreatorProjectionCollisionPolicy()
    {
    }

    static boolean globallyEnabled(boolean renderingEnabled, boolean schematicRenderingEnabled)
    {
        return renderingEnabled && schematicRenderingEnabled;
    }

    static boolean chunkEligible(boolean loadEntireSchematics, boolean clientChunkLoaded)
    {
        return loadEntireSchematics || clientChunkLoaded;
    }

    static <T> T applyContribution(T previous, boolean covered, boolean structureVoid, T candidate)
    {
        return covered && !structureVoid ? candidate : previous;
    }
}

package io.github.urntt.litematicacreator.config;

final class CreatorTranslationApplyGate
{
    private boolean loading;
    private boolean applying;

    void beginLoading()
    {
        this.loading = true;
    }

    void endLoading()
    {
        this.loading = false;
    }

    boolean beginApplying()
    {
        if (this.loading || this.applying)
        {
            return false;
        }

        this.applying = true;
        return true;
    }

    void endApplying()
    {
        this.applying = false;
    }
}

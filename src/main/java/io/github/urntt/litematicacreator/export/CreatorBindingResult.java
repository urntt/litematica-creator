package io.github.urntt.litematicacreator.export;

import javax.annotation.Nullable;

public record CreatorBindingResult(boolean success, boolean bound, boolean clean, @Nullable String error)
{
    static CreatorBindingResult exported()
    {
        return new CreatorBindingResult(true, false, false, null);
    }

    static CreatorBindingResult bound(boolean clean)
    {
        return new CreatorBindingResult(true, true, clean, null);
    }

    static CreatorBindingResult failed(String error)
    {
        return new CreatorBindingResult(false, false, false, error);
    }
}

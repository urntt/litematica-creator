package io.github.urntt.litematicacreator.compat.litematica;

/**
 * Mirrors the conditions under which Litematica's tool hotkeys and tool scroll handler act, so Creator can leave
 * those inputs to the tool instead of also placing, removing, picking or scrolling the virtual hotbar.
 */
final class CreatorToolInputPolicy
{
    private CreatorToolInputPolicy()
    {
    }

    /**
     * @param toolKeyPressed the input belongs to the corner 1, corner 2 or select-elements tool hotkey
     * @param blockSelectPressed the input is the select-elements hotkey with a block modifier the current tool mode
     *                           uses; Litematica handles that even without the tool item
     */
    static boolean claimsPress(boolean toolEnabled, boolean holdsTool, boolean toolKeyPressed, boolean blockSelectPressed)
    {
        return toolEnabled && (blockSelectPressed || holdsTool && toolKeyPressed);
    }

    static boolean claimsScroll(boolean toolEnabled, boolean holdsTool, boolean scrollModifierHeld)
    {
        return toolEnabled && holdsTool && scrollModifierHeld;
    }
}

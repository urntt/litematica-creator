package io.github.urntt.litematicacreator.gui;

/** A vanilla container screen opened on a projection; its clicks run through the interaction instead of the server. */
public interface CreatorProjectionContainerScreen
{
    CreatorProjectionContainerInteraction interaction();
}

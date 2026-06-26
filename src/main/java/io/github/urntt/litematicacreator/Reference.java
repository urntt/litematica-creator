package io.github.urntt.litematicacreator;

import net.minecraft.SharedConstants;

import fi.dy.masa.malilib.util.StringUtils;

public final class Reference
{
    public static final String MOD_ID = "litematica-creator";
    public static final String MOD_NAME = "Litematica Creator";
    public static final String MOD_VERSION = StringUtils.getModVersionString(MOD_ID);
    public static final String MC_VERSION = SharedConstants.getCurrentVersion().id();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID + "-" + MOD_TYPE + "-" + MC_VERSION + "-" + MOD_VERSION;

    private Reference()
    {
    }
}

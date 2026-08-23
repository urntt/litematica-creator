package io.github.urntt.litematicacreator.export;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

record CreatorEntitySnapshot(Vec3 posVec, CompoundTag nbt)
{
    CreatorEntitySnapshot
    {
        posVec = new Vec3(posVec.x, posVec.y, posVec.z);
        nbt = nbt.copy();
    }

    CreatorEntitySnapshot copy()
    {
        return new CreatorEntitySnapshot(this.posVec, this.nbt);
    }
}

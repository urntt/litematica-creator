package io.github.urntt.litematicacreator.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.chat.ChatAbilities;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.mixin.LocalPlayerAccessor;
import io.github.urntt.litematicacreator.render.CreatorVirtualLoadout;

public final class CreatorCameraEntity extends LocalPlayer
{
    static final int CLIENT_ENTITY_ID = Integer.MIN_VALUE;
    private static final double VANILLA_GROUND_SPEED = 0.1D;
    private static final float VANILLA_FLIGHT_SPEED = 0.05F;

    private final Minecraft minecraft;
    private final LocalPlayer appearancePlayer;

    CreatorCameraEntity(Minecraft minecraft, ClientLevel level, LocalPlayer player, Entity source, boolean flying)
    {
        super(
                minecraft,
                level,
                player.connection,
                player.getStats(),
                player.getRecipeBook(),
                Input.EMPTY,
                false,
                ChatAbilities.NO_RESTRICTIONS
        );
        this.minecraft = minecraft;
        this.appearancePlayer = player;
        this.setId(CLIENT_ENTITY_ID);
        this.input = new CreatorCameraInput(minecraft);
        this.setPosRaw(source.getX(), source.getY(), source.getZ());
        this.setYRot(source.getYRot());
        this.setXRot(source.getXRot());
        this.setYHeadRot(source.getYRot());
        this.setYBodyRot(source.getYRot());
        this.setPose(Pose.STANDING);
        this.setSwimming(false);
        this.setShiftKeyDown(false);
        this.setSprinting(false);
        this.setJumping(false);
        this.setOnGround(false);
        this.setDeltaMovement(Vec3.ZERO);

        this.getAbilities().invulnerable = true;
        this.getAbilities().mayfly = true;
        this.getAbilities().flying = flying;
        this.noPhysics = flying;
        this.applyConfiguredSpeeds();
        this.updateOldPositionAndRotation();
    }

    public void creatorTick()
    {
        this.updateOldPositionAndRotation();
        this.applyConfiguredSpeeds();
        ((LocalPlayerAccessor) (Object) this).litematicacreator$setAutoJumpEnabled(
                this.minecraft.options.autoJump().get()
        );
        this.baseTick();
        this.updateSwingTime();
        this.aiStep();
        this.noPhysics = this.getAbilities().flying;
        this.resetFallDistance();
    }

    public boolean isCreatorFlying()
    {
        return this.getAbilities().flying;
    }

    @Override
    public HumanoidArm getMainArm()
    {
        return this.appearancePlayer != null ? this.appearancePlayer.getMainArm() : super.getMainArm();
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart modelPart)
    {
        return this.appearancePlayer != null ? this.appearancePlayer.isModelPartShown(modelPart) : super.isModelPartShown(modelPart);
    }

    public void turnCamera(double yawChange, double pitchChange)
    {
        this.turn(yawChange, pitchChange);
        this.setYHeadRot(this.getYRot());
        this.setYBodyRot(this.getYRot());
    }

    @Override
    public boolean isSpectator()
    {
        return this.getAbilities().flying;
    }

    @Override
    public boolean startRiding(Entity entity, boolean force, boolean sendPacket)
    {
        return false;
    }

    @Override
    public void move(MoverType moverType, Vec3 movement)
    {
        super.move(moverType, movement);
    }

    @Override
    public void onUpdateAbilities()
    {
        this.noPhysics = this.getAbilities().flying;
    }

    @Override
    protected void sendRidingJump()
    {
    }

    @Override
    public boolean tryToStartFallFlying()
    {
        return false;
    }

    @Override
    public ItemStack getMainHandItem()
    {
        return CreatorVirtualLoadout.getMainHand();
    }

    @Override
    public ItemStack getOffhandItem()
    {
        return CreatorVirtualLoadout.getOffhand();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot)
    {
        return switch (slot)
        {
            case MAINHAND -> CreatorVirtualLoadout.getMainHand();
            case OFFHAND -> CreatorVirtualLoadout.getOffhand();
            case HEAD, CHEST, LEGS, FEET -> CreatorVirtualLoadout.getEquipment(slot);
            default -> ItemStack.EMPTY;
        };
    }

    private void applyConfiguredSpeeds()
    {
        double groundMultiplier = Configs.Generic.CREATOR_CAMERA_GROUND_SPEED_MULTIPLIER.getDoubleValue();
        double flightMultiplier = Configs.Generic.CREATOR_CAMERA_FLIGHT_SPEED_MULTIPLIER.getDoubleValue();
        AttributeInstance movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movementSpeed != null)
        {
            movementSpeed.setBaseValue(VANILLA_GROUND_SPEED * groundMultiplier);
        }

        this.getAbilities().setWalkingSpeed((float) (VANILLA_GROUND_SPEED * groundMultiplier));
        this.getAbilities().setFlyingSpeed((float) (VANILLA_FLIGHT_SPEED * flightMultiplier));
    }

    private void updateOldPositionAndRotation()
    {
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.yHeadRotO = this.getYHeadRot();
    }
}

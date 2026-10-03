package io.github.urntt.litematicacreator.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.camera.CreatorCameraController;
import io.github.urntt.litematicacreator.render.CreatorCameraAvatarRenderPolicy;
import io.github.urntt.litematicacreator.render.CreatorVirtualLoadout;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin
{
    private static final int CREATOR_AVATAR_TINT = 0x26FFFFFF;

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void litematicacreator$applyVirtualLocalLoadout(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci)
    {
        Minecraft minecraft = Minecraft.getInstance();
        CreatorCameraController cameraController = CreatorCameraController.getInstance();
        boolean creatorCameraAvatar = cameraController.isCamera(entity);
        boolean localPlayerWithoutCamera = CreatorManager.getInstance().isCreatorModeEnabled() &&
                !cameraController.isActive() && entity == minecraft.player;

        if (!creatorCameraAvatar && !localPlayerWithoutCamera)
        {
            return;
        }

        ItemStack mainHand = CreatorVirtualLoadout.getMainHand();
        ItemStack offhand = CreatorVirtualLoadout.getOffhand();
        ItemStack rightHand = CreatorVirtualLoadout.getForArm(state.mainArm, HumanoidArm.RIGHT);
        ItemStack leftHand = CreatorVirtualLoadout.getForArm(state.mainArm, HumanoidArm.LEFT);
        ItemModelResolver resolver = ((LivingEntityRendererAccessor)this).litematicacreator$getItemModelResolver();

        resolver.updateForLiving(state.rightHandItemState, rightHand, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
        resolver.updateForLiving(state.leftHandItemState, leftHand, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
        state.rightHandItemStack = rightHand.copy();
        state.leftHandItemStack = leftHand.copy();
        this.applyArmPoses(entity, state, mainHand, offhand);

        state.headEquipment = renderableEquipment(EquipmentSlot.HEAD);
        state.chestEquipment = renderableEquipment(EquipmentSlot.CHEST);
        state.legsEquipment = renderableEquipment(EquipmentSlot.LEGS);
        state.feetEquipment = renderableEquipment(EquipmentSlot.FEET);

        if (creatorCameraAvatar)
        {
            Pose pose = entity.getPose();
            state.pose = pose;
            state.isCrouching = entity.isCrouching();
            state.isFallFlying = CreatorCameraAvatarRenderPolicy.renderAsFallFlying(entity.isFallFlying(), pose);
            state.isVisuallySwimming = !state.isFallFlying && entity.isVisuallySwimming();
            state.swimAmount = state.isFallFlying ? 0.0F : entity.getSwimAmount(partialTicks);
            state.fallFlyingTimeInTicks = entity.getFallFlyingTicks() + partialTicks;
            state.elytraRotX = entity.elytraAnimationState.getRotX(partialTicks);
            state.elytraRotY = entity.elytraAnimationState.getRotY(partialTicks);
            state.elytraRotZ = entity.elytraAnimationState.getRotZ(partialTicks);
        }

        state.isUsingItem = false;
        state.ticksUsingItem = 0.0F;
        state.heldOnHead.clear();
    }

    private void applyArmPoses(Avatar entity, AvatarRenderState state, ItemStack mainHand, ItemStack offhand)
    {
        HumanoidModel.ArmPose mainPose = armPose(entity, mainHand, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose offhandPose = armPose(entity, offhand, InteractionHand.OFF_HAND);

        if (mainPose.isTwoHanded())
        {
            offhandPose = offhand.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }

        if (state.mainArm == HumanoidArm.RIGHT)
        {
            state.rightArmPose = mainPose;
            state.leftArmPose = offhandPose;
        }
        else
        {
            state.leftArmPose = mainPose;
            state.rightArmPose = offhandPose;
        }
    }

    private static HumanoidModel.ArmPose armPose(Avatar entity, ItemStack stack, InteractionHand hand)
    {
        if (stack.isEmpty())
        {
            return HumanoidModel.ArmPose.EMPTY;
        }

        if (!entity.isSwinging() && stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack))
        {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }

        return HumanoidMobRenderer.usesSpearPose(stack, hand.asArm(entity.getMainArm()), entity) ?
                HumanoidModel.ArmPose.SPEAR :
                HumanoidModel.ArmPose.ITEM;
    }

    private static ItemStack renderableEquipment(EquipmentSlot equipmentSlot)
    {
        ItemStack stack = CreatorVirtualLoadout.getEquipment(equipmentSlot);
        return HumanoidArmorLayer.shouldRender(stack, equipmentSlot) ? stack : ItemStack.EMPTY;
    }

    @Redirect(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/UvMapping;)V"
            )
    )
    private void litematicacreator$renderTranslucentCreatorFirstPersonArm(
            SubmitNodeCollector collector,
            ModelPart modelPart,
            PoseStack poseStack,
            RenderType renderType,
            int light,
            int overlay,
            UvMapping uvMapping)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (CreatorCameraController.getInstance().isActive() && minecraft.options.getCameraType() == CameraType.FIRST_PERSON)
        {
            collector.submitModelPart(
                    modelPart,
                    poseStack,
                    renderType,
                    light,
                    overlay,
                    uvMapping,
                    CREATOR_AVATAR_TINT
            );
        }
        else
        {
            collector.submitModelPart(modelPart, poseStack, renderType, light, overlay, uvMapping);
        }
    }
}

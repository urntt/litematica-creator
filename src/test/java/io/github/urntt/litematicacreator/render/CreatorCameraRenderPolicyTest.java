package io.github.urntt.litematicacreator.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorCameraRenderPolicyTest
{
    @Test
    void realPlayerRequiresAnActiveCameraVisibilityAndNoExistingState()
    {
        assertTrue(CreatorCameraRenderPolicy.shouldAppendRealPlayer(true, true, true, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendRealPlayer(false, true, true, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendRealPlayer(true, false, true, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendRealPlayer(true, true, false, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendRealPlayer(true, true, true, true));
    }

    @Test
    void cameraAvatarOnlyRendersAsAWorldModelOutsideFirstPerson()
    {
        assertTrue(CreatorCameraRenderPolicy.shouldAppendCameraAvatar(true, true, false, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendCameraAvatar(true, true, true, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendCameraAvatar(false, true, false, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendCameraAvatar(true, false, false, false));
        assertFalse(CreatorCameraRenderPolicy.shouldAppendCameraAvatar(true, true, false, true));
    }
}

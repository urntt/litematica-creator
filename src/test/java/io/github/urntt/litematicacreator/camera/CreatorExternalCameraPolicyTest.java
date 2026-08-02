package io.github.urntt.litematicacreator.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorExternalCameraPolicyTest
{
    @Test
    void staleTweakerooCameraIsNotRestoredWhenFreeCameraIsOff()
    {
        assertFalse(CreatorExternalCameraPolicy.canRestoreCapturedCamera(true, false));
        assertTrue(CreatorExternalCameraPolicy.canRestoreCapturedCamera(true, true));
        assertTrue(CreatorExternalCameraPolicy.canRestoreCapturedCamera(false, false));
    }

    @Test
    void aNewExternalCameraIsCapturedOnTweakerooActivation()
    {
        assertTrue(CreatorExternalCameraPolicy.shouldCaptureNewTweakerooCamera(false, true, true));
        assertFalse(CreatorExternalCameraPolicy.shouldCaptureNewTweakerooCamera(true, true, true));
        assertFalse(CreatorExternalCameraPolicy.shouldCaptureNewTweakerooCamera(false, true, false));
        assertFalse(CreatorExternalCameraPolicy.shouldCaptureNewTweakerooCamera(true, false, true));
        assertFalse(CreatorExternalCameraPolicy.shouldCaptureNewTweakerooCamera(false, false, true));
    }
}

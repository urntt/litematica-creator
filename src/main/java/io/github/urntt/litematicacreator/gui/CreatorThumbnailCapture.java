package io.github.urntt.litematicacreator.gui;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;

public final class CreatorThumbnailCapture
{
    private static final int PREVIEW_SIZE = 120;
    private static Request pending;

    private CreatorThumbnailCapture()
    {
    }

    public static synchronized boolean request(
            Minecraft minecraft,
            Consumer<int[]> success,
            Consumer<Throwable> failure)
    {
        if (pending != null || minecraft.level == null)
        {
            return false;
        }

        pending = new Request(minecraft.level, success, failure);
        return true;
    }

    public static void afterFrameRendered(Minecraft minecraft)
    {
        Request request;
        boolean worldChanged = false;

        synchronized (CreatorThumbnailCapture.class)
        {
            if (pending == null)
            {
                return;
            }

            if (minecraft.level != pending.level)
            {
                request = pending;
                pending = null;
                worldChanged = true;
            }
            else if (minecraft.gui.screen() != null)
            {
                return;
            }
            else
            {
                request = pending;
                pending = null;
            }
        }

        if (worldChanged)
        {
            request.failure.accept(new IllegalStateException("The client world changed before thumbnail capture"));
            return;
        }

        try
        {
            Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), screenshot -> capture(screenshot, request));
        }
        catch (Exception exception)
        {
            request.failure.accept(exception);
        }
    }

    private static void capture(NativeImage screenshot, Request request)
    {
        try (screenshot; NativeImage scaled = new NativeImage(PREVIEW_SIZE, PREVIEW_SIZE, false))
        {
            int x = screenshot.getWidth() >= screenshot.getHeight() ?
                    (screenshot.getWidth() - screenshot.getHeight()) / 2 : 0;
            int y = screenshot.getHeight() >= screenshot.getWidth() ?
                    (screenshot.getHeight() - screenshot.getWidth()) / 2 : 0;
            int side = Math.min(screenshot.getWidth(), screenshot.getHeight());
            screenshot.resizeSubRectTo(x, y, side, side, scaled);
            @SuppressWarnings("deprecation") int[] pixels = scaled.makePixelArray();
            request.success.accept(pixels);
        }
        catch (Exception exception)
        {
            request.failure.accept(exception);
        }
    }

    private record Request(ClientLevel level, Consumer<int[]> success, Consumer<Throwable> failure)
    {
    }
}

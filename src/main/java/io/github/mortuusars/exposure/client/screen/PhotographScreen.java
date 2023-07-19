package io.github.mortuusars.exposure.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.camera.Photograph;
import io.github.mortuusars.exposure.client.render.PhotographRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class PhotographScreen extends Screen {
    private final Photograph photograph;
    private float zoom = 0f;

    private double xPos;
    private double yPos;

    public PhotographScreen(Photograph photograph) {
        super(Component.empty());
        this.photograph = photograph;
    }

    @Override
    protected void init() {
        super.init();

        xPos = width / 2f;
        yPos = height / 2f;
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        float phWidth = PhotographRenderer.SIZE;
        float phHeight = PhotographRenderer.SIZE;

        float scale = (height - (height / 6f)) / phHeight;
        scale += zoom;

        poseStack.pushPose();

        // Move to center
        poseStack.translate(xPos, yPos, 0);
        // Scale
        poseStack.scale(scale, scale, scale);
        // Set origin point to center (for scale)
        poseStack.translate(phWidth / -2d, phHeight / -2d, 0);

        // Paper (frame)
        fill(poseStack, -8, -8, (int) (phWidth + 8), (int) (phHeight + 8), 0xFFDDDDDD);
        PhotographRenderer.render(photograph, poseStack);

        poseStack.popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float zoomChange = delta > 0.0 ? 0.05f : -0.05f;
        float modifier = Mth.map(zoom, -0.5f, 2f, 1f, 8f);
        zoom = Mth.clamp(zoom + (zoomChange * modifier), -0.5f, 2f);

        if (zoom > -0.5f & zoom < 2f) {
            if (zoom < 1.5f) {
                double moveDelta = (zoomChange * modifier) * -1;
                xPos = Mth.lerp(moveDelta, xPos, mouseX);
                yPos = Mth.lerp(moveDelta, yPos, mouseY);
            }
        }

        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {

    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        boolean handled = super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);

        if (!handled && pButton == 1) {
            this.xPos += pDragX;
            this.yPos += pDragY;

            this.xPos = Mth.clamp(xPos, -128f * zoom, width + 128f * zoom);
            this.yPos = Mth.clamp(yPos, -128f * zoom, height + 128f * zoom);
            handled = true;
        }

        return handled;
    }
}

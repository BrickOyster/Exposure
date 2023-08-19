package io.github.mortuusars.exposure.client.gui.screen.element;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.component.ShutterSpeed;
import io.github.mortuusars.exposure.camera.infrastructure.SynchronizedCameraInHandActions;
import io.github.mortuusars.exposure.config.Config;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ShutterSpeedButton extends CycleButton {
    private final Screen screen;
    private final List<ShutterSpeed> shutterSpeeds;

    public ShutterSpeedButton(Screen screen, int x, int y, int width, int height, int u, int v, ResourceLocation texture) {
        super(screen, x, y, width, height, u, v, height, texture);
        this.screen = screen;

        CameraInHand camera = Exposure.getCamera().getCameraInHand(Minecraft.getInstance().player);

        List<ShutterSpeed> speeds = new ArrayList<>(camera.getItem().getAllShutterSpeeds(camera.getStack()));
        Collections.reverse(speeds);
        shutterSpeeds = speeds;

        ShutterSpeed shutterSpeed = camera.getItem().getShutterSpeed(camera.getStack());
        if (!shutterSpeeds.contains(shutterSpeed))
            shutterSpeed = camera.getItem().getDefaultShutterSpeed(camera.getStack());

        int currentShutterSpeedIndex = 0;
        for (int i = 0; i < shutterSpeeds.size(); i++) {
            if (shutterSpeed.equals(shutterSpeeds.get(i)))
                currentShutterSpeedIndex = i;
        }

        setupButtonElements(shutterSpeeds.size(), currentShutterSpeedIndex);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(Exposure.SoundEvents.CAMERA_DIAL_CLICK.get(),
                Objects.requireNonNull(Minecraft.getInstance().level).random.nextFloat() * 0.05f + 0.9f + index * 0.01f, 0.7f));
    }

    @Override
    public void renderButton(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.renderButton(poseStack, mouseX, mouseY, partialTick);

        CameraInHand camera = Exposure.getCamera().getCameraInHand(Minecraft.getInstance().player);
        ShutterSpeed shutterSpeed = camera.getItem().getShutterSpeed(camera.getStack());
        String text = shutterSpeed.toString();
        if (shutterSpeed.equals(camera.getItem().getDefaultShutterSpeed(camera.getStack())))
            text = text + "•";

        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(text);
        int xPos = 35 - (textWidth / 2);

        font.draw(poseStack, text, x + xPos, y + 4, Config.Client.getSecondaryFontColor());
        font.draw(poseStack, text, x + xPos, y + 3, Config.Client.getMainFontColor());
    }

    @Override
    public void renderToolTip(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
        screen.renderTooltip(poseStack, Component.translatable("gui.exposure.viewfinder.shutter_speed.tooltip"), mouseX, mouseY);
    }

    @Override
    protected void onCycle() {
        CameraInHand camera = Exposure.getCamera().getCameraInHand(Minecraft.getInstance().player);
        if (!camera.isEmpty()) {
            if (camera.getItem().getShutterSpeed(camera.getStack()) != shutterSpeeds.get(index)) {
                SynchronizedCameraInHandActions.setShutterSpeed(shutterSpeeds.get(index));
            }
        }
    }
}

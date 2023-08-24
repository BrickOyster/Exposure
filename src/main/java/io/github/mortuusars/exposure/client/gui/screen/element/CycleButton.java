package io.github.mortuusars.exposure.client.gui.screen.element;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public abstract class CycleButton extends ImageButton {
    protected final Screen screen;
    protected int count = 1;
    protected int index = 0;
    protected boolean loop = true;

    protected int scrollTimeoutMS = 40;
    private long lastChangeTime;

    public CycleButton(Screen screen, int x, int y, int width, int height, int u, int v, int yDiffTex,  ResourceLocation texture) {
        super(x, y, width, height, u, v, yDiffTex, texture, button -> {});
        this.screen = screen;
    }

    public void setupButtonElements(int count, int startingIndex) {
        this.count = count;
        this.index = startingIndex;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isActive() && visible && (button == 0 || button == 1) && clicked(mouseX, mouseY)) {
            cycle(button == 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (System.currentTimeMillis() - lastChangeTime > scrollTimeoutMS)
            cycle(delta < 0d);
        return true;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        boolean pressed = super.keyPressed(pKeyCode, pScanCode, pModifiers);

        if (pressed)
            cycle(Screen.hasShiftDown());

        return pressed;
    }

    protected void cycle(boolean reverse) {
        int value = index;
        value += reverse ? -1 : 1;
        if (value < 0)
            value = loop ? count - 1 : 0;
        else if (value >= count)
            value = loop ? 0 : count - 1;

        if (index != value) {
            index = value;
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            lastChangeTime = System.currentTimeMillis();
            onCycle();
        }
    }

    protected void onCycle() { }

    @Override
    public void renderToolTip(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderToolTip(pPoseStack, pMouseX, pMouseY);
    }
}
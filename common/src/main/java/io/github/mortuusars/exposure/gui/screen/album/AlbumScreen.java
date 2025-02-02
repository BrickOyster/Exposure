package io.github.mortuusars.exposure.gui.screen.album;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.gui.screen.element.Pager;
import io.github.mortuusars.exposure.gui.screen.element.TextBlock;
import io.github.mortuusars.exposure.gui.screen.element.textbox.HorizontalAlignment;
import io.github.mortuusars.exposure.gui.screen.element.textbox.TextBox;
import io.github.mortuusars.exposure.item.AlbumPage;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.menu.AlbumMenu;
import io.github.mortuusars.exposure.menu.AlbumPlayerInventorySlot;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.AlbumSyncNoteC2SP;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.PagingDirection;
import io.github.mortuusars.exposure.util.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class AlbumScreen extends AbstractContainerScreen<AlbumMenu> {
    
    public static final ResourceLocation TEXTURE = Exposure.resource("textures/gui/album.png");
    public static final int MAIN_FONT_COLOR = 0xFFB59774;
    public static final int SECONDARY_FONT_COLOR = 0xFFEFE4CA;
    public static final int SELECTION_COLOR = 0xFF8888FF;
    public static final int SELECTION_UNFOCUSED_COLOR = 0xFFBBBBFF;

    @NotNull
    protected final Minecraft minecraft;
    @NotNull
    protected final Player player;
    @NotNull
    protected final MultiPlayerGameMode gameMode;

    protected final Pager pager = new Pager(SoundEvents.BOOK_PAGE_TURN) {
        @Override
        public void onPageChanged(PagingDirection pagingDirection, int prevPage, int currentPage) {
            super.onPageChanged(pagingDirection, prevPage, currentPage);
            sendButtonClick(pagingDirection == PagingDirection.PREVIOUS ? AlbumMenu.PREVIOUS_PAGE_BUTTON : AlbumMenu.NEXT_PAGE_BUTTON);
        }
    };

    protected final List<Page> pages = new ArrayList<>();

    @Nullable
    protected Button enterSignModeButton;

    public AlbumScreen(AlbumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        minecraft = Minecraft.getInstance();
        player = Objects.requireNonNull(minecraft.player);
        gameMode = Objects.requireNonNull(minecraft.gameMode);
    }

    @Override
    protected void containerTick() {
        forEachPage(page -> page.noteWidget.ifLeft(TextBox::tick));
    }

    @Override
    protected void init() {
        this.imageWidth = 298;
        this.imageHeight = 188;
        super.init();

        titleLabelY = -999;
        inventoryLabelX = 69;
        inventoryLabelY = -999;

        pages.clear();

        // LEFT:
        Page leftPage = createPage(Side.LEFT, 0);
        pages.add(leftPage);

        ImageButton previousButton = new ImageButton(leftPos + 12, topPos + 164, 13, 15,
                216, 188, 15, TEXTURE, 512, 512,
                button -> pager.changePage(PagingDirection.PREVIOUS), Component.translatable("gui.exposure.previous_page"));
        previousButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.previous_page")));
        addRenderableWidget(previousButton);

        // RIGHT:
        Page rightPage = createPage(Side.RIGHT, 140);
        pages.add(rightPage);

        ImageButton nextButton = new ImageButton(leftPos + 273, topPos + 164, 13, 15,
                229, 188, 15, TEXTURE, 512, 512,
                button -> pager.changePage(PagingDirection.NEXT), Component.translatable("gui.exposure.next_page"));
        nextButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.next_page")));
        addRenderableWidget(nextButton);

        // MISC:
        if (getMenu().isAlbumEditable()) {
            enterSignModeButton = new ImageButton(leftPos - 23, topPos + 17, 22, 22, 242, 188,
                    22, TEXTURE, 512, 512,
                    b -> enterSignMode(), Component.translatable("gui.exposure.album.sign"));
            enterSignModeButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.album.sign")));
            addRenderableWidget(enterSignModeButton);
        }

        int spreadsCount = (int) Math.ceil(getMenu().getPages().size() / 2f);
        pager.init(spreadsCount, false, previousButton, nextButton);
    }

    protected Page createPage(Side side, int xOffset) {
        int x = leftPos + xOffset;
        int y = topPos;

        Rect2i page = new Rect2i(x, y, 149, 188);
        Rect2i photo = new Rect2i(x + 25, y + 21, 108, 108);
        Rect2i exposure = new Rect2i(x + 31, y + 27, 96, 96);
        Rect2i note = new Rect2i(x + 22, y + 133, 114, 27);

        PhotographSlotButton photographButton = new PhotographSlotButton(exposure, photo.getX(), photo.getY(),
                photo.getWidth(), photo.getHeight(), 0, 188, 108, TEXTURE, 512, 512,
                b -> {
                    PhotographSlotButton button = (PhotographSlotButton) b;
                    ItemStack photograph = button.getPhotograph();
                    if (photograph.isEmpty()) {
                        if (button.isEditable) {
                            sendButtonClick(side == Side.LEFT ? AlbumMenu.LEFT_PAGE_PHOTO_BUTTON : AlbumMenu.RIGHT_PAGE_PHOTO_BUTTON);
                            button.playDownSound(minecraft.getSoundManager());
                        }
                    } else
                        inspectPhotograph(photograph);
                },
                b -> {
                    PhotographSlotButton button = (PhotographSlotButton) b;
                    if (button.isEditable && !button.getPhotograph().isEmpty()) {
                        sendButtonClick(side == Side.LEFT ? AlbumMenu.LEFT_PAGE_PHOTO_BUTTON : AlbumMenu.RIGHT_PAGE_PHOTO_BUTTON);
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                                Exposure.SoundEvents.PHOTOGRAPH_PLACE.get(), 0.7f, 1.1f));
                    }
                }, () -> getMenu().getPhotograph(side), getMenu().isAlbumEditable()) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return !isInAddingMode() && super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean isHovered() {
                return !isInAddingMode() && super.isHovered();
            }
        };
        addRenderableWidget(photographButton);

        Either<TextBox, TextBlock> noteWidget;
        if (getMenu().isAlbumEditable()) {
            TextBox textBox = new TextBox(font, note.getX(), note.getY(), note.getWidth(), note.getHeight(),
                    () -> getMenu().getPage(side).map(p -> p.getNote().left().orElseThrow()).orElse(""),
                    text -> onNoteChanged(side, text))
                    .setFontColor(MAIN_FONT_COLOR, MAIN_FONT_COLOR)
                    .setSelectionColor(SELECTION_COLOR, SELECTION_UNFOCUSED_COLOR);
            textBox.horizontalAlignment = HorizontalAlignment.CENTER;
            addRenderableWidget(textBox);
            noteWidget = Either.left(textBox);
        } else {
            TextBlock textBlock = new TextBlock(font, note.getX(), note.getY(),
                    note.getWidth(), note.getHeight(), getNoteComponent(side), this::handleComponentClicked);
            textBlock.fontColor = MAIN_FONT_COLOR;
            textBlock.alignment = HorizontalAlignment.CENTER;
            textBlock.drawShadow = false;

            //  TextBlock is rendered manually to not be a part of TAB navigation.
            //  addRenderableWidget(textBlock);

            noteWidget = Either.right(textBlock);
        }

        return new Page(side, page, photo, exposure, note, photographButton, noteWidget);
    }

    protected void onNoteChanged(Side side, String noteText) {
        getMenu().getPage(side).ifPresent(page -> {
            page.setNote(Either.left(noteText));
            int pageIndex = getMenu().getCurrentSpreadIndex() * 2 + side.getIndex();
            Packets.sendToServer(new AlbumSyncNoteC2SP(pageIndex, noteText));
        });
    }

    
    // RENDER
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        pager.update();

        if (enterSignModeButton != null)
            enterSignModeButton.visible = getMenu().canSignAlbum();

        boolean isInAddingPhotographMode = isInAddingMode();

        // Note should be hidden when adding photograph because it's drawn over the slots. Blit offset does not help.
        forEachPage(page -> page.getNoteWidget().visible = !isInAddingPhotographMode);

        for (Page page : pages) {
            page.photographButton.visible = !getMenu().getPhotograph(page.side).isEmpty()
                    || (!isInAddingPhotographMode && getMenu().isAlbumEditable());
        }

        inventoryLabelY = isInAddingPhotographMode ? getMenu().getPlayerInventorySlots().get(0).y - 12 : -999;

        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (Page page : pages) {
            AbstractWidget noteWidget = page.getNoteWidget();
            if (noteWidget instanceof TextBlock textBlock) {
                textBlock.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        if (isInAddingPhotographMode) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (Slot slot : getMenu().slots) {
                if (!slot.getItem().isEmpty() && !(slot.getItem().getItem() instanceof PhotographItem)) {
                    guiGraphics.blit(TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 350, 176, 404,
                            18, 18, 512, 512);
                }
            }
            RenderSystem.disableBlend();
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 15);
        super.renderLabels(guiGraphics, mouseX, mouseY);

        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (isInAddingMode() && hoveredSlot != null && !hoveredSlot.getItem()
                .isEmpty() && !(hoveredSlot.getItem().getItem() instanceof PhotographItem))
            return; // Do not onRender tooltips for greyed-out items

        if (!isInAddingMode()) {
            for (Page page : pages) {
                if (page.photographButton.isHoveredOrFocused()) {
                    page.photographButton.renderTooltip(guiGraphics, x, y);
                    return;
                }

                if (getMenu().isAlbumEditable() && page.isMouseOver(page.noteArea, x, y)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable("gui.exposure.album.note"));

                    if (!page.getNoteWidget().isFocused())
                        tooltip.add(Component.translatable("gui.exposure.album.left_click_to_edit"));

                    boolean hasText = page.noteWidget.left().map(box -> !box.getText().isEmpty()).orElse(false);
                    if (hasText)
                        tooltip.add(Component.translatable("gui.exposure.album.right_click_to_clear"));

                    guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), x, y);

                    return;
                }
            }
        }

        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected @NotNull List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltipLines = super.getTooltipFromContainerItem(stack);
        if (isInAddingMode() && hoveredSlot != null && hoveredSlot.getItem() == stack
                && stack.getItem() instanceof PhotographItem) {
            tooltipLines.add(Component.empty());
            tooltipLines.add(Component.translatable("gui.exposure.album.left_click_to_add"));
        }
        return tooltipLines;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, 0,
                imageWidth, imageHeight, 512, 512);

        if (enterSignModeButton != null && enterSignModeButton.visible) {
            guiGraphics.blit(TEXTURE, leftPos - 27, topPos + 14, 447, 0,
                    27, 28, 512, 512);
        }

        int currentSpreadIndex = getMenu().getCurrentSpreadIndex();
        drawPageNumbers(guiGraphics, currentSpreadIndex);

        if (isInAddingMode()) {
            @Nullable Side pageBeingAddedTo = getMenu().getSideBeingAddedTo();
            for (Page page : pages) {
                if (page.side == pageBeingAddedTo) {
                    guiGraphics.blit(TEXTURE, page.photoArea.getX(), page.photoArea.getY(), 10, 0, 296,
                            page.photoArea.getWidth(), page.photoArea.getHeight(), 512, 512);
                    break;
                }
            }

            AlbumPlayerInventorySlot firstSlot = getMenu().getPlayerInventorySlots().get(0);
            int x = firstSlot.x - 8;
            int y = firstSlot.y - 18;
            guiGraphics.blit(TEXTURE, leftPos + x, topPos + y, 10, 0, 404, 176, 100, 512, 512);
        }
    }

    protected void drawPageNumbers(GuiGraphics guiGraphics, int currentSpreadIndex) {
        Font font = minecraft.font;

        String leftPageNumber = Integer.toString(currentSpreadIndex * 2 + 1);
        String rightPageNumber = Integer.toString(currentSpreadIndex * 2 + 2);

        guiGraphics.drawString(font, leftPageNumber, leftPos + 71 + (8 - font.width(leftPageNumber) / 2),
                topPos + 167, SECONDARY_FONT_COLOR, false);

        guiGraphics.drawString(font, rightPageNumber, leftPos + 212 + (8 - font.width(rightPageNumber) / 2),
                topPos + 167, SECONDARY_FONT_COLOR, false);
    }


    // CONTROLS:

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInAddingMode()) {
            if (!isHoveringOverInventory(mouseX, mouseY)
                && (!hasClickedOutside(mouseX, mouseY, leftPos, topPos, button) || getMenu().getCarried().isEmpty())) {
                sendButtonClick(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (Page page : pages) {
            if (getMenu().isAlbumEditable() && button == InputConstants.MOUSE_BUTTON_RIGHT && page.isMouseOver(page.noteArea, mouseX, mouseY)) {
                page.noteWidget.ifLeft(box -> {
                    box.setText(""); // Clear the note
                });
                return true;
            }
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        for (Page page : pages) {
            AbstractWidget noteWidget = page.getNoteWidget();
            if (noteWidget instanceof TextBlock textBlock && textBlock.mouseClicked(mouseX, mouseY, button)) {
                handled = true;
                break;
            }
        }

        for (Page page : pages) {
            if (page.getNoteWidget().isFocused() && !page.isMouseOver(page.noteArea, mouseX, mouseY)) {
                setFocused(null);
                return true;
            }
        }

        if (!(getFocused() instanceof TextBox))
            setFocused(null); // Clear focus on mouse click because it's annoying. But keep on textbox to type.

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isQuickCrafting && !getMenu().getCarried().isEmpty() && getMenu().getCarried().getCount() == 1) {
            isQuickCrafting = false; // Fixes weird issue with carried item not placing when dragging slightly
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean handleComponentClicked(@Nullable Style style) {
        if (style == null)
            return false;

        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null)
            return false;
        else if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            String pageIndexStr = clickEvent.getValue();
            int pageIndex = Integer.parseInt(pageIndexStr) - 1;
            forcePage(pageIndex);
            return true;
        }

        boolean handled = super.handleComponentClicked(style);
        if (handled && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND)
            onClose();
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isInAddingMode())
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        else
            return this.getFocused() != null && this.isDragging() && button == 0
                    && this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void sendButtonClick(int buttonId) {
        getMenu().clickMenuButton(player, buttonId);
        gameMode.handleInventoryButtonClick(getMenu().containerId, buttonId);

        if (buttonId == AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON)
            setFocused(null);

        if (buttonId == AlbumMenu.PREVIOUS_PAGE_BUTTON || buttonId == AlbumMenu.NEXT_PAGE_BUTTON) {
            for (Page page : pages) {
                page.noteWidget
                        .ifLeft(TextBox::setCursorToEnd)
                        .ifRight(textBlock -> textBlock.setMessage(getNoteComponent(page.side)));
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean isHoveringOverInventory(double mouseX, double mouseY) {
        if (!isInAddingMode())
            return false;

        AlbumPlayerInventorySlot firstSlot = getMenu().getPlayerInventorySlots().get(0);
        int x = firstSlot.x - 8;
        int y = firstSlot.y - 18;
        return isHovering(x, y, 176, 100, mouseX, mouseY);
    }

    protected boolean isHoveringOverSignElement(double mouseX, double mouseY) {
        return enterSignModeButton == null
                || (enterSignModeButton.visible && isHovering(leftPos - 27, topPos + 14, 27, 28, mouseX, mouseY));
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && !isHoveringOverInventory(mouseX, mouseY)
                && !isHoveringOverSignElement(mouseX, mouseY);
    }

    @SuppressWarnings("UnusedReturnValue")
    protected boolean forcePage(int pageIndex) {
        try {
            int newSpreadIndex = pageIndex / 2;

            if (newSpreadIndex == getMenu().getCurrentSpreadIndex() || newSpreadIndex < 0
                    || newSpreadIndex > getMenu().getPages().size() / 2) {
                return false;
            }

            PagingDirection pagingDirection = newSpreadIndex < getMenu().getCurrentSpreadIndex()
                    ? PagingDirection.PREVIOUS : PagingDirection.NEXT;

            int pageChanges = 0; // Safeguard against infinite loop. Probably not needed. But I don't mind it.
            while (newSpreadIndex != getMenu().getCurrentSpreadIndex() || !pager.canChangePage(pagingDirection)) {
                if (pageChanges > 16)
                    break;

                pager.changePage(pagingDirection);
                pageChanges++;
            }
            return true;
        } catch (Exception e) {
            Exposure.LOGGER.error("Cannot force page: {}", e.toString());
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_TAB)
            return super.keyPressed(keyCode, scanCode, modifiers);

        for (Page page : pages) {
            AbstractWidget widget = page.noteWidget.map(box -> box, block -> block);
            if (widget.isFocused()) {
                if (keyCode == InputConstants.KEY_ESCAPE) {
                    this.setFocused(null);
                    return true;
                }

                return widget.keyPressed(keyCode, scanCode, modifiers);
            }
        }

        if (isInAddingMode() && (minecraft.options.keyInventory.matches(keyCode, scanCode)
                || keyCode == InputConstants.KEY_ESCAPE)) {
            sendButtonClick(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);
            return true;
        }

        return pager.handleKeyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (Page page : pages) {
            if (page.noteWidget.map(box -> box, block -> block).isFocused())
                return super.keyReleased(keyCode, scanCode, modifiers);
        }

        return pager.handleKeyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }


    // MISC:
    
    protected void inspectPhotograph(ItemStack photograph) {
        if (!(photograph.getItem() instanceof PhotographItem))
            return;

        minecraft.setScreen(new AlbumPhotographScreen(this, List.of(new ItemAndStack<>(photograph))));
        minecraft.getSoundManager()
                .play(SimpleSoundInstance.forUI(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(),
                        player.level().getRandom().nextFloat() * 0.2f + 1.3f, 0.75f));
    }

    @NotNull
    protected Component getNoteComponent(Side page) {
        return getMenu().getPage(page)
                .map(AlbumPage::getNote)
                .map(n -> n.map(Component::literal, comp -> comp))
                .orElse(Component.empty());
    }

    protected void enterSignMode() {
        if (isInAddingMode())
            sendButtonClick(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);

        minecraft.setScreen(new AlbumSigningScreen(this, TEXTURE, 512, 512));
    }

    protected boolean isInAddingMode() {
        return getMenu().isInAddingPhotographMode();
    }
    
    protected void forEachPage(Consumer<Page> pageAction) {
        for (Page page : pages) {
            pageAction.accept(page);
        }
    }

    protected class Page {
        public final Side side;
        public final Rect2i pageArea;
        public final Rect2i photoArea;
        public final Rect2i exposureArea;
        public final Rect2i noteArea;

        public final PhotographSlotButton photographButton;
        public final Either<TextBox, TextBlock> noteWidget;

        private Page(Side side, Rect2i pageArea, Rect2i photoArea, Rect2i exposureArea, Rect2i noteArea,
                     PhotographSlotButton photographButton, Either<TextBox, TextBlock> noteWidget) {
            this.side = side;
            this.pageArea = pageArea;
            this.photoArea = photoArea;
            this.exposureArea = exposureArea;
            this.noteArea = noteArea;
            this.photographButton = photographButton;
            this.noteWidget = noteWidget;
        }

        public boolean isMouseOver(Rect2i area, double mouseX, double mouseY) {
            return isHovering(area.getX() - leftPos, area.getY() - topPos,
                    area.getWidth(), area.getHeight(), mouseX, mouseY);
        }

        public AbstractWidget getNoteWidget() {
            return noteWidget.map(box -> box, block -> block);
        }
    }
}

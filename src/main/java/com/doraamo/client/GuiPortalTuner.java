package com.doraamo.client;

import com.doraamo.config.DimensionConfig;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.destination.DestinationLocator;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.network.PacketHandler;
import com.doraamo.network.SaveTunerPayload;
import com.doraamo.network.ValidateTunerPayload;
import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import com.doraamo.util.SearchFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import com.doraamo.util.RegistryHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.biome.Biome;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GuiPortalTuner extends Screen {

    private final InteractionHand hand;
    private final DestinationSettings settings;
    private final BlockPos portalPos;
    private final boolean hasExistingBinding;
    private final DestinationSettings originalBinding;

    private EditBox filterField;
    private EditBox fieldX;
    private EditBox fieldY;
    private EditBox fieldZ;
    private String lastFilterText = "";

    private final List<DimensionConfig.DimensionEntry> dimEntries = new ArrayList<>();
    private final List<String> contentLabels = new ArrayList<>();
    private final List<String> contentBiomeKeys = new ArrayList<>();
    private final List<String> contentStructureIds = new ArrayList<>();

    private int dimScroll;
    private int dimCursor;
    private int contentScroll;
    private int contentCursor;
    private FocusPanel focus = FocusPanel.CONTENT;

    private final int rowH = 12;
    private final int pad = 6;

    private int dimLeft, dimTop, dimW, dimH, dimRows;
    private int midLeft, midTop, midW, midH;
    private int contentTop, contentH, contentRows;
    private int rightLeft, rightTop, rightW, rightH;
    private int infoTop;

    private boolean searching;
    private boolean found;
    private boolean placeSafe;
    private boolean showForceOption;
    private PortalDoorPlacer.PlaceHazard hazard = PortalDoorPlacer.PlaceHazard.NONE;
    private String statusMessage = "";
    private int statusColor = 0xA0A0A0;
    private int foundX, foundY, foundZ;

    private Button exploreButton;
    private Button saveButton;
    private Button forceButton;
    private Button coordsModeButton;
    private Button biomeModeButton;
    private Button structureModeButton;

    private enum FocusPanel { DIM, CONTENT }

    public GuiPortalTuner(InteractionHand hand, DestinationSettings settings, BlockPos portalPos, boolean hasExistingBinding) {
        super(Component.translatable(LangKeys.GUI_TITLE));
        this.hand = hand;
        this.settings = settings;
        this.portalPos = portalPos.immutable();
        this.hasExistingBinding = hasExistingBinding;
        this.originalBinding = hasExistingBinding ? settings.copy() : null;
        if (settings.mode == DestinationSettings.Mode.SCALED) {
            settings.mode = DestinationSettings.Mode.COORDS;
        }
    }

    @Override
    protected void init() {
        super.init();
        ClientSetup.refreshLanguagePreference();
        clearWidgets();

        int margin = 8;
        int topBar = 36;
        int usableW = width - margin * 2;
        int usableH = height - topBar - margin;

        dimLeft = margin;
        dimTop = topBar;
        dimW = Math.max(70, usableW / 4);
        dimH = usableH;
        dimRows = Math.max(4, (dimH - 16 - pad * 2) / rowH);

        midLeft = dimLeft + dimW + 4;
        midW = Math.max(120, usableW / 2);
        midTop = topBar;
        midH = usableH;

        rightLeft = midLeft + midW + 4;
        rightW = Math.max(100, width - margin - rightLeft);
        rightTop = topBar;
        rightH = usableH;

        int modeRowH = 22;
        int filterH = 18;
        contentTop = midTop + modeRowH + filterH + 8;
        contentH = midTop + midH - contentTop;
        contentRows = Math.max(3, (contentH - pad) / rowH);

        int btnH = 20;
        int by = rightTop;
        exploreButton = addRenderableWidget(Button.builder(Component.translatable(LangKeys.GUI_EXPLORE), b -> startExplore())
                .bounds(rightLeft, by, rightW, btnH).build());
        by += btnH + 4;
        saveButton = addRenderableWidget(Button.builder(Component.translatable(LangKeys.GUI_SAVE), b -> trySave(false))
                .bounds(rightLeft, by, rightW, btnH).build());
        by += btnH + 4;
        forceButton = addRenderableWidget(Button.builder(Component.translatable(LangKeys.GUI_FORCE_SAVE), b -> doSave(true))
                .bounds(rightLeft, by, rightW, btnH).build());
        infoTop = by + btnH + 10;

        int modeBtnW = (midW - 8) / 3;
        coordsModeButton = addRenderableWidget(Button.builder(Component.translatable(LangKeys.modeKey("coords")),
                b -> switchMode(DestinationSettings.Mode.COORDS)).bounds(midLeft, midTop, modeBtnW, 20).build());
        biomeModeButton = addRenderableWidget(Button.builder(Component.translatable(LangKeys.modeKey("biome")),
                b -> switchMode(DestinationSettings.Mode.BIOME)).bounds(midLeft + modeBtnW + 4, midTop, modeBtnW, 20).build());
        structureModeButton = addRenderableWidget(Button.builder(Component.translatable(LangKeys.modeKey("structure")),
                b -> switchMode(DestinationSettings.Mode.STRUCTURE)).bounds(midLeft + (modeBtnW + 4) * 2, midTop, modeBtnW, 20).build());

        filterField = new EditBox(font, midLeft, midTop + modeRowH + 2, midW, filterH, Component.literal(""));
        filterField.setMaxLength(64);
        filterField.setValue(lastFilterText);
        addRenderableWidget(filterField);

        int fieldW = Math.max(50, (midW - 40) / 3);
        fieldX = new EditBox(font, midLeft + 14, contentTop + 8, fieldW, 18, Component.literal("X"));
        fieldY = new EditBox(font, midLeft + 14 + fieldW + 20, contentTop + 8, fieldW, 18, Component.literal("Y"));
        fieldZ = new EditBox(font, midLeft + 14 + (fieldW + 20) * 2, contentTop + 8, fieldW, 18, Component.literal("Z"));
        fieldX.setMaxLength(12);
        fieldY.setMaxLength(12);
        fieldZ.setMaxLength(12);
        fieldX.setValue(Integer.toString(settings.x));
        fieldY.setValue(Integer.toString(settings.y));
        fieldZ.setValue(Integer.toString(settings.z));
        addRenderableWidget(fieldX);
        addRenderableWidget(fieldY);
        addRenderableWidget(fieldZ);

        if (statusMessage.isEmpty()) {
            statusMessage = Component.translatable(LangKeys.GUI_STATUS_NEED_SEARCH).getString();
            statusColor = 0xA0A0A0;
        }

        rebuildDimList();
        rebuildContentList();
        refreshWidgets();
    }

    private void switchMode(DestinationSettings.Mode mode) {
        settings.mode = mode;
        rebuildContentList();
        invalidateValidation();
    }

    private void rebuildDimList() {
        dimEntries.clear();
        dimEntries.addAll(DimensionConfig.getGuiDimensions());
        dimCursor = 0;
        for (int i = 0; i < dimEntries.size(); i++) {
            if (dimEntries.get(i).dimKey.equals(DimUtil.normalize(settings.dimension))) {
                dimCursor = i;
                break;
            }
        }
        ensureDimCursorVisible();
    }

    private void rebuildContentList() {
        contentLabels.clear();
        contentBiomeKeys.clear();
        contentStructureIds.clear();
        String filter = filterField != null ? filterField.getValue() : lastFilterText;

        if (settings.mode == DestinationSettings.Mode.BIOME) {
            for (Biome b : DestinationLocator.allBiomes()) {
                ResourceLocation key = RegistryHelper.biomeKey(b);
                if (key == null) continue;
                String reg = key.toString();
                String label = BiomeNames.localize(b);
                String path = key.getPath();
                String[] tokens = DisplayCatalog.searchTokens(DisplayCatalog.Kind.BIOME, reg, label, path);
                if (SearchFilter.matches(filter, tokens)) {
                    contentLabels.add(label);
                    contentBiomeKeys.add(reg);
                }
            }
            contentCursor = 0;
            for (int i = 0; i < contentBiomeKeys.size(); i++) {
                if (contentBiomeKeys.get(i).equals(settings.biomeKey)) {
                    contentCursor = i;
                    break;
                }
            }
        } else if (settings.mode == DestinationSettings.Mode.STRUCTURE) {
            for (String id : DisplayCatalog.structuresForDim(settings.dimension)) {
                String label = localizeStructure(id);
                String[] tokens = DisplayCatalog.searchTokens(DisplayCatalog.Kind.STRUCTURE, id, label);
                if (SearchFilter.matches(filter, tokens)) {
                    contentStructureIds.add(id);
                    contentLabels.add(label);
                }
            }
            contentCursor = 0;
            for (int i = 0; i < contentStructureIds.size(); i++) {
                if (contentStructureIds.get(i).equals(settings.structureName)) {
                    contentCursor = i;
                    break;
                }
            }
        }
        contentScroll = Math.min(contentScroll, Math.max(0, contentLabels.size() - contentRows));
        ensureContentCursorVisible();
    }

    private void ensureContentCursorVisible() {
        if (contentCursor < contentScroll) contentScroll = contentCursor;
        else if (contentCursor >= contentScroll + contentRows) contentScroll = contentCursor - contentRows + 1;
        contentScroll = Math.max(0, Math.min(contentScroll, Math.max(0, contentLabels.size() - contentRows)));
    }

    private void ensureDimCursorVisible() {
        if (dimCursor < dimScroll) dimScroll = dimCursor;
        else if (dimCursor >= dimScroll + dimRows) dimScroll = dimCursor - dimRows + 1;
        dimScroll = Math.max(0, Math.min(dimScroll, Math.max(0, dimEntries.size() - dimRows)));
    }

    private void invalidateValidation() {
        searching = false;
        found = false;
        placeSafe = false;
        showForceOption = false;
        hazard = PortalDoorPlacer.PlaceHazard.NONE;
        statusMessage = Component.translatable(LangKeys.GUI_STATUS_NEED_SEARCH).getString();
        statusColor = 0xA0A0A0;
        refreshWidgets();
    }

    public void onValidateResult(boolean found, int x, int y, int z, PortalDoorPlacer.PlaceHazard hazard) {
        searching = false;
        this.found = found;
        this.hazard = hazard;
        this.foundX = x;
        this.foundY = y;
        this.foundZ = z;
        this.placeSafe = found && hazard == PortalDoorPlacer.PlaceHazard.NONE;
        this.showForceOption = false;

        if (!found) {
            statusMessage = Component.translatable(LangKeys.GUI_STATUS_NOT_FOUND).getString();
            statusColor = 0xFF5555;
        } else if (placeSafe) {
            statusMessage = Component.translatable(LangKeys.GUI_STATUS_FOUND_SAFE, x, y, z).getString();
            statusColor = 0x55FF55;
            applyFoundCoords(x, y, z);
        } else {
            statusMessage = Component.translatable(LangKeys.GUI_STATUS_FOUND_UNSAFE, x, y, z,
                    Component.translatable(hazardLangKey(hazard))).getString();
            statusColor = 0xFFFF55;
            applyFoundCoords(x, y, z);
        }
        refreshWidgets();
    }

    private void applyFoundCoords(int x, int y, int z) {
        if (settings.mode == DestinationSettings.Mode.COORDS) {
            fieldX.setValue(Integer.toString(x));
            fieldY.setValue(Integer.toString(y));
            fieldZ.setValue(Integer.toString(z));
        } else {
            settings.x = x;
            settings.y = y;
            settings.z = z;
        }
    }

    private static String hazardLangKey(PortalDoorPlacer.PlaceHazard hazard) {
        switch (hazard) {
            case FLOATING: return LangKeys.GUI_HAZARD_FLOATING;
            case WALL: return LangKeys.GUI_HAZARD_WALL;
            case FLOODED: return LangKeys.GUI_HAZARD_FLOODED;
            case LAVA: return LangKeys.GUI_HAZARD_LAVA;
            case FIRE: return LangKeys.GUI_HAZARD_FIRE;
            case OUT_OF_BOUNDS: return LangKeys.GUI_HAZARD_BOUNDS;
            case NONE:
            default: return LangKeys.GUI_HAZARD_NONE;
        }
    }

    private void refreshWidgets() {
        boolean coords = settings.mode == DestinationSettings.Mode.COORDS;
        fieldX.visible = coords;
        fieldY.visible = coords;
        fieldZ.visible = coords;
        filterField.visible = !coords;
        filterField.active = !coords;

        coordsModeButton.active = settings.mode != DestinationSettings.Mode.COORDS;
        biomeModeButton.active = settings.mode != DestinationSettings.Mode.BIOME;
        structureModeButton.active = settings.mode != DestinationSettings.Mode.STRUCTURE;
        exploreButton.active = !searching;
        saveButton.active = found && !searching;
        forceButton.visible = showForceOption;
        forceButton.active = showForceOption && found && !placeSafe && !searching;
    }

    private static String localizeStructure(String structureId) {
        return DisplayCatalog.displayStructure(structureId);
    }

    private static String localizeDimension(String dimensionKey) {
        if (DimUtil.isBlank(dimensionKey)) {
            return Component.translatable(LangKeys.DIMENSION_BLANK).getString();
        }
        return DisplayCatalog.displayDimension(dimensionKey);
    }

    private static String localizeMode(DestinationSettings.Mode mode) {
        return Component.translatable(LangKeys.modeKey(mode.name())).getString();
    }

    private String formatBinding(DestinationSettings s) {
        if (s == null) return Component.translatable(LangKeys.GUI_BOUND_NONE).getString();
        String dim = localizeDimension(s.dimension);
        String mode = localizeMode(s.mode);
        if (s.mode == DestinationSettings.Mode.COORDS || s.mode == DestinationSettings.Mode.SCALED) {
            return Component.translatable(LangKeys.GUI_BOUND_COORDS, dim, mode, s.x, s.y, s.z).getString();
        }
        if (s.mode == DestinationSettings.Mode.BIOME) {
            ResourceLocation biomeLoc = ResourceLocation.tryParse(s.biomeKey.contains(":") ? s.biomeKey : "minecraft:" + s.biomeKey);
            Biome biome = biomeLoc != null ? RegistryHelper.biomeByKey(biomeLoc) : null;
            String biomeName = biome != null ? BiomeNames.localize(biome) : s.biomeKey;
            return Component.translatable(LangKeys.GUI_BOUND_BIOME, dim, mode, biomeName).getString();
        }
        return Component.translatable(LangKeys.GUI_BOUND_STRUCTURE, dim, mode, localizeStructure(s.structureName)).getString();
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public void tick() {
        if (filterField.visible) {
            String t = filterField.getValue();
            if (!t.equals(lastFilterText)) {
                lastFilterText = t;
                rebuildContentList();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY;
        int dir = delta > 0 ? -1 : 1;
        if (mouseX >= dimLeft && mouseX < dimLeft + dimW && mouseY >= dimTop && mouseY < dimTop + dimH) {
            dimScroll = clampScroll(dimScroll + dir, dimEntries.size(), dimRows);
            focus = FocusPanel.DIM;
            return true;
        }
        if (mouseX >= midLeft && mouseX < midLeft + midW && mouseY >= contentTop && mouseY < contentTop + contentH
                && settings.mode != DestinationSettings.Mode.COORDS) {
            contentScroll = clampScroll(contentScroll + dir, contentLabels.size(), contentRows);
            focus = FocusPanel.CONTENT;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static int clampScroll(int scroll, int size, int rows) {
        return Math.max(0, Math.min(scroll, Math.max(0, size - rows)));
    }

    private void selectDim(int index) {
        if (index < 0 || index >= dimEntries.size()) return;
        dimCursor = index;
        ensureDimCursorVisible();
        String key = dimEntries.get(index).dimKey;
        if (!DimUtil.normalize(settings.dimension).equals(key)) {
            settings.dimension = key;
            rebuildContentList();
            invalidateValidation();
        }
    }

    private void selectContent(int index) {
        if (settings.mode == DestinationSettings.Mode.COORDS) return;
        if (index < 0 || index >= contentLabels.size()) return;
        contentCursor = index;
        ensureContentCursorVisible();
        if (settings.mode == DestinationSettings.Mode.BIOME) {
            String key = contentBiomeKeys.get(index);
            if (!key.equals(settings.biomeKey)) {
                settings.biomeKey = key;
                invalidateValidation();
            }
        } else {
            String id = contentStructureIds.get(index);
            if (!id.equals(settings.structureName)) {
                settings.structureName = id;
                invalidateValidation();
            }
        }
    }

    private void confirmFocused() {
        if (focus == FocusPanel.DIM) selectDim(dimCursor);
        else selectContent(contentCursor);
    }

    private void trySave(boolean force) {
        if (!found) return;
        readCoordFields();
        if (!placeSafe && !force) {
            showForceOption = true;
            statusMessage = Component.translatable(LangKeys.GUI_STATUS_SAVE_BLOCKED,
                    Component.translatable(hazardLangKey(hazard))).getString();
            statusColor = 0xFFFF55;
            refreshWidgets();
            return;
        }
        doSave(force);
    }

    private void doSave(boolean force) {
        readCoordFields();
        if (settings.mode != DestinationSettings.Mode.COORDS && found) {
            settings.x = foundX;
            settings.y = foundY;
            settings.z = foundZ;
        }
        settings.forceUnsafe = force;
        PacketHandler.sendToServer(new SaveTunerPayload(hand, settings, portalPos, force));
        minecraft.setScreen(null);
    }

    private void startExplore() {
        readCoordFields();
        if (settings.mode == DestinationSettings.Mode.BIOME) {
            if (contentBiomeKeys.isEmpty()) {
                onValidateResult(false, 0, 0, 0, PortalDoorPlacer.PlaceHazard.NONE);
                return;
            }
            if (!contentBiomeKeys.contains(settings.biomeKey)) {
                settings.biomeKey = contentBiomeKeys.get(Math.max(0, contentCursor));
            }
        }
        if (settings.mode == DestinationSettings.Mode.STRUCTURE) {
            if (contentStructureIds.isEmpty()) {
                onValidateResult(false, 0, 0, 0, PortalDoorPlacer.PlaceHazard.NONE);
                return;
            }
            if (!contentStructureIds.contains(settings.structureName)) {
                settings.structureName = contentStructureIds.get(Math.max(0, contentCursor));
            }
        }
        searching = true;
        found = false;
        placeSafe = false;
        showForceOption = false;
        statusMessage = Component.translatable(LangKeys.GUI_STATUS_SEARCHING).getString();
        statusColor = 0xFFFF55;
        refreshWidgets();
        PacketHandler.sendToServer(new ValidateTunerPayload(settings));
    }

    private void readCoordFields() {
        try { settings.x = Integer.parseInt(fieldX.getValue().trim()); } catch (Exception ignored) { }
        try { settings.y = Integer.parseInt(fieldY.getValue().trim()); } catch (Exception ignored) { }
        try { settings.z = Integer.parseInt(fieldZ.getValue().trim()); } catch (Exception ignored) { }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (filterField.visible && filterField.isFocused() && filterField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (fieldX.visible) {
            if (fieldX.keyPressed(keyCode, scanCode, modifiers)
                    || fieldY.keyPressed(keyCode, scanCode, modifiers)
                    || fieldZ.keyPressed(keyCode, scanCode, modifiers)) {
                invalidateValidation();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) { focus = FocusPanel.DIM; return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { focus = FocusPanel.CONTENT; return true; }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (focus == FocusPanel.DIM) {
                dimCursor = Math.max(0, dimCursor - 1);
                ensureDimCursorVisible();
            } else if (settings.mode != DestinationSettings.Mode.COORDS && !contentLabels.isEmpty()) {
                contentCursor = Math.max(0, contentCursor - 1);
                ensureContentCursorVisible();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (focus == FocusPanel.DIM) {
                dimCursor = Math.min(dimEntries.size() - 1, dimCursor + 1);
                ensureDimCursorVisible();
            } else if (settings.mode != DestinationSettings.Mode.COORDS && !contentLabels.isEmpty()) {
                contentCursor = Math.min(contentLabels.size() - 1, contentCursor + 1);
                ensureContentCursorVisible();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirmFocused();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (filterField.visible && filterField.isFocused() && filterField.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (fieldX.visible && (fieldX.isFocused() || fieldY.isFocused() || fieldZ.isFocused())) {
            if (fieldX.charTyped(codePoint, modifiers) || fieldY.charTyped(codePoint, modifiers)
                    || fieldZ.charTyped(codePoint, modifiers)) {
                invalidateValidation();
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (filterField.mouseClicked(mouseX, mouseY, button)) return true;
        if (fieldX.visible) {
            if (fieldX.mouseClicked(mouseX, mouseY, button)) return true;
            if (fieldY.mouseClicked(mouseX, mouseY, button)) return true;
            if (fieldZ.mouseClicked(mouseX, mouseY, button)) return true;
        }
        if (button == 0) {
            if (mouseX >= dimLeft && mouseX < dimLeft + dimW && mouseY >= dimTop && mouseY < dimTop + dimH) {
                focus = FocusPanel.DIM;
                int row = (int) ((mouseY - dimTop - pad) / rowH);
                selectDim(dimScroll + row);
                return true;
            }
            if (settings.mode != DestinationSettings.Mode.COORDS
                    && mouseX >= midLeft && mouseX < midLeft + midW
                    && mouseY >= contentTop && mouseY < contentTop + contentH) {
                focus = FocusPanel.CONTENT;
                int row = (int) ((mouseY - contentTop - pad) / rowH);
                selectContent(contentScroll + row);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        String boundLine = hasExistingBinding
                ? Component.translatable(LangKeys.GUI_BOUND_CURRENT, formatBinding(originalBinding)).getString()
                : Component.translatable(LangKeys.GUI_BOUND_NONE).getString();
        graphics.drawCenteredString(font, boundLine, width / 2, 18, hasExistingBinding ? 0xFFAA00 : 0x808080);
        if (hasExistingBinding) {
            graphics.drawCenteredString(font, Component.translatable(LangKeys.GUI_BOUND_OVERWRITE_HINT), width / 2, 28, 0xFF5555);
        }

        graphics.fill(dimLeft, dimTop, dimLeft + dimW, dimTop + dimH, 0x99000000);
        if (focus == FocusPanel.DIM) graphics.fill(dimLeft, dimTop, dimLeft + 2, dimTop + dimH, 0xFFFFFF55);
        graphics.drawString(font, Component.translatable(LangKeys.GUI_DIM_LIST), dimLeft + pad, dimTop + 2, 0xA0A0A0);
        int dimListTop = dimTop + 14;
        for (int i = 0; i < dimRows; i++) {
            int idx = dimScroll + i;
            if (idx >= dimEntries.size()) break;
            DimensionConfig.DimensionEntry e = dimEntries.get(idx);
            boolean selected = e.dimKey.equals(DimUtil.normalize(settings.dimension));
            boolean hovered = idx == dimCursor;
            int y = dimListTop + i * rowH;
            if (selected) graphics.fill(dimLeft + 2, y, dimLeft + dimW - 2, y + rowH, 0x33FFFF00);
            else if (hovered && focus == FocusPanel.DIM) graphics.fill(dimLeft + 2, y, dimLeft + dimW - 2, y + rowH, 0x22FFFFFF);
            graphics.drawString(font, localizeDimension(e.dimKey), dimLeft + pad, y + 2, selected ? 0xFFFF55 : 0xE0E0E0);
        }

        if (filterField.visible) {
            filterField.render(graphics, mouseX, mouseY, partialTicks);
            if (filterField.getValue().isEmpty() && !filterField.isFocused()) {
                graphics.drawString(font, Component.translatable(LangKeys.GUI_FILTER_HINT),
                        filterField.getX() + 4, filterField.getY() + 5, 0x707070);
            }
        }

        graphics.fill(midLeft, contentTop, midLeft + midW, contentTop + contentH, 0x99000000);
        if (focus == FocusPanel.CONTENT) graphics.fill(midLeft, contentTop, midLeft + 2, contentTop + contentH, 0xFFFFFF55);

        if (settings.mode == DestinationSettings.Mode.COORDS) {
            graphics.drawString(font, "X", fieldX.getX() - 12, fieldX.getY() + 5, 0xFFFFFF);
            graphics.drawString(font, "Y", fieldY.getX() - 12, fieldY.getY() + 5, 0xFFFFFF);
            graphics.drawString(font, "Z", fieldZ.getX() - 12, fieldZ.getY() + 5, 0xFFFFFF);
            fieldX.render(graphics, mouseX, mouseY, partialTicks);
            fieldY.render(graphics, mouseX, mouseY, partialTicks);
            fieldZ.render(graphics, mouseX, mouseY, partialTicks);
        } else {
            for (int i = 0; i < contentRows; i++) {
                int idx = contentScroll + i;
                if (idx >= contentLabels.size()) break;
                boolean selected = settings.mode == DestinationSettings.Mode.BIOME
                        ? contentBiomeKeys.get(idx).equals(settings.biomeKey)
                        : contentStructureIds.get(idx).equals(settings.structureName);
                boolean hovered = idx == contentCursor;
                int y = contentTop + pad + i * rowH;
                if (selected) graphics.fill(midLeft + 2, y, midLeft + midW - 2, y + rowH, 0x33FFFF00);
                else if (hovered && focus == FocusPanel.CONTENT) graphics.fill(midLeft + 2, y, midLeft + midW - 2, y + rowH, 0x22FFFFFF);
                graphics.drawString(font, contentLabels.get(idx), midLeft + pad, y + 2, selected ? 0xFFFF55 : 0xE0E0E0);
            }
        }

        graphics.fill(rightLeft, infoTop, rightLeft + rightW, rightTop + rightH, 0x66000000);
        int iy = infoTop + 4;
        graphics.drawString(font,
                Component.translatable(LangKeys.GUI_STATUS, localizeDimension(settings.dimension), localizeMode(settings.mode)),
                rightLeft + 4, iy, 0xA0A0A0);
        iy += 14;
        for (FormattedCharSequence line : font.split(Component.literal(statusMessage), rightW - 8)) {
            graphics.drawString(font, line, rightLeft + 4, iy, statusColor);
            iy += 12;
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

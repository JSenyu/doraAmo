package com.doraamo.client;

import com.doraamo.DoraAmo;
import com.doraamo.config.DimensionConfig;
import com.doraamo.config.catalog.DisplayCatalog;
import com.doraamo.destination.DestinationLocator;
import com.doraamo.destination.DestinationSettings;
import com.doraamo.network.PacketHandler;
import com.doraamo.network.PacketSaveTuner;
import com.doraamo.network.PacketValidateTuner;
import com.doraamo.portal.PortalDoorPlacer;
import com.doraamo.util.DimUtil;
import com.doraamo.util.LangKeys;
import com.doraamo.util.SearchFilter;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiPortalTuner extends Screen {

    private static final int ID_MODE_COORDS = 10;
    private static final int ID_MODE_BIOME = 11;
    private static final int ID_MODE_STRUCTURE = 12;
    private static final int ID_EXPLORE = 90;
    private static final int ID_SAVE = 100;
    private static final int ID_FORCE = 101;

    private enum FocusPanel { DIM, CONTENT }

    private final Hand hand;
    private final DestinationSettings settings;
    private final BlockPos portalPos;
    private final boolean hasExistingBinding;
    private final DestinationSettings originalBinding;

    private TextFieldWidget filterField;
    private TextFieldWidget fieldX;
    private TextFieldWidget fieldY;
    private TextFieldWidget fieldZ;
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

    public GuiPortalTuner(Hand hand, DestinationSettings settings, BlockPos portalPos, boolean hasExistingBinding) {
        super(new TranslationTextComponent(LangKeys.GUI_TITLE));
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
        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.buttons.clear();
        this.children.clear();

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
        addButton(new Button(rightLeft, by, rightW, btnH, new TranslationTextComponent(LangKeys.GUI_EXPLORE), b -> startExplore()));
        by += btnH + 4;
        addButton(new Button(rightLeft, by, rightW, btnH, new TranslationTextComponent(LangKeys.GUI_SAVE), b -> trySave(false)));
        by += btnH + 4;
        addButton(new Button(rightLeft, by, rightW, btnH, new TranslationTextComponent(LangKeys.GUI_FORCE_SAVE), b -> doSave(true)));
        infoTop = by + btnH + 10;

        int modeBtnW = (midW - 8) / 3;
        addButton(new Button(midLeft, midTop, modeBtnW, 20, new TranslationTextComponent(LangKeys.modeKey("coords")),
                b -> switchMode(DestinationSettings.Mode.COORDS)));
        addButton(new Button(midLeft + modeBtnW + 4, midTop, modeBtnW, 20, new TranslationTextComponent(LangKeys.modeKey("biome")),
                b -> switchMode(DestinationSettings.Mode.BIOME)));
        addButton(new Button(midLeft + (modeBtnW + 4) * 2, midTop, modeBtnW, 20, new TranslationTextComponent(LangKeys.modeKey("structure")),
                b -> switchMode(DestinationSettings.Mode.STRUCTURE)));

        filterField = new TextFieldWidget(font, midLeft, midTop + modeRowH + 2, midW, filterH, new StringTextComponent(""));
        filterField.setMaxLength(64);
        filterField.setValue(lastFilterText);
        children.add(filterField);

        int fieldW = Math.max(50, (midW - 40) / 3);
        fieldX = new TextFieldWidget(font, midLeft + 14, contentTop + 8, fieldW, 18, new StringTextComponent("X"));
        fieldY = new TextFieldWidget(font, midLeft + 14 + fieldW + 20, contentTop + 8, fieldW, 18, new StringTextComponent("Y"));
        fieldZ = new TextFieldWidget(font, midLeft + 14 + (fieldW + 20) * 2, contentTop + 8, fieldW, 18, new StringTextComponent("Z"));
        fieldX.setMaxLength(12);
        fieldY.setMaxLength(12);
        fieldZ.setMaxLength(12);
        fieldX.setValue(Integer.toString(settings.x));
        fieldY.setValue(Integer.toString(settings.y));
        fieldZ.setValue(Integer.toString(settings.z));
        children.add(fieldX);
        children.add(fieldY);
        children.add(fieldZ);

        if (statusMessage.isEmpty()) {
            statusMessage = I18n.get(LangKeys.GUI_STATUS_NEED_SEARCH);
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
                ResourceLocation key = ForgeRegistries.BIOMES.getKey(b);
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
        statusMessage = I18n.get(LangKeys.GUI_STATUS_NEED_SEARCH);
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
            statusMessage = I18n.get(LangKeys.GUI_STATUS_NOT_FOUND);
            statusColor = 0xFF5555;
        } else if (placeSafe) {
            statusMessage = I18n.get(LangKeys.GUI_STATUS_FOUND_SAFE, x, y, z);
            statusColor = 0x55FF55;
            applyFoundCoords(x, y, z);
        } else {
            statusMessage = I18n.get(LangKeys.GUI_STATUS_FOUND_UNSAFE, x, y, z, I18n.get(hazardLangKey(hazard)));
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

        for (net.minecraft.client.gui.widget.Widget w : buttons) {
            if (!(w instanceof Button)) continue;
            Button b = (Button) w;
            if (b.getMessage().getString().equals(I18n.get(LangKeys.modeKey("coords")))) {
                b.active = settings.mode != DestinationSettings.Mode.COORDS;
            } else if (b.getMessage().getString().equals(I18n.get(LangKeys.modeKey("biome")))) {
                b.active = settings.mode != DestinationSettings.Mode.BIOME;
            } else if (b.getMessage().getString().equals(I18n.get(LangKeys.modeKey("structure")))) {
                b.active = settings.mode != DestinationSettings.Mode.STRUCTURE;
            } else if (b.getMessage().getString().equals(I18n.get(LangKeys.GUI_EXPLORE))) {
                b.active = !searching;
            } else if (b.getMessage().getString().equals(I18n.get(LangKeys.GUI_SAVE))) {
                b.active = found && !searching;
            } else if (b.getMessage().getString().equals(I18n.get(LangKeys.GUI_FORCE_SAVE))) {
                b.visible = showForceOption;
                b.active = showForceOption && found && !placeSafe && !searching;
            }
        }
    }

    private static String localizeStructure(String structureId) {
        return DisplayCatalog.displayStructure(structureId);
    }

    private static String localizeDimension(String dimensionKey) {
        if (DimUtil.isBlank(dimensionKey)) {
            return I18n.get(LangKeys.DIMENSION_BLANK);
        }
        return DisplayCatalog.displayDimension(dimensionKey);
    }

    private static String localizeMode(DestinationSettings.Mode mode) {
        return I18n.get(LangKeys.modeKey(mode.name()));
    }

    private String formatBinding(DestinationSettings s) {
        if (s == null) return I18n.get(LangKeys.GUI_BOUND_NONE);
        String dim = localizeDimension(s.dimension);
        String mode = localizeMode(s.mode);
        if (s.mode == DestinationSettings.Mode.COORDS || s.mode == DestinationSettings.Mode.SCALED) {
            return I18n.get(LangKeys.GUI_BOUND_COORDS, dim, mode, s.x, s.y, s.z);
        }
        if (s.mode == DestinationSettings.Mode.BIOME) {
            Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(s.biomeKey.contains(":") ? s.biomeKey : "minecraft:" + s.biomeKey));
            String biomeName = biome != null ? BiomeNames.localize(biome) : s.biomeKey;
            return I18n.get(LangKeys.GUI_BOUND_BIOME, dim, mode, biomeName);
        }
        return I18n.get(LangKeys.GUI_BOUND_STRUCTURE, dim, mode, localizeStructure(s.structureName));
    }

    @Override
    public void removed() {
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        super.removed();
    }

    @Override
    public void tick() {
        filterField.tick();
        fieldX.tick();
        fieldY.tick();
        fieldZ.tick();
        if (filterField.visible) {
            String t = filterField.getValue();
            if (!t.equals(lastFilterText)) {
                lastFilterText = t;
                rebuildContentList();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
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
        return super.mouseScrolled(mouseX, mouseY, delta);
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
            statusMessage = I18n.get(LangKeys.GUI_STATUS_SAVE_BLOCKED, I18n.get(hazardLangKey(hazard)));
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
        PacketHandler.CHANNEL.sendToServer(new PacketSaveTuner(hand, settings, portalPos, force));
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
        statusMessage = I18n.get(LangKeys.GUI_STATUS_SEARCHING);
        statusColor = 0xFFFF55;
        refreshWidgets();
        PacketHandler.CHANNEL.sendToServer(new PacketValidateTuner(settings));
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
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        drawCenteredString(matrixStack, font, title, width / 2, 6, 0xFFFFFF);
        String boundLine = hasExistingBinding
                ? I18n.get(LangKeys.GUI_BOUND_CURRENT, formatBinding(originalBinding))
                : I18n.get(LangKeys.GUI_BOUND_NONE);
        drawCenteredString(matrixStack, font, boundLine, width / 2, 18, hasExistingBinding ? 0xFFAA00 : 0x808080);
        if (hasExistingBinding) {
            drawCenteredString(matrixStack, font, I18n.get(LangKeys.GUI_BOUND_OVERWRITE_HINT), width / 2, 28, 0xFF5555);
        }

        fill(matrixStack, dimLeft, dimTop, dimLeft + dimW, dimTop + dimH, 0x99000000);
        if (focus == FocusPanel.DIM) fill(matrixStack, dimLeft, dimTop, dimLeft + 2, dimTop + dimH, 0xFFFFFF55);
        drawString(matrixStack, font, I18n.get(LangKeys.GUI_DIM_LIST), dimLeft + pad, dimTop + 2, 0xA0A0A0);
        int dimListTop = dimTop + 14;
        for (int i = 0; i < dimRows; i++) {
            int idx = dimScroll + i;
            if (idx >= dimEntries.size()) break;
            DimensionConfig.DimensionEntry e = dimEntries.get(idx);
            boolean selected = e.dimKey.equals(DimUtil.normalize(settings.dimension));
            boolean hovered = idx == dimCursor;
            int y = dimListTop + i * rowH;
            if (selected) fill(matrixStack, dimLeft + 2, y, dimLeft + dimW - 2, y + rowH, 0x33FFFF00);
            else if (hovered && focus == FocusPanel.DIM) fill(matrixStack, dimLeft + 2, y, dimLeft + dimW - 2, y + rowH, 0x22FFFFFF);
            drawString(matrixStack, font, localizeDimension(e.dimKey), dimLeft + pad, y + 2, selected ? 0xFFFF55 : 0xE0E0E0);
        }

        if (filterField.visible) {
            filterField.render(matrixStack, mouseX, mouseY, partialTicks);
            if (filterField.getValue().isEmpty() && !filterField.isFocused()) {
                drawString(matrixStack, font, I18n.get(LangKeys.GUI_FILTER_HINT),
                        filterField.x + 4, filterField.y + 5, 0x707070);
            }
        }

        fill(matrixStack, midLeft, contentTop, midLeft + midW, contentTop + contentH, 0x99000000);
        if (focus == FocusPanel.CONTENT) fill(matrixStack, midLeft, contentTop, midLeft + 2, contentTop + contentH, 0xFFFFFF55);

        if (settings.mode == DestinationSettings.Mode.COORDS) {
            drawString(matrixStack, font, "X", fieldX.x - 12, fieldX.y + 5, 0xFFFFFF);
            drawString(matrixStack, font, "Y", fieldY.x - 12, fieldY.y + 5, 0xFFFFFF);
            drawString(matrixStack, font, "Z", fieldZ.x - 12, fieldZ.y + 5, 0xFFFFFF);
            fieldX.render(matrixStack, mouseX, mouseY, partialTicks);
            fieldY.render(matrixStack, mouseX, mouseY, partialTicks);
            fieldZ.render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            for (int i = 0; i < contentRows; i++) {
                int idx = contentScroll + i;
                if (idx >= contentLabels.size()) break;
                boolean selected = settings.mode == DestinationSettings.Mode.BIOME
                        ? contentBiomeKeys.get(idx).equals(settings.biomeKey)
                        : contentStructureIds.get(idx).equals(settings.structureName);
                boolean hovered = idx == contentCursor;
                int y = contentTop + pad + i * rowH;
                if (selected) fill(matrixStack, midLeft + 2, y, midLeft + midW - 2, y + rowH, 0x33FFFF00);
                else if (hovered && focus == FocusPanel.CONTENT) fill(matrixStack, midLeft + 2, y, midLeft + midW - 2, y + rowH, 0x22FFFFFF);
                drawString(matrixStack, font, contentLabels.get(idx), midLeft + pad, y + 2, selected ? 0xFFFF55 : 0xE0E0E0);
            }
        }

        fill(matrixStack, rightLeft, infoTop, rightLeft + rightW, rightTop + rightH, 0x66000000);
        int iy = infoTop + 4;
        drawString(matrixStack, font,
                I18n.get(LangKeys.GUI_STATUS, localizeDimension(settings.dimension), localizeMode(settings.mode)),
                rightLeft + 4, iy, 0xA0A0A0);
        iy += 14;
        for (net.minecraft.util.text.ITextProperties line : font.getSplitter().splitLines(new StringTextComponent(statusMessage), rightW - 8, net.minecraft.util.text.Style.EMPTY)) {
            drawString(matrixStack, font, line.getString(), rightLeft + 4, iy, statusColor);
            iy += 12;
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

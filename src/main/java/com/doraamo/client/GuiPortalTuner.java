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
import com.doraamo.util.LangKeys;
import com.doraamo.util.SearchFilter;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Layout (L→R): dimension list | mode + filter + content list | actions + info.
 */
@SideOnly(Side.CLIENT)
public class GuiPortalTuner extends GuiScreen {

    private static final int ID_MODE_COORDS = 10;
    private static final int ID_MODE_BIOME = 11;
    private static final int ID_MODE_STRUCTURE = 12;
    private static final int ID_EXPLORE = 90;
    private static final int ID_SAVE = 100;
    private static final int ID_FORCE = 101;

    private enum FocusPanel {
        DIM,
        CONTENT
    }

    private final EnumHand hand;
    private final DestinationSettings settings;
    private final BlockPos portalPos;
    private final boolean hasExistingBinding;
    private final DestinationSettings originalBinding;

    private GuiTextField filterField;
    private GuiTextField fieldX;
    private GuiTextField fieldY;
    private GuiTextField fieldZ;
    private String lastFilterText = "";

    private final List<DimensionConfig.DimensionEntry> dimEntries = new ArrayList<DimensionConfig.DimensionEntry>();
    private final List<String> contentLabels = new ArrayList<String>();
    private final List<Integer> contentBiomeIds = new ArrayList<Integer>();
    private final List<String> contentStructureIds = new ArrayList<String>();

    private int dimScroll;
    private int dimCursor;
    private int contentScroll;
    private int contentCursor;
    private FocusPanel focus = FocusPanel.CONTENT;

    private int rowH = 12;
    private int pad = 6;

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

    public GuiPortalTuner(EnumHand hand, DestinationSettings settings, BlockPos portalPos,
                          boolean hasExistingBinding) {
        this.hand = hand;
        this.settings = settings;
        this.portalPos = portalPos.toImmutable();
        this.hasExistingBinding = hasExistingBinding;
        this.originalBinding = hasExistingBinding ? settings.copy() : null;
        if (settings.mode == DestinationSettings.Mode.SCALED) {
            settings.mode = DestinationSettings.Mode.COORDS;
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

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

        int btnW = rightW;
        int btnH = 20;
        int by = rightTop;
        buttonList.add(new GuiButton(ID_EXPLORE, rightLeft, by, btnW, btnH, I18n.format(LangKeys.GUI_EXPLORE)));
        by += btnH + 4;
        buttonList.add(new GuiButton(ID_SAVE, rightLeft, by, btnW, btnH, I18n.format(LangKeys.GUI_SAVE)));
        by += btnH + 4;
        buttonList.add(new GuiButton(ID_FORCE, rightLeft, by, btnW, btnH, I18n.format(LangKeys.GUI_FORCE_SAVE)));
        infoTop = by + btnH + 10;

        int modeBtnW = (midW - 8) / 3;
        buttonList.add(new GuiButton(ID_MODE_COORDS, midLeft, midTop, modeBtnW, 20, I18n.format(LangKeys.modeKey("coords"))));
        buttonList.add(new GuiButton(ID_MODE_BIOME, midLeft + modeBtnW + 4, midTop, modeBtnW, 20, I18n.format(LangKeys.modeKey("biome"))));
        buttonList.add(new GuiButton(ID_MODE_STRUCTURE, midLeft + (modeBtnW + 4) * 2, midTop, modeBtnW, 20, I18n.format(LangKeys.modeKey("structure"))));

        filterField = new GuiTextField(1, fontRenderer, midLeft, midTop + modeRowH + 2, midW, filterH);
        filterField.setMaxStringLength(64);
        filterField.setText(lastFilterText);

        int fieldW = Math.max(50, (midW - 40) / 3);
        fieldX = new GuiTextField(2, fontRenderer, midLeft + 14, contentTop + 8, fieldW, 18);
        fieldY = new GuiTextField(3, fontRenderer, midLeft + 14 + fieldW + 20, contentTop + 8, fieldW, 18);
        fieldZ = new GuiTextField(4, fontRenderer, midLeft + 14 + (fieldW + 20) * 2, contentTop + 8, fieldW, 18);
        fieldX.setMaxStringLength(12);
        fieldY.setMaxStringLength(12);
        fieldZ.setMaxStringLength(12);
        fieldX.setText(Integer.toString(settings.x));
        fieldY.setText(Integer.toString(settings.y));
        fieldZ.setText(Integer.toString(settings.z));

        if (statusMessage.isEmpty()) {
            statusMessage = I18n.format(LangKeys.GUI_STATUS_NEED_SEARCH);
            statusColor = 0xA0A0A0;
        }

        rebuildDimList();
        rebuildContentList();
        refreshWidgets();
    }

    private void rebuildDimList() {
        dimEntries.clear();
        dimEntries.addAll(DimensionConfig.getGuiDimensions());
        dimCursor = 0;
        for (int i = 0; i < dimEntries.size(); i++) {
            if (dimEntries.get(i).id == settings.dimensionId) {
                dimCursor = i;
                break;
            }
        }
        ensureDimCursorVisible();
    }

    private void rebuildContentList() {
        contentLabels.clear();
        contentBiomeIds.clear();
        contentStructureIds.clear();
        String filter = filterField != null ? filterField.getText() : lastFilterText;

        if (settings.mode == DestinationSettings.Mode.BIOME) {
            for (Biome b : DestinationLocator.allBiomes()) {
                String label = BiomeNames.localize(b);
                ResourceLocation key = ForgeRegistries.BIOMES.getKey(b);
                String reg = key != null ? key.toString() : "";
                String path = key != null ? key.getResourcePath() : "";
                String idStr = Integer.toString(Biome.getIdForBiome(b));
                String[] tokens = DisplayCatalog.searchTokens(DisplayCatalog.Kind.BIOME, reg, label, path, idStr);
                if (SearchFilter.matches(filter, tokens)) {
                    contentLabels.add(label);
                    contentBiomeIds.add(Integer.valueOf(Biome.getIdForBiome(b)));
                }
            }
            contentCursor = 0;
            for (int i = 0; i < contentBiomeIds.size(); i++) {
                if (contentBiomeIds.get(i).intValue() == settings.biomeId) {
                    contentCursor = i;
                    break;
                }
            }
        } else if (settings.mode == DestinationSettings.Mode.STRUCTURE) {
            for (String id : DisplayCatalog.structuresForDim(settings.dimensionId)) {
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
        if (contentCursor < contentScroll) {
            contentScroll = contentCursor;
        } else if (contentCursor >= contentScroll + contentRows) {
            contentScroll = contentCursor - contentRows + 1;
        }
        contentScroll = Math.max(0, Math.min(contentScroll, Math.max(0, contentLabels.size() - contentRows)));
    }

    private void ensureDimCursorVisible() {
        if (dimCursor < dimScroll) {
            dimScroll = dimCursor;
        } else if (dimCursor >= dimScroll + dimRows) {
            dimScroll = dimCursor - dimRows + 1;
        }
        dimScroll = Math.max(0, Math.min(dimScroll, Math.max(0, dimEntries.size() - dimRows)));
    }

    private void invalidateValidation() {
        searching = false;
        found = false;
        placeSafe = false;
        showForceOption = false;
        hazard = PortalDoorPlacer.PlaceHazard.NONE;
        statusMessage = I18n.format(LangKeys.GUI_STATUS_NEED_SEARCH);
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
            statusMessage = I18n.format(LangKeys.GUI_STATUS_NOT_FOUND);
            statusColor = 0xFF5555;
        } else if (placeSafe) {
            statusMessage = I18n.format(LangKeys.GUI_STATUS_FOUND_SAFE,
                    Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
            statusColor = 0x55FF55;
            applyFoundCoords(x, y, z);
        } else {
            statusMessage = I18n.format(LangKeys.GUI_STATUS_FOUND_UNSAFE,
                    Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z),
                    I18n.format(hazardLangKey(hazard)));
            statusColor = 0xFFFF55;
            applyFoundCoords(x, y, z);
        }
        refreshWidgets();
    }

    private void applyFoundCoords(int x, int y, int z) {
        if (settings.mode == DestinationSettings.Mode.COORDS) {
            fieldX.setText(Integer.toString(x));
            fieldY.setText(Integer.toString(y));
            fieldZ.setText(Integer.toString(z));
        } else {
            settings.x = x;
            settings.y = y;
            settings.z = z;
        }
    }

    private static String hazardLangKey(PortalDoorPlacer.PlaceHazard hazard) {
        switch (hazard) {
            case FLOATING:
                return LangKeys.GUI_HAZARD_FLOATING;
            case WALL:
                return LangKeys.GUI_HAZARD_WALL;
            case FLOODED:
                return LangKeys.GUI_HAZARD_FLOODED;
            case LAVA:
                return LangKeys.GUI_HAZARD_LAVA;
            case FIRE:
                return LangKeys.GUI_HAZARD_FIRE;
            case OUT_OF_BOUNDS:
                return LangKeys.GUI_HAZARD_BOUNDS;
            case NONE:
            default:
                return LangKeys.GUI_HAZARD_NONE;
        }
    }

    private void refreshWidgets() {
        boolean coords = settings.mode == DestinationSettings.Mode.COORDS;
        fieldX.setVisible(coords);
        fieldY.setVisible(coords);
        fieldZ.setVisible(coords);
        filterField.setVisible(!coords);
        filterField.setEnabled(!coords);

        for (GuiButton b : buttonList) {
            if (b.id == ID_MODE_COORDS) {
                b.enabled = settings.mode != DestinationSettings.Mode.COORDS;
            } else if (b.id == ID_MODE_BIOME) {
                b.enabled = settings.mode != DestinationSettings.Mode.BIOME;
            } else if (b.id == ID_MODE_STRUCTURE) {
                b.enabled = settings.mode != DestinationSettings.Mode.STRUCTURE;
            } else if (b.id == ID_EXPLORE) {
                b.enabled = !searching;
            } else if (b.id == ID_SAVE) {
                b.enabled = found && !searching;
            } else if (b.id == ID_FORCE) {
                b.visible = showForceOption;
                b.enabled = showForceOption && found && !placeSafe && !searching;
            }
        }
    }

    private static String localizeStructure(String structureId) {
        return DisplayCatalog.displayStructure(structureId);
    }

    private static String localizeDimension(int dimensionId) {
        if (dimensionId == DoraAmo.BLANK_DIMENSION) {
            return I18n.format(LangKeys.DIMENSION_BLANK);
        }
        return DisplayCatalog.displayDimension(dimensionId);
    }

    private static String localizeMode(DestinationSettings.Mode mode) {
        return I18n.format(LangKeys.modeKey(mode.name()));
    }

    private String formatBinding(DestinationSettings s) {
        if (s == null) {
            return I18n.format(LangKeys.GUI_BOUND_NONE);
        }
        String dim = localizeDimension(s.dimensionId);
        String mode = localizeMode(s.mode);
        if (s.mode == DestinationSettings.Mode.COORDS || s.mode == DestinationSettings.Mode.SCALED) {
            return I18n.format(LangKeys.GUI_BOUND_COORDS, dim, mode,
                    Integer.valueOf(s.x), Integer.valueOf(s.y), Integer.valueOf(s.z));
        }
        if (s.mode == DestinationSettings.Mode.BIOME) {
            Biome biome = Biome.getBiome(s.biomeId);
            String biomeName = biome != null ? BiomeNames.localize(biome) : ("#" + s.biomeId);
            return I18n.format(LangKeys.GUI_BOUND_BIOME, dim, mode, biomeName);
        }
        return I18n.format(LangKeys.GUI_BOUND_STRUCTURE, dim, mode, localizeStructure(s.structureName));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        filterField.updateCursorCounter();
        fieldX.updateCursorCounter();
        fieldY.updateCursorCounter();
        fieldZ.updateCursorCounter();
        if (filterField.getVisible()) {
            String t = filterField.getText();
            if (!t.equals(lastFilterText)) {
                lastFilterText = t;
                rebuildContentList();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int dir = dWheel > 0 ? -1 : 1;
        if (mx >= dimLeft && mx < dimLeft + dimW && my >= dimTop && my < dimTop + dimH) {
            dimScroll = clampScroll(dimScroll + dir, dimEntries.size(), dimRows);
            focus = FocusPanel.DIM;
        } else if (mx >= midLeft && mx < midLeft + midW && my >= contentTop && my < contentTop + contentH
                && settings.mode != DestinationSettings.Mode.COORDS) {
            contentScroll = clampScroll(contentScroll + dir, contentLabels.size(), contentRows);
            focus = FocusPanel.CONTENT;
        }
    }

    private static int clampScroll(int scroll, int size, int rows) {
        return Math.max(0, Math.min(scroll, Math.max(0, size - rows)));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) {
            return;
        }
        if (button.id == ID_MODE_COORDS) {
            settings.mode = DestinationSettings.Mode.COORDS;
            rebuildContentList();
            invalidateValidation();
        } else if (button.id == ID_MODE_BIOME) {
            settings.mode = DestinationSettings.Mode.BIOME;
            rebuildContentList();
            invalidateValidation();
        } else if (button.id == ID_MODE_STRUCTURE) {
            settings.mode = DestinationSettings.Mode.STRUCTURE;
            rebuildContentList();
            invalidateValidation();
        } else if (button.id == ID_EXPLORE) {
            startExplore();
        } else if (button.id == ID_SAVE) {
            trySave(false);
        } else if (button.id == ID_FORCE) {
            doSave(true);
        }
    }

    private void selectDim(int index) {
        if (index < 0 || index >= dimEntries.size()) {
            return;
        }
        dimCursor = index;
        ensureDimCursorVisible();
        int id = dimEntries.get(index).id;
        if (settings.dimensionId != id) {
            settings.dimensionId = id;
            rebuildContentList();
            invalidateValidation();
        }
    }

    private void selectContent(int index) {
        if (settings.mode == DestinationSettings.Mode.COORDS) {
            return;
        }
        if (index < 0 || index >= contentLabels.size()) {
            return;
        }
        contentCursor = index;
        ensureContentCursorVisible();
        if (settings.mode == DestinationSettings.Mode.BIOME) {
            int id = contentBiomeIds.get(index).intValue();
            if (settings.biomeId != id) {
                settings.biomeId = id;
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
        if (focus == FocusPanel.DIM) {
            selectDim(dimCursor);
        } else {
            selectContent(contentCursor);
        }
    }

    private void trySave(boolean force) {
        if (!found) {
            return;
        }
        readCoordFields();
        if (!placeSafe && !force) {
            showForceOption = true;
            statusMessage = I18n.format(LangKeys.GUI_STATUS_SAVE_BLOCKED,
                    I18n.format(hazardLangKey(hazard)));
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
        mc.player.closeScreen();
    }

    private void startExplore() {
        readCoordFields();
        if (settings.mode == DestinationSettings.Mode.BIOME) {
            if (contentBiomeIds.isEmpty()) {
                onValidateResult(false, 0, 0, 0, PortalDoorPlacer.PlaceHazard.NONE);
                return;
            }
            if (!contentBiomeIds.contains(Integer.valueOf(settings.biomeId))) {
                settings.biomeId = contentBiomeIds.get(Math.max(0, contentCursor)).intValue();
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
        statusMessage = I18n.format(LangKeys.GUI_STATUS_SEARCHING);
        statusColor = 0xFFFF55;
        refreshWidgets();
        PacketHandler.CHANNEL.sendToServer(new PacketValidateTuner(settings));
    }

    private void readCoordFields() {
        try {
            settings.x = Integer.parseInt(fieldX.getText().trim());
        } catch (Exception ignored) {
        }
        try {
            settings.y = Integer.parseInt(fieldY.getText().trim());
        } catch (Exception ignored) {
        }
        try {
            settings.z = Integer.parseInt(fieldZ.getText().trim());
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (filterField.getVisible() && filterField.isFocused()) {
            if (filterField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        if (fieldX.getVisible()) {
            if (fieldX.textboxKeyTyped(typedChar, keyCode)
                    || fieldY.textboxKeyTyped(typedChar, keyCode)
                    || fieldZ.textboxKeyTyped(typedChar, keyCode)) {
                invalidateValidation();
                return;
            }
        }

        if (keyCode == Keyboard.KEY_LEFT) {
            focus = FocusPanel.DIM;
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            focus = FocusPanel.CONTENT;
            return;
        }
        if (keyCode == Keyboard.KEY_UP) {
            if (focus == FocusPanel.DIM) {
                dimCursor = Math.max(0, dimCursor - 1);
                ensureDimCursorVisible();
            } else if (settings.mode != DestinationSettings.Mode.COORDS && !contentLabels.isEmpty()) {
                contentCursor = Math.max(0, contentCursor - 1);
                ensureContentCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_DOWN) {
            if (focus == FocusPanel.DIM) {
                dimCursor = Math.min(dimEntries.size() - 1, dimCursor + 1);
                ensureDimCursorVisible();
            } else if (settings.mode != DestinationSettings.Mode.COORDS && !contentLabels.isEmpty()) {
                contentCursor = Math.min(contentLabels.size() - 1, contentCursor + 1);
                ensureContentCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            confirmFocused();
            return;
        }
        if (filterField.getVisible() && !filterField.isFocused()
                && !isCtrlKeyDown() && typedChar >= 32) {
            filterField.setFocused(true);
            fieldX.setFocused(false);
            fieldY.setFocused(false);
            fieldZ.setFocused(false);
            filterField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        filterField.mouseClicked(mouseX, mouseY, mouseButton);
        if (fieldX.getVisible()) {
            fieldX.mouseClicked(mouseX, mouseY, mouseButton);
            fieldY.mouseClicked(mouseX, mouseY, mouseButton);
            fieldZ.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mouseButton == 0) {
            if (mouseX >= dimLeft && mouseX < dimLeft + dimW && mouseY >= dimTop && mouseY < dimTop + dimH) {
                focus = FocusPanel.DIM;
                int row = (mouseY - dimTop - pad) / rowH;
                int idx = dimScroll + row;
                if (idx >= 0 && idx < dimEntries.size()) {
                    selectDim(idx);
                }
                return;
            }
            if (settings.mode != DestinationSettings.Mode.COORDS
                    && mouseX >= midLeft && mouseX < midLeft + midW
                    && mouseY >= contentTop && mouseY < contentTop + contentH) {
                focus = FocusPanel.CONTENT;
                int row = (mouseY - contentTop - pad) / rowH;
                int idx = contentScroll + row;
                if (idx >= 0 && idx < contentLabels.size()) {
                    selectContent(idx);
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format(LangKeys.GUI_TITLE), width / 2, 6, 0xFFFFFF);
        String boundLine = hasExistingBinding
                ? I18n.format(LangKeys.GUI_BOUND_CURRENT, formatBinding(originalBinding))
                : I18n.format(LangKeys.GUI_BOUND_NONE);
        drawCenteredString(fontRenderer, boundLine, width / 2, 18, hasExistingBinding ? 0xFFAA00 : 0x808080);
        if (hasExistingBinding) {
            drawCenteredString(fontRenderer, I18n.format(LangKeys.GUI_BOUND_OVERWRITE_HINT), width / 2, 28, 0xFF5555);
        }

        drawRect(dimLeft, dimTop, dimLeft + dimW, dimTop + dimH, 0x99000000);
        if (focus == FocusPanel.DIM) {
            drawRect(dimLeft, dimTop, dimLeft + 2, dimTop + dimH, 0xFFFFFF55);
        }
        drawString(fontRenderer, I18n.format(LangKeys.GUI_DIM_LIST), dimLeft + pad, dimTop + 2, 0xA0A0A0);
        int dimListTop = dimTop + 14;
        for (int i = 0; i < dimRows; i++) {
            int idx = dimScroll + i;
            if (idx >= dimEntries.size()) {
                break;
            }
            DimensionConfig.DimensionEntry e = dimEntries.get(idx);
            boolean selected = e.id == settings.dimensionId;
            boolean hovered = idx == dimCursor;
            int y = dimListTop + i * rowH;
            if (selected) {
                drawRect(dimLeft + 2, y, dimLeft + dimW - 2, y + rowH, 0x33FFFF00);
            } else if (hovered && focus == FocusPanel.DIM) {
                drawRect(dimLeft + 2, y, dimLeft + dimW - 2, y + rowH, 0x22FFFFFF);
            }
            int color = selected ? 0xFFFF55 : 0xE0E0E0;
            drawString(fontRenderer, localizeDimension(e.id), dimLeft + pad, y + 2, color);
        }

        if (filterField.getVisible()) {
            filterField.drawTextBox();
            if (filterField.getText().isEmpty() && !filterField.isFocused()) {
                drawString(fontRenderer, I18n.format(LangKeys.GUI_FILTER_HINT),
                        filterField.x + 4, filterField.y + 5, 0x707070);
            }
        }

        drawRect(midLeft, contentTop, midLeft + midW, contentTop + contentH, 0x99000000);
        if (focus == FocusPanel.CONTENT) {
            drawRect(midLeft, contentTop, midLeft + 2, contentTop + contentH, 0xFFFFFF55);
        }

        if (settings.mode == DestinationSettings.Mode.COORDS) {
            drawString(fontRenderer, "X", fieldX.x - 12, fieldX.y + 5, 0xFFFFFF);
            drawString(fontRenderer, "Y", fieldY.x - 12, fieldY.y + 5, 0xFFFFFF);
            drawString(fontRenderer, "Z", fieldZ.x - 12, fieldZ.y + 5, 0xFFFFFF);
            fieldX.drawTextBox();
            fieldY.drawTextBox();
            fieldZ.drawTextBox();
        } else {
            for (int i = 0; i < contentRows; i++) {
                int idx = contentScroll + i;
                if (idx >= contentLabels.size()) {
                    break;
                }
                boolean selected = settings.mode == DestinationSettings.Mode.BIOME
                        ? contentBiomeIds.get(idx).intValue() == settings.biomeId
                        : contentStructureIds.get(idx).equals(settings.structureName);
                boolean hovered = idx == contentCursor;
                int y = contentTop + pad + i * rowH;
                if (selected) {
                    drawRect(midLeft + 2, y, midLeft + midW - 2, y + rowH, 0x33FFFF00);
                } else if (hovered && focus == FocusPanel.CONTENT) {
                    drawRect(midLeft + 2, y, midLeft + midW - 2, y + rowH, 0x22FFFFFF);
                }
                int color = selected ? 0xFFFF55 : 0xE0E0E0;
                drawString(fontRenderer, contentLabels.get(idx), midLeft + pad, y + 2, color);
            }
        }

        drawRect(rightLeft, infoTop, rightLeft + rightW, rightTop + rightH, 0x66000000);
        int iy = infoTop + 4;
        drawString(fontRenderer,
                I18n.format(LangKeys.GUI_STATUS, localizeDimension(settings.dimensionId), localizeMode(settings.mode)),
                rightLeft + 4, iy, 0xA0A0A0);
        iy += 14;
        List<String> lines = fontRenderer.listFormattedStringToWidth(statusMessage, rightW - 8);
        for (String line : lines) {
            drawString(fontRenderer, line, rightLeft + 4, iy, statusColor);
            iy += 12;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

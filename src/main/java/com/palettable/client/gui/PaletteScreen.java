package com.palettable.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.palettable.Palettable;
import com.palettable.client.BlockColorAnalyzer;
import com.palettable.color.BlockColorEntry;
import com.palettable.color.ColorUtil;
import com.palettable.color.PaletteEngine;
import com.palettable.palette.PaletteGuiSettings;
import com.palettable.palette.PaletteStorage;
import com.palettable.palette.SavedPalette;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Inventory-styled palette creator with two tabs (Creator / Saved Palettes), a visible+clickable
 * hotbar, color-sortable search, and click-any-result-to-set-an-endpoint editing.
 */
public class PaletteScreen extends Screen {
    private static final int CELL = 18;
    private static final int COLS = 9;
    private static final int PAD = 8;
    private static final int GUTTER = 6;
    private static final int COL_GAP = 12;
    private static final int PANEL_W = PAD + COLS * CELL + COL_GAP + COLS * CELL + PAD; // 352
    private static final int PANEL_H = 288;
    private static final int PALETTE_SLOT_ROWS = 2;
    /** Full width of the creator columns (9 search + 9 gradient). */
    private static final int PALETTE_SLOT_COLS = COLS * 2;
    private static final int PALETTE_SLOT_COUNT = PALETTE_SLOT_ROWS * PALETTE_SLOT_COLS;
    private static final int GRADIENT_AREA_W = COLS * CELL;
    private static final int GRADIENT_ROW_MIN = 3;
    private static final int GRADIENT_ROW_MAX = 10;

    // Inventory-like palette.
    private static final int COL_PANEL = 0xFFC6C6C6;
    private static final int COL_HILIGHT = 0xFFFFFFFF;
    private static final int COL_SHADOW = 0xFF555555;
    private static final int COL_INSET_DARK = 0xFF373737;
    private static final int COL_INSET_BG = 0xFF8B8B8B;
    private static final int COL_INSET_SEL = 0xFFA8A8A8;
    private static final int COL_TEXT = 0xFF252525;
    private static final int COL_TEXT_MUTED = 0xFF4A4A4A;
    private static final int COL_ACTIVE = 0xFFFFE14D;
    private static final int COL_HOVER = 0x60FFFFFF;
    private static final int COL_DIVIDER = 0xFF9A9A9A;

    private static final double MAX_THRESHOLD = 40.0;

    // ---- Widgets ----
    private EditBox searchBox;
    private EditBox maxBox;
    private EditBox nameBox;
    private Button fullBlocksButton;
    private Button sortButton;
    private Button saveButton;
    private Button loadButton;
    private Button gradientZoomInButton;
    private Button gradientZoomOutButton;
    private ThresholdSlider thresholdSlider;

    // ---- State ----
    private int tab = 0; // 0 = creator, 1 = saved
    /** Visible row count in the gradient grid; fewer rows = larger icons (zoom in). */
    private int gradientRowCount = 7;
    private boolean fullBlocksOnly = false;
    private boolean sortByColor = false;
    private double threshold = 14.0;
    private int activeSlot = 0; // 0 = A, 1 = B
    private Block slotA;
    private Block slotB;
    /** User-curated palette slots (right-click search/gradient to fill). */
    private final Block[] paletteSlots = new Block[PALETTE_SLOT_COUNT];
    private String status = "";

    private List<BlockColorEntry> allEntries = List.of();
    private List<BlockColorEntry> searchResults = List.of();
    private List<BlockColorEntry> gradient = List.of();

    private List<SavedPalette> savedPalettes = new ArrayList<>();
    private int selectedSaved = -1;
    /** {@code >= 0} while the inline delete confirmation is shown. */
    private int pendingDeleteIndex = -1;
    private int deleteConfirmX, deleteConfirmY, deleteConfirmW, deleteConfirmH;
    private int deleteConfirmYesX, deleteConfirmNoX, deleteConfirmBtnY;

    private int searchScrollRow = 0;
    private int gradientScrollRow = 0;
    private int savedListScroll = 0;

    // ---- Layout (resolved in init) ----
    private int left, top;
    private int rowSearchY, rowControlsY, rowHeadersY, rowSaveY, rowPaletteLabelY, sectionRuleY, paletteSlotsY;
    private int slotAX, slotBX, slotsY;
    private int gridY, gridRows;
    private int searchX, gradientX, dividerX;
    private int searchFieldW, maxFieldW, maxFieldX;
    private int hotbarX, hotbarY;
    private int savedListX, savedListY, savedListW, savedListRows;
    private int previewX, previewY;
    private int tabX, tab0Y, tab1Y;
    private static final int TAB_W = 28;
    private static final int TAB_H = 28;

    public PaletteScreen() {
        super(Component.translatable("screen.palettable.title"));
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;

        searchX = left + PAD;
        gradientX = searchX + COLS * CELL + COL_GAP;
        dividerX = searchX + COLS * CELL + COL_GAP / 2;
        searchFieldW = COLS * CELL - 4;

        rowSearchY = top + 20;
        rowControlsY = top + 40;
        slotsY = rowControlsY + 1;
        slotAX = left + PAD;
        slotBX = slotAX + CELL + 4;
        rowHeadersY = top + 64;
        gridY = top + 76;
        gridRows = 7;
        int gridBottom = gridY + gridRows * CELL;
        sectionRuleY = gridBottom + GUTTER;
        rowSaveY = sectionRuleY + 1 + GUTTER - 2;
        rowPaletteLabelY = rowSaveY + 16 + 4;
        paletteSlotsY = rowPaletteLabelY + 9;

        hotbarX = this.width / 2 - GuiTextures.HOTBAR_LEFT_OFFSET;
        hotbarY = this.height - GuiTextures.HOTBAR_BOTTOM_OFFSET;

        savedListX = searchX;
        savedListY = top + 30;
        savedListW = COLS * CELL;
        savedListRows = 11;

        previewX = gradientX;
        previewY = top + 68;

        tabX = left - TAB_W + 4;
        tab0Y = top + PAD;
        tab1Y = tab0Y + TAB_H + 2;

        int sortX = slotBX + CELL + GUTTER;
        maxFieldW = maxFieldW();
        maxFieldX = left + PANEL_W - PAD - maxFieldW;
        int sliderX = sortX + 88 + GUTTER;
        int sliderRight = maxFieldX - GUTTER - this.font.width("Max") - 4;
        int sliderW = Math.max(72, sliderRight - sliderX);

        searchBox = addRenderableWidget(borderless(searchX, rowSearchY, searchFieldW, 16, "search blocks..."));
        searchBox.setResponder(s -> updateSearch());

        int fullBlocksW = left + PANEL_W - PAD - (searchX + searchFieldW + GUTTER);
        fullBlocksButton = addRenderableWidget(TexturedButton.wide(
                searchX + searchFieldW + GUTTER, rowSearchY, fullBlocksW, 16,
                fullBlocksLabel(), b -> {
                    fullBlocksOnly = !fullBlocksOnly;
                    b.setMessage(fullBlocksLabel());
                    updateSearch();
                    updateGradient();
                }, GuiTextures.BUTTON));

        sortButton = addRenderableWidget(TexturedButton.wide(
                sortX, rowControlsY, 88, 16, sortLabel(), b -> {
                    sortByColor = !sortByColor;
                    b.setMessage(sortLabel());
                    updateSearch();
                }, GuiTextures.BUTTON));

        PaletteGuiSettings guiSettings = PaletteGuiSettings.load();
        fullBlocksOnly = guiSettings.fullBlocksOnly;
        sortByColor = guiSettings.sortByColor;
        threshold = guiSettings.threshold;
        gradientRowCount = loadGradientRowCount(guiSettings);
        fullBlocksButton.setMessage(fullBlocksLabel());
        sortButton.setMessage(sortLabel());

        thresholdSlider = addRenderableWidget(new ThresholdSlider(sliderX, rowControlsY, sliderW, 16));

        maxBox = addRenderableWidget(borderless(maxFieldX, rowControlsY, maxFieldW, 16, "all"));
        maxBox.setValue(guiSettings.maxList);
        maxBox.setResponder(s -> updateGradient());

        int zoomBtnSize = 16;
        int zoomBtnY = rowHeadersY - 5;
        int zoomPlusX = gradientX + GRADIENT_AREA_W - zoomBtnSize;
        int zoomMinusX = zoomPlusX - GUTTER - zoomBtnSize;
        gradientZoomInButton = addRenderableWidget(TexturedButton.square(
                zoomPlusX, zoomBtnY, zoomBtnSize, Component.literal("+"), b -> zoomGradientRows(-1),
                GuiTextures.BUTTON_ZOOM_IN));
        gradientZoomOutButton = addRenderableWidget(TexturedButton.square(
                zoomMinusX, zoomBtnY, zoomBtnSize, Component.literal("-"), b -> zoomGradientRows(1),
                GuiTextures.BUTTON_ZOOM_OUT));

        nameBox = addRenderableWidget(borderless(searchX, rowSaveY, 196, 16, "palette name..."));

        int saveW = left + PANEL_W - PAD - (searchX + 196 + GUTTER);
        saveButton = addRenderableWidget(TexturedButton.wide(
                searchX + 196 + GUTTER, rowSaveY, saveW, 16,
                Component.literal("Save palette"), b -> saveCurrent(),
                GuiTextures.BUTTON));

        loadButton = addRenderableWidget(TexturedButton.wide(
                gradientX, top + PANEL_H - PAD - 16, COLS * CELL, 16,
                Component.literal("Load into creator"), b -> {
                    if (selectedSaved >= 0 && selectedSaved < savedPalettes.size()) {
                        loadSaved(selectedSaved);
                    }
                }, GuiTextures.BUTTON));

        savedPalettes = PaletteStorage.load();

        allEntries = BlockColorAnalyzer.getAll();
        updateSearch();
        updateGradient();
        setTab(0);
        setInitialFocus(searchBox);
    }

    private static int maxFieldW() {
        return 44;
    }

    /**
     * Creates a borderless edit box laid out inside the given field rectangle. A borderless
     * {@link EditBox} draws its text at its top edge, so we vertically center it by offsetting the
     * y and using an 8px-tall text band; the recessed background is drawn separately at the full rect.
     */
    private EditBox borderless(int fieldX, int fieldY, int fieldW, int fieldH, String hint) {
        int textY = fieldY + (fieldH - 8) / 2;
        EditBox box = new EditBox(this.font, fieldX + 4, textY, fieldW - 8, 8, Component.literal(""));
        box.setBordered(false);
        box.setTextColor(0xE8E8E8);
        box.setMaxLength(64);
        if (hint != null) {
            box.setHint(Component.literal(hint));
        }
        return box;
    }

    private Component fullBlocksLabel() {
        return Component.literal("Full blocks: " + (fullBlocksOnly ? "ON" : "OFF"));
    }

    private Component sortLabel() {
        return Component.literal("Sort: " + (sortByColor ? "Color" : "Name"));
    }

    private void setTab(int newTab) {
        tab = newTab;
        this.setFocused(null);
        boolean creator = tab == 0;
        searchBox.visible = creator;
        fullBlocksButton.visible = creator;
        sortButton.visible = creator;
        thresholdSlider.visible = creator;
        maxBox.visible = creator;
        nameBox.visible = creator;
        saveButton.visible = creator;
        gradientZoomInButton.visible = creator;
        gradientZoomOutButton.visible = creator;
        loadButton.visible = !creator && selectedSaved >= 0 && selectedSaved < savedPalettes.size();
    }

    private int gradientViewportH() {
        return gridRows * CELL;
    }

    /** Column/row sizes that always tile the fixed gradient viewport; zoom changes visible row count. */
    private record GradientLayout(int cols, int[] colWidths, int[] rowHeights) {}

    private GradientLayout gradientLayout() {
        int rows = gradientRowCount;
        int cols = Math.max(1, (GRADIENT_AREA_W * rows + gradientViewportH() / 2) / gradientViewportH());
        return new GradientLayout(cols, distributePixels(GRADIENT_AREA_W, cols), distributePixels(gradientViewportH(), rows));
    }

    /** Split {@code total} px into {@code count} parts that differ by at most 1 px. */
    private static int[] distributePixels(int total, int count) {
        int[] sizes = new int[count];
        int base = total / count;
        int extra = total % count;
        for (int i = 0; i < count; i++) {
            sizes[i] = base + (i < extra ? 1 : 0);
        }
        return sizes;
    }

    private int gradientColX(int col, int[] colWidths) {
        int x = gradientX;
        for (int c = 0; c < col; c++) {
            x += colWidths[c];
        }
        return x;
    }

    private int gradientRowY(int row, int[] rowHeights) {
        int y = gridY;
        for (int r = 0; r < row; r++) {
            y += rowHeights[r];
        }
        return y;
    }

    private int gradientVisibleRows() {
        return gradientRowCount;
    }

    private int loadGradientRowCount(PaletteGuiSettings settings) {
        if (settings.gradientRows >= GRADIENT_ROW_MIN && settings.gradientRows <= GRADIENT_ROW_MAX) {
            return settings.gradientRows;
        }
        int fromCell = gradientViewportH() / Math.max(12, settings.gradientCellSize);
        return Mth.clamp(fromCell, GRADIENT_ROW_MIN, GRADIENT_ROW_MAX);
    }

    private void zoomGradientRows(int delta) {
        int next = Mth.clamp(gradientRowCount + delta, GRADIENT_ROW_MIN, GRADIENT_ROW_MAX);
        if (next == gradientRowCount) {
            return;
        }
        gradientRowCount = next;
        gradientScrollRow = clampScroll(gradientScrollRow, gradient.size(), gradientVisibleRows(), gradientLayout().cols);
    }

    // ---- Data ------------------------------------------------------------------------------------

    private void updateSearch() {
        if (searchBox == null) {
            return;
        }
        String q = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        searchResults = allEntries.stream()
                .filter(e -> !fullBlocksOnly || e.fullCube())
                .filter(e -> matches(e, q))
                .sorted(sortByColor ? colorComparator() : nameComparator())
                .limit(1000)
                .toList();
        searchScrollRow = clampScroll(searchScrollRow, searchResults.size(), gridRows, COLS);
    }

    private Comparator<BlockColorEntry> nameComparator() {
        return Comparator.comparing(e -> e.block().getName().getString().toLowerCase(Locale.ROOT));
    }

    private Comparator<BlockColorEntry> colorComparator() {
        return Comparator
                .comparingDouble((BlockColorEntry e) -> ColorUtil.colorSortKey(e.rgb()))
                .thenComparing(e -> BuiltInRegistries.BLOCK.getKey(e.block()).toString());
    }

    private boolean matches(BlockColorEntry e, String q) {
        if (q.isEmpty()) {
            return true;
        }
        if (e.block().getName().getString().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        return BuiltInRegistries.BLOCK.getKey(e.block()).toString().contains(q);
    }

    private void updateGradient() {
        if (slotA == null || slotB == null) {
            gradient = List.of();
            return;
        }
        BlockColorEntry a = BlockColorAnalyzer.get(slotA);
        BlockColorEntry b = BlockColorAnalyzer.get(slotB);
        if (a == null || b == null) {
            gradient = List.of();
            return;
        }
        List<BlockColorEntry> pool = fullBlocksOnly
                ? allEntries.stream().filter(BlockColorEntry::fullCube).toList()
                : allEntries;
        int max = parseMaxList(maxBox);
        List<BlockColorEntry> hits = PaletteEngine.gradientOrdered(pool, a, b, threshold);
        gradient = PaletteEngine.sampleEvenly(hits, max);
        gradientScrollRow = clampScroll(gradientScrollRow, gradient.size(), gradientVisibleRows(), gradientLayout().cols);
    }

    private void saveCurrent() {
        List<String> ids = paletteSlotIds();
        if (ids.stream().allMatch(String::isEmpty)) {
            status = "Add blocks to the palette (right-click results).";
            return;
        }
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            name = "Palette " + (savedPalettes.size() + 1);
        }
        savedPalettes.add(new SavedPalette(name, ids));
        PaletteStorage.save(savedPalettes);
        nameBox.setValue("");
        status = "Saved \"" + name + "\".";
    }

    private void loadSaved(int index) {
        SavedPalette sp = savedPalettes.get(index);
        clearPaletteSlots();
        List<String> blocks = sp.blocks;
        for (int i = 0; i < paletteSlots.length && i < blocks.size(); i++) {
            String bid = blocks.get(i);
            if (bid != null && !bid.isEmpty()) {
                paletteSlots[i] = resolve(bid);
            }
        }
        nameBox.setValue(sp.name);
        setTab(0);
        status = "Loaded \"" + sp.name + "\" into palette slots.";
    }

    private void deleteSaved(int index) {
        SavedPalette sp = savedPalettes.remove(index);
        PaletteStorage.save(savedPalettes);
        if (selectedSaved >= savedPalettes.size()) {
            selectedSaved = savedPalettes.size() - 1;
        }
        setTab(1);
        status = "Deleted \"" + sp.name + "\".";
    }

    private void confirmDeleteSaved(int index) {
        if (index < 0 || index >= savedPalettes.size()) {
            return;
        }
        pendingDeleteIndex = index;
        layoutDeleteConfirmPopup();
    }

    private void layoutDeleteConfirmPopup() {
        String text = "Delete?";
        int textW = this.font.width(text);
        deleteConfirmW = PAD + textW + GUTTER + GuiTextures.ACTION_BTN_SIZE + 4 + GuiTextures.ACTION_BTN_SIZE + PAD;
        deleteConfirmH = 24;
        deleteConfirmX = left + (PANEL_W - deleteConfirmW) / 2;
        deleteConfirmY = top + (PANEL_H - deleteConfirmH) / 2;
        deleteConfirmBtnY = deleteConfirmY + (deleteConfirmH - GuiTextures.ACTION_BTN_SIZE) / 2;
        deleteConfirmYesX = deleteConfirmX + PAD + textW + GUTTER;
        deleteConfirmNoX = deleteConfirmYesX + GuiTextures.ACTION_BTN_SIZE + 4;
    }

    private void cancelDeleteConfirm() {
        pendingDeleteIndex = -1;
    }

    private void selectSaved(int index) {
        selectedSaved = index;
        setTab(1);
    }

    private void clearPaletteSlots() {
        for (int i = 0; i < paletteSlots.length; i++) {
            paletteSlots[i] = null;
        }
    }

    private List<String> paletteSlotIds() {
        List<String> ids = new ArrayList<>(paletteSlots.length);
        for (Block block : paletteSlots) {
            ids.add(block != null ? id(block) : "");
        }
        return ids;
    }

    private int paletteSlotColX(int col, int originX) {
        return originX + col * CELL;
    }

    private int paletteSlotIndex(double mx, double my) {
        return paletteSlotIndexAt(mx, my, searchX, paletteSlotsY, PALETTE_SLOT_COLS);
    }

    private int paletteSlotIndexAt(double mx, double my, int originX, int originY, int cols) {
        for (int i = 0; i < paletteSlots.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = paletteSlotColX(col, originX);
            int y = originY + row * CELL;
            if (inside(mx, my, x, y, CELL, CELL)) {
                return i;
            }
        }
        return -1;
    }

    private void addToPaletteSlot(Block block) {
        if (block == null || BlockColorAnalyzer.get(block) == null) {
            return;
        }
        for (int i = 0; i < paletteSlots.length; i++) {
            if (paletteSlots[i] == null) {
                paletteSlots[i] = block;
                status = "";
                return;
            }
        }
        status = "Palette slots are full.";
    }

    private int filledPaletteSlotCount() {
        int count = 0;
        for (Block block : paletteSlots) {
            if (block != null) {
                count++;
            }
        }
        return count;
    }

    private String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private Block resolve(String idStr) {
        try {
            return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(idStr)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** @return 0 to show every gradient match (no cap). */
    private int parseMaxList(EditBox box) {
        if (box == null) {
            return 0;
        }
        String raw = box.getValue().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            int value = Integer.parseInt(raw);
            return value <= 0 ? 0 : value;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatMaxList(int max) {
        return max <= 0 ? "" : Integer.toString(max);
    }

    private void saveGuiSettings() {
        PaletteGuiSettings settings = new PaletteGuiSettings();
        settings.fullBlocksOnly = fullBlocksOnly;
        settings.sortByColor = sortByColor;
        settings.threshold = threshold;
        settings.maxList = maxBox != null ? maxBox.getValue().trim() : "";
        settings.gradientRows = gradientRowCount;
        PaletteGuiSettings.save(settings);
    }

    private void drawLabel(GuiGraphics g, String text, int x, int y) {
        g.drawString(this.font, text, x, y, COL_TEXT, false);
    }

    private void drawPanelTitle(GuiGraphics g) {
        String text = this.title.getString();
        int x = left + (PANEL_W - this.font.width(text)) / 2;
        g.drawString(this.font, text, x, top + 5, 0xFFFFFFFF, true);
    }

    // ---- Rendering -------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Do not call super — vanilla renderBackground triggers the world blur shader.
        g.fill(0, 0, this.width, this.height, 0x99101010);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        renderTabs(g, mouseX, mouseY);
        drawPanel(g, left, top, PANEL_W, PANEL_H);
        if (tab == 0) {
            drawTextField(g, searchX, rowSearchY, searchFieldW, 16);
            drawTextField(g, maxFieldX, rowControlsY, maxFieldW, 16);
            drawTextField(g, searchX, rowSaveY, 196, 16);
        }
        super.render(g, mouseX, mouseY, partialTick);

        drawPanelTitle(g);

        BlockColorEntry hoveredEntry;
        ItemStack hoveredStack = ItemStack.EMPTY;

        if (tab == 0) {
            hoveredEntry = renderCreator(g, mouseX, mouseY);
            hoveredStack = renderHotbar(g, mouseX, mouseY);
        } else {
            hoveredEntry = renderSaved(g, mouseX, mouseY);
        }

        if (!status.isEmpty()) {
            g.drawString(this.font, status, left + 8, top + PANEL_H + 4, 0xFFFFFFFF, true);
        }

        if (hoveredEntry != null) {
            renderEntryTooltip(g, hoveredEntry, mouseX, mouseY);
        } else if (!hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }

        if (pendingDeleteIndex >= 0) {
            renderDeleteConfirmPopup(g, mouseX, mouseY);
        }
    }

    private void renderDeleteConfirmPopup(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0x90000000);
        drawPanel(g, deleteConfirmX, deleteConfirmY, deleteConfirmW, deleteConfirmH);
        g.drawString(this.font, "Delete?", deleteConfirmX + PAD, deleteConfirmY + 8, COL_TEXT, false);
        drawActionButton(g, deleteConfirmYesX, deleteConfirmBtnY, GuiTextures.ACTION_CONFIRM, mouseX, mouseY);
        drawActionButton(g, deleteConfirmNoX, deleteConfirmBtnY, GuiTextures.ACTION_DELETE, mouseX, mouseY);
    }

    private void drawActionButton(GuiGraphics g, int x, int y, ResourceLocation texture, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, GuiTextures.ACTION_BTN_SIZE, GuiTextures.ACTION_BTN_SIZE);
        GuiTextures.blitActionButton(g, texture, x, y, hover ? GuiTextures.TINT_HOVER : GuiTextures.TINT_NORMAL);
    }

    private BlockColorEntry renderCreator(GuiGraphics g, int mouseX, int mouseY) {
        BlockColorEntry hovered = null;

        drawColumnDivider(g, rowHeadersY - 2, gridY + gradientViewportH() + 2);

        hovered = orElse(hovered, drawBigSlot(g, mouseX, mouseY, slotAX, slotsY, slotA, activeSlot == 0, "A"));
        hovered = orElse(hovered, drawBigSlot(g, mouseX, mouseY, slotBX, slotsY, slotB, activeSlot == 1, "B"));

        int maxLabelX = maxFieldX - GUTTER - this.font.width("Max");
        drawLabel(g, "Max", maxLabelX, rowControlsY + 4);

        drawLabel(g, "Search results", searchX, rowHeadersY);
        String gradientHeader = "Gradient (" + gradient.size() + ")";
        drawLabel(g, gradientHeader, gradientX, rowHeadersY);

        hovered = orElse(hovered, drawGrid(g, mouseX, mouseY, searchResults, searchX, gridY, gridRows, searchScrollRow, CELL, COLS, false));
        drawGradientViewport(g);
        hovered = orElse(hovered, drawGradientGrid(g, mouseX, mouseY));

        if (gradient.isEmpty()) {
            String hint = (slotA == null || slotB == null) ? "Pick blocks for A and B" : "No matches; raise threshold";
            g.drawString(this.font, hint, gradientX + 6, gridY + 4, COL_TEXT_MUTED, false);
        }

        drawSectionRule(g, sectionRuleY);

        drawLabel(g, "Palette (" + filledPaletteSlotCount() + "/" + PALETTE_SLOT_COUNT + ")", searchX, rowPaletteLabelY);
        hovered = orElse(hovered, drawPaletteSlots(g, mouseX, mouseY, paletteSlots, searchX, paletteSlotsY, PALETTE_SLOT_COLS));
        return hovered;
    }

    private BlockColorEntry drawPaletteSlots(GuiGraphics g, int mouseX, int mouseY, Block[] slots,
                                             int originX, int originY, int cols) {
        BlockColorEntry hovered = null;
        for (int i = 0; i < slots.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = paletteSlotColX(col, originX);
            int cy = originY + row * CELL;
            drawInset(g, cx, cy, CELL, CELL);
            Block block = slots[i];
            if (block != null) {
                g.renderItem(new ItemStack(block), cx + 1, cy + 1);
                BlockColorEntry entry = BlockColorAnalyzer.get(block);
                if (inside(mouseX, mouseY, cx, cy, CELL, CELL)) {
                    g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, COL_HOVER);
                    hovered = entry;
                }
            }
        }
        return hovered;
    }

    private Block[] blocksFromSaved(SavedPalette sp) {
        Block[] slots = new Block[PALETTE_SLOT_COUNT];
        List<String> blocks = sp.blocks;
        for (int i = 0; i < slots.length && i < blocks.size(); i++) {
            String bid = blocks.get(i);
            if (bid != null && !bid.isEmpty()) {
                slots[i] = resolve(bid);
            }
        }
        return slots;
    }

    private int countSavedBlocks(SavedPalette sp) {
        int count = 0;
        for (String bid : sp.blocks) {
            if (bid != null && !bid.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void drawColumnDivider(GuiGraphics g, int y1, int y2) {
        g.fill(dividerX, y1, dividerX + 1, y2, COL_DIVIDER);
    }

    private void drawSectionRule(GuiGraphics g, int y) {
        g.fill(left + PAD, y, left + PANEL_W - PAD, y + 1, COL_DIVIDER);
    }

    /** Fixed-size gradient selection area; cells are stretched to tile it exactly at every zoom level. */
    private void drawGradientViewport(GuiGraphics g) {
        drawInset(g, gradientX, gridY, GRADIENT_AREA_W, gradientViewportH());
    }

    private ItemStack renderHotbar(GuiGraphics g, int mouseX, int mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack hovered = ItemStack.EMPTY;
        int slot = GuiTextures.HOTBAR_SLOT_SIZE;
        int itemInset = GuiTextures.HOTBAR_ITEM_INSET;
        for (int i = 0; i < COLS; i++) {
            int cx = hotbarX + i * slot;
            int cy = hotbarY;
            GuiTextures.blitHotbarSlot(g, cx, cy);
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, cx + itemInset, cy + itemInset);
                g.renderItemDecorations(this.font, stack, cx + itemInset, cy + itemInset);
            }
            if (inside(mouseX, mouseY, cx, cy, slot, slot)) {
                g.fill(cx + 1, cy + 1, cx + slot - 1, cy + slot - 1, COL_HOVER);
                hovered = stack;
            }
        }
        return hovered;
    }

    private BlockColorEntry renderSaved(GuiGraphics g, int mouseX, int mouseY) {
        drawLabel(g, "Saved palettes (" + savedPalettes.size() + ")", savedListX, top + 18);
        drawInset(g, savedListX, savedListY, savedListW, savedListRows * 16);
        drawColumnDivider(g, savedListY - 2, savedListY + savedListRows * 16 + 2);

        for (int r = 0; r < savedListRows; r++) {
            int idx = savedListScroll + r;
            if (idx >= savedPalettes.size()) {
                break;
            }
            int ry = savedListY + r * 16;
            boolean sel = idx == selectedSaved;
            g.fill(savedListX + 1, ry + 1, savedListX + savedListW - 1, ry + 15, sel ? COL_INSET_SEL : COL_INSET_BG);
            String name = savedPalettes.get(idx).name;
            g.drawString(this.font, trim(name, savedListW - 22), savedListX + 4, ry + 4, COL_TEXT, false);
            int xb = savedListX + savedListW - 15;
            drawActionButton(g, xb, ry + 1, GuiTextures.ACTION_DELETE, mouseX, mouseY);
        }

        if (savedPalettes.isEmpty()) {
            g.drawString(this.font, "Save palettes from the Creator tab.", savedListX + 4, savedListY + 4, COL_TEXT_MUTED, false);
        }

        drawLabel(g, "Preview", previewX, top + 18);

        if (selectedSaved >= 0 && selectedSaved < savedPalettes.size()) {
            SavedPalette sp = savedPalettes.get(selectedSaved);
            g.drawString(this.font, trim(sp.name, COLS * CELL), previewX, top + 30, COL_TEXT, false);
            g.drawString(this.font, countSavedBlocks(sp) + " blocks", previewX + 44, top + 46, COL_TEXT, false);
            return drawPaletteSlots(g, mouseX, mouseY, blocksFromSaved(sp), previewX, previewY, COLS);
        }
        g.drawString(this.font, "Select a palette to preview.", previewX, top + 30, COL_TEXT_MUTED, false);
        return null;
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        drawTab(g, tabX, tab0Y, tab == 0, new ItemStack(Palettable.PALETTE.get()), mouseX, mouseY);
        drawTab(g, tabX, tab1Y, tab == 1, new ItemStack(Items.BOOK), mouseX, mouseY);
    }

    private void drawTab(GuiGraphics g, int x, int y, boolean active, ItemStack icon, int mouseX, int mouseY) {
        GuiTextures.blitTab(g, active ? GuiTextures.TAB_ACTIVE : GuiTextures.TAB_INACTIVE, x, y);
        g.renderItem(icon, x + 5, y + 5);
        if (inside(mouseX, mouseY, x, y, TAB_W, TAB_H)) {
            g.fill(x + 1, y + 1, x + TAB_W - 1, y + TAB_H - 1, COL_HOVER);
        }
    }

    private BlockColorEntry orElse(BlockColorEntry current, BlockColorEntry candidate) {
        return current != null ? current : candidate;
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        GuiTextures.blitPanel(g, x, y, w, h);
    }

    /** Recessed slot / grid cell background ({@code slot.png}). */
    private void drawInset(GuiGraphics g, int x, int y, int w, int h) {
        GuiTextures.blitSlot(g, x, y, w, h);
    }

    /** Text input background ({@code text_field.png}). */
    private void drawTextField(GuiGraphics g, int x, int y, int w, int h) {
        GuiTextures.blitTextField(g, x, y, w, h);
    }

    private BlockColorEntry drawBigSlot(GuiGraphics g, int mouseX, int mouseY, int x, int y, Block block, boolean active, String label) {
        drawInset(g, x, y, CELL, CELL);
        BlockColorEntry entry = block == null ? null : BlockColorAnalyzer.get(block);
        if (block != null) {
            g.renderItem(new ItemStack(block), x + 1, y + 1);
        } else {
            g.drawString(this.font, label, x + CELL / 2 - this.font.width(label) / 2, y + 5, COL_TEXT, false);
        }
        if (active) {
            g.renderOutline(x, y, CELL, CELL, COL_ACTIVE);
        }
        if (inside(mouseX, mouseY, x, y, CELL, CELL)) {
            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, COL_HOVER);
            return entry;
        }
        return null;
    }

    private BlockColorEntry drawGrid(GuiGraphics g, int mouseX, int mouseY, List<BlockColorEntry> list,
                                     int x, int y, int rows, int scrollRow, int cellSize, int cols, boolean showSwatch) {
        BlockColorEntry hovered = null;
        int start = scrollRow * cols;
        for (int i = 0; i < cols * rows; i++) {
            int idx = start + i;
            if (idx >= list.size()) {
                break;
            }
            BlockColorEntry e = list.get(idx);
            int cx = x + (i % cols) * cellSize;
            int cy = y + (i / cols) * cellSize;
            drawInset(g, cx, cy, cellSize, cellSize);
            renderScaledItem(g, new ItemStack(e.block()), cx, cy, cellSize, cellSize);
            if (showSwatch && cellSize >= 10) {
                g.fill(cx + 1, cy + cellSize - 3, cx + cellSize - 1, cy + cellSize - 1, 0xFF000000 | (e.rgb() & 0xFFFFFF));
            }
            if (inside(mouseX, mouseY, cx, cy, cellSize, cellSize)) {
                g.fill(cx + 1, cy + 1, cx + cellSize - 1, cy + cellSize - 1, COL_HOVER);
                hovered = e;
            }
        }
        return hovered;
    }

    /** Gradient grid tiles the fixed viewport; column count and cell sizes adjust with zoom. */
    private BlockColorEntry drawGradientGrid(GuiGraphics g, int mouseX, int mouseY) {
        GradientLayout layout = gradientLayout();
        BlockColorEntry hovered = null;
        int start = gradientScrollRow * layout.cols;
        for (int row = 0; row < gradientRowCount; row++) {
            int cy = gradientRowY(row, layout.rowHeights);
            int rowH = layout.rowHeights[row];
            for (int col = 0; col < layout.cols; col++) {
                int idx = start + row * layout.cols + col;
                if (idx >= gradient.size()) {
                    return hovered;
                }
                BlockColorEntry e = gradient.get(idx);
                int cx = gradientColX(col, layout.colWidths);
                int cellW = layout.colWidths[col];
                drawInset(g, cx, cy, cellW, rowH);
                renderScaledItem(g, new ItemStack(e.block()), cx, cy, cellW, rowH);
                if (rowH >= 10 && cellW >= 4) {
                    g.fill(cx + 1, cy + rowH - 3, cx + cellW - 1, cy + rowH - 1, 0xFF000000 | (e.rgb() & 0xFFFFFF));
                }
                if (inside(mouseX, mouseY, cx, cy, cellW, rowH)) {
                    g.fill(cx + 1, cy + 1, cx + cellW - 1, cy + rowH - 1, COL_HOVER);
                    hovered = e;
                }
            }
        }
        return hovered;
    }

    /** Renders a 16x16 item icon scaled to fit inside a grid cell. */
    private void renderScaledItem(GuiGraphics g, ItemStack stack, int cx, int cy, int cellW, int cellH) {
        int inner = Math.min(cellW, cellH) - 2;
        float scale = inner / 16f;
        g.pose().pushPose();
        g.pose().translate(cx + 1, cy + 1, 0);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private void renderEntryTooltip(GuiGraphics g, BlockColorEntry e, int mouseX, int mouseY) {
        int rgb = e.rgb() & 0xFFFFFF;
        List<Component> lines = List.of(
                e.block().getName(),
                Component.literal(BuiltInRegistries.BLOCK.getKey(e.block()).toString()).withStyle(ChatFormatting.DARK_GRAY),
                Component.literal(String.format("#%06X", rgb)).withStyle(s -> s.withColor(rgb)));
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private String trim(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) {
            return s;
        }
        return this.font.plainSubstrByWidth(s, maxWidth - this.font.width("...")) + "...";
    }

    // ---- Input -----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingDeleteIndex >= 0 && handleDeleteConfirmClick(mouseX, mouseY)) {
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (inside(mouseX, mouseY, tabX, tab0Y, TAB_W, TAB_H)) {
            setTab(0);
            return true;
        }
        if (inside(mouseX, mouseY, tabX, tab1Y, TAB_W, TAB_H)) {
            setTab(1);
            return true;
        }

        if (tab == 0) {
            return creatorClicked(mouseX, mouseY, button);
        }
        return savedClicked(mouseX, mouseY, button);
    }

    private boolean handleDeleteConfirmClick(double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, deleteConfirmYesX, deleteConfirmBtnY, GuiTextures.ACTION_BTN_SIZE, GuiTextures.ACTION_BTN_SIZE)) {
            int index = pendingDeleteIndex;
            pendingDeleteIndex = -1;
            deleteSaved(index);
            return true;
        }
        if (inside(mouseX, mouseY, deleteConfirmNoX, deleteConfirmBtnY, GuiTextures.ACTION_BTN_SIZE, GuiTextures.ACTION_BTN_SIZE)) {
            cancelDeleteConfirm();
            return true;
        }
        if (!inside(mouseX, mouseY, deleteConfirmX, deleteConfirmY, deleteConfirmW, deleteConfirmH)) {
            cancelDeleteConfirm();
        }
        return true;
    }

    private boolean creatorClicked(double mouseX, double mouseY, int button) {
        // Route clicks anywhere in a recessed text field (not just the 8px text band) to its edit box.
        if (focusField(mouseX, mouseY, searchBox, searchX, rowSearchY, searchFieldW, 16)
                || focusField(mouseX, mouseY, maxBox, maxFieldX, rowControlsY, maxFieldW, 16)
                || focusField(mouseX, mouseY, nameBox, searchX, rowSaveY, 196, 16)) {
            return true;
        }

        if (inside(mouseX, mouseY, slotAX, slotsY, CELL, CELL)) {
            if (button == 1) {
                slotA = null;
                updateGradient();
            } else {
                activeSlot = 0;
            }
            return true;
        }
        if (inside(mouseX, mouseY, slotBX, slotsY, CELL, CELL)) {
            if (button == 1) {
                slotB = null;
                updateGradient();
            } else {
                activeSlot = 1;
            }
            return true;
        }

        int sIdx = cellIndex(mouseX, mouseY, searchX, gridY, gridRows, searchScrollRow, CELL, COLS);
        if (sIdx >= 0 && sIdx < searchResults.size()) {
            Block block = searchResults.get(sIdx).block();
            if (button == 1) {
                addToPaletteSlot(block);
            } else {
                assignToActiveSlot(block);
            }
            return true;
        }
        int gIdx = gradientCellIndex(mouseX, mouseY);
        if (gIdx >= 0 && gIdx < gradient.size()) {
            Block block = gradient.get(gIdx).block();
            if (button == 1) {
                addToPaletteSlot(block);
            } else {
                assignToActiveSlot(block);
            }
            return true;
        }

        int pIdx = paletteSlotIndex(mouseX, mouseY);
        if (pIdx >= 0) {
            if (button == 1) {
                paletteSlots[pIdx] = null;
            }
            return true;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            for (int i = 0; i < COLS; i++) {
                if (inside(mouseX, mouseY, hotbarX + i * GuiTextures.HOTBAR_SLOT_SIZE, hotbarY,
                        GuiTextures.HOTBAR_SLOT_SIZE, GuiTextures.HOTBAR_SLOT_SIZE)) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.getItem() instanceof BlockItem blockItem
                            && BlockColorAnalyzer.get(blockItem.getBlock()) != null) {
                        assignToActiveSlot(blockItem.getBlock());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean savedClicked(double mouseX, double mouseY, int button) {
        for (int r = 0; r < savedListRows; r++) {
            int idx = savedListScroll + r;
            if (idx >= savedPalettes.size()) {
                break;
            }
            int ry = savedListY + r * 16;
            int xb = savedListX + savedListW - 15;
            if (inside(mouseX, mouseY, xb, ry + 1, GuiTextures.ACTION_BTN_SIZE, GuiTextures.ACTION_BTN_SIZE)) {
                confirmDeleteSaved(idx);
                return true;
            }
            if (inside(mouseX, mouseY, savedListX, ry, savedListW, 15)) {
                selectSaved(idx);
                return true;
            }
        }
        return false;
    }

    private boolean focusField(double mx, double my, EditBox box, int x, int y, int w, int h) {
        if (box.visible && inside(mx, my, x, y, w, h)) {
            this.setFocused(box);
            box.setFocused(true);
            return true;
        }
        return false;
    }

    private void assignToActiveSlot(Block block) {
        if (activeSlot == 0) {
            slotA = block;
            if (slotB == null) {
                activeSlot = 1;
            }
        } else {
            slotB = block;
            if (slotA == null) {
                activeSlot = 0;
            }
        }
        updateGradient();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int dir = (int) Math.signum(scrollY);
        if (tab == 0) {
            if (inside(mouseX, mouseY, searchX, gridY, COLS * CELL, gridRows * CELL)) {
                searchScrollRow = clampScroll(searchScrollRow - dir, searchResults.size(), gridRows, COLS);
                return true;
            }
            if (inside(mouseX, mouseY, gradientX, gridY, GRADIENT_AREA_W, gradientViewportH())) {
                gradientScrollRow = clampScroll(gradientScrollRow - dir, gradient.size(), gradientVisibleRows(), gradientLayout().cols);
                return true;
            }
        } else {
            if (inside(mouseX, mouseY, savedListX, savedListY, savedListW, savedListRows * 16)) {
                int max = Math.max(0, savedPalettes.size() - savedListRows);
                savedListScroll = Math.max(0, Math.min(max, savedListScroll - dir));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int clampScroll(int row, int size, int visibleRows, int cols) {
        int totalRows = (size + cols - 1) / cols;
        int max = Math.max(0, totalRows - visibleRows);
        return Math.max(0, Math.min(max, row));
    }

    private int cellIndex(double mx, double my, int gx, int gy, int rows, int scrollRow, int cellSize, int cols) {
        if (mx < gx || my < gy) {
            return -1;
        }
        int col = (int) ((mx - gx) / cellSize);
        int row = (int) ((my - gy) / cellSize);
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return -1;
        }
        return (scrollRow + row) * cols + col;
    }

    private int gradientCellIndex(double mx, double my) {
        if (!inside(mx, my, gradientX, gridY, GRADIENT_AREA_W, gradientViewportH())) {
            return -1;
        }
        GradientLayout layout = gradientLayout();
        int col = -1;
        int x = gradientX;
        for (int c = 0; c < layout.cols; c++) {
            if (mx >= x && mx < x + layout.colWidths[c]) {
                col = c;
                break;
            }
            x += layout.colWidths[c];
        }
        if (col < 0) {
            return -1;
        }
        int row = -1;
        int y = gridY;
        for (int r = 0; r < gradientRowCount; r++) {
            if (my >= y && my < y + layout.rowHeights[r]) {
                row = r;
                break;
            }
            y += layout.rowHeights[r];
        }
        if (row < 0) {
            return -1;
        }
        return (gradientScrollRow + row) * layout.cols + col;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void onClose() {
        saveGuiSettings();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Threshold slider with editable track/handle PNGs. */
    private class ThresholdSlider extends TexturedSlider {
        ThresholdSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), clampValue(threshold / MAX_THRESHOLD));
            updateMessage();
        }

        void syncFromThreshold() {
            this.value = clampValue(threshold / MAX_THRESHOLD);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format(Locale.ROOT, "Thresh: %.1f", threshold)));
        }

        @Override
        protected void applyValue() {
            threshold = this.value * MAX_THRESHOLD;
            updateGradient();
        }
    }
}

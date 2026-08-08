package io.github.urntt.litematicacreator.gui;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.GuiSchematicLoadedList;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo;
import fi.dy.masa.malilib.gui.wrappers.TextFieldType;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.urntt.litematicacreator.Reference;
import io.github.urntt.litematicacreator.config.Configs;
import io.github.urntt.litematicacreator.config.CreatorExportRegionMode;
import io.github.urntt.litematicacreator.creator.CreatorFocus;
import io.github.urntt.litematicacreator.creator.CreatorManager;
import io.github.urntt.litematicacreator.export.CreatorBindingResult;
import io.github.urntt.litematicacreator.export.CreatorBindingReservation;
import io.github.urntt.litematicacreator.export.CreatorExportMetadata;
import io.github.urntt.litematicacreator.export.CreatorExportOperation;
import io.github.urntt.litematicacreator.export.CreatorExportPreview;
import io.github.urntt.litematicacreator.export.CreatorExportWriteResult;
import io.github.urntt.litematicacreator.export.CreatorPreparedExport;
import io.github.urntt.litematicacreator.export.CreatorSchematicBindingService;
import io.github.urntt.litematicacreator.export.CreatorSchematicExportService;
import io.github.urntt.litematicacreator.mixin.LitematicaSchematicAccessor;
import io.github.urntt.litematicacreator.recovery.CreatorRecoveryManager;

public final class CreatorSchematicManagerScreen extends GuiBase
{
    private static final int ROW_HEIGHT = 22;
    private static final int PANEL_TOP = 50;
    private static final int PANEL_BOTTOM = 32;
    private static final int CONTROL_GAP = 4;
    private static final Identifier PREVIEW_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "manager_preview");

    private final Screen navigationParent;
    private ManagerTab tab = ManagerTab.OVERVIEW;
    private String searchQuery = "";
    private int listScroll;
    @Nullable private LitematicaSchematic inspectedSchematic;
    @Nullable private SchematicPlacement inspectedPlacement;
    @Nullable private SchematicPlacement samplingPlacement;
    @Nullable private CreatorExportPreview exportPreview;
    @Nullable private DynamicTexture previewTexture;
    private String status = "";
    private boolean exportRunning;
    private boolean previewRunning;
    private boolean searchRefreshQueued;
    private boolean restoreSearchFocus;
    private int searchCursor;
    private int overviewDetailsY = 190;
    private int placementDetailsY = 166;
    private int savePreviewY = 270;
    @Nullable private Confirmation pendingConfirmation;
    @Nullable private LitematicaSchematic saveFormSchematic;
    private String saveDirectory = "";
    private String saveFileName = "";
    private String saveInternalName = "";
    private String saveAuthor = "";
    private String saveDescription = "";

    @Nullable private GuiTextFieldGeneric searchField;
    @Nullable private GuiTextFieldGeneric nameField;
    @Nullable private GuiTextFieldGeneric authorField;
    @Nullable private GuiTextFieldGeneric descriptionField;
    @Nullable private GuiTextFieldGeneric placementNameField;
    @Nullable private GuiTextFieldGeneric directoryField;
    @Nullable private GuiTextFieldGeneric fileNameField;
    @Nullable private GuiTextFieldGeneric exportNameField;
    @Nullable private GuiTextFieldGeneric exportAuthorField;
    @Nullable private GuiTextFieldGeneric exportDescriptionField;

    private CreatorSchematicManagerScreen(@Nullable Screen navigationParent)
    {
        this.navigationParent = navigationParent;
        this.title = StringUtils.translate("litematica-creator.gui.title.schematic_manager");
        this.setParent(navigationParent);
    }

    public static void open(@Nullable Screen parent)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null)
        {
            return;
        }

        GuiBase.openGui(new CreatorSchematicManagerScreen(parent));
    }

    public static void openFromLitematica(GuiBase source)
    {
        open(source.getParent());
    }

    @Override
    public void initGui()
    {
        this.captureSaveForm();
        this.directoryField = null;
        this.fileNameField = null;
        this.exportNameField = null;
        this.exportAuthorField = null;
        this.exportDescriptionField = null;
        this.releasePreviewTexture();
        super.initGui();
        this.ensureInspectionIsValid();
        this.ensureSaveForm();
        this.createNavigationButtons();
        this.createSearchAndList();
        this.createTabs();

        if (this.inspectedSchematic != null)
        {
            switch (this.tab)
            {
                case OVERVIEW -> this.createOverviewControls();
                case PLACEMENT -> this.createPlacementControls();
                case SAVE_EXPORT -> this.createSaveControls();
            }
        }

        this.createPreviewTexture();
    }

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY)
    {
        super.drawScreenBackground(ctx, mouseX, mouseY);
        int leftWidth = this.leftWidth();
        int panelHeight = Math.max(0, this.height - PANEL_TOP - PANEL_BOTTOM);
        RenderUtils.drawRect(ctx, 8, PANEL_TOP, leftWidth - 12, panelHeight, 0x70000000);
        RenderUtils.drawRect(ctx, leftWidth, PANEL_TOP, this.width - leftWidth - 8, panelHeight, 0x50000000);
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        int leftWidth = this.leftWidth();

        if (this.inspectedSchematic == null)
        {
            this.drawString(ctx, tr("litematica-creator.gui.manager.empty"), leftWidth + 14, 62, COLOR_WHITE);
        }
        else
        {
            switch (this.tab)
            {
                case OVERVIEW -> this.drawOverview(ctx, leftWidth);
                case PLACEMENT -> this.drawPlacement(ctx, leftWidth);
                case SAVE_EXPORT -> this.drawSave(ctx, leftWidth);
            }
        }

        if (!this.status.isEmpty())
        {
            this.drawWrappedStatus(ctx, leftWidth + 12, this.height - 49, this.width - leftWidth - 26);
        }
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (mouseX < this.leftWidth() && mouseY >= 50 && mouseY < this.height - 32)
        {
            int rows = this.visibleRows();
            int max = Math.max(0, this.filteredRows().size() - rows);
            this.listScroll = Math.max(0, Math.min(max, this.listScroll - (int) verticalAmount));
            this.initGui();
            return true;
        }

        return super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void tick()
    {
        super.tick();

        if (this.searchRefreshQueued && Minecraft.getInstance().gui.screen() == this)
        {
            this.searchRefreshQueued = false;
            this.initGui();
        }
    }

    @Override
    public void onClose()
    {
        this.releasePreviewTexture();
        super.onClose();
    }

    private void createNavigationButtons()
    {
        int y = this.height - 26;
        String close = tr("gui.done");
        int right = this.width - 10;
        int doneWidth = this.getStringWidth(close) + 16;
        int doneX = right - doneWidth;
        this.addActionButton(doneX, y, doneWidth, close, "litematica-creator.gui.manager.hover.done", () -> this.closeGui(true));

        String clearSelectedLabel = tr("litematica-creator.gui.manager.clear_selected");
        int clearSelectedWidth = this.getStringWidth(clearSelectedLabel) + 16;
        int clearSelectedX = doneX - CONTROL_GAP - clearSelectedWidth;
        ButtonGeneric clearSelected = this.addActionButton(
                clearSelectedX, y, clearSelectedWidth, clearSelectedLabel,
                "litematica-creator.gui.manager.hover.clear_selected", this::clearSelected
        );
        clearSelected.setEnabled(DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement() != null);

        String clearFocusLabel = tr("litematica-creator.gui.manager.clear_focus");
        int clearFocusWidth = this.getStringWidth(clearFocusLabel) + 16;
        int clearFocusX = clearSelectedX - CONTROL_GAP - clearFocusWidth;
        ButtonGeneric clearFocus = this.addActionButton(
                clearFocusX, y, clearFocusWidth, clearFocusLabel,
                "litematica-creator.gui.manager.hover.clear_focus", this::clearCreatorFocus
        );
        clearFocus.setEnabled(CreatorManager.getInstance().getFocus() != null);

        String[] labels = {
                tr("litematica-creator.gui.manager.nav.main"),
                tr("litematica-creator.gui.manager.nav.loaded"),
                tr("litematica-creator.gui.manager.nav.placements")
        };
        int naturalWidth = Math.max(this.getStringWidth(labels[0]), Math.max(this.getStringWidth(labels[1]), this.getStringWidth(labels[2]))) + 16;
        int availableWidth = Math.max(90, clearFocusX - 18);
        int navWidth = Math.max(28, Math.min(naturalWidth, (availableWidth - CONTROL_GAP * 2) / 3));
        int x = 10;
        this.addActionButton(x, y, navWidth, this.clampLabel(labels[0], navWidth - 8), "litematica-creator.gui.manager.hover.nav.main", () -> this.openPeer(new GuiMainMenu()));
        x += navWidth + CONTROL_GAP;
        this.addActionButton(x, y, navWidth, this.clampLabel(labels[1], navWidth - 8), "litematica-creator.gui.manager.hover.nav.loaded", () -> this.openPeer(new GuiSchematicLoadedList()));
        x += navWidth + CONTROL_GAP;
        this.addActionButton(x, y, navWidth, this.clampLabel(labels[2], navWidth - 8), "litematica-creator.gui.manager.hover.nav.placements", () -> this.openPeer(new GuiSchematicPlacementsList()));
    }

    private void createSearchAndList()
    {
        int leftWidth = this.leftWidth();
        this.searchField = new GuiTextFieldGeneric(10, 29, leftWidth - 20, 16, this.font);
        this.searchField.setMaxLengthWrapper(128);
        this.searchField.setValueWrapper(this.searchQuery);
        this.searchField.setSuggestion(tr("litematica-creator.gui.manager.search_hint"));
        this.searchField.setHoverTooltip("litematica-creator.gui.manager.hover.search");
        this.addTextField(this.searchField, this::onSearchChanged, TextFieldType.STRING);

        if (this.restoreSearchFocus)
        {
            this.searchField.setFocusedWrapper(true);
            this.searchField.setCursorPosition(Math.min(this.searchCursor, this.searchQuery.length()));
            this.restoreSearchFocus = false;
        }

        List<ManagerRow> rows = this.filteredRows();
        this.listScroll = Math.min(this.listScroll, Math.max(0, rows.size() - this.visibleRows()));
        int end = Math.min(rows.size(), this.listScroll + this.visibleRows());
        int y = 54;

        for (int index = this.listScroll; index < end; index++)
        {
            ManagerRow row = rows.get(index);
            int rowWidth = leftWidth - 22;
            ButtonGeneric button = new ButtonGeneric(12, y, rowWidth, 20, this.rowLabel(row, rowWidth - 10));
            boolean selected = row.placement != null ? row.placement == this.inspectedPlacement :
                    row.schematic == this.inspectedSchematic && this.inspectedPlacement == null;
            button.setEnabled(!selected);
            this.addButton(button, (pressed, mouseButton) -> this.inspect(row));
            this.addWidget(new WidgetHoverInfo(12, y, rowWidth, 20, this.rowHoverText(row)));
            y += ROW_HEIGHT;
        }
    }

    private void createTabs()
    {
        int x = this.leftWidth() + 10;
        int y = 27;

        for (ManagerTab value : ManagerTab.values())
        {
            ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, value.displayName());
            boolean available = value != ManagerTab.PLACEMENT || this.hasPlacements(this.inspectedSchematic);
            button.setEnabled(this.tab != value && available);
            this.addButton(button, (pressed, mouseButton) -> {
                if (value == ManagerTab.PLACEMENT && this.inspectedPlacement == null)
                {
                    this.inspectedPlacement = this.preferredPlacement(this.inspectedSchematic);
                }
                this.tab = value;
                this.status = "";
                this.initGui();
            });
            this.addWidget(new WidgetHoverInfo(
                    x, y, button.getWidth(), 20,
                    "litematica-creator.gui.manager.hover.tab." + value.key
            ));
            x += button.getWidth() + 3;
        }
    }

    private void createOverviewControls()
    {
        PageLayout layout = this.pageLayout(ManagerTab.OVERVIEW);
        int x = layout.controlX;
        int y = 60;
        int width = layout.controlWidth;
        SchematicMetadata metadata = this.inspectedSchematic.getMetadata();
        this.nameField = this.addField(x, y, width, metadata.getName(), 256, "litematica-creator.gui.manager.hover.field.name");
        this.authorField = this.addField(x, y + 24, width, metadata.getAuthor(), 256, "litematica-creator.gui.manager.hover.field.author");
        this.descriptionField = this.addField(x, y + 48, width, metadata.getDescription(), 1024, "litematica-creator.gui.manager.hover.field.description");
        y += 74;
        y = this.addButtonGrid(x, y, width, List.of(
                new ButtonSpec(tr("litematica-creator.gui.manager.apply"), "litematica-creator.gui.manager.hover.apply", this::applyMetadata, true),
                new ButtonSpec(tr("litematica-creator.gui.manager.thumbnail.capture"), "litematica-creator.gui.manager.hover.thumbnail.capture", this::captureThumbnail, true),
                new ButtonSpec(tr("litematica-creator.gui.manager.thumbnail.clear"), "litematica-creator.gui.manager.hover.thumbnail.clear", this::clearThumbnail, this.previewTexture != null)
        ));
        y = this.addButtonGrid(x, y, width, List.of(
                new ButtonSpec(tr("litematica-creator.gui.manager.reload"), "litematica-creator.gui.manager.hover.reload", this::reloadSchematic, this.inspectedSchematic.getFile() != null),
                new ButtonSpec(tr("litematica-creator.gui.manager.unload"), "litematica-creator.gui.manager.hover.unload", this::confirmUnload, !this.exportRunning)
        ));
        this.overviewDetailsY = y + 4;
    }

    private void createPlacementControls()
    {
        if (this.inspectedPlacement == null)
        {
            return;
        }

        PageLayout layout = this.pageLayout(ManagerTab.PLACEMENT);
        int x = layout.controlX;
        int y = 60;
        int width = layout.controlWidth;
        this.placementNameField = this.addField(
                x, y, width, this.inspectedPlacement.getName(), 256,
                "litematica-creator.gui.manager.hover.field.placement_name"
        );
        y += 26;
        String enabledLabel = tr(this.inspectedPlacement.isEnabled() ?
                "litematica-creator.gui.manager.disable_placement" : "litematica-creator.gui.manager.enable_placement");
        String renderLabel = tr(this.inspectedPlacement.isRenderingEnabled() ?
                "litematica-creator.gui.manager.hide_placement" : "litematica-creator.gui.manager.show_placement");
        y = this.addButtonGrid(x, y, width, List.of(
                new ButtonSpec(tr("litematica-creator.gui.manager.rename"), "litematica-creator.gui.manager.hover.rename", this::renamePlacement, true),
                new ButtonSpec(enabledLabel, "litematica-creator.gui.manager.hover.enabled", () -> {
                    this.inspectedPlacement.toggleEnabled();
                    this.status = tr(this.inspectedPlacement.isEnabled() ?
                            "litematica-creator.gui.manager.placement_enabled" : "litematica-creator.gui.manager.placement_disabled");
                    this.initGui();
                }, true),
                new ButtonSpec(renderLabel, "litematica-creator.gui.manager.hover.render", () -> {
                    this.inspectedPlacement.setRenderSchematic(!this.inspectedPlacement.isRenderingEnabled());
                    this.status = tr(this.inspectedPlacement.isRenderingEnabled() ?
                            "litematica-creator.gui.manager.placement_shown" : "litematica-creator.gui.manager.placement_hidden");
                    this.initGui();
                }, true)
        ));
        CreatorFocus focus = CreatorManager.getInstance().getFocus();
        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        y = this.addButtonGrid(x, y, width, List.of(
                new ButtonSpec(tr("litematica-creator.gui.manager.set_focus"), "litematica-creator.gui.manager.hover.set_focus", this::setFocus, focus == null || focus.placement() != this.inspectedPlacement),
                new ButtonSpec(tr("litematica-creator.gui.manager.set_selected"), "litematica-creator.gui.manager.hover.set_selected", this::setSelected, selected != this.inspectedPlacement)
        ));
        y = this.addButtonGrid(x, y, width, List.of(
                new ButtonSpec(tr("litematica-creator.gui.manager.native_config"), "litematica-creator.gui.manager.hover.native_config", this::openPlacementConfiguration, true),
                new ButtonSpec(tr("litematica-creator.gui.manager.remove_placement"), "litematica-creator.gui.manager.hover.remove_placement", this::confirmRemovePlacement, !this.exportRunning)
        ));
        this.placementDetailsY = y + 4;
    }

    private void createSaveControls()
    {
        PageLayout layout = this.pageLayout(ManagerTab.SAVE_EXPORT);
        int x = layout.controlX;
        int y = 60;
        int width = layout.controlWidth;
        this.directoryField = this.addField(x, y, width, this.saveDirectory, 2048, "litematica-creator.gui.manager.hover.field.directory");
        this.fileNameField = this.addField(x, y + 24, width, this.saveFileName, 256, "litematica-creator.gui.manager.hover.field.file_name");
        this.exportNameField = this.addField(x, y + 48, width, this.saveInternalName, 256, "litematica-creator.gui.manager.hover.field.export_name");
        this.exportAuthorField = this.addField(x, y + 72, width, this.saveAuthor, 256, "litematica-creator.gui.manager.hover.field.export_author");
        this.exportDescriptionField = this.addField(x, y + 96, width, this.saveDescription, 1024, "litematica-creator.gui.manager.hover.field.export_description");
        y += 120;
        CreatorExportRegionMode mode = this.exportMode();
        ButtonGeneric modeButton = this.addActionButton(
                x, y, width, mode.getDisplayName(),
                "litematica-creator.gui.manager.hover.export_mode." + mode.getStringValue(), () -> {
            CreatorExportRegionMode next = mode.cycle(true);
            Configs.Generic.CREATOR_EXPORT_REGION_MODE.setOptionListValue(next);
            this.exportPreview = null;
            this.status = tr("litematica-creator.gui.manager.mode_changed", next.getDisplayName());
            this.initGui();
        });
        modeButton.setEnabled(!this.exportRunning);
        y += 24;

        if (mode == CreatorExportRegionMode.ENCLOSING_WITH_WORLD)
        {
            ButtonGeneric sampling = this.addActionButton(
                    x, y, width, this.samplingPlacementLabel(),
                    "litematica-creator.gui.manager.hover.sampling", this::cycleSamplingPlacement
            );
            sampling.setEnabled(!this.exportRunning && this.hasPlacements(this.inspectedSchematic));
            y += 24;
        }

        int previewWidth = Math.min(width, Math.max(108, this.getStringWidth(tr("litematica-creator.gui.manager.preview")) + 16));
        ButtonGeneric preview = this.addActionButton(
                x, y, previewWidth, tr("litematica-creator.gui.manager.preview"),
                "litematica-creator.gui.manager.hover.preview", this::refreshExportPreview
        );
        preview.setEnabled(!this.exportRunning);
        y += 24;
        y = this.addButtonGrid(x, y, width, List.of(
                new ButtonSpec(tr("litematica-creator.gui.manager.save"), "litematica-creator.gui.manager.hover.save", () -> this.beginExport(CreatorExportOperation.SAVE), !this.exportRunning),
                new ButtonSpec(tr("litematica-creator.gui.manager.save_bind"), "litematica-creator.gui.manager.hover.save_bind", () -> this.beginExport(CreatorExportOperation.SAVE_AS_AND_BIND), !this.exportRunning),
                new ButtonSpec(tr("litematica-creator.gui.manager.export_copy"), "litematica-creator.gui.manager.hover.export_copy", () -> this.beginExport(CreatorExportOperation.EXPORT_COPY), !this.exportRunning)
        ));
        this.savePreviewY = y + 4;
    }

    private void drawOverview(GuiContext ctx, int leftWidth)
    {
        SchematicMetadata metadata = this.inspectedSchematic.getMetadata();
        PageLayout layout = this.pageLayout(ManagerTab.OVERVIEW);
        int x = layout.labelX;
        this.drawString(ctx, tr("litematica-creator.gui.manager.name"), x, 64, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.author"), x, 88, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.description"), x, 112, COLOR_WHITE);

        int previewSize = Math.max(64, Math.min(96, layout.contentWidth / 5));
        boolean showThumbnail = layout.contentWidth >= 360;
        int previewX = layout.right - previewSize;
        int textWidth = showThumbnail ? Math.max(80, previewX - x - 12) : layout.right - x;
        int y = this.overviewDetailsY;
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.path", displayPath(this.inspectedSchematic.getFile())), x, y, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.created", formatTime(metadata.getTimeCreated())), x, y + 12, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.modified", formatTime(metadata.getTimeModified())), x, y + 24, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.state", this.stateText(this.inspectedSchematic)), x, y + 36, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.counts", metadata.getRegionCount(), metadata.getTotalBlocks(), metadata.getTotalVolume()), x, y + 48, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.enclosing", metadata.getEnclosingSizeAsBlockPos().toShortString()), x, y + 60, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.attached", this.entityCount(), this.blockEntityCount()), x, y + 72, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.placements", this.placementCount()), x, y + 84, textWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.ticks", this.blockTickCount(), this.fluidTickCount()), x, y + 96, textWidth, COLOR_WHITE);

        if (showThumbnail)
        {
            this.drawString(ctx, tr("litematica-creator.gui.manager.thumbnail.title"), previewX, y, COLOR_WHITE);
            int imageY = y + 14;
            RenderUtils.drawRect(ctx, previewX - 1, imageY - 1, previewSize + 2, previewSize + 2, 0xFF8A8A8A);
            RenderUtils.drawRect(ctx, previewX, imageY, previewSize, previewSize, 0xCC101010);

            if (this.previewTexture != null)
            {
                ctx.blit(RenderPipelines.GUI_TEXTURED, PREVIEW_TEXTURE, previewX, imageY, 0, 0, previewSize, previewSize, previewSize, previewSize);
            }
            else
            {
                this.drawClampedString(ctx, tr("litematica-creator.gui.manager.thumbnail.none"), previewX + 5, imageY + previewSize / 2 - 4, previewSize - 10, 0xFFAAAAAA);
            }
        }
    }

    private void drawPlacement(GuiContext ctx, int leftWidth)
    {
        PageLayout layout = this.pageLayout(ManagerTab.PLACEMENT);
        int x = layout.labelX;

        if (this.inspectedPlacement == null)
        {
            this.drawString(ctx, tr("litematica-creator.gui.manager.no_placement"), x, 65, COLOR_WHITE);
            return;
        }

        SchematicPlacement placement = this.inspectedPlacement;
        int maxWidth = layout.right - x;
        int y = this.placementDetailsY;
        CreatorFocus focus = CreatorManager.getInstance().getFocus();
        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        this.drawString(ctx, tr("litematica-creator.gui.manager.placement_name"), x, 64, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.origin", placement.getOrigin().toShortString()), x, y, maxWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.transform", placement.getRotation(), placement.getMirror()), x, y + 12, maxWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.placement_state", this.yesNo(placement.isEnabled()), this.yesNo(placement.isRenderingEnabled())), x, y + 24, maxWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.focus_state", this.yesNo(focus != null && focus.placement() == placement)), x, y + 36, maxWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.selected_state", this.yesNo(selected == placement)), x, y + 48, maxWidth, COLOR_WHITE);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.subregions", placement.getSubRegionCount()), x, y + 60, maxWidth, COLOR_WHITE);
    }

    private void drawSave(GuiContext ctx, int leftWidth)
    {
        PageLayout layout = this.pageLayout(ManagerTab.SAVE_EXPORT);
        int x = layout.labelX;
        this.drawString(ctx, tr("litematica-creator.gui.manager.directory"), x, 65, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.file_name"), x, 89, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.export_name"), x, 113, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.author"), x, 137, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.description"), x, 161, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.export_mode"), x, 185, COLOR_WHITE);

        if (this.exportMode() == CreatorExportRegionMode.ENCLOSING_WITH_WORLD)
        {
            this.drawString(ctx, tr("litematica-creator.gui.manager.sampling"), x, 209, COLOR_WHITE);
        }

        int y = this.savePreviewY;
        int maxWidth = layout.right - x;
        this.drawString(ctx, tr("litematica-creator.gui.manager.preview_heading"), x, y, 0xFFFFFFFF);
        y += 14;

        if (this.previewRunning)
        {
            this.drawClampedString(ctx, tr("litematica-creator.gui.manager.previewing"), x, y, maxWidth, 0xFFFFFF55);
            y += 12;
        }
        else if (this.exportPreview == null)
        {
            this.drawClampedString(ctx, tr("litematica-creator.gui.manager.preview_empty"), x, y, maxWidth, 0xFFAAAAAA);
            y += 12;
        }
        else
        {
            this.drawClampedString(ctx, tr("litematica-creator.gui.manager.preview_regions", this.exportPreview.regionCount()), x, y, maxWidth, COLOR_WHITE);
            this.drawClampedString(ctx, tr("litematica-creator.gui.manager.preview_blocks", this.exportPreview.totalBlocks()), x, y + 12, maxWidth, COLOR_WHITE);
            this.drawClampedString(ctx, tr("litematica-creator.gui.manager.preview_volume", this.exportPreview.totalVolume()), x, y + 24, maxWidth, COLOR_WHITE);
            this.drawClampedString(ctx, tr("litematica-creator.gui.manager.preview_size", this.exportPreview.enclosingSize().toShortString()), x, y + 36, maxWidth, COLOR_WHITE);
            this.drawClampedString(ctx, tr("litematica-creator.gui.manager.preview_version", LitematicaSchematic.SCHEMATIC_VERSION), x, y + 48, maxWidth, COLOR_WHITE);
            y += 60;
        }

        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.current_binding", displayPath(this.inspectedSchematic.getFile())), x, y, maxWidth, 0xFFCCCCCC);
        this.drawClampedString(ctx, tr("litematica-creator.gui.manager.output_target", this.displayOutputTarget()), x, y + 12, maxWidth, 0xFFCCCCCC);
    }

    private void inspect(ManagerRow row)
    {
        this.captureSaveForm();
        this.inspectedSchematic = row.schematic;
        this.inspectedPlacement = row.placement != null ? row.placement :
                (this.tab == ManagerTab.PLACEMENT ? this.preferredPlacement(row.schematic) : null);
        this.samplingPlacement = this.defaultSamplingPlacement(row.schematic, this.inspectedPlacement);
        this.saveFormSchematic = null;
        this.exportPreview = null;
        this.pendingConfirmation = null;
        this.status = "";
        this.initGui();
    }

    private void applyMetadata()
    {
        SchematicMetadata metadata = this.inspectedSchematic.getMetadata();
        CreatorExportMetadata previous = CreatorExportMetadata.from(metadata);
        metadata.setName(this.nameField.getValueWrapper());
        metadata.setAuthor(this.authorField.getValueWrapper());
        metadata.setDescription(this.descriptionField.getValueWrapper());

        if (this.saveFormSchematic == this.inspectedSchematic)
        {
            if (this.saveInternalName.equals(previous.name()))
            {
                this.saveInternalName = metadata.getName();
            }
            if (this.saveAuthor.equals(previous.author()))
            {
                this.saveAuthor = metadata.getAuthor();
            }
            if (this.saveDescription.equals(previous.description()))
            {
                this.saveDescription = metadata.getDescription();
            }
        }

        this.markMetadataChanged();
        this.status = tr("litematica-creator.gui.manager.metadata_applied");
        this.initGui();
    }

    private void clearThumbnail()
    {
        this.inspectedSchematic.getMetadata().setPreviewImagePixelData(null);
        this.markMetadataChanged();
        this.status = tr("litematica-creator.gui.manager.thumbnail_cleared");
        this.initGui();
    }

    private void captureThumbnail()
    {
        LitematicaSchematic schematic = this.inspectedSchematic;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(null);
        minecraft.execute(() -> Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), screenshot -> {
            try (screenshot; NativeImage scaled = new NativeImage(120, 120, false))
            {
                int x = screenshot.getWidth() >= screenshot.getHeight() ? (screenshot.getWidth() - screenshot.getHeight()) / 2 : 0;
                int y = screenshot.getHeight() >= screenshot.getWidth() ? (screenshot.getHeight() - screenshot.getWidth()) / 2 : 0;
                int side = Math.min(screenshot.getWidth(), screenshot.getHeight());
                screenshot.resizeSubRectTo(x, y, side, side, scaled);
                @SuppressWarnings("deprecation") int[] pixels = scaled.makePixelArray();
                schematic.getMetadata().setPreviewImagePixelData(pixels);
                this.markMetadataChanged(schematic);
                this.status = tr("litematica-creator.gui.manager.thumbnail_updated");
            }
            catch (Exception exception)
            {
                this.status = localizedErrorMessage(exception);
            }
            finally
            {
                minecraft.execute(() -> GuiBase.openGui(this));
            }
        }));
    }

    private void renamePlacement()
    {
        if (this.inspectedPlacement != null)
        {
            this.inspectedPlacement.setName(this.placementNameField.getValueWrapper());
            this.status = tr("litematica-creator.gui.manager.placement_renamed");
            this.initGui();
        }
    }

    private void setFocus()
    {
        if (this.inspectedPlacement != null)
        {
            CreatorManager.getInstance().focusPlacement(this.inspectedPlacement);
            this.status = tr("litematica-creator.gui.manager.focus_set", this.inspectedPlacement.getName());
        }

        this.initGui();
    }

    private void clearCreatorFocus()
    {
        CreatorManager.getInstance().clearFocus();
        this.status = tr("litematica-creator.gui.manager.focus_cleared");
        this.initGui();
    }

    private void setSelected()
    {
        if (this.inspectedPlacement != null)
        {
            DataManager.getSchematicPlacementManager().setSelectedSchematicPlacement(this.inspectedPlacement);
            this.status = tr("litematica-creator.gui.manager.selected_set", this.inspectedPlacement.getName());
        }

        this.initGui();
    }

    private void clearSelected()
    {
        DataManager.getSchematicPlacementManager().setSelectedSchematicPlacement(null);
        this.status = tr("litematica-creator.gui.manager.selected_cleared");
        this.initGui();
    }

    private void openPlacementConfiguration()
    {
        if (this.inspectedPlacement != null)
        {
            GuiPlacementConfiguration gui = new GuiPlacementConfiguration(this.inspectedPlacement);
            gui.setParent(this);
            GuiBase.openGui(gui);
        }
    }

    private void reloadSchematic()
    {
        Confirmation confirmation = new Confirmation("reload", Integer.toHexString(System.identityHashCode(this.inspectedSchematic)));

        if (this.inspectedSchematic.getMetadata().wasModifiedSinceSaved() && !confirmation.equals(this.pendingConfirmation))
        {
            this.pendingConfirmation = confirmation;
            this.status = tr("litematica-creator.gui.manager.confirm_reload");
            return;
        }

        this.pendingConfirmation = null;
        boolean success = CreatorSchematicBindingService.getInstance().reloadBoundSchematic(this.inspectedSchematic);
        this.status = tr(success ? "litematica-creator.gui.manager.reload_success" : "litematica-creator.gui.manager.reload_failed");
        this.initGui();
    }

    private void confirmUnload()
    {
        if (this.exportRunning)
        {
            return;
        }

        Confirmation marker = new Confirmation("unload", Integer.toHexString(System.identityHashCode(this.inspectedSchematic)));

        if (!marker.equals(this.pendingConfirmation))
        {
            this.pendingConfirmation = marker;
            this.status = tr("litematica-creator.gui.manager.confirm_unload");
            return;
        }

        CreatorRecoveryManager.getInstance().discardSchematic(this.inspectedSchematic);
        SchematicHolder.getInstance().removeSchematic(this.inspectedSchematic);
        this.inspectedSchematic = null;
        this.inspectedPlacement = null;
        this.pendingConfirmation = null;
        this.status = tr("litematica-creator.gui.manager.unloaded");
        this.initGui();
    }

    private void confirmRemovePlacement()
    {
        if (this.inspectedPlacement == null)
        {
            return;
        }

        Confirmation marker = new Confirmation("remove-placement", this.inspectedPlacement.getHashId().toString());

        if (!marker.equals(this.pendingConfirmation))
        {
            this.pendingConfirmation = marker;
            this.status = tr("litematica-creator.gui.manager.confirm_remove_placement");
            return;
        }

        DataManager.getSchematicPlacementManager().removeSchematicPlacement(this.inspectedPlacement);
        this.inspectedPlacement = null;
        this.pendingConfirmation = null;
        this.status = tr("litematica-creator.gui.manager.placement_removed");
        this.initGui();
    }

    private void refreshExportPreview()
    {
        if (this.exportRunning)
        {
            return;
        }

        try
        {
            this.captureSaveForm();
            this.exportRunning = true;
            this.previewRunning = true;
            this.status = tr("litematica-creator.gui.manager.previewing");
            this.initGui();
            CreatorSchematicExportService.getInstance().previewAsync(
                    this.inspectedSchematic,
                    this.exportMode(),
                    this.samplingPlacement
            ).whenComplete((preview, error) -> Minecraft.getInstance().execute(() -> {
                this.exportRunning = false;
                this.previewRunning = false;

                if (error != null)
                {
                    this.status = localizedErrorMessage(error);
                }
                else
                {
                    this.exportPreview = preview;
                    this.status = tr("litematica-creator.gui.manager.preview_ready");
                }

                this.refreshIfOpen();
            }));
        }
        catch (Exception exception)
        {
            this.exportRunning = false;
            this.previewRunning = false;
            this.status = localizedErrorMessage(exception);
        }
    }

    private void beginExport(CreatorExportOperation operation)
    {
        if (this.exportRunning)
        {
            return;
        }

        CreatorBindingReservation reservation = null;

        try
        {
            this.captureSaveForm();
            Path target = CreatorSchematicExportService.ensureExtension(this.resolveTarget(operation));

            String conflict = CreatorSchematicBindingService.getInstance().validateWriteTarget(
                    this.inspectedSchematic,
                    target,
                    operation
            );

            if (conflict != null)
            {
                this.status = localizeKnownMessage(conflict);
                return;
            }

            Confirmation overwrite = new Confirmation(operation.name(), target.toString());
            boolean currentBoundSave = operation == CreatorExportOperation.SAVE &&
                    samePath(this.inspectedSchematic.getFile(), target);

            if (!currentBoundSave && java.nio.file.Files.exists(target) && !overwrite.equals(this.pendingConfirmation))
            {
                this.pendingConfirmation = overwrite;
                this.status = tr("litematica-creator.gui.manager.confirm_overwrite", target.getFileName());
                return;
            }

            this.pendingConfirmation = null;
            reservation = CreatorSchematicBindingService.getInstance().reserveWriteTarget(
                    this.inspectedSchematic,
                    target,
                    operation
            );

            CreatorBindingReservation activeReservation = reservation;
            this.exportRunning = true;
            this.previewRunning = false;
            this.status = tr("litematica-creator.gui.manager.preparing");
            this.initGui();
            CreatorSchematicExportService service = CreatorSchematicExportService.getInstance();
            service.prepareAsync(
                    this.inspectedSchematic,
                    target,
                    operation,
                    this.exportMode(),
                    this.samplingPlacement,
                    new CreatorExportMetadata(this.saveInternalName, this.saveAuthor, this.saveDescription)
            ).thenCompose(prepared -> {
                Minecraft.getInstance().execute(() -> this.status = tr("litematica-creator.gui.manager.writing"));
                return service.write(prepared).thenApply(result -> new ExportCompletion(prepared, result));
            }).whenComplete((completion, error) -> Minecraft.getInstance().execute(() -> {
                this.exportRunning = false;

                if (error != null)
                {
                    if (activeReservation != null)
                    {
                        activeReservation.close();
                    }
                    this.status = tr("litematica-creator.gui.manager.save_failed", localizedErrorMessage(error));
                    this.refreshIfOpen();
                    return;
                }

                CreatorBindingResult binding = CreatorSchematicBindingService.getInstance().commit(
                        completion.prepared(),
                        completion.result(),
                        activeReservation
                );
                this.exportPreview = completion.prepared().preview();
                if (binding.success() && binding.bound())
                {
                    this.refreshSaveFormAfterBinding(completion.result().target());
                }
                this.status = binding.success() ?
                        tr(binding.bound() ?
                                (binding.clean() ? "litematica-creator.gui.manager.saved_bound" : "litematica-creator.gui.manager.saved_bound_dirty") :
                                "litematica-creator.gui.manager.exported", completion.result().target().getFileName()) :
                        tr("litematica-creator.gui.manager.save_failed", localizeKnownMessage(binding.error()));
                this.refreshIfOpen();
            }));
        }
        catch (Exception exception)
        {
            if (reservation != null)
            {
                reservation.close();
            }
            this.exportRunning = false;
            this.previewRunning = false;
            this.status = localizedErrorMessage(exception);
        }
    }

    private Path resolveTarget(CreatorExportOperation operation)
    {
        if (operation == CreatorExportOperation.SAVE && this.inspectedSchematic.getFile() != null)
        {
            return this.inspectedSchematic.getFile().toAbsolutePath().normalize();
        }

        return this.resolveFormTarget(this.saveDirectory, this.saveFileName);
    }

    private Path resolveFormTarget(String directoryValue, String fileNameValue)
    {

        try
        {
            Path directory = Path.of(directoryValue);
            String fileName = FileNameUtils.generateSimpleUnicodeSafeFileName(fileNameValue);

            if (fileName.isBlank())
            {
                throw new IllegalArgumentException(tr("litematica-creator.gui.manager.invalid_file_name"));
            }

            return CreatorSchematicExportService.ensureExtension(directory.resolve(fileName).toAbsolutePath().normalize());
        }
        catch (InvalidPathException exception)
        {
            throw new IllegalArgumentException(tr("litematica-creator.gui.manager.invalid_path"), exception);
        }
    }

    private void cycleSamplingPlacement()
    {
        this.captureSaveForm();
        List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(this.inspectedSchematic);

        if (placements.isEmpty())
        {
            this.samplingPlacement = null;
        }
        else
        {
            int index = placements.indexOf(this.samplingPlacement);
            this.samplingPlacement = placements.get(Math.floorMod(index + 1, placements.size()));
        }

        this.exportPreview = null;
        this.status = this.samplingPlacement != null ?
                tr("litematica-creator.gui.manager.sampling_changed", this.samplingPlacement.getName()) :
                tr("litematica-creator.gui.manager.no_sampling");
        this.initGui();
    }

    private void markMetadataChanged()
    {
        this.markMetadataChanged(this.inspectedSchematic);
    }

    private void markMetadataChanged(LitematicaSchematic schematic)
    {
        schematic.getMetadata().setTimeModifiedToNow();
        schematic.getMetadata().setModifiedSinceSaved();
        CreatorRecoveryManager.getInstance().onSchematicChanged(schematic);
    }

    private void ensureInspectionIsValid()
    {
        Collection<LitematicaSchematic> loaded = SchematicHolder.getInstance().getAllSchematics();

        if (this.inspectedSchematic == null || !loaded.contains(this.inspectedSchematic))
        {
            this.inspectedSchematic = loaded.stream().findFirst().orElse(null);
            this.inspectedPlacement = null;
            this.samplingPlacement = this.inspectedSchematic != null ? this.defaultSamplingPlacement(this.inspectedSchematic, null) : null;
        }

        if (this.inspectedPlacement != null &&
            (!DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().contains(this.inspectedPlacement) ||
             this.inspectedPlacement.getSchematic() != this.inspectedSchematic))
        {
            this.inspectedPlacement = null;
        }

        if (this.tab == ManagerTab.PLACEMENT && this.inspectedPlacement == null)
        {
            this.inspectedPlacement = this.preferredPlacement(this.inspectedSchematic);
        }

        if (this.samplingPlacement != null && this.samplingPlacement.getSchematic() != this.inspectedSchematic)
        {
            this.samplingPlacement = this.defaultSamplingPlacement(this.inspectedSchematic, this.inspectedPlacement);
        }
    }

    private List<ManagerRow> filteredRows()
    {
        String query = this.searchQuery.toLowerCase(Locale.ROOT).trim();
        List<ManagerRow> rows = new ArrayList<>();

        for (LitematicaSchematic schematic : SchematicHolder.getInstance().getAllSchematics())
        {
            List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic);
            boolean schematicMatches = query.isEmpty() || schematic.getMetadata().getName().toLowerCase(Locale.ROOT).contains(query);
            boolean placementMatches = placements.stream().anyMatch(placement -> placement.getName().toLowerCase(Locale.ROOT).contains(query));

            if (!schematicMatches && !placementMatches)
            {
                continue;
            }

            rows.add(new ManagerRow(schematic, null));

            for (SchematicPlacement placement : placements)
            {
                if (query.isEmpty() || schematicMatches || placement.getName().toLowerCase(Locale.ROOT).contains(query))
                {
                    rows.add(new ManagerRow(schematic, placement));
                }
            }
        }

        return rows;
    }

    private String rowLabel(ManagerRow row, int maxWidth)
    {
        CreatorFocus focus = CreatorManager.getInstance().getFocus();
        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        List<String> states = new ArrayList<>();

        if (row.placement == null)
        {
            states.add(tr(row.schematic.getFile() == null ?
                    "litematica-creator.gui.manager.row.memory" : "litematica-creator.gui.manager.row.file"));
            if (row.schematic.getMetadata().wasModifiedSinceSaved())
            {
                states.add(tr("litematica-creator.gui.manager.row.dirty"));
            }
            if (CreatorRecoveryManager.getInstance().hasRecoveryEntry(row.schematic))
            {
                states.add(tr("litematica-creator.gui.manager.row.recovery"));
            }
            if (focus != null && focus.schematic() == row.schematic)
            {
                states.add("Focus");
            }
            if (selected != null && selected.getSchematic() == row.schematic)
            {
                states.add(tr("litematica-creator.gui.manager.row.selected"));
            }
            return this.clampLabel(String.join(" | ", states) + " | " + row.schematic.getMetadata().getName(), maxWidth);
        }

        if (focus != null && focus.placement() == row.placement)
        {
            states.add("Focus");
        }
        if (selected == row.placement)
        {
            states.add(tr("litematica-creator.gui.manager.row.selected"));
        }
        if (!row.placement.isEnabled())
        {
            states.add(tr("litematica-creator.gui.manager.row.disabled"));
        }
        if (!row.placement.isRenderingEnabled())
        {
            states.add(tr("litematica-creator.gui.manager.row.hidden"));
        }
        String prefix = states.isEmpty() ? tr("litematica-creator.gui.manager.row.placement") : String.join(" | ", states);
        return this.clampLabel("  " + prefix + " | " + row.placement.getName(), maxWidth);
    }

    private String rowHoverText(ManagerRow row)
    {
        CreatorFocus focus = CreatorManager.getInstance().getFocus();
        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (row.placement == null)
        {
            return tr(
                    "litematica-creator.gui.manager.hover.row.schematic",
                    row.schematic.getMetadata().getName(),
                    displayPath(row.schematic.getFile()),
                    this.stateText(row.schematic),
                    DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(row.schematic).size(),
                    this.yesNo(focus != null && focus.schematic() == row.schematic),
                    this.yesNo(selected != null && selected.getSchematic() == row.schematic)
            );
        }

        return tr(
                "litematica-creator.gui.manager.hover.row.placement",
                row.placement.getName(),
                row.schematic.getMetadata().getName(),
                row.placement.getOrigin().toShortString(),
                this.yesNo(row.placement.isEnabled()),
                this.yesNo(row.placement.isRenderingEnabled()),
                this.yesNo(focus != null && focus.placement() == row.placement),
                this.yesNo(selected == row.placement)
        );
    }

    @Nullable
    private SchematicPlacement preferredPlacement(@Nullable LitematicaSchematic schematic)
    {
        if (schematic == null)
        {
            return null;
        }

        List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic);
        CreatorFocus focus = CreatorManager.getInstance().getFocus();

        if (focus != null && focus.schematic() == schematic && placements.contains(focus.placement()))
        {
            return focus.placement();
        }

        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (selected != null && selected.getSchematic() == schematic && placements.contains(selected))
        {
            return selected;
        }

        return placements.isEmpty() ? null : placements.getFirst();
    }

    private boolean hasPlacements(@Nullable LitematicaSchematic schematic)
    {
        return schematic != null && !DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic).isEmpty();
    }

    @Nullable
    private SchematicPlacement defaultSamplingPlacement(LitematicaSchematic schematic, @Nullable SchematicPlacement inspected)
    {
        CreatorFocus focus = CreatorManager.getInstance().getFocus();

        if (focus != null && focus.schematic() == schematic)
        {
            return focus.placement();
        }

        if (inspected != null && inspected.getSchematic() == schematic)
        {
            return inspected;
        }

        return null;
    }

    private CreatorExportRegionMode exportMode()
    {
        return (CreatorExportRegionMode) Configs.Generic.CREATOR_EXPORT_REGION_MODE.getOptionListValue();
    }

    private String samplingPlacementLabel()
    {
        return this.samplingPlacement != null ?
                tr("litematica-creator.gui.manager.sampling_value", this.samplingPlacement.getName()) :
                tr("litematica-creator.gui.manager.no_sampling");
    }

    private String stateText(LitematicaSchematic schematic)
    {
        String dirty = schematic.getMetadata().wasModifiedSinceSaved() ? tr("litematica-creator.gui.manager.dirty") : tr("litematica-creator.gui.manager.clean");
        String recovery = CreatorRecoveryManager.getInstance().hasRecoveryEntry(schematic) ? tr("litematica-creator.gui.manager.recovery") : tr("litematica-creator.gui.manager.no_recovery");
        return dirty + ", " + recovery;
    }

    private int entityCount()
    {
        return ((LitematicaSchematicAccessor) this.inspectedSchematic).litematicacreator$getEntities().values().stream().mapToInt(List::size).sum();
    }

    private int blockEntityCount()
    {
        return ((LitematicaSchematicAccessor) this.inspectedSchematic).litematicacreator$getTileEntities().values().stream().mapToInt(java.util.Map::size).sum();
    }

    private int placementCount()
    {
        return DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(this.inspectedSchematic).size();
    }

    private int blockTickCount()
    {
        return ((LitematicaSchematicAccessor) this.inspectedSchematic).litematicacreator$getPendingBlockTicks().values().stream().mapToInt(java.util.Map::size).sum();
    }

    private int fluidTickCount()
    {
        return ((LitematicaSchematicAccessor) this.inspectedSchematic).litematicacreator$getPendingFluidTicks().values().stream().mapToInt(java.util.Map::size).sum();
    }

    private void ensureSaveForm()
    {
        if (this.inspectedSchematic == null)
        {
            this.saveFormSchematic = null;
            return;
        }

        if (this.saveFormSchematic != this.inspectedSchematic)
        {
            Path file = this.inspectedSchematic.getFile();
            Path directory = file != null && file.getParent() != null ? file.getParent() : DataManager.getSchematicsBaseDirectory();
            SchematicMetadata metadata = this.inspectedSchematic.getMetadata();
            this.saveFormSchematic = this.inspectedSchematic;
            this.saveDirectory = directory.toString();
            this.saveFileName = file != null ? file.getFileName().toString() : metadata.getName() + ".litematic";
            this.saveInternalName = metadata.getName();
            this.saveAuthor = metadata.getAuthor();
            this.saveDescription = metadata.getDescription();
        }
    }

    private void captureSaveForm()
    {
        if (this.saveFormSchematic == null)
        {
            return;
        }

        if (this.directoryField != null)
        {
            this.saveDirectory = this.directoryField.getValueWrapper();
        }
        if (this.fileNameField != null)
        {
            this.saveFileName = this.fileNameField.getValueWrapper();
        }
        if (this.exportNameField != null)
        {
            this.saveInternalName = this.exportNameField.getValueWrapper();
        }
        if (this.exportAuthorField != null)
        {
            this.saveAuthor = this.exportAuthorField.getValueWrapper();
        }
        if (this.exportDescriptionField != null)
        {
            this.saveDescription = this.exportDescriptionField.getValueWrapper();
        }
    }

    private void refreshSaveFormAfterBinding(Path target)
    {
        Path normalized = target.toAbsolutePath().normalize();
        SchematicMetadata metadata = this.inspectedSchematic.getMetadata();
        this.saveFormSchematic = this.inspectedSchematic;
        this.saveDirectory = normalized.getParent() != null ? normalized.getParent().toString() : this.saveDirectory;
        this.saveFileName = normalized.getFileName().toString();
        this.saveInternalName = metadata.getName();
        this.saveAuthor = metadata.getAuthor();
        this.saveDescription = metadata.getDescription();
    }

    private String displayOutputTarget()
    {
        try
        {
            String directory = this.directoryField != null ? this.directoryField.getValueWrapper() : this.saveDirectory;
            String fileName = this.fileNameField != null ? this.fileNameField.getValueWrapper() : this.saveFileName;
            return CreatorSchematicExportService.ensureExtension(this.resolveFormTarget(directory, fileName)).toString();
        }
        catch (Exception exception)
        {
            return localizedErrorMessage(exception);
        }
    }

    private void openPeer(GuiBase gui)
    {
        gui.setParent(this.navigationParent);
        GuiBase.openGui(gui);
    }

    private GuiTextFieldGeneric addField(int x, int y, int width, String value, int maxLength, String hoverKey)
    {
        GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, width, 16, this.font);
        field.setMaxLengthWrapper(maxLength);
        field.setValueWrapper(value != null ? value : "");
        field.setHoverTooltip(hoverKey);
        this.addTextField(field, null, TextFieldType.STRING);
        return field;
    }

    private ButtonGeneric addActionButton(int x, int y, int width, String label, String hoverKey, Runnable action)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, this.clampLabel(label, width - 8));
        this.addButton(button, (pressed, mouseButton) -> action.run());
        this.addWidget(new WidgetHoverInfo(x, y, width, 20, hoverKey));
        return button;
    }

    private int addButtonGrid(int x, int y, int width, List<ButtonSpec> specs)
    {
        if (specs.isEmpty())
        {
            return y;
        }

        int widest = specs.stream().mapToInt(spec -> this.getStringWidth(spec.label) + 16).max().orElse(width);
        int columns = CreatorManagerLayout.buttonColumns(width, specs.size(), widest, CONTROL_GAP);
        int cellWidth = CreatorManagerLayout.buttonCellWidth(width, columns, CONTROL_GAP);

        for (int index = 0; index < specs.size(); index++)
        {
            ButtonSpec spec = specs.get(index);
            int column = index % columns;
            int row = index / columns;
            ButtonGeneric button = this.addActionButton(
                    x + column * (cellWidth + CONTROL_GAP), y + row * 24,
                    cellWidth, spec.label, spec.hoverKey, spec.action
            );
            button.setEnabled(spec.enabled);
        }

        return y + ((specs.size() + columns - 1) / columns) * 24;
    }

    private PageLayout pageLayout(ManagerTab page)
    {
        int left = this.leftWidth() + 14;
        int right = this.width - 18;
        String[] labels = switch (page)
        {
            case OVERVIEW -> new String[] {
                    tr("litematica-creator.gui.manager.name"),
                    tr("litematica-creator.gui.manager.author"),
                    tr("litematica-creator.gui.manager.description")
            };
            case PLACEMENT -> new String[] { tr("litematica-creator.gui.manager.placement_name") };
            case SAVE_EXPORT -> new String[] {
                    tr("litematica-creator.gui.manager.directory"),
                    tr("litematica-creator.gui.manager.file_name"),
                    tr("litematica-creator.gui.manager.export_name"),
                    tr("litematica-creator.gui.manager.author"),
                    tr("litematica-creator.gui.manager.description"),
                    tr("litematica-creator.gui.manager.export_mode"),
                    tr("litematica-creator.gui.manager.sampling")
            };
        };
        int contentWidth = Math.max(160, right - left);
        int widestLabel = 0;

        for (String label : labels)
        {
            widestLabel = Math.max(widestLabel, this.getStringWidth(label));
        }

        int labelWidth = CreatorManagerLayout.labelColumnWidth(contentWidth, widestLabel);
        int controlX = left + labelWidth;
        return new PageLayout(left, right, contentWidth, controlX, Math.max(80, right - controlX));
    }

    private boolean onSearchChanged(GuiTextFieldGeneric field)
    {
        this.searchQuery = field.getValueWrapper();
        this.searchCursor = field.getCursorWrapper();
        this.listScroll = 0;
        this.restoreSearchFocus = true;

        this.searchRefreshQueued = true;

        return true;
    }

    private void drawWrappedStatus(GuiContext ctx, int x, int y, int maxWidth)
    {
        List<String> lines = new ArrayList<>();
        StringUtils.splitTextToLines(lines, this.status, Math.max(40, maxWidth));

        for (int index = 0; index < Math.min(2, lines.size()); index++)
        {
            this.drawClampedString(ctx, lines.get(index), x, y + index * 10, maxWidth, 0xFFFFFF55);
        }
    }

    private void drawClampedString(GuiContext ctx, String text, int x, int y, int maxWidth, int color)
    {
        this.drawString(ctx, this.clampLabel(text, maxWidth), x, y, color);
    }

    private String clampLabel(String text, int maxWidth)
    {
        return StringUtils.clampTextToRenderLength(text, Math.max(8, maxWidth), LeftRight.RIGHT, "...");
    }

    private String yesNo(boolean value)
    {
        return tr(value ? "litematica-creator.gui.manager.yes" : "litematica-creator.gui.manager.no");
    }

    private int leftWidth()
    {
        return CreatorManagerLayout.leftPaneWidth(this.width);
    }

    private int visibleRows()
    {
        return Math.max(1, (this.height - 88) / ROW_HEIGHT);
    }

    private void createPreviewTexture()
    {
        if (this.inspectedSchematic == null)
        {
            return;
        }

        int[] pixels = this.inspectedSchematic.getMetadata().getPreviewImagePixelData();

        if (pixels == null || pixels.length == 0)
        {
            return;
        }

        int size = (int) Math.sqrt(pixels.length);

        if (size * size != pixels.length)
        {
            return;
        }

        NativeImage image = new NativeImage(size, size, false);

        for (int y = 0, index = 0; y < size; y++)
        {
            for (int x = 0; x < size; x++)
            {
                image.setPixel(x, y, pixels[index++]);
            }
        }

        this.previewTexture = new DynamicTexture(PREVIEW_TEXTURE::toString, image);
        this.mc.getTextureManager().register(PREVIEW_TEXTURE, this.previewTexture);
        this.previewTexture.upload();
    }

    private void releasePreviewTexture()
    {
        if (this.previewTexture != null)
        {
            this.mc.getTextureManager().release(PREVIEW_TEXTURE);
            this.previewTexture = null;
        }
    }

    private static String formatTime(long time)
    {
        return time > 0L ? DateFormat.getDateTimeInstance().format(time) : "-";
    }

    private static String displayPath(@Nullable Path path)
    {
        return path != null ? path.toAbsolutePath().toString() : tr("litematica-creator.gui.manager.memory_only");
    }

    private static boolean samePath(@Nullable Path first, Path second)
    {
        return first != null && first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }

    private static String tr(String key, Object... args)
    {
        return StringUtils.translate(key, args);
    }

    private static String errorMessage(Throwable error)
    {
        Throwable current = error;

        while (current instanceof CompletionException && current.getCause() != null)
        {
            current = current.getCause();
        }

        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }

    private static String localizedErrorMessage(Throwable error)
    {
        return localizeKnownMessage(errorMessage(error));
    }

    private static String localizeKnownMessage(@Nullable String message)
    {
        if (message == null || message.isBlank())
        {
            return tr("litematica-creator.gui.manager.error.unknown");
        }

        String targetBoundPrefix = "Target file is already bound to loaded schematic '";
        if (message.startsWith(targetBoundPrefix) && message.endsWith("'"))
        {
            return tr("litematica-creator.gui.manager.error.target_bound", message.substring(targetBoundPrefix.length(), message.length() - 1));
        }
        if (message.equals("Another Creator save is already using the target file"))
        {
            return tr("litematica-creator.gui.manager.error.target_busy");
        }
        if (message.equals("Sampling placement belongs to a different schematic"))
        {
            return tr("litematica-creator.gui.manager.error.sampling_wrong_schematic");
        }
        if (message.equals("World-backed export requires a sampling placement"))
        {
            return tr("litematica-creator.gui.manager.error.sampling_required");
        }
        if (message.startsWith("World sampling reached an unloaded chunk at "))
        {
            return tr("litematica-creator.gui.manager.error.unloaded_chunk", message.substring("World sampling reached an unloaded chunk at ".length()));
        }
        if (message.startsWith("Export bounds are too large: "))
        {
            return tr("litematica-creator.gui.manager.error.bounds_too_large", message.substring("Export bounds are too large: ".length()));
        }
        if (message.equals("The client world changed during schematic sampling"))
        {
            return tr("litematica-creator.gui.manager.error.world_changed");
        }

        return message;
    }

    private void refreshIfOpen()
    {
        if (Minecraft.getInstance().gui.screen() == this)
        {
            this.initGui();
        }
    }

    private record ExportCompletion(CreatorPreparedExport prepared, CreatorExportWriteResult result)
    {
    }

    private record ManagerRow(LitematicaSchematic schematic, @Nullable SchematicPlacement placement)
    {
    }

    private record Confirmation(String action, String identity)
    {
    }

    private record ButtonSpec(String label, String hoverKey, Runnable action, boolean enabled)
    {
    }

    private record PageLayout(int labelX, int right, int contentWidth, int controlX, int controlWidth)
    {
    }

    private enum ManagerTab
    {
        OVERVIEW("overview"),
        PLACEMENT("placement"),
        SAVE_EXPORT("save_export");

        private final String key;

        ManagerTab(String key)
        {
            this.key = key;
        }

        String displayName()
        {
            return tr("litematica-creator.gui.manager.tab." + this.key);
        }
    }
}

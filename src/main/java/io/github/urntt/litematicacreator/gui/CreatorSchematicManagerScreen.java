package io.github.urntt.litematicacreator.gui;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
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
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        int leftWidth = this.leftWidth();
        RenderUtils.drawRect(ctx, 8, 50, leftWidth - 12, this.height - 82, 0x70000000);
        RenderUtils.drawRect(ctx, leftWidth, 50, this.width - leftWidth - 8, this.height - 82, 0x50000000);

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
            this.drawString(ctx, this.status, leftWidth + 12, this.height - 46, 0xFFFFFF55);
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
    public void onClose()
    {
        this.releasePreviewTexture();
        super.onClose();
    }

    private void createNavigationButtons()
    {
        int x = 10;
        int y = this.height - 26;
        x += this.addActionButton(x, y, tr("litematica-creator.gui.manager.nav.main"), () -> this.openPeer(new GuiMainMenu())) + 4;
        x += this.addActionButton(x, y, tr("litematica-creator.gui.manager.nav.loaded"), () -> this.openPeer(new GuiSchematicLoadedList())) + 4;
        this.addActionButton(x, y, tr("litematica-creator.gui.manager.nav.placements"), () -> this.openPeer(new GuiSchematicPlacementsList()));
        String close = tr("gui.done");
        this.addActionButton(this.width - this.getStringWidth(close) - 24, y, close, () -> this.closeGui(true));
    }

    private void createSearchAndList()
    {
        int leftWidth = this.leftWidth();
        this.searchField = new GuiTextFieldGeneric(10, 29, leftWidth - 92, 16, this.font);
        this.searchField.setMaxLengthWrapper(128);
        this.searchField.setValueWrapper(this.searchQuery);
        this.addTextField(this.searchField, null, TextFieldType.STRING);
        this.addActionButton(leftWidth - 76, 27, 66, tr("litematica-creator.gui.manager.filter"), () -> {
            this.searchQuery = Objects.requireNonNull(this.searchField).getValueWrapper();
            this.listScroll = 0;
            this.initGui();
        });

        List<ManagerRow> rows = this.filteredRows();
        this.listScroll = Math.min(this.listScroll, Math.max(0, rows.size() - this.visibleRows()));
        int end = Math.min(rows.size(), this.listScroll + this.visibleRows());
        int y = 54;

        for (int index = this.listScroll; index < end; index++)
        {
            ManagerRow row = rows.get(index);
            int rowWidth = leftWidth - 22;
            ButtonGeneric button = new ButtonGeneric(12, y, rowWidth, 20, this.rowLabel(row));
            boolean selected = row.placement != null ? row.placement == this.inspectedPlacement :
                    row.schematic == this.inspectedSchematic && this.inspectedPlacement == null;
            button.setEnabled(!selected);
            this.addButton(button, (pressed, mouseButton) -> this.inspect(row));
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
            button.setEnabled(this.tab != value && (value != ManagerTab.PLACEMENT || this.inspectedPlacement != null));
            this.addButton(button, (pressed, mouseButton) -> {
                this.tab = value;
                this.status = "";
                this.initGui();
            });
            x += button.getWidth() + 3;
        }
    }

    private void createOverviewControls()
    {
        int x = this.leftWidth() + 118;
        int y = 61;
        int width = Math.max(120, Math.min(360, this.width - x - 24));
        SchematicMetadata metadata = this.inspectedSchematic.getMetadata();
        this.nameField = this.addField(x, y, width, metadata.getName(), 256);
        this.authorField = this.addField(x, y + 24, width, metadata.getAuthor(), 256);
        this.descriptionField = this.addField(x, y + 48, width, metadata.getDescription(), 1024);
        y += 76;
        this.addActionButton(x, y, 78, tr("litematica-creator.gui.manager.apply"), this::applyMetadata);
        this.addActionButton(x + 82, y, 106, tr("litematica-creator.gui.manager.thumbnail.update"), this::captureThumbnail);
        this.addActionButton(x + 192, y, 92, tr("litematica-creator.gui.manager.thumbnail.clear"), this::clearThumbnail);
        y += 24;
        this.addActionButton(x, y, 86, tr("litematica-creator.gui.manager.reload"), this::reloadSchematic);
        this.addActionButton(x + 90, y, 86, tr("litematica-creator.gui.manager.unload"), this::confirmUnload);
    }

    private void createPlacementControls()
    {
        if (this.inspectedPlacement == null)
        {
            return;
        }

        int x = this.leftWidth() + 118;
        int y = 61;
        int width = Math.max(120, Math.min(360, this.width - x - 24));
        this.placementNameField = this.addField(x, y, width, this.inspectedPlacement.getName(), 256);
        y += 26;
        this.addActionButton(x, y, 82, tr("litematica-creator.gui.manager.rename"), this::renamePlacement);
        this.addActionButton(x + 86, y, 90, tr("litematica-creator.gui.manager.toggle_enabled"), () -> {
            this.inspectedPlacement.toggleEnabled();
            this.initGui();
        });
        this.addActionButton(x + 180, y, 90, tr("litematica-creator.gui.manager.toggle_render"), () -> {
            this.inspectedPlacement.setRenderSchematic(!this.inspectedPlacement.isRenderingEnabled());
            this.initGui();
        });
        y += 24;
        this.addActionButton(x, y, 104, tr("litematica-creator.gui.manager.toggle_focus"), this::toggleFocus);
        this.addActionButton(x + 108, y, 112, tr("litematica-creator.gui.manager.toggle_selected"), this::toggleSelected);
        y += 24;
        this.addActionButton(x, y, 150, tr("litematica-creator.gui.manager.native_config"), this::openPlacementConfiguration);
        this.addActionButton(x + 154, y, 96, tr("litematica-creator.gui.manager.remove_placement"), this::confirmRemovePlacement);
    }

    private void createSaveControls()
    {
        int x = this.leftWidth() + 118;
        int y = 61;
        int width = Math.max(140, Math.min(420, this.width - x - 24));
        this.directoryField = this.addField(x, y, width, this.saveDirectory, 2048);
        this.fileNameField = this.addField(x, y + 24, width, this.saveFileName, 256);
        this.exportNameField = this.addField(x, y + 48, width, this.saveInternalName, 256);
        this.exportAuthorField = this.addField(x, y + 72, width, this.saveAuthor, 256);
        this.exportDescriptionField = this.addField(x, y + 96, width, this.saveDescription, 1024);
        y += 122;
        CreatorExportRegionMode mode = this.exportMode();
        int halfWidth = Math.max(68, (width - 4) / 2);
        this.addActionButton(x, y, halfWidth, mode.getDisplayName(), () -> {
            CreatorExportRegionMode next = mode.cycle(true);
            Configs.Generic.CREATOR_EXPORT_REGION_MODE.setOptionListValue(next);
            this.exportPreview = null;
            this.initGui();
        });
        this.addActionButton(x + halfWidth + 4, y, halfWidth, this.samplingPlacementLabel(), this::cycleSamplingPlacement);
        y += 24;
        this.addActionButton(x, y, 92, tr("litematica-creator.gui.manager.preview"), this::refreshExportPreview);
        y += 24;
        int buttonX = x;
        buttonX += this.addActionButton(buttonX, y, tr("litematica-creator.gui.manager.save"), () -> this.beginExport(CreatorExportOperation.SAVE)) + 4;
        buttonX += this.addActionButton(buttonX, y, tr("litematica-creator.gui.manager.save_bind"), () -> this.beginExport(CreatorExportOperation.SAVE_AS_AND_BIND)) + 4;
        this.addActionButton(buttonX, y, tr("litematica-creator.gui.manager.export_copy"), () -> this.beginExport(CreatorExportOperation.EXPORT_COPY));
    }

    private void drawOverview(GuiContext ctx, int leftWidth)
    {
        SchematicMetadata metadata = this.inspectedSchematic.getMetadata();
        int x = leftWidth + 14;
        int y = 65;
        this.drawString(ctx, tr("litematica-creator.gui.manager.name"), x, y, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.author"), x, y + 24, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.description"), x, y + 48, COLOR_WHITE);
        y = 194;
        this.drawString(ctx, tr("litematica-creator.gui.manager.path", displayPath(this.inspectedSchematic.getFile())), x, y, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.created", formatTime(metadata.getTimeCreated())), x, y + 12, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.modified", formatTime(metadata.getTimeModified())), x, y + 24, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.state", this.stateText(this.inspectedSchematic)), x, y + 36, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.counts", metadata.getRegionCount(), metadata.getTotalBlocks(), metadata.getTotalVolume()), x, y + 48, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.enclosing", metadata.getEnclosingSizeAsBlockPos().toShortString()), x, y + 60, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.attached", this.entityCount(), this.blockEntityCount()), x, y + 72, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.placements", this.placementCount()), x, y + 84, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.ticks", this.blockTickCount(), this.fluidTickCount()), x, y + 96, COLOR_WHITE);

        if (this.previewTexture != null)
        {
            int previewSize = Math.min(120, Math.max(48, this.width - leftWidth - 430));
            ctx.blit(RenderPipelines.GUI_TEXTURED, PREVIEW_TEXTURE, this.width - previewSize - 18, 60, 0, 0, previewSize, previewSize, previewSize, previewSize);
        }
    }

    private void drawPlacement(GuiContext ctx, int leftWidth)
    {
        int x = leftWidth + 14;

        if (this.inspectedPlacement == null)
        {
            this.drawString(ctx, tr("litematica-creator.gui.manager.no_placement"), x, 65, COLOR_WHITE);
            return;
        }

        SchematicPlacement placement = this.inspectedPlacement;
        this.drawString(ctx, tr("litematica-creator.gui.manager.placement_name"), x, 65, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.origin", placement.getOrigin().toShortString()), x, 152, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.transform", placement.getRotation(), placement.getMirror()), x, 164, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.placement_state", placement.isEnabled(), placement.isRenderingEnabled()), x, 176, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.subregions", placement.getSubRegionCount()), x, 188, COLOR_WHITE);
    }

    private void drawSave(GuiContext ctx, int leftWidth)
    {
        int x = leftWidth + 14;
        this.drawString(ctx, tr("litematica-creator.gui.manager.directory"), x, 65, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.file_name"), x, 89, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.export_name"), x, 113, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.author"), x, 137, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.description"), x, 161, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.export_mode"), x, 187, COLOR_WHITE);
        this.drawString(ctx, tr("litematica-creator.gui.manager.sampling"), x, 211, COLOR_WHITE);

        if (this.exportPreview != null)
        {
            int y = 280;
            this.drawString(ctx, tr("litematica-creator.gui.manager.preview_regions", this.exportPreview.regionCount()), x, y, COLOR_WHITE);
            this.drawString(ctx, tr("litematica-creator.gui.manager.preview_blocks", this.exportPreview.totalBlocks()), x, y + 12, COLOR_WHITE);
            this.drawString(ctx, tr("litematica-creator.gui.manager.preview_volume", this.exportPreview.totalVolume()), x, y + 24, COLOR_WHITE);
            this.drawString(ctx, tr("litematica-creator.gui.manager.preview_size", this.exportPreview.enclosingSize().toShortString()), x, y + 36, COLOR_WHITE);
            this.drawString(ctx, tr("litematica-creator.gui.manager.preview_version", LitematicaSchematic.SCHEMATIC_VERSION), x, y + 48, COLOR_WHITE);
            this.drawString(ctx, this.bindingPreviewText(), x, y + 60, COLOR_WHITE);
        }
    }

    private void inspect(ManagerRow row)
    {
        this.captureSaveForm();
        this.inspectedSchematic = row.schematic;
        this.inspectedPlacement = row.placement;
        this.samplingPlacement = this.defaultSamplingPlacement(row.schematic, row.placement);
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
                this.status = errorMessage(exception);
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

    private void toggleFocus()
    {
        CreatorFocus focus = CreatorManager.getInstance().getFocus();

        if (focus != null && focus.placement() == this.inspectedPlacement)
        {
            CreatorManager.getInstance().clearFocus();
        }
        else
        {
            CreatorManager.getInstance().focusPlacement(this.inspectedPlacement);
        }

        this.initGui();
    }

    private void toggleSelected()
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        manager.setSelectedSchematicPlacement(manager.getSelectedSchematicPlacement() == this.inspectedPlacement ? null : this.inspectedPlacement);
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
            this.status = tr("litematica-creator.gui.manager.previewing");
            CreatorSchematicExportService.getInstance().previewAsync(
                    this.inspectedSchematic,
                    this.exportMode(),
                    this.samplingPlacement
            ).whenComplete((preview, error) -> Minecraft.getInstance().execute(() -> {
                this.exportRunning = false;

                if (error != null)
                {
                    this.status = errorMessage(error);
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
            this.status = errorMessage(exception);
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
                this.status = conflict;
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
            this.status = tr("litematica-creator.gui.manager.preparing");
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
                    this.status = tr("litematica-creator.gui.manager.save_failed", errorMessage(error));
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
                        tr("litematica-creator.gui.manager.save_failed", binding.error());
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
            this.status = errorMessage(exception);
        }
    }

    private Path resolveTarget(CreatorExportOperation operation)
    {
        if (operation == CreatorExportOperation.SAVE && this.inspectedSchematic.getFile() != null)
        {
            return this.inspectedSchematic.getFile().toAbsolutePath().normalize();
        }

        try
        {
            Path directory = Path.of(this.saveDirectory);
            String fileName = FileNameUtils.generateSimpleUnicodeSafeFileName(this.saveFileName);

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

        if (this.inspectedPlacement != null && !DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().contains(this.inspectedPlacement))
        {
            this.inspectedPlacement = null;
        }

        if (this.tab == ManagerTab.PLACEMENT && this.inspectedPlacement == null)
        {
            this.tab = ManagerTab.OVERVIEW;
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

    private String rowLabel(ManagerRow row)
    {
        CreatorFocus focus = CreatorManager.getInstance().getFocus();
        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (row.placement == null)
        {
            String file = row.schematic.getFile() == null ? "MEM" : "FILE";
            String dirty = row.schematic.getMetadata().wasModifiedSinceSaved() ? " *" : "";
            String recovery = CreatorRecoveryManager.getInstance().hasRecoveryEntry(row.schematic) ? " R" : "";
            return "[" + file + dirty + recovery + "] " + row.schematic.getMetadata().getName();
        }

        String markers = (focus != null && focus.placement() == row.placement ? "F" : "-") +
                (selected == row.placement ? "S" : "-") +
                (row.placement.isEnabled() ? "E" : "-") +
                (row.placement.isRenderingEnabled() ? "R" : "-");
        return "  [" + markers + "] " + row.placement.getName();
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
        return this.samplingPlacement != null ? this.samplingPlacement.getName() : tr("litematica-creator.gui.manager.no_sampling");
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

    private String bindingPreviewText()
    {
        try
        {
            Path target = CreatorSchematicExportService.ensureExtension(this.resolveTarget(CreatorExportOperation.SAVE_AS_AND_BIND));
            Path current = this.inspectedSchematic.getFile();

            if (current == null)
            {
                return tr("litematica-creator.gui.manager.preview_bind_new", target);
            }

            if (current.toAbsolutePath().normalize().equals(target))
            {
                return tr("litematica-creator.gui.manager.preview_overwrite_binding", target);
            }

            return tr("litematica-creator.gui.manager.preview_rebind", target);
        }
        catch (Exception exception)
        {
            return errorMessage(exception);
        }
    }

    private void openPeer(GuiBase gui)
    {
        gui.setParent(this.navigationParent);
        GuiBase.openGui(gui);
    }

    private GuiTextFieldGeneric addField(int x, int y, int width, String value, int maxLength)
    {
        GuiTextFieldGeneric field = new GuiTextFieldGeneric(x, y, width, 16, this.font);
        field.setMaxLengthWrapper(maxLength);
        field.setValueWrapper(value != null ? value : "");
        this.addTextField(field, null, TextFieldType.STRING);
        return field;
    }

    private int addActionButton(int x, int y, String label, Runnable action)
    {
        return this.addActionButton(x, y, this.getStringWidth(label) + 12, label, action);
    }

    private int addActionButton(int x, int y, int width, String label, Runnable action)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);
        this.addButton(button, (pressed, mouseButton) -> action.run());
        return button.getWidth();
    }

    private int leftWidth()
    {
        return Math.max(230, Math.min(360, this.width * 2 / 5));
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

package dev.effecttimer.client.screen;

import dev.effecttimer.client.hud.EffectsHudRenderer;
import dev.effecttimer.config.ConfigValidator;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.HudAnchor;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.layout.EffectsLayoutResult;
import dev.effecttimer.layout.GuideLine;
import dev.effecttimer.layout.HudLayoutEngine;
import dev.effecttimer.layout.HudRect;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Editor visual del HUD de efectos (portado de {@code HudEditorScreen} del repo
 * padre, sin el bloque de armadura ni la selección de módulo). Permite:
 * <ul>
 *   <li>arrastrar el HUD para reposicionarlo: al moverlo, el anclaje pasa
 *       automáticamente a {@code CUSTOM} conservando la posición visual;</li>
 *   <li>redimensionar arrastrando las 4 esquinas (define {@code scale}, con
 *       clamp 0.50–2.50);</li>
 *   <li>imán a la rejilla ({@code editorSnapToGrid}, tamaño
 *       {@code editorGridSize}) y guías del centro de pantalla y bordes;</li>
 *   <li>cancelar restaura la snapshot de entrada (los cambios no se guardan).</li>
 * </ul>
 */
public final class HudEditorScreen extends AbstractCustomizableScreen {
	private static final int HANDLE_HIT_RADIUS = 6;
	private static final int PANEL_PAD = 4;

	private final HudConfig entrySnapshot;
	private final Consumer<HudConfig> onSyncParent;
	private boolean dockAtTop = false;

	private boolean draggingElement = false;
	private int dragOffsetX = 0;
	private int dragOffsetY = 0;
	private HudLayoutEngine.ResizeCorner resizingCorner = null;
	private HudRect resizeStartBounds = HudRect.EMPTY;
	private int resizeStartUnscaledW = 0;
	private int resizeStartUnscaledH = 0;
	private List<GuideLine> activeGuides = List.of();

	private EffectsLayoutResult lastEffectsLayout;

	private Button toggleEnabledBtn;
	private Button anchorBtn;
	private ScaleSliderButton scaleSlider;
	private Button snapBtn;
	private Button dockBtn;
	private Button cancelBtn;
	private Button saveLayoutBtn;
	private Button doneBtn;
	private int computedToolbarHeight = 30;

	public HudEditorScreen(Screen parent, HudConfig workingConfig, Consumer<HudConfig> onSyncParent) {
		super("effecttimer.screen.editor.title", parent, workingConfig);
		this.entrySnapshot = workingConfig.copy();
		this.onSyncParent = onSyncParent;
	}

	public boolean isDraggingElement() {
		return draggingElement || resizingCorner != null;
	}

	public boolean isInteracting() {
		return draggingElement || resizingCorner != null;
	}

	private EffectsConfig effects() {
		return workingConfig.effects;
	}

	private int toolbarWidth() {
		return Math.max(180, Math.min(620, this.width - 12));
	}

	private int toolbarX(int panelW) {
		return (this.width - panelW) / 2;
	}

	private int toolbarY() {
		return dockAtTop ? 2 : Math.max(2, this.height - computedToolbarHeight - 2);
	}

	private boolean isInsideToolbar(double x, double y) {
		int panelW = toolbarWidth();
		int panelX = toolbarX(panelW);
		int panelY = toolbarY();
		return x >= panelX && x <= panelX + panelW && y >= panelY && y <= panelY + computedToolbarHeight;
	}

	@Override
	protected void init() {
		int panelW = toolbarWidth();
		int panelX = toolbarX(panelW);
		int innerW = panelW - PANEL_PAD * 2;
		int gap = 3;
		int rowY = toolbarY() + PANEL_PAD + 4;
		int x = panelX + PANEL_PAD;

		int enabledW = 74;
		toggleEnabledBtn = Button.builder(Component.empty(), btn -> {
			effects().enabled = !effects().enabled;
			syncEnabledLabel();
			notifyConfigEdited();
		}).bounds(x, rowY, enabledW, 20).tooltip(Tooltip.create(t("effecttimer.tooltip.editor.toggle_enabled"))).build();
		addRenderableWidget(toggleEnabledBtn);
		syncEnabledLabel();
		x += enabledW + gap;

		int anchorW = 104;
		anchorBtn = Button.builder(Component.empty(), btn -> cycleAnchor()).bounds(x, rowY, anchorW, 20)
				.tooltip(Tooltip.create(t("effecttimer.tooltip.editor.cycle_anchor"))).build();
		addRenderableWidget(anchorBtn);
		syncAnchorLabel();
		x += anchorW + gap;

		int scaleW = Math.max(90, Math.min(170, panelX + panelW - PANEL_PAD - x - (124 + gap * 4) - 150));
		scaleSlider = new ScaleSliderButton(
				x, rowY, scaleW, 20,
				t("effecttimer.option.scale"),
				effects().scale,
				value -> {
					effects().scale = ConfigValidator.clampScale(value);
					notifyConfigEdited();
				}
		);
		addRenderableWidget(scaleSlider);
		x += scaleW + gap;

		int snapW = 62;
		snapBtn = Button.builder(Component.empty(), btn -> {
			workingConfig.editorSnapToGrid = !workingConfig.editorSnapToGrid;
			syncSnapLabel();
			notifyConfigEdited();
		}).bounds(x, rowY, snapW, 20).tooltip(Tooltip.create(t("effecttimer.tooltip.editor.snap"))).build();
		addRenderableWidget(snapBtn);
		syncSnapLabel();
		x += snapW + gap;

		int dockW = 62;
		dockBtn = Button.builder(Component.empty(), btn -> {
			dockAtTop = !dockAtTop;
			this.rebuildWidgets();
		}).bounds(x, rowY, dockW, 20).tooltip(Tooltip.create(t("effecttimer.tooltip.editor.dock"))).build();
		addRenderableWidget(dockBtn);
		syncDockLabel();

		int rightBtnW = 46;
		int rx = panelX + panelW - PANEL_PAD;
		doneBtn = Button.builder(t("effecttimer.button.done"), btn -> {
			saveConfigNow();
			syncParent();
			this.minecraft.setScreen(parent);
		}).bounds(rx - rightBtnW, rowY, rightBtnW, 20).build();
		addRenderableWidget(doneBtn);
		rx -= rightBtnW + gap;
		saveLayoutBtn = Button.builder(t("effecttimer.button.save"), btn -> {
			saveConfigNow();
			syncParent();
		}).bounds(rx - rightBtnW, rowY, rightBtnW, 20).tooltip(Tooltip.create(t("effecttimer.tooltip.save_now"))).build();
		addRenderableWidget(saveLayoutBtn);
		rx -= rightBtnW + gap;
		cancelBtn = Button.builder(t("effecttimer.button.cancel"), btn -> cancelChanges()).bounds(rx - rightBtnW, rowY, rightBtnW, 20)
				.tooltip(Tooltip.create(t("effecttimer.tooltip.editor.cancel"))).build();
		addRenderableWidget(cancelBtn);

		computedToolbarHeight = 20 + PANEL_PAD * 2;
	}

	private void syncEnabledLabel() {
		if (toggleEnabledBtn != null) {
			toggleEnabledBtn.setMessage(Component.literal("HUD: " + (effects().enabled ? "ON" : "OFF")));
		}
	}

	private void syncAnchorLabel() {
		if (anchorBtn != null) {
			anchorBtn.setMessage(Component.literal("⚓ " + t("effecttimer.anchor." + effects().anchor.name().toLowerCase(java.util.Locale.ROOT)).getString()));
		}
	}

	private void syncSnapLabel() {
		if (snapBtn != null) {
			snapBtn.setMessage(Component.literal(t(workingConfig.editorSnapToGrid ? "effecttimer.editor.snap_on" : "effecttimer.editor.snap_off").getString()));
		}
	}

	private void syncDockLabel() {
		if (dockBtn != null) {
			dockBtn.setMessage(Component.literal(t(dockAtTop ? "effecttimer.editor.dock_bottom" : "effecttimer.editor.dock_top").getString()));
		}
	}

	private void cycleAnchor() {
		HudAnchor[] values = HudAnchor.values();
		int idx = effects().anchor != null ? effects().anchor.ordinal() : 0;
		HudAnchor next = values[(idx + 1) % values.length];
		applyAnchor(next);
	}

	/** Al salir de {@code CUSTOM} los offsets se resetean (mismo contrato que la pantalla de configuración). */
	private void applyAnchor(HudAnchor anchor) {
		if (anchor != HudAnchor.CUSTOM && (effects().offsetX != 0 || effects().offsetY != 0)) {
			effects().offsetX = 0;
			effects().offsetY = 0;
		}
		effects().anchor = anchor;
		syncAnchorLabel();
		notifyConfigEdited();
	}

	private void cancelChanges() {
		syncWorkingFromManager(entrySnapshot.copy());
		notifyConfigEdited();
		syncParent();
		this.minecraft.setScreen(parent);
	}

	private void syncParent() {
		if (onSyncParent != null) {
			onSyncParent.accept(workingConfig);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.renderBackground(graphics, mouseX, mouseY, delta);

		lastEffectsLayout = EffectsHudRenderer.renderForEditor(
				graphics,
				this.minecraft,
				effects(),
				activeLang(),
				0.95f
		);
		if (lastEffectsLayout == null) {
			lastEffectsLayout = new EffectsLayoutResult(0, 0, 0, 0, 1.0f, HudRect.EMPTY, List.of());
		}
		HudRect bounds = lastEffectsLayout.bounds();

		if (workingConfig.editorShowGuides) {
			for (GuideLine guide : activeGuides) {
				if (guide.vertical()) {
					graphics.fill(guide.coordinate(), 0, guide.coordinate() + 1, this.height, 0x6638BDF8);
				} else {
					graphics.fill(0, guide.coordinate(), this.width, guide.coordinate() + 1, 0x6638BDF8);
				}
			}
		}

		if (!bounds.isEmpty()) {
			graphics.renderOutline(bounds.x() - 2, bounds.y() - 2, bounds.width() + 4, bounds.height() + 4, 0x9938BDF8);
			drawHandle(graphics, bounds.x() - 2, bounds.y() - 2);
			drawHandle(graphics, bounds.right() + 2, bounds.y() - 2);
			drawHandle(graphics, bounds.x() - 2, bounds.bottom() + 2);
			drawHandle(graphics, bounds.right() + 2, bounds.bottom() + 2);
		}

		// Barra de herramientas
		int panelW = toolbarWidth();
		int panelX = toolbarX(panelW);
		int panelY = toolbarY();
		graphics.fill(panelX, panelY, panelX + panelW, panelY + computedToolbarHeight, 0xE60D1117);
		graphics.renderOutline(panelX, panelY, panelW, computedToolbarHeight, 0xFF2D3748);

		Component hint = t("effecttimer.editor.hint");
		graphics.drawCenteredString(this.font, hint, this.width / 2, panelY - 14, 0xFFA0AEC0);

		super.render(graphics, mouseX, mouseY, delta);
		renderSaveStatusBadge(graphics, panelX + panelW + 8, panelY + 3);
	}

	private void drawHandle(GuiGraphics graphics, int cx, int cy) {
		graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFF8FAFC);
		graphics.renderOutline(cx - 2, cy - 2, 5, 5, 0xFF0D1117);
	}

	private HudLayoutEngine.ResizeCorner cornerAt(HudRect bounds, double mouseX, double mouseY) {
		if (bounds.isEmpty()) {
			return null;
		}
		double tlX = bounds.x() - 2.0;
		double tlY = bounds.y() - 2.0;
		double brX = bounds.right() + 2.0;
		double brY = bounds.bottom() + 2.0;
		if (near(mouseX, mouseY, tlX, tlY)) {
			return HudLayoutEngine.ResizeCorner.TOP_LEFT;
		}
		if (near(mouseX, mouseY, brX, tlY)) {
			return HudLayoutEngine.ResizeCorner.TOP_RIGHT;
		}
		if (near(mouseX, mouseY, tlX, brY)) {
			return HudLayoutEngine.ResizeCorner.BOTTOM_LEFT;
		}
		if (near(mouseX, mouseY, brX, brY)) {
			return HudLayoutEngine.ResizeCorner.BOTTOM_RIGHT;
		}
		return null;
	}

	private static boolean near(double mx, double my, double x, double y) {
		return Math.abs(mx - x) <= HANDLE_HIT_RADIUS && Math.abs(my - y) <= HANDLE_HIT_RADIUS;
	}

	private boolean insideHudBox(double mouseX, double mouseY) {
		HudRect bounds = lastEffectsLayout != null ? lastEffectsLayout.bounds() : HudRect.EMPTY;
		return bounds.contains(mouseX - 3, mouseY - 3) || bounds.contains(mouseX + 3, mouseY + 3)
				|| (bounds.contains(mouseX, mouseY));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean dragging) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (isInsideToolbar(mouseX, mouseY)) {
			return super.mouseClicked(event, dragging);
		}
		HudRect bounds = lastEffectsLayout != null ? lastEffectsLayout.bounds() : HudRect.EMPTY;
		HudLayoutEngine.ResizeCorner corner = cornerAt(bounds, mouseX, mouseY);
		if (corner != null) {
			resizingCorner = corner;
			resizeStartBounds = bounds;
			resizeStartUnscaledW = Math.max(1, lastEffectsLayout.unscaledWidth());
			resizeStartUnscaledH = Math.max(1, lastEffectsLayout.unscaledHeight());
			return true;
		}
		if (insideHudBox(mouseX, mouseY)) {
			draggingElement = true;
			dragOffsetX = (int) Math.round(mouseX) - bounds.x();
			dragOffsetY = (int) Math.round(mouseY) - bounds.y();
			ensureCustomAnchor();
			return true;
		}
		return super.mouseClicked(event, dragging);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		double mouseX = event.x();
		double mouseY = event.y();
		if (resizingCorner != null) {
			EffectsLayoutResult layout = lastEffectsLayout;
			HudLayoutEngine.ResizeResult result = HudLayoutEngine.computeResizeFromCorner(
					resizeStartBounds,
					resizeStartUnscaledW,
					resizeStartUnscaledH,
					resizingCorner,
					(int) Math.round(mouseX),
					(int) Math.round(mouseY),
					this.width,
					this.height
			);
			effects().scale = result.scale();
			applyCustomOrigin(result.bounds().x(), result.bounds().y());
			if (scaleSlider != null) {
				scaleSlider.setScaleValue(result.scale());
			}
			notifyConfigEdited();
			return true;
		}
		if (draggingElement && layoutSizeValid(layoutSize())) {
			HudRect bounds = lastEffectsLayout.bounds();
			int desiredX = (int) Math.round(mouseX) - dragOffsetX;
			int desiredY = (int) Math.round(mouseY) - dragOffsetY;
			int[] size = layoutSize();
			List<HudRect> refs = List.of();
			HudLayoutEngine.SnapResult snapped = HudLayoutEngine.snapPosition(
					desiredX,
					desiredY,
					size[0],
					size[1],
					this.width,
					this.height,
					workingConfig.editorGridSize,
					workingConfig.editorSnapToGrid,
					workingConfig.editorShowGuides,
					refs
			);
			activeGuides = snapped.guides();
			applyCustomOrigin(snapped.x(), snapped.y());
			notifyConfigEdited();
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	private int[] layoutSize() {
		if (lastEffectsLayout == null || lastEffectsLayout.bounds().isEmpty()) {
			return new int[] { 1, 1 };
		}
		HudRect b = lastEffectsLayout.bounds();
		return new int[] { b.width(), b.height() };
	}

	private static boolean layoutSizeValid(int[] size) {
		return size[0] > 0 && size[1] > 0;
	}

	/**
	 * Si el anclaje no era CUSTOM, se cambia a CUSTOM preservando el origen
	 * actual para que el HUD no "salte" al empezar a arrastrar.
	 */
	private void ensureCustomAnchor() {
		if (effects().anchor != HudAnchor.CUSTOM) {
			HudRect bounds = lastEffectsLayout != null ? lastEffectsLayout.bounds() : HudRect.EMPTY;
			effects().anchor = HudAnchor.CUSTOM;
			if (!bounds.isEmpty()) {
				effects().offsetX = clampOffset(bounds.x());
				effects().offsetY = clampOffset(bounds.y());
			}
			syncAnchorLabel();
		}
	}

	private void applyCustomOrigin(int x, int y) {
		if (effects().anchor != HudAnchor.CUSTOM) {
			effects().anchor = HudAnchor.CUSTOM;
			syncAnchorLabel();
		}
		effects().offsetX = clampOffset(x);
		effects().offsetY = clampOffset(y);
	}

	private static int clampOffset(int value) {
		return ConfigValidator.clampInt(value, -ConfigValidator.MAX_OFFSET, ConfigValidator.MAX_OFFSET);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0) {
			draggingElement = false;
			resizingCorner = null;
			activeGuides = List.of();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isEscape()) { // cancelar y restaurar la snapshot de entrada
			cancelChanges();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		syncParent();
		this.minecraft.setScreen(parent);
	}
}

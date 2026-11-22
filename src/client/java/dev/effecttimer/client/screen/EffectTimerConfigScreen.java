package dev.effecttimer.client.screen;

import dev.effecttimer.client.EffectTimerMod;
import dev.effecttimer.client.config.ColorHex;
import dev.effecttimer.client.config.EffectTimerConfig;
import dev.effecttimer.client.config.Orientation;
import dev.effecttimer.client.hud.EffectTimerHudRenderer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Pantalla nativa de configuración del Effect Timer.
 *
 * <p>Limpia según la Fase 3.2 del encargo: solo expone los parámetros del
 * Effect Timer (enabled, posX, posY, scale, orientation, showIcons,
 * showDuration, showName, textColor, blinkOnExpire). Los sliders son la
 * reducción directa de {@code IntSliderButton}/{@code ScaleSliderButton} del
 * mod original y el editor hexadecimal reutiliza el patrón de validación en
 * vivo de {@code EffectColorsScreen}.</p>
 *
 * <p>Fase 4 (MUST-HAVE): el botón "Modo de Prueba" dibuja pociones ficticias
 * en vivo sobre esta misma pantalla, y los sliders de posición/escala también
 * las fuerzan mientras se arrastran. Al cerrar la GUI o apagar el toggle, la
 * vista previa desaparece y el HUD del juego vuelve a leer los StatusEffects
 * reales del jugador ({@code client.player.getActiveEffects()}).</p>
 */
public final class EffectTimerConfigScreen extends Screen {
	private static final int PANEL_WIDTH = 236;
	private static final int ROW_STEP = 24;
	private static final int WIDGET_HEIGHT = 20;
	private static final int HALF_GAP = 6;
	private static final int HEX_LABEL_WIDTH = 48;
	private static final int PALETTE_BUTTON_WIDTH = 60;

	private final Screen parent;
	private final EffectTimerConfig cfg;

	private boolean testMode;
	private boolean draggingSliders;
	private long previewAnchorMs = System.currentTimeMillis();

	private EditBox hexEditBox;
	private boolean hexInvalid;

	private int paletteX;
	private int paletteY;
	private int paletteW;
	private int paletteH;

	public EffectTimerConfigScreen(Screen parent) {
		super(Component.translatable("effecttimer.screen.title"));
		this.parent = parent;
		this.cfg = EffectTimerMod.getConfig();
	}

	@Override
	protected void init() {
		this.previewAnchorMs = System.currentTimeMillis();
		int left = (this.width - PANEL_WIDTH) / 2;
		int halfW = (PANEL_WIDTH - HALF_GAP) / 2;
		int rightX = left + halfW + HALF_GAP;
		int y = 42;

		// enabled
		this.addRenderableWidget(CycleButton.booleanBuilder(t("effecttimer.state.on"), t("effecttimer.state.off"), cfg.enabled)
				.create(left, y, PANEL_WIDTH, WIDGET_HEIGHT, t("effecttimer.option.enabled"), (btn, val) -> {
					cfg.enabled = val;
					EffectTimerMod.markDirty();
				}));
		y += ROW_STEP;

		// orientation | scale
		this.addRenderableWidget(CycleButton.builder((Orientation o) -> t(o.translationKey()), cfg.orientation)
				.withValues(List.of(Orientation.values()))
				.create(left, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.orientation"), (btn, val) -> {
					cfg.orientation = val;
					EffectTimerMod.markDirty();
				}));
		this.addRenderableWidget(new ScaleSlider(rightX, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.scale"),
				cfg.scale, val -> {
					cfg.scale = val;
				}));
		y += ROW_STEP;

		// posX | posY
		int posXMax = Math.max(16, this.width - 40);
		int posYMax = Math.max(16, this.height - 40);
		this.addRenderableWidget(new IntSlider(left, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.posX"),
				0, posXMax, cfg.posX, val -> cfg.posX = val));
		this.addRenderableWidget(new IntSlider(rightX, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.posY"),
				0, posYMax, cfg.posY, val -> cfg.posY = val));
		y += ROW_STEP;

		// showIcons | showName
		this.addRenderableWidget(CycleButton.booleanBuilder(t("effecttimer.state.on"), t("effecttimer.state.off"), cfg.showIcons)
				.create(left, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.showIcons"), (btn, val) -> {
					cfg.showIcons = val;
					EffectTimerMod.markDirty();
				}));
		this.addRenderableWidget(CycleButton.booleanBuilder(t("effecttimer.state.on"), t("effecttimer.state.off"), cfg.showName)
				.create(rightX, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.showName"), (btn, val) -> {
					cfg.showName = val;
					EffectTimerMod.markDirty();
				}));
		y += ROW_STEP;

		// showDuration | blinkOnExpire
		this.addRenderableWidget(CycleButton.booleanBuilder(t("effecttimer.state.on"), t("effecttimer.state.off"), cfg.showDuration)
				.create(left, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.showDuration"), (btn, val) -> {
					cfg.showDuration = val;
					EffectTimerMod.markDirty();
				}));
		this.addRenderableWidget(CycleButton.booleanBuilder(t("effecttimer.state.on"), t("effecttimer.state.off"), cfg.blinkOnExpire)
				.create(rightX, y, halfW, WIDGET_HEIGHT, t("effecttimer.option.blinkOnExpire"), (btn, val) -> {
					cfg.blinkOnExpire = val;
					EffectTimerMod.markDirty();
				}));
		y += ROW_STEP;

		// textColor: etiqueta + EditBox hex validado + botón de paleta cíclica
		int hexX = left + HEX_LABEL_WIDTH;
		int hexW = PANEL_WIDTH - HEX_LABEL_WIDTH - PALETTE_BUTTON_WIDTH - HALF_GAP;
		this.hexEditBox = new EditBox(this.font, hexX, y + 1, hexW, 18, t("effecttimer.option.textColor"));
		this.hexEditBox.setMaxLength(7);
		this.hexEditBox.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
		this.hexEditBox.setValue(ColorHex.normalizeOrDefault(cfg.textColor, "#FFFFFF"));
		this.hexEditBox.setResponder(text -> {
			Optional<String> normalized = ColorHex.normalizeHex(text);
			if (normalized.isPresent()) {
				this.hexInvalid = false;
				if (!normalized.get().equals(cfg.textColor)) {
					cfg.textColor = normalized.get();
					EffectTimerMod.markDirty();
				}
			} else {
				this.hexInvalid = true;
			}
		});
		this.addRenderableWidget(this.hexEditBox);

		this.paletteX = left + PANEL_WIDTH - PALETTE_BUTTON_WIDTH;
		this.paletteY = y;
		this.paletteW = PALETTE_BUTTON_WIDTH;
		this.paletteH = WIDGET_HEIGHT;
		this.addRenderableWidget(Button.builder(t("effecttimer.button.palette"), btn -> {
			String next = ColorHex.nextPaletteColor(cfg.textColor, ColorHex.TEXT_PALETTE);
			cfg.textColor = next;
			if (this.hexEditBox != null) {
				this.hexEditBox.setValue(next);
			}
			this.hexInvalid = false;
			EffectTimerMod.markDirty();
		}).bounds(this.paletteX, this.paletteY, this.paletteW, this.paletteH).build());
		y += ROW_STEP;

		// Modo de Prueba (Fase 4): dibuja pociones ficticias mientras esté ON.
		this.addRenderableWidget(CycleButton.booleanBuilder(t("effecttimer.state.on"), t("effecttimer.state.off"), testMode)
				.create(left, y, PANEL_WIDTH, WIDGET_HEIGHT, t("effecttimer.option.testMode"), (btn, val) -> {
					this.testMode = val;
					if (val) {
						this.previewAnchorMs = System.currentTimeMillis();
					}
				}));

		this.addRenderableWidget(Button.builder(t("effecttimer.button.done"), btn -> onClose())
				.bounds(this.width / 2 - 50, this.height - 28, 100, 20)
				.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);

		graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFF7FAFC);
		graphics.drawCenteredString(this.font, t("effecttimer.screen.subtitle"), this.width / 2, 26, 0xFFA0AEC0);
		graphics.drawString(this.font, t("effecttimer.option.textColor"), (this.width - PANEL_WIDTH) / 2, this.paletteY + 6, 0xFFD8DEE9, true);

		// Fase 4: vista previa ficticia con el toggle ON o mientras se arrastran sliders;
		// en cualquier otro caso, efectos reales del jugador (WYSIWYG) si los hay.
		boolean preview = testMode || draggingSliders;
		List<EffectTimerHudRenderer.Row> rows = preview
				? EffectTimerHudRenderer.createPreviewRows(previewElapsedTicks())
				: EffectTimerHudRenderer.collectLiveRows(this.minecraft);
		if (!rows.isEmpty()) {
			EffectTimerHudRenderer.drawRows(graphics, cfg, this.font, rows, EffectTimerHudRenderer.currentTicksPerSecond(this.minecraft));
		}

		// Marco de validación + muestra de color (patrón de EffectColorsScreen del original).
		if (this.hexEditBox != null) {
			int validRgb = ColorHex.parseRgb(cfg.textColor).orElse(0xFFFFFF);
			int outline = hexInvalid ? 0xFFEF4444 : 0xFF000000 | validRgb;
			graphics.renderOutline(
					this.hexEditBox.getX() - 1,
					this.hexEditBox.getY() - 1,
					this.hexEditBox.getWidth() + 2,
					this.hexEditBox.getHeight() + 2,
					outline
			);
		}
		graphics.fill(paletteX + 4, paletteY + 6, paletteX + 12, paletteY + 14, 0xFF000000 | ColorHex.parseRgb(cfg.textColor).orElse(0xFFFFFF));
		graphics.renderOutline(paletteX + 4, paletteY + 6, 8, 8, 0xFF000000);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean handled = super.mouseReleased(event);
		this.draggingSliders = false;
		return handled;
	}

	/** Llamado por los sliders propios mientras el usuario los arrastra (fuerza la vista previa). */
	void setSlidersDragging(boolean dragging) {
		this.draggingSliders = dragging;
	}

	private long previewElapsedTicks() {
		return Math.max(0L, (System.currentTimeMillis() - this.previewAnchorMs) / 50L);
	}

	@Override
	public void removed() {
		// Al cerrar la GUI se persiste la config y la vista previa desaparece:
		// el HUD del juego vuelve a leer los StatusEffects reales del jugador.
		EffectTimerMod.saveNow();
		super.removed();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private static Component t(String key, Object... args) {
		return Component.translatable(key, args);
	}

	private static String formatScale(float value) {
		return String.format(Locale.ROOT, "%.2fx", value);
	}

	// --- Sliders: reducción directa de IntSliderButton / ScaleSliderButton del mod original ---

	private final class IntSlider extends AbstractSliderButton {
		private final Component prefix;
		private final int min;
		private final int max;
		private final IntConsumer onChanged;
		private boolean suppressCallback;

		private IntSlider(int x, int y, int width, int height, Component prefix, int min, int max, int initialValue,
				IntConsumer onChanged) {
			super(x, y, width, height, Component.empty(), toNormalized(initialValue, min, max));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.onChanged = onChanged;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.empty().append(prefix).append(": ").append(String.valueOf(getIntValue())));
		}

		@Override
		protected void applyValue() {
			if (!suppressCallback && onChanged != null) {
				onChanged.accept(getIntValue());
				EffectTimerMod.markDirty();
				setSlidersDragging(true);
			}
		}

		private int getIntValue() {
			int v = (int) Math.round(min + value * (max - min));
			return Math.max(min, Math.min(max, v));
		}

		private static double toNormalized(int value, int min, int max) {
			if (max <= min) {
				return 0.0;
			}
			double v = (value - min) / (double) (max - min);
			return Math.max(0.0, Math.min(1.0, v));
		}
	}

	private final class ScaleSlider extends AbstractSliderButton {
		private final Component prefix;
		private final Consumer<Float> onChanged;
		private boolean suppressCallback;

		private ScaleSlider(int x, int y, int width, int height, Component prefix, float initialValue,
				Consumer<Float> onChanged) {
			super(x, y, width, height, Component.empty(), toNormalized(initialValue));
			this.prefix = prefix;
			this.onChanged = onChanged;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.empty().append(prefix).append(": ").append(formatScale(getScaleValue())));
		}

		@Override
		protected void applyValue() {
			if (!suppressCallback && onChanged != null) {
				onChanged.accept(getScaleValue());
				EffectTimerMod.markDirty();
				setSlidersDragging(true);
			}
		}

		private float getScaleValue() {
			float raw = (float) (EffectTimerConfig.MIN_SCALE + value * (EffectTimerConfig.MAX_SCALE - EffectTimerConfig.MIN_SCALE));
			return Math.round(raw * 20.0F) / 20.0F;
		}

		private static double toNormalized(float scale) {
			float clamped = Math.max(EffectTimerConfig.MIN_SCALE, Math.min(EffectTimerConfig.MAX_SCALE, scale));
			return (clamped - EffectTimerConfig.MIN_SCALE) / (EffectTimerConfig.MAX_SCALE - EffectTimerConfig.MIN_SCALE);
		}
	}
}

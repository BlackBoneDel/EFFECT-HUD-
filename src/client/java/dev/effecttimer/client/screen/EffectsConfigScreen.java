package dev.effecttimer.client.screen;

import dev.effecttimer.client.hud.EffectsHudRenderer;
import dev.effecttimer.client.hud.LiveDataCollector;
import dev.effecttimer.config.ConfigValidator;
import dev.effecttimer.config.EffectsFramePolicy;
import dev.effecttimer.config.EffectColorOverride;
import dev.effecttimer.config.EffectSortMode;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.EffectsVisualStyle;
import dev.effecttimer.config.HudAnchor;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.HudOrientation;
import dev.effecttimer.config.LevelFormat;
import dev.effecttimer.config.PotionHudMode;
import dev.effecttimer.config.PotionMiniDisplayMode;
import dev.effecttimer.config.VanillaEffectsMode;
import dev.effecttimer.format.EffectOrderer;
import dev.effecttimer.format.EffectSnapshot;
import dev.effecttimer.layout.HudRect;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Pantalla "Configure Effects…" (apariencia / contenido / orden / filtro /
 * comportamiento) con vista previa en vivo, con el botón **Modo de Prueba**:
 * con ON, la vista previa fuerza pociones ficticias con cuenta regresiva en bucle
 * (aunque el jugador tenga efectos reales); al cerrar la GUI el modo se apaga solo.
 */
public final class EffectsConfigScreen extends AbstractScrollableConfigScreen {
	/** Plantas del Modo de Prueba: (efecto, amplifier, duración ticks, ambient). */
	private static final Object[][] TEST_SAMPLES = {
			{ MobEffects.REGENERATION, 0, 840, false },
			{ MobEffects.SPEED, 1, 3080, false },
			{ MobEffects.HASTE, 0, 840, true },
			{ MobEffects.NIGHT_VISION, 0, MobEffectInstance.INFINITE_DURATION, false },
			{ MobEffects.WEAKNESS, 0, 280, false }
	};
	/** Pausa (ticks) entre vueltas del bucle de cuenta regresiva. */
	private static final int TEST_LOOP_PAUSE_TICKS = 100;

	private boolean testMode;
	private long previewAnchorMs = System.currentTimeMillis();

	public EffectsConfigScreen(Screen parent, HudConfig workingConfig) {
		super("effecttimer.screen.effects.title", parent, workingConfig);
	}

	@Override
	protected boolean hasPreviewPane() {
		return true;
	}

	@Override
	protected Component subtitleText() {
		return t("effecttimer.screen.effects.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		EffectsConfig effects = workingConfig.effects;
		int panelW = contentPanelWidth();
		int leftX = contentLeftX();
		int halfW = (panelW - 6) / 2;
		int rightX = leftX + halfW + 6;
		int y = 4;

		if (this.width < 540 && this.height < 245) {
			addRenderableWidget(
					Button.builder(
							t(compactPreviewExpanded ? "effecttimer.preview.hide" : "effecttimer.preview.show"),
							btn -> {
								compactPreviewExpanded = !compactPreviewExpanded;
								this.rebuildWidgets();
							}
					).bounds(6, 4, 58, 15)
							.tooltip(Tooltip.create(t("effecttimer.tooltip.toggle_preview")))
							.build()
			);
		}

		// El HUD vanilla es el primer control de la sección Visibilidad.
		addSectionLabel(t("effecttimer.section.visibility"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.builder((VanillaEffectsMode m) -> t(m.translationKey()), effects.vanillaEffectsMode)
						.withValues(List.of(VanillaEffectsMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.vanilla_mode"), (btn, val) -> {
							effects.vanillaEffectsMode = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.enabled
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.enabled_on"
						: "effecttimer.tooltip.effects.enabled_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.option.enabled"), (btn, val) -> {
							effects.enabled = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addSectionLabel(t("effecttimer.section.general"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.builder((EffectsVisualStyle st) -> t(st.translationKey()), effects.style)
						.withValues(List.of(EffectsVisualStyle.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("effecttimer.option.style"), (btn, val) -> {
							effects.style = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((HudAnchor a) -> t(a.translationKey()), effects.anchor)
						.withValues(List.of(HudAnchor.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("effecttimer.option.anchor"), (btn, val) -> {
							effects.anchor = val;
							if (val != HudAnchor.CUSTOM) {
								effects.offsetX = 0;
								effects.offsetY = 0;
							}
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		// Selector rápido 3x2 (Alta/Media/Baja × I/D). Al pulsar
		// un botón fijo se aplican offsets 0 (contrato de salida de CUSTOM).
		addSectionLabel(t("effecttimer.section.quick_positions"), leftX, y);
		y += 14;
		java.util.List<net.minecraft.client.gui.components.Button> quickButtons = AnchorQuickPicker.build(
				effects.anchor,
				anchor -> {
					AnchorQuickPicker.applyFixedAnchor(effects, anchor);
					notifyConfigEdited();
					this.rebuildWidgets();
				},
				this::t, leftX, 6, y, 3, (panelW - 6) / 2, 20);
		for (int qRow = 0; qRow < AnchorQuickPicker.ROWS; qRow++) {
			for (int qCol = 0; qCol < AnchorQuickPicker.COLS; qCol++) {
				int qIndex = qRow * AnchorQuickPicker.COLS + qCol;
				if (qIndex < quickButtons.size()) {
					addScrollWidget(quickButtons.get(qIndex), y);
				}
			}
			y += 23;
		}
		y += 6;

		addScrollWidget(
				CycleButton.builder((HudOrientation o) -> t(o.translationKey()), effects.orientation)
						.withValues(List.of(HudOrientation.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("effecttimer.option.orientation"), (btn, val) -> {
							effects.orientation = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				new ScaleSliderButton(
						rightX,
						0,
						halfW,
						20,
						t("effecttimer.option.scale"),
						effects.scale,
						val -> {
							effects.scale = val;
							notifyConfigEdited();
						},
						val -> t("effecttimer.tooltip.effects.scale", String.format(Locale.ROOT, "%.2fx", val))
				),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				new IntSliderButton(
						leftX,
						0,
						panelW,
						20,
						t("effecttimer.option.spacing"),
						" px",
						ConfigValidator.MIN_SPACING,
						ConfigValidator.MAX_SPACING,
						effects.rowSpacing,
						val -> {
							effects.rowSpacing = val;
							notifyConfigEdited();
						},
						val -> t("effecttimer.tooltip.effects.spacing", val)
				),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("effecttimer.section.effects_appearance"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.showEffectBackground
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.show_background_on"
						: "effecttimer.tooltip.effects.show_background_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.show_background"), (btn, val) -> {
							effects.showEffectBackground = val;
							notifyConfigEdited();
							// El control de borde depende del estado del fondo.
							this.rebuildWidgets();
						}),
				y
		);
		// Sin fondo no hay marco: el control de borde se muestra
		// deshabilitado (en gris) con tooltip, pero el valor guardado se conserva.
		CycleButton<Boolean> borderBtn =
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.showEffectBorder
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.show_border_on"
						: "effecttimer.tooltip.effects.show_border_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.effects.show_border"), (btn, val) -> {
							effects.showEffectBorder = val;
							notifyConfigEdited();
						});
		borderBtn.active = EffectsFramePolicy.borderControlEnabled(effects.showEffectBackground);
		if (!EffectsFramePolicy.borderControlEnabled(effects.showEffectBackground)) {
			borderBtn.setTooltip(Tooltip.create(t("effecttimer.tooltip.effects.border_requires_background")));
		}
		addScrollWidget(borderBtn, y);
		y += ROW_HEIGHT;

		addScrollWidget(
				new IntSliderButton(
						leftX,
						0,
						panelW,
						20,
						t("effecttimer.effects.bg_opacity"),
						"%",
						ConfigValidator.MIN_OPACITY_PERCENT,
						ConfigValidator.MAX_OPACITY_PERCENT,
						effects.backgroundOpacityPercent,
						val -> {
							effects.backgroundOpacityPercent = val;
							notifyConfigEdited();
						},
						val -> t("effecttimer.tooltip.effects.bg_opacity", val)
				),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((PotionHudMode pm) -> t(pm.translationKey()), effects.potionHudMode)
						.withValues(List.of(PotionHudMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.potion_mode"), (btn, val) -> {
							effects.potionHudMode = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((PotionMiniDisplayMode md) -> t(md.translationKey()), effects.potionMiniDisplay)
						.withValues(List.of(PotionMiniDisplayMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("effecttimer.effects.mini_display"), (btn, val) -> {
							effects.potionMiniDisplay = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				Button.builder(
						t("effecttimer.button.effect_colors"),
						btn -> this.minecraft.setScreen(new EffectColorsScreen(this, workingConfig))
				).bounds(leftX, 0, panelW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.effect_colors")))
						.build(),
				y
		);
		y += ROW_HEIGHT;

		// Interruptor GLOBAL de colores personalizados.
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.customColorsEnabled
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.custom_colors_on"
						: "effecttimer.tooltip.effects.custom_colors_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.custom_colors"), (btn, val) -> {
							effects.customColorsEnabled = val;
							notifyConfigEdited();
						}),
				y
		);
		// Sombra de texto.
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.textShadow
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.text_shadow_on"
						: "effecttimer.tooltip.effects.text_shadow_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.option.text_shadow"), (btn, val) -> {
							effects.textShadow = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		// Modo de Prueba.
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						testMode
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.test_mode_on"
						: "effecttimer.tooltip.test_mode_off")))
						.create(leftX, 0, panelW, 20, t("effecttimer.option.testMode"), (btn, value) -> {
							testMode = value;
							if (value) {
								previewAnchorMs = System.currentTimeMillis();
							}
						}),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("effecttimer.section.effects_elements"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.showIcon
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.show_icon_on"
						: "effecttimer.tooltip.effects.show_icon_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.option.show_icon"), (btn, val) -> {
							effects.showIcon = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.showName
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.show_name_on"
						: "effecttimer.tooltip.effects.show_name_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.effects.show_name"), (btn, val) -> {
							effects.showName = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.showDuration
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.show_duration_on"
						: "effecttimer.tooltip.effects.show_duration_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.show_duration"), (btn, val) -> {
							effects.showDuration = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.showLevel
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.show_level_on"
						: "effecttimer.tooltip.effects.show_level_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.effects.show_level"), (btn, val) -> {
							effects.showLevel = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.hideLevelOne
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.hide_level_one_on"
						: "effecttimer.tooltip.effects.hide_level_one_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.hide_level_one"), (btn, val) -> {
							effects.hideLevelOne = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((LevelFormat f) -> t(f.translationKey()), effects.levelFormat)
						.withValues(List.of(LevelFormat.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("effecttimer.effects.level_format"), (btn, val) -> {
							effects.levelFormat = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("effecttimer.section.effects_behavior"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.showEffectsWithoutParticles
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.without_particles_on"
						: "effecttimer.tooltip.effects.without_particles_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.without_particles"), (btn, val) -> {
							effects.showEffectsWithoutParticles = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.groupByEffectCategory
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.group_category_on"
						: "effecttimer.tooltip.effects.group_category_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.effects.group_category"), (btn, val) -> {
							effects.groupByEffectCategory = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((EffectSortMode sm) -> t(sm.translationKey()), effects.sortMode)
						.withValues(List.of(EffectSortMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, panelW, 20, t("effecttimer.effects.sort_mode"), (btn, val) -> {
							effects.sortMode = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.fadeOnLowDuration
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.fade_low_on"
						: "effecttimer.tooltip.effects.fade_low_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.effects.fade_low"), (btn, val) -> {
							effects.fadeOnLowDuration = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						effects.colorDurationByRemaining
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.effects.color_duration_on"
						: "effecttimer.tooltip.effects.color_duration_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.effects.color_duration"), (btn, val) -> {
							effects.colorDurationByRemaining = val;
							notifyConfigEdited();
						}),
				y
		);

		syncScrollPositions();

		int footerY = this.height - FOOTER_HEIGHT + 7;
		int totalFooterW = Math.min(420, this.width - 20);
		int btnGap = 5;
		int btnW = (totalFooterW - btnGap * 3) / 4;
		int footerX = (this.width - (btnW * 4 + btnGap * 3)) / 2;

		addRenderableWidget(
				Button.builder(
						t("effecttimer.button.open_editor_short"),
						btn -> this.minecraft.setScreen(new HudEditorScreen(this, workingConfig, updated -> {
							syncWorkingFromManager(updated);
						}))
				).bounds(footerX, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.open_editor")))
						.build()
		);

		addRenderableWidget(
				Button.builder(
						t("effecttimer.button.reset_module"),
						btn -> {
							Map<String, EffectColorOverride> preservedColors =
									workingConfig.effects != null ? workingConfig.effects.customEffectColors : null;
							workingConfig.effects = new EffectsConfig();
							if (preservedColors != null) {
								workingConfig.effects.customEffectColors = preservedColors;
							}
							notifyConfigEdited();
							this.rebuildWidgets();
						}
				).bounds(footerX + (btnW + btnGap), footerY, btnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.effects.reset_module")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("effecttimer.button.save"), btn -> saveConfigNow())
						.bounds(footerX + (btnW + btnGap) * 2, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.save_now")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("effecttimer.button.back"), btn -> onClose())
						.bounds(footerX + (btnW + btnGap) * 3, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.back_screen")))
						.build()
		);
	}

	@Override
	protected void renderPreviewPanel(GuiGraphics graphics, HudRect previewBox) {
		Component label = workingConfig.effects.enabled
				? t("effecttimer.preview.title")
				: t("effecttimer.preview.disabled");
		graphics.drawCenteredString(this.font, label, previewBox.centerX(), previewBox.y() + 4,
				workingConfig.effects.enabled ? 0xFF7FD4E6 : 0xFFFBBF24);

		int contentTop = previewBox.y() + 15;
		int contentH = Math.max(12, previewBox.height() - 18);
		float alpha = workingConfig.effects.enabled ? 1.0f : 0.55f;
		if (testMode) {
			EffectsHudRenderer.renderPreviewEntries(
					graphics,
					this.minecraft,
					workingConfig.effects,
					activeLang(),
					previewBox.x() + 4,
					contentTop,
					previewBox.width() - 8,
					contentH,
					alpha,
					buildTestPreviewEntries(),
					true
			);
			return;
		}
		EffectsHudRenderer.renderPreviewInBox(
				graphics,
				this.minecraft,
				workingConfig.effects,
				activeLang(),
				previewBox.x() + 4,
				contentTop,
				previewBox.width() - 8,
				contentH,
				alpha,
				null
		);
	}

	/**
	 * Pociones ficticias del Modo de Prueba con cuenta regresiva en bucle: la
	 * duración restante se calcula contra {@link #previewAnchorMs} y el TPS del
	 * cliente, y al llegar a 0 la fila descansa {@value #TEST_LOOP_PAUSE_TICKS}
	 * ticks y vuelve a empezar.
	 */
	private List<LiveDataCollector.EffectRenderEntry> buildTestPreviewEntries() {
		long elapsedMs = Math.max(0L, System.currentTimeMillis() - previewAnchorMs);
		double elapsedTicks = elapsedMs * LiveDataCollector.currentTicksPerSecond(this.minecraft) / 1000.0;
		List<EffectSnapshot> snapshots = new ArrayList<>(TEST_SAMPLES.length);
		List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> holders = new ArrayList<>(TEST_SAMPLES.length);
		for (Object[] sample : TEST_SAMPLES) {
			@SuppressWarnings("unchecked")
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> holder =
					(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) sample[0];
			int amplifier = (Integer) sample[1];
			int totalTicks = (Integer) sample[2];
			boolean ambient = (Boolean) sample[3];
			MobEffectInstance instance;
			if (totalTicks == MobEffectInstance.INFINITE_DURATION) {
				instance = new MobEffectInstance(holder, MobEffectInstance.INFINITE_DURATION, amplifier, ambient, true, true);
			} else {
				double cycle = totalTicks + TEST_LOOP_PAUSE_TICKS;
				double pos = elapsedTicks % cycle;
				int remaining = (int) Math.max(1, Math.ceil(totalTicks - pos));
				instance = new MobEffectInstance(holder, remaining, amplifier, ambient, true, true);
			}
			snapshots.add(LiveDataCollector.snapshotFromEffect(instance, activeLang(), false));
			holders.add(holder);
		}
		// Ordenar con la regla configurada manteniendo el vínculo snapshot→holder.
		List<LiveDataCollector.EffectRenderEntry> pairs = new ArrayList<>(snapshots.size());
		for (int i = 0; i < snapshots.size(); i++) {
			pairs.add(new LiveDataCollector.EffectRenderEntry(snapshots.get(i), holders.get(i)));
		}
		List<EffectSnapshot> ordered = EffectOrderer.prepare(snapshots, workingConfig.effects);
		List<LiveDataCollector.EffectRenderEntry> entries = new ArrayList<>(ordered.size());
		for (EffectSnapshot snapshot : ordered) {
			for (LiveDataCollector.EffectRenderEntry pair : pairs) {
				if (pair.snapshot() == snapshot) {
					entries.add(pair);
					break;
				}
			}
		}
		return entries;
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		this.minecraft.setScreen(parent);
	}
}

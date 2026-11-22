package dev.effecttimer.client.screen;

import dev.effecttimer.client.EffectTimerMod;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.ModLanguage;
import dev.effecttimer.config.PresetCatalog;
import dev.effecttimer.config.PresetId;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MainConfigScreen extends AbstractScrollableConfigScreen {
	private PresetId selectedPreset = PresetId.VANILLA_COMPACT;

	public MainConfigScreen(Screen parent) {
		this(parent, EffectTimerMod.getConfigManager().getConfig().copy());
	}

	private MainConfigScreen(Screen parent, HudConfig workingConfig) {
		super("effecttimer.screen.main.title", parent, workingConfig);
	}

	@Override
	protected Component subtitleText() {
		return t("effecttimer.screen.main.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		int panelW = contentPanelWidth();
		int leftX = contentLeftX();
		int halfW = (panelW - 6) / 2;
		int rightX = leftX + halfW + 6;
		int y = 4;

		addSectionLabel(t("effecttimer.section.language"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.builder((ModLanguage lang) -> Component.literal(lang.displayName()), activeLang())
						.withValues(List.of(ModLanguage.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, panelW, 20, t("effecttimer.option.language"), (btn, value) -> {
							workingConfig.language = value;
							notifyConfigEdited();
							this.rebuildWidgets();
						}),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("effecttimer.section.effects"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						workingConfig.effects.enabled
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.module.effects_on"
						: "effecttimer.tooltip.module.effects_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.module.effects"), (btn, value) -> {
							workingConfig.effects.enabled = value;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				Button.builder(
						t("effecttimer.button.configure_effects"),
						btn -> this.minecraft.setScreen(new EffectsConfigScreen(this, workingConfig))
				).bounds(rightX, 0, halfW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.configure_effects")))
						.build(),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				Button.builder(
								t("effecttimer.button.open_editor"),
								btn -> this.minecraft.setScreen(new HudEditorScreen(this, workingConfig, updated -> {
									syncWorkingFromManager(updated);
								}))
						)
						.bounds(leftX, 0, panelW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.open_editor")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("effecttimer.section.presets"), leftX, y);
		y += 16;

		int applyBtnW = 92;
		int presetW = panelW - applyBtnW - 6;
		addScrollWidget(
				CycleButton.builder((PresetId p) -> t(p.translationKey()), selectedPreset)
						.withValues(List.of(PresetId.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, presetW, 20, t("effecttimer.option.preset"), (btn, value) -> selectedPreset = value),
				y
		);
		addScrollWidget(
				Button.builder(
						t("effecttimer.button.apply_preset"),
						btn -> {
							PresetCatalog.applyPreset(workingConfig, selectedPreset);
							notifyConfigEdited();
							this.rebuildWidgets();
						}
				).bounds(leftX + presetW + 6, 0, applyBtnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.apply_preset")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("effecttimer.section.visibility"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						workingConfig.visibility.hideInInventory
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.hide_inventory_on"
						: "effecttimer.tooltip.hide_inventory_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.option.hide_inventory"), (btn, value) -> {
							workingConfig.visibility.hideInInventory = value;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						workingConfig.visibility.hideInChat
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.hide_chat_on"
						: "effecttimer.tooltip.hide_chat_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.option.hide_chat"), (btn, value) -> {
							workingConfig.visibility.hideInChat = value;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						workingConfig.visibility.hideInPauseScreen
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.hide_pause_on"
						: "effecttimer.tooltip.hide_pause_off")))
						.create(leftX, 0, halfW, 20, t("effecttimer.option.hide_pause"), (btn, value) -> {
							workingConfig.visibility.hideInPauseScreen = value;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("effecttimer.state.on"),
						t("effecttimer.state.off"),
						workingConfig.editorShowGuides
				).withTooltip(val -> Tooltip.create(t(val
						? "effecttimer.tooltip.editor_guides_on"
						: "effecttimer.tooltip.editor_guides_off")))
						.create(rightX, 0, halfW, 20, t("effecttimer.option.editor_guides"), (btn, value) -> {
							workingConfig.editorShowGuides = value;
							notifyConfigEdited();
						}),
				y
		);

		syncScrollPositions();

		int footerY = this.height - FOOTER_HEIGHT + 7;
		int btnGap = 6;
		int btnW = Math.min(116, (panelW - btnGap * 2) / 3);
		int totalFooterW = btnW * 3 + btnGap * 2;
		int footerX = (this.width - totalFooterW) / 2;

		addRenderableWidget(
				Button.builder(
						t("effecttimer.button.reset_all"),
						btn -> {
							HudConfig defaults = HudConfig.createDefault();
							syncWorkingFromManager(defaults);
							notifyConfigEdited();
							this.rebuildWidgets();
						}
				).bounds(footerX, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.reset_all")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("effecttimer.button.save"), btn -> saveConfigNow())
						.bounds(footerX + btnW + btnGap, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.save_now")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("effecttimer.button.close"), btn -> onClose())
						.bounds(footerX + (btnW + btnGap) * 2, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("effecttimer.tooltip.close_screen")))
						.build()
		);
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		this.minecraft.setScreen(parent);
	}
}

package dev.effecttimer.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preajustes del módulo Effects HUD (portados de {@code PresetCatalog} del repo
 * original, con los bloques de armadura eliminados). Como en el original, los
 * colores personalizados por efecto SE CONSERVAN al aplicar un preajuste.
 */
public final class PresetCatalog {
	private PresetCatalog() {
	}

	public static HudConfig createPreset(PresetId presetId) {
		HudConfig cfg = HudConfig.createDefault();
		applyPreset(cfg, presetId);
		return cfg;
	}

	public static void applyPreset(HudConfig target, PresetId presetId) {
		if (target == null || presetId == null) {
			return;
		}
		target.configVersion = HudConfig.CURRENT_CONFIG_VERSION;
		Map<String, EffectColorOverride> preservedColors = new LinkedHashMap<>();
		if (target.effects != null && target.effects.customEffectColors != null) {
			for (Map.Entry<String, EffectColorOverride> entry : target.effects.customEffectColors.entrySet()) {
				if (entry.getKey() != null && entry.getValue() != null) {
					preservedColors.put(entry.getKey(), entry.getValue().copy());
				}
			}
		}

		switch (presetId) {
			case VANILLA_COMPACT -> {
				target.effects = new EffectsConfig();
			}
			case COMPACT_TEXT -> {
				EffectsConfig effects = new EffectsConfig();
				effects.enabled = true;
				effects.anchor = HudAnchor.CENTER_RIGHT;
				effects.orientation = HudOrientation.VERTICAL;
				effects.style = EffectsVisualStyle.MINIMAL_TEXT;
				effects.rowSpacing = 1;
				effects.scale = 0.90f;
				effects.showEffectBackground = false;
				effects.showEffectBorder = false;
				effects.showIcon = true;
				effects.showName = true;
				effects.showDuration = true;
				effects.showLevel = true;
				effects.hideLevelOne = true;
				effects.levelFormat = LevelFormat.ARABIC;
				effects.vanillaEffectsMode = VanillaEffectsMode.SHOW_BOTH;
				target.effects = effects;
			}
			case EXPANDED_READOUT -> {
				EffectsConfig effects = new EffectsConfig();
				effects.enabled = true;
				effects.anchor = HudAnchor.CENTER_RIGHT;
				effects.orientation = HudOrientation.VERTICAL;
				effects.style = EffectsVisualStyle.CLEAN_CARD;
				effects.rowSpacing = 3;
				effects.scale = 1.10f;
				effects.showEffectBackground = false;
				effects.showEffectBorder = true;
				effects.showIcon = true;
				effects.showName = true;
				effects.showDuration = true;
				effects.showLevel = true;
				effects.hideLevelOne = false;
				effects.levelFormat = LevelFormat.ROMAN;
				effects.colorDurationByRemaining = true;
				effects.vanillaEffectsMode = VanillaEffectsMode.SHOW_BOTH;
				target.effects = effects;
			}
		}

		target.effects.customEffectColors = preservedColors;
	}
}

package dev.effecttimer.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ConfigValidator {
	public static final float MIN_SCALE = 0.50f;
	public static final float MAX_SCALE = 2.50f;
	public static final int MIN_SPACING = 0;
	public static final int MAX_SPACING = 16;
	public static final int MIN_OFFSET = -4000;
	public static final int MAX_OFFSET = 4000;
	public static final int MIN_OPACITY_PERCENT = 0;
	public static final int MAX_OPACITY_PERCENT = 100;
	public static final int MIN_GRID_SIZE = 1;
	public static final int MAX_GRID_SIZE = 32;

	private static final Pattern NAMESPACED_ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9/._-]+$");
	private static final Pattern PATH_ONLY_PATTERN = Pattern.compile("^[a-z0-9/._-]+$");

	private ConfigValidator() {
	}

	public record ValidationResult(HudConfig config, boolean modified) {
	}

	public static ValidationResult validateAndSanitize(HudConfig input) {
		boolean modified = false;
		if (input == null) {
			return new ValidationResult(HudConfig.createDefault(), true);
		}

		HudConfig cfg = input.copy();
		if (cfg.configVersion <= 0 || cfg.configVersion > HudConfig.CURRENT_CONFIG_VERSION) {
			cfg.configVersion = HudConfig.CURRENT_CONFIG_VERSION;
			modified = true;
		}
		if (input.language == null) {
			cfg.language = ModLanguage.ENGLISH;
			modified = true;
		}

		if (input.effects == null) {
			cfg.effects = new EffectsConfig();
			modified = true;
		}
		if (input.visibility == null) {
			cfg.visibility = new VisibilityConfig();
			modified = true;
		}

		modified |= sanitizeEffects(cfg.effects);

		int clampedGrid = clampInt(cfg.editorGridSize, MIN_GRID_SIZE, MAX_GRID_SIZE);
		if (clampedGrid != cfg.editorGridSize) {
			cfg.editorGridSize = clampedGrid;
			modified = true;
		}

		return new ValidationResult(cfg, modified);
	}

	public static Optional<String> normalizeEffectId(String rawId) {
		if (rawId == null) {
			return Optional.empty();
		}
		String trimmed = rawId.trim().toLowerCase(Locale.ROOT);
		if (trimmed.isEmpty()) {
			return Optional.empty();
		}
		if (!trimmed.contains(":")) {
			if (PATH_ONLY_PATTERN.matcher(trimmed).matches()) {
				return Optional.of("minecraft:" + trimmed);
			}
			return Optional.empty();
		}
		if (NAMESPACED_ID_PATTERN.matcher(trimmed).matches()) {
			return Optional.of(trimmed);
		}
		return Optional.empty();
	}


	private static boolean sanitizeEffects(EffectsConfig effects) {
		boolean modified = false;
		if (effects.anchor == null) {
			effects.anchor = HudAnchor.CENTER_RIGHT;
			modified = true;
		}
		if (effects.orientation == null) {
			effects.orientation = HudOrientation.VERTICAL;
			modified = true;
		}
		if (effects.style == null) {
			effects.style = EffectsVisualStyle.MINECRAFT_COMPACT;
			modified = true;
		}
		if (effects.potionHudMode == null) {
			effects.potionHudMode = PotionHudMode.NORMAL;
			modified = true;
		}
		if (effects.potionMiniDisplay == null) {
			effects.potionMiniDisplay = PotionMiniDisplayMode.TIMER_TEXT;
			modified = true;
		}
		if (effects.levelFormat == null) {
			effects.levelFormat = LevelFormat.ROMAN;
			modified = true;
		}
		if (effects.sortMode == null) {
			effects.sortMode = EffectSortMode.CATEGORY_THEN_NAME;
			modified = true;
		}
		if (effects.vanillaEffectsMode == null) {
			effects.vanillaEffectsMode = VanillaEffectsMode.SHOW_BOTH;
			modified = true;
		}

		float clampedScale = clampScale(effects.scale);
		if (Float.compare(clampedScale, effects.scale) != 0) {
			effects.scale = clampedScale;
			modified = true;
		}

		int clampedSpacing = clampInt(effects.rowSpacing, MIN_SPACING, MAX_SPACING);
		if (clampedSpacing != effects.rowSpacing) {
			effects.rowSpacing = clampedSpacing;
			modified = true;
		}

		int clampedOpacity = clampInt(effects.backgroundOpacityPercent, MIN_OPACITY_PERCENT, MAX_OPACITY_PERCENT);
		if (clampedOpacity != effects.backgroundOpacityPercent) {
			effects.backgroundOpacityPercent = clampedOpacity;
			modified = true;
		}

		int clampedOffsetX = clampInt(effects.offsetX, MIN_OFFSET, MAX_OFFSET);
		int clampedOffsetY = clampInt(effects.offsetY, MIN_OFFSET, MAX_OFFSET);
		if (clampedOffsetX != effects.offsetX || clampedOffsetY != effects.offsetY) {
			effects.offsetX = clampedOffsetX;
			effects.offsetY = clampedOffsetY;
			modified = true;
		}

		if (effects.customEffectColors == null) {
			effects.customEffectColors = new LinkedHashMap<>();
			modified = true;
		} else {
			Map<String, EffectColorOverride> cleaned = new LinkedHashMap<>();
			for (Map.Entry<String, EffectColorOverride> entry : effects.customEffectColors.entrySet()) {
				Optional<String> normId = normalizeEffectId(entry.getKey());
				EffectColorOverride override = entry.getValue();
				if (normId.isEmpty() || override == null) {
					modified = true;
					continue;
				}
				Optional<String> normHex = ColorHexUtils.normalizeHex(override.colorHex);
				if (normHex.isEmpty()) {
					modified = true;
					continue;
				}
				if (!normId.get().equals(entry.getKey()) || !normHex.get().equals(override.colorHex)) {
					modified = true;
				}
				cleaned.put(normId.get(), new EffectColorOverride(override.enabled, normHex.get()));
			}
			effects.customEffectColors = cleaned;
		}

		return modified;
	}

	public static float clampScale(float value) {
		if (!Float.isFinite(value) || value <= 0.0f) {
			return 1.0f;
		}
		float clamped = Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
		return Math.round(clamped * 100.0f) / 100.0f;
	}


	public static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}

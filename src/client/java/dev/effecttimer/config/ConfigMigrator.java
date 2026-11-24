package dev.effecttimer.config;

import com.google.gson.JsonObject;

/**
 * Migraciones JSON de una sola vez de Potion Effect.
 *
 * <p>Regla principal: si existe un config plano heredado
 * de v1.0.0 (claves {@code enabled, posX, posY, scale, orientation, showIcons,
 * showDuration, showName, blinkOnExpire, textColor} sin {@code configVersion}),
 * se traslada lo equivalente al nuevo esquema anidado {@code effects} y se
 * descartan las claves sin equivalente ({@code textColor}: la v1.1 usa colores
 * automáticos + overrides por efecto; {@code posX/posY} se conservan como
 * anclaje {@code CUSTOM} con esos desplazamientos):</p>
 * <ul>
 *   <li>{@code blinkOnExpire → fadeOnLowDuration}</li>
 *   <li>{@code showIcons → showIcon}</li>
 *   <li>{@code posX/posY → anchor=CUSTOM + offsetX/offsetY}</li>
 * </ul>
 *
 * <p>Sin {@code configVersion} se detecta un config heredado; la migración modifica el target
 * y el flag {@code migrated} fuerza el re-guardado.</p>
 */
public final class ConfigMigrator {
	public static final int LEGACY_STANDALONE_VERSION = 1;

	private ConfigMigrator() {
	}

	/**
	 * Migración v2 a v3: añade {@code customColorsEnabled=true} y
	 * {@code textShadow=true} sin tocar nada más. Gson ya deja el default de
	 * campo cuando la clave falta; este paso lo hace explícito y verificable.
	 */
	static boolean migrateToV3(HudConfig target) {
		if (target == null || target.effects == null) {
			return false;
		}
		boolean changed = false;
		if (!target.effects.customColorsEnabled) {
			// Gson ya deja el default cuando falta el campo; esto lo hace explícito.
			changed = true;
		}
		target.effects.customColorsEnabled = true;
		target.effects.textShadow = true;
		return changed;
	}

	/** @return true si el target cambió y conviene re-guardar el archivo. */
	public static boolean migrate(JsonObject rawRoot, HudConfig target) {
		if (rawRoot == null || target == null) {
			return false;
		}
		boolean migrated = false;
		int sourceVersion = LEGACY_STANDALONE_VERSION;
		boolean hasVersionField = rawRoot.has("configVersion") && rawRoot.get("configVersion").isJsonPrimitive();
		if (hasVersionField) {
			try {
				sourceVersion = rawRoot.get("configVersion").getAsInt();
			} catch (RuntimeException ignored) {
				sourceVersion = LEGACY_STANDALONE_VERSION;
			}
		} else {
			migrated = true; // archivo sin versionar → se normaliza
		}

		if (!hasVersionField && looksLikeLegacyStandalone(rawRoot)) {
			migrated |= migrateLegacyStandalone(rawRoot, target);
		}

		if (sourceVersion < HudConfig.CURRENT_CONFIG_VERSION || !hasVersionField) {
			migrated |= migrateToV3(target);
			target.configVersion = HudConfig.CURRENT_CONFIG_VERSION;
			migrated = true;
		}

		return migrated;
	}

	private static boolean looksLikeLegacyStandalone(JsonObject rawRoot) {
		for (String key : new String[] {"showIcons", "blinkOnExpire", "textColor", "posX", "posY"}) {
			if (rawRoot.has(key)) {
				return true;
			}
		}
		return false;
	}

	private static boolean migrateLegacyStandalone(JsonObject raw, HudConfig target) {
		EffectsConfig effects = target.effects != null ? target.effects : new EffectsConfig();
		boolean changed = false;

		if (raw.has("enabled") && raw.get("enabled").isJsonPrimitive()) {
			effects.enabled = raw.get("enabled").getAsBoolean();
			changed = true;
		}
		if (raw.has("scale") && raw.get("scale").isJsonPrimitive()) {
			try {
				effects.scale = raw.get("scale").getAsFloat();
				changed = true;
			} catch (RuntimeException ignored) {
				// keep default
			}
		}
		if (raw.has("orientation") && raw.get("orientation").isJsonPrimitive()) {
			try {
				effects.orientation = HudOrientation.valueOf(raw.get("orientation").getAsString().toUpperCase(java.util.Locale.ROOT));
				changed = true;
			} catch (RuntimeException ignored) {
				// keep default
			}
		}
		if (raw.has("showIcons") && raw.get("showIcons").isJsonPrimitive()) {
			effects.showIcon = raw.get("showIcons").getAsBoolean();
			changed = true;
		}
		if (raw.has("showDuration") && raw.get("showDuration").isJsonPrimitive()) {
			effects.showDuration = raw.get("showDuration").getAsBoolean();
			changed = true;
		}
		if (raw.has("showName") && raw.get("showName").isJsonPrimitive()) {
			effects.showName = raw.get("showName").getAsBoolean();
			changed = true;
		}
		if (raw.has("blinkOnExpire") && raw.get("blinkOnExpire").isJsonPrimitive()) {
			effects.fadeOnLowDuration = raw.get("blinkOnExpire").getAsBoolean();
			changed = true;
		}
		boolean moved = false;
		if (raw.has("posX") && raw.get("posX").isJsonPrimitive()) {
			try {
				effects.offsetX = raw.get("posX").getAsInt();
				moved = true;
			} catch (RuntimeException ignored) {
				// keep default
			}
		}
		if (raw.has("posY") && raw.get("posY").isJsonPrimitive()) {
			try {
				effects.offsetY = raw.get("posY").getAsInt();
				moved = true;
			} catch (RuntimeException ignored) {
				// keep default
			}
		}
		if (moved) {
			effects.anchor = HudAnchor.CUSTOM;
			changed = true;
		}
		// "textColor" (global) no tiene equivalente en la v1.1: se descarta
		// deliberadamente; los colores por efecto viven en effects.customEffectColors.

		target.effects = effects;
		return changed;
	}
}

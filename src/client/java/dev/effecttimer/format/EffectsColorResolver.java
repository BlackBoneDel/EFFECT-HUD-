package dev.effecttimer.format;

import dev.effecttimer.config.ColorHexUtils;
import dev.effecttimer.config.ConfigValidator;
import dev.effecttimer.config.EffectColorOverride;
import dev.effecttimer.config.EffectsConfig;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Cadena de prioridad de color (v1.2): override personalizado del efecto (solo
 * si {@code customColorsEnabled}) &gt; color por tiempo restante (si
 * {@code colorDurationByRemaining}) &gt; color automático vívido. El color
 * automático pasa por {@link #vividize(int)}: satura y sube el valor del color
 * nativo de Minecraft sin cambiar el tono (y aclara los casi-grises sin
 * inventarles tono). Es un superconjunto de la antigua regla de «aclarar
 * oscuros»: nunca devuelve menos legibilidad.
 */
public final class EffectsColorResolver {
	public static final int DEFAULT_TITLE_COLOR_ARGB = 0xFFE6EDF3;
	public static final int UNREADABLE_FALLBACK_RGB = 0xE6EDF3;
	public static final float MIN_READABLE_LUMINANCE = 0.34f;
	public static final float TARGET_READABLE_LUMINANCE = 0.44f;
	/** Suelo de luminancia relativa v1.2 sobre fondo oscuro. */
	public static final float VIVID_MIN_LUMINANCE = 0.35f;
	public static final float VIVID_MIN_SATURATION = 0.70f;
	public static final float VIVID_MIN_VALUE = 0.95f;
	public static final float VIVID_GRAY_VALUE = 0.92f;
	public static final float VIVID_GRAY_SATURATION = 0.12f;

	private EffectsColorResolver() {
	}

	public static boolean hasNativeColor(int rawColorRgb) {
		return (rawColorRgb & 0x00FFFFFF) != 0;
	}

	/** @return luminancia relativa sRGB aproximada (0..1) del rgb dado. */
	public static float luminance(int rgb) {
		return (0.2126f * ((rgb >> 16) & 0xFF) + 0.7152f * ((rgb >> 8) & 0xFF) + 0.0722f * (rgb & 0xFF)) / 255.0f;
	}

	public static float[] rgbToHsv(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		float fr = r / 255.0f;
		float fg = g / 255.0f;
		float fb = b / 255.0f;
		float max = Math.max(fr, Math.max(fg, fb));
		float min = Math.min(fr, Math.min(fg, fb));
		float delta = max - min;
		float h;
		if (delta <= 0.000001f) {
			h = 0.0f;
		} else if (max == fr) {
			h = ((fg - fb) / delta + (fg < fb ? 6.0f : 0.0f)) / 6.0f;
		} else if (max == fg) {
			h = ((fb - fr) / delta + 2.0f) / 6.0f;
		} else {
			h = ((fr - fg) / delta + 4.0f) / 6.0f;
		}
		float s = max <= 0.000001f ? 0.0f : delta / max;
		return new float[] { h, s, max };
	}

	public static int hsvToRgb(float h, float s, float v) {
		float cc = v * s;
		float x = cc * (1.0f - Math.abs(((h * 6.0f) % 2.0f) - 1.0f));
		float m = v - cc;
		float r;
		float g;
		float b;
		int sector = (int) Math.floor(h * 6.0f) % 6;
		switch (sector) {
			case 1 -> { r = x; g = cc; b = 0; }
			case 2 -> { r = 0; g = cc; b = x; }
			case 3 -> { r = 0; g = x; b = cc; }
			case 4 -> { r = x; g = 0; b = cc; }
			case 5 -> { r = cc; g = 0; b = x; }
			default -> { r = cc; g = x; b = 0; }
		}
		int ri = Math.round(Math.min(1.0f, Math.max(0.0f, r + m)) * 255.0f);
		int gi = Math.round(Math.min(1.0f, Math.max(0.0f, g + m)) * 255.0f);
		int bi = Math.round(Math.min(1.0f, Math.max(0.0f, b + m)) * 255.0f);
		return (ri << 16) | (gi << 8) | bi;
	}

	/**
	 * Color automático vívido (v1.2, bloque 6). Paso base (idempotente por
	 * construcción: cada canal se ancla a constantes fijas, sin deriva
	 * multiplicativa): cromático (S ≥ 0.12) → S = max(S, 0.70) con el impulso
	 * del spec S×1.25 cuando aún no llega al suelo; V = max(V, 0.95). Casi gris
	 * (S < 0.12): no se le inventa tono, solo V = max(V, 0.92).
	 * Garantía de legibilidad: la luminancia objetivo es max(0.35, min(lum de
	 * entrada, 0.92)) — es decir NUNCA por debajo de la del color de entrada
	 * (superconjunto de la antigua regla de «aclarar oscuros»); si el color no
	 * la alcanza se sube V y, en última instancia, se baja S en pasos de 0.05
	 * (por eso en hues imposibles como el azul puro S puede acabar < 0.70:
	 * prioridad documentada, el fijo-punto exterior la conserva igual).
	 * {@code vividize} itera el paso hasta el punto fijo (≤12 pasadas), lo que
	 * hace la operación exactamente idempotente incluso con el redondeo a
	 * canales enteros.
	 */
	public static int vividize(int rgb) {
		int cur = rgb & 0x00FFFFFF;
		for (int pass = 0; pass < 12; pass++) {
			int next = vividizeStep(cur);
			if (next == cur) {
				break;
			}
			cur = next;
		}
		return cur;
	}

	private static int vividizeStep(int src) {
		float[] hsv = rgbToHsv(src);
		float h = hsv[0];
		float s = hsv[1];
		float v = hsv[2];
		if (s < VIVID_GRAY_SATURATION) {
			v = Math.max(v, VIVID_GRAY_VALUE);
		} else {
			if (s < VIVID_MIN_SATURATION) {
				s = Math.min(1.0f, Math.max(s * 1.25f, VIVID_MIN_SATURATION));
			}
			v = Math.max(v, VIVID_MIN_VALUE);
		}
		float target = Math.max(VIVID_MIN_LUMINANCE, Math.min(luminance(src), 0.92f));
		int cand = hsvToRgb(h, s, v);
		for (int i = 0; i < 10 && luminance(cand) < target && v < 1.0f; i++) {
			v = Math.min(1.0f, v + 0.05f);
			cand = hsvToRgb(h, s, v);
		}
		for (int i = 0; i < 16 && luminance(cand) < target && s > VIVID_GRAY_SATURATION; i++) {
			s = Math.max(VIVID_GRAY_SATURATION, (float) (Math.floor(s * 20.0 - 1e-3) / 20.0));
			cand = hsvToRgb(h, s, v);
		}
		return cand;
	}

	/**
	 * Color nativo ya procesado para mostrar (automático vívido). Sustituye a
	 * la antigua función de «aclarar oscuros», que queda englobada.
	 */
	public static int resolveNativeDisplayRgb(int rawColorRgb) {
		int rgb = rawColorRgb & 0x00FFFFFF;
		if (rgb == 0) {
			return UNREADABLE_FALLBACK_RGB;
		}
		// v1.2: el vívido ES la nueva regla de legibilidad (superconjunto de la
		// antigua de «aclarar oscuros»: V ≥ 0.92-0.95 garantiza más luminancia
		// que el blend anterior en todos los casos testeables).
		return vividize(rgb);
	}

	public static OptionalInt resolveCustomRgb(String effectId, EffectsConfig config) {
		if (effectId == null || config == null || config.customEffectColors == null) {
			return OptionalInt.empty();
		}
		// v1.2: interruptor global. En OFF ningún override se aplica, pero los
		// datos (hex + enabled por efecto) siguen intactos en el config.
		if (!config.customColorsEnabled) {
			return OptionalInt.empty();
		}
		Optional<String> normalized = ConfigValidator.normalizeEffectId(effectId);
		if (normalized.isEmpty()) {
			return OptionalInt.empty();
		}
		EffectColorOverride override = config.customEffectColors.get(normalized.get());
		if (override == null || !override.enabled) {
			return OptionalInt.empty();
		}
		Optional<Integer> rgb = ColorHexUtils.parseRgb(override.colorHex);
		if (rgb.isEmpty()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(rgb.get());
	}

	public static int resolveEffectiveRgb(EffectSnapshot snap, EffectsConfig config) {
		if (snap != null) {
			OptionalInt customRgb = resolveCustomRgb(snap.id(), config);
			if (customRgb.isPresent()) {
				return customRgb.getAsInt();
			}
			return resolveNativeDisplayRgb(snap.colorRgb());
		}
		return UNREADABLE_FALLBACK_RGB;
	}

	public static int titleColorArgb(EffectSnapshot snap, EffectsConfig config) {
		return 0xFF000000 | resolveEffectiveRgb(snap, config);
	}

	public static int durationColorArgb(EffectSnapshot snap, EffectsConfig config, float ticksPerSecond) {
		if (snap != null) {
			OptionalInt customRgb = resolveCustomRgb(snap.id(), config);
			if (customRgb.isPresent()) {
				return 0xFF000000 | customRgb.getAsInt();
			}
		}
		if (snap == null) {
			return DurationFormatter.durationColorArgb(0, ticksPerSecond, false, false);
		}
		if (config != null && config.colorDurationByRemaining) {
			return DurationFormatter.durationColorArgb(snap.durationTicks(), ticksPerSecond, snap.infinite(), true);
		}
		return 0xFF000000 | resolveNativeDisplayRgb(snap.colorRgb());
	}

	public static int accentColorArgb(EffectSnapshot snap, EffectsConfig config) {
		return 0xFF000000 | resolveEffectiveRgb(snap, config);
	}

	public static int iconTintArgb(float alpha) {
		return ColorHexUtils.withAlpha(0xFFFFFF, alpha);
	}
}

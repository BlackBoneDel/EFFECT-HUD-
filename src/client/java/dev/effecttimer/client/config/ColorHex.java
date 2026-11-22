package dev.effecttimer.client.config;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Utilidades de color hexadecimal (#RRGGBB).
 * Versión reducida de {@code ColorHexUtils} del proyecto original: conserva la
 * validación/normalización y una paleta para el selector de color de texto,
 * sin utilidades de otros módulos del mod padre.
 */
public final class ColorHex {
	/** Paleta rápida para el botón de color de texto (heredada de la del mod original). */
	public static final List<String> TEXT_PALETTE = List.of(
			"#F8FAFC",
			"#FF5555",
			"#FFA94D",
			"#FFE066",
			"#6EEB83",
			"#38BDF8",
			"#A78BFA",
			"#E879F9",
			"#FF66B2"
	);

	private ColorHex() {
	}

	/** Normaliza "rrggbb", "#rrggbb" o mayúsculas a "#RRGGBB". Vacío si no es hex válido. */
	public static Optional<String> normalizeHex(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		String trimmed = raw.trim();
		if (trimmed.startsWith("#")) {
			trimmed = trimmed.substring(1);
		}
		if (trimmed.length() != 6) {
			return Optional.empty();
		}
		for (int i = 0; i < 6; i++) {
			char c = trimmed.charAt(i);
			boolean hexDigit = (c >= '0' && c <= '9')
					|| (c >= 'a' && c <= 'f')
					|| (c >= 'A' && c <= 'F');
			if (!hexDigit) {
				return Optional.empty();
			}
		}
		return Optional.of("#" + trimmed.toUpperCase(Locale.ROOT));
	}

	public static String normalizeOrDefault(String raw, String fallbackHex) {
		return normalizeHex(raw).orElseGet(() -> normalizeHex(fallbackHex).orElse("#FFFFFF"));
	}

	/** Devuelve el RGB (0xRRGGBB) o vacío si el texto no es un hex válido. */
	public static OptionalInt parseRgb(String raw) {
		Optional<String> normalized = normalizeHex(raw);
		if (normalized.isEmpty()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(Integer.parseInt(normalized.get().substring(1), 16));
	}

	/** Convierte el hex en ARGB opaco (0xFFRRGGBB), con fallback. */
	public static int parseOpaqueArgbOrDefault(String raw, int fallbackRgb) {
		int rgb = parseRgb(raw).orElse(fallbackRgb & 0xFFFFFF);
		return 0xFF000000 | rgb;
	}

	/** Devuelve el siguiente color de la paleta tras el actual (cíclico). */
	public static String nextPaletteColor(String currentHex, List<String> palette) {
		if (palette == null || palette.isEmpty()) {
			return normalizeOrDefault(currentHex, "#FFFFFF");
		}
		String current = normalizeOrDefault(currentHex, palette.getFirst());
		for (int i = 0; i < palette.size(); i++) {
			if (palette.get(i).equalsIgnoreCase(current)) {
				return palette.get((i + 1) % palette.size());
			}
		}
		return palette.getFirst();
	}
}

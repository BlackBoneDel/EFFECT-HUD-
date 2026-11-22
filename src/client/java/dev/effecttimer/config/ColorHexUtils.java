package dev.effecttimer.config;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ColorHexUtils {
	public record PaletteColor(String hex, String translationKey) {
	}

	public static final List<PaletteColor> EFFECT_PALETTE = List.of(
			new PaletteColor("#FF66B2", "effecttimer.color.pink"),
			new PaletteColor("#E879F9", "effecttimer.color.magenta"),
			new PaletteColor("#FF5555", "effecttimer.color.red"),
			new PaletteColor("#FFA94D", "effecttimer.color.orange"),
			new PaletteColor("#FFE066", "effecttimer.color.yellow"),
			new PaletteColor("#6EEB83", "effecttimer.color.green"),
			new PaletteColor("#38BDF8", "effecttimer.color.cyan"),
			new PaletteColor("#60A5FA", "effecttimer.color.blue"),
			new PaletteColor("#A78BFA", "effecttimer.color.violet"),
			new PaletteColor("#F8FAFC", "effecttimer.color.white")
	);


	private ColorHexUtils() {
	}

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

	public static boolean isValidHex(String raw) {
		return normalizeHex(raw).isPresent();
	}

	public static String normalizeOrDefault(String raw, String fallbackHex) {
		return normalizeHex(raw).orElseGet(() -> normalizeHex(fallbackHex).orElse("#FFFFFF"));
	}

	public static Optional<Integer> parseRgb(String raw) {
		Optional<String> normalized = normalizeHex(raw);
		if (normalized.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(Integer.parseInt(normalized.get().substring(1), 16));
	}

	public static int parseOpaqueArgbOrDefault(String raw, int fallbackRgb) {
		int rgb = parseRgb(raw).orElse(fallbackRgb & 0xFFFFFF);
		return 0xFF000000 | rgb;
	}

	public static int withAlpha(int rgbOrArgb, float alpha) {
		float clamped = Math.max(0.0f, Math.min(1.0f, alpha));
		int a = Math.round(clamped * 255.0f) & 0xFF;
		return (a << 24) | (rgbOrArgb & 0x00FFFFFF);
	}

	public static String nextPresetColor(String currentHex, List<String> presets) {
		if (presets == null || presets.isEmpty()) {
			return normalizeOrDefault(currentHex, "#FFFFFF");
		}
		String current = normalizeOrDefault(currentHex, presets.getFirst());
		for (int i = 0; i < presets.size(); i++) {
			if (presets.get(i).equalsIgnoreCase(current)) {
				return presets.get((i + 1) % presets.size());
			}
		}
		return presets.getFirst();
	}
}

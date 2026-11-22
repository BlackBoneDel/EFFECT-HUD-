package dev.effecttimer.client.format;

/**
 * Nombres de nivel de efecto en números romanos.
 * Extraído de {@code RomanNumerals} + la regla por defecto {@code hideLevelOne = true}
 * del módulo original: el nivel I se oculta (igual que vanilla), de modo que
 * "Velocidad II" se muestra con sufijo y "Regeneración" sin él.
 */
public final class LevelNames {
	private static final int[] VALUES = {
			1000, 900, 500, 400,
			100, 90, 50, 40,
			10, 9, 5, 4,
			1
	};
	private static final String[] SYMBOLS = {
			"M", "CM", "D", "CD",
			"C", "XC", "L", "XL",
			"X", "IX", "V", "IV",
			"I"
	};

	private LevelNames() {
	}

	/** Entero positivo a numeral romano; fuera de rango lo devuelve en arábigos. */
	public static String roman(int number) {
		if (number <= 0 || number >= 4000) {
			return Integer.toString(number);
		}
		StringBuilder sb = new StringBuilder();
		int remaining = number;
		for (int i = 0; i < VALUES.length && remaining > 0; i++) {
			while (remaining >= VALUES[i]) {
				sb.append(SYMBOLS[i]);
				remaining -= VALUES[i];
			}
		}
		return sb.toString();
	}

	/**
	 * Sufijo legible del nivel para el nombre del efecto (incluye el espacio
	 * separador). Vacío en nivel I, comportamiento idéntico al por defecto del
	 * mod original ({@code levelFormat = ROMAN}, {@code hideLevelOne = true}).
	 */
	public static String suffixFor(int amplifier) {
		int level = amplifier >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, amplifier + 1);
		if (level == 1) {
			return "";
		}
		return " " + roman(level);
	}

	/** Nombre + sufijo de nivel, p. ej. {@code "Velocidad II"}. */
	public static String withLevel(String name, int amplifier) {
		if (name == null || name.isBlank()) {
			return "Unknown";
		}
		return name + suffixFor(amplifier);
	}
}

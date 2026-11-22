package dev.effecttimer.client.format;

import java.util.Locale;

/**
 * Formatea duraciones en ticks como texto "mm:ss" (o "h:mm:ss").
 * Extraído de {@code DurationFormatter} del módulo Effects HUD original, con el
 * relleno a dos dígitos de los minutos (formato estable "01:30") que fija la spec.
 */
public final class DurationFormatter {
	private DurationFormatter() {
	}

	public static String formatTicks(int ticks, float ticksPerSecond, boolean infinite) {
		if (infinite || ticks < 0) {
			return "\u221E"; // "∞"
		}
		float safeTps = Float.isFinite(ticksPerSecond) && ticksPerSecond > 0.1F ? ticksPerSecond : 20.0F;
		long totalSeconds = Math.max(0L, (long) Math.floor(ticks / safeTps));
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		return hours > 0L
				? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
				: String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
	}
}

package dev.effecttimer.format;

import java.util.Locale;

/**
 * Filtro de búsqueda de la pantalla de colores por efecto (query sobre nombre o
 * id, case-insensitive, con recorte; query vacía = todo). Extraído como método
 * puro para poder testearlo sin cliente de Minecraft; la pantalla lo delega.
 */
public final class EffectSearchFilter {
	private EffectSearchFilter() {
	}

	public static boolean matches(String displayName, String id, String rawQuery) {
		String q = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			return true;
		}
		if (displayName != null && displayName.toLowerCase(Locale.ROOT).contains(q)) {
			return true;
		}
		return id != null && id.toLowerCase(Locale.ROOT).contains(q);
	}
}

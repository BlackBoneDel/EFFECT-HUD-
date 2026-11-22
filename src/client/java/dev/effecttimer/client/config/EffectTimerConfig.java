package dev.effecttimer.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Configuración aislada del Effect Timer (JSON/GSON).
 * Guarda y lee ÚNICAMENTE los parámetros del temporizador de efectos; el resto
 * de módulos del mod original (armadura, presets, editor, idioma, etc.) fueron
 * eliminados por completo.
 *
 * <p>La persistencia replica el mecanismo probado de {@code ConfigManager} del
 * proyecto padre: escritura atómica vía fichero temporal y copia de seguridad
 * del JSON corrupto antes de restaurar los valores por defecto.</p>
 */
public final class EffectTimerConfig {
	public static final float MIN_SCALE = 0.50F;
	public static final float MAX_SCALE = 2.50F;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	// --- Parámetros del Effect Timer (única superficie de configuración) ---
	/** Activar/desactivar el HUD de efectos. */
	public boolean enabled = true;
	/** Posición X en pantalla (px lógicos). */
	public int posX = 4;
	/** Posición Y en pantalla (px lógicos). */
	public int posY = 4;
	/** Escala del HUD (0.50× – 2.50×). */
	public float scale = 1.0F;
	/** Renderizado vertical u horizontal. */
	public Orientation orientation = Orientation.VERTICAL;
	/** Mostrar u ocultar el ícono del efecto. */
	public boolean showIcons = true;
	/** Mostrar u ocultar el texto del tiempo (ej: 01:30). */
	public boolean showDuration = true;
	/** Mostrar u ocultar el nombre del efecto (ej: "Velocidad II"). */
	public boolean showName = true;
	/** Color personalizado para el texto (hex #RRGGBB). */
	public String textColor = "#FFFFFF";
	/** Parpadeo cuando quedan menos de 10 segundos (comportamiento heredado del fade original). */
	public boolean blinkOnExpire = true;

	/** ARGB opaco del color de texto, con fallback a blanco. */
	public int textColorArgb() {
		return ColorHex.parseOpaqueArgbOrDefault(textColor, 0xFFFFFF);
	}

	public void normalize() {
		if (!Float.isFinite(scale)) {
			scale = 1.0F;
		}
		scale = Math.round(clamp(scale, MIN_SCALE, MAX_SCALE) * 20.0F) / 20.0F;
		posX = Math.max(0, posX);
		posY = Math.max(0, posY);
		if (orientation == null) {
			orientation = Orientation.VERTICAL;
		}
		textColor = ColorHex.normalizeOrDefault(textColor, "#FFFFFF");
	}

	/**
	 * Carga la configuración desde {@code path}. Si el archivo no existe, no es
	 * un JSON válido o está corrupto, se copia a {@code .corrupt-*.bak} y se
	 * devuelven los valores por defecto (escritos en disco).
	 */
	public static EffectTimerConfig load(Path path) {
		if (path == null) {
			return createDefault();
		}
		try {
			if (!Files.exists(path)) {
				EffectTimerConfig defaults = createDefault();
				defaults.save(path);
				return defaults;
			}
			String raw = Files.readString(path, StandardCharsets.UTF_8);
			if (raw.isBlank()) {
				throw new JsonParseException("Configuration file is empty");
			}
			JsonElement element = JsonParser.parseString(raw);
			if (!element.isJsonObject()) {
				throw new JsonParseException("Configuration root must be a JSON object");
			}
			EffectTimerConfig parsed = GSON.fromJson(element.getAsJsonObject(), EffectTimerConfig.class);
			if (parsed == null) {
				throw new JsonParseException("Parsed configuration was null");
			}
			parsed.normalize();
			return parsed;
		} catch (IOException | RuntimeException ex) {
			logWarning("Failed to read Effect Timer config; restoring defaults", ex);
			backupCorruptFile(path);
			EffectTimerConfig defaults = createDefault();
			defaults.save(path);
			return defaults;
		}
	}

	/** Guarda la configuración de forma atómica. Devuelve true si tuvo éxito. */
	public boolean save(Path path) {
		if (path == null) {
			return false;
		}
		this.normalize();
		Path tempPath = null;
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			String json = GSON.toJson(this) + System.lineSeparator();
			tempPath = path.resolveSibling(path.getFileName() + ".tmp");
			Files.writeString(tempPath, json, StandardCharsets.UTF_8);
			try {
				Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException | RuntimeException ex) {
			logWarning("Failed to save Effect Timer config to " + path, ex);
			return false;
		} finally {
			if (tempPath != null) {
				try {
					Files.deleteIfExists(tempPath);
				} catch (IOException ignored) {
					// Best-effort temp cleanup
				}
			}
		}
	}

	public static EffectTimerConfig createDefault() {
		EffectTimerConfig config = new EffectTimerConfig();
		config.normalize();
		return config;
	}

	private static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}

	private static void backupCorruptFile(Path path) {
		try {
			if (!Files.exists(path)) {
				return;
			}
			String backupName = path.getFileName() + ".corrupt-" + System.currentTimeMillis() + ".bak";
			Files.copy(path, path.resolveSibling(backupName), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			logWarning("Failed to back up corrupt Effect Timer config", ex);
		}
	}

	private static void logWarning(String message, Throwable ex) {
		System.Logger LOGGER = System.getLogger("EffectTimer/Config");
		LOGGER.log(System.Logger.Level.WARNING, message, ex);
	}
}

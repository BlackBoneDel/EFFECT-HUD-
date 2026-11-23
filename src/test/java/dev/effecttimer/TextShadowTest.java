package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.effecttimer.config.ConfigManager;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.PresetCatalog;
import dev.effecttimer.config.PresetId;
import dev.effecttimer.config.TextShadowRule;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Bloque 7 (v1.2): {@code textShadow} (default true) decide el flag de sombra
 * de TODAS las llamadas de texto (nombre, nivel, temporizador —incluido el
 * temporizador de PotionHUD Mini— en HUD real, vista previa y Modo de Prueba).
 * El renderer delega en {@link TextShadowRule}; este test cubre la regla, la
 * persistencia y que ningún drawString del renderer mantenga {@code , true).
 */
public final class TextShadowTest {

	@Test
	public void defaultTrue() {
		assertTrue(new EffectsConfig().textShadow);
		assertTrue(TextShadowRule.shadow(new EffectsConfig()));
		assertFalse(TextShadowRule.shadow(null), "config nula => sin sombra (defensivo)");
	}

	@Test
	public void ruleIsTheFlagSource() {
		assertTrue(TextShadowRule.shadow(true));
		assertFalse(TextShadowRule.shadow(false));
	}

	@Test
	public void rendererPassesTheFlagToEveryTextCall() throws Exception {
		Path renderer = Path.of("src/client/java/dev/effecttimer/client/hud/EffectsHudRenderer.java");
		if (!Files.exists(renderer)) {
			renderer = Path.of("..", "src/client/java/dev/effecttimer/client/hud/EffectsHudRenderer.java");
		}
		String src = Files.readString(renderer);
		for (String line : src.split("\n")) {
			if (line.contains("graphics.drawString(") && !line.contains("graphics.drawString(this.font")) {
				assertTrue(line.contains(", shadow)"), "drawString sin flag configurable: " + line.trim());
			}
		}
		assertFalse(src.contains(", true);"), "no debe quedar ningún literal de sombra en drawRow");
	}

	@Test
	public void roundTripKeepsShadowFlag(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		HudConfig cfg = mgr.getConfig().copy();
		cfg.effects.textShadow = false;
		assertTrue(mgr.applyAndSaveNow(cfg));
		String raw = Files.readString(file);
		assertTrue(raw.contains("\"textShadow\": false"), "el flag debe serializarse");
		ConfigManager again = new ConfigManager(file);
		again.load();
		assertFalse(again.getConfig().effects.textShadow);
	}

	@Test
	public void presetsAndResetDoNotBreakDefaults() {
		// Reset = new EffectsConfig() -> sombra otra vez en true.
		assertTrue(new EffectsConfig().textShadow);
		HudConfig base = HudConfig.createDefault();
		base.effects.textShadow = false;
		PresetCatalog.applyPreset(base, PresetId.VANILLA_COMPACT);
		assertFalse(base.effects.textShadow, "los preajustes NO deben modificar textShadow");
	}
}

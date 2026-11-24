package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.effecttimer.config.ConfigManager;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.EffectsFramePolicy;
import dev.effecttimer.config.EffectsVisualStyle;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.PresetCatalog;
import dev.effecttimer.config.PresetId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * «sin fondo = sin marco». El marco (outline de fila/tarjeta y
 * bordes de fichas PotionHUD normal/mini) solo se dibuja con ambos flags ON; el
 * valor {@code showEffectBorder} se conserva guardado; el acento de CLEAN_CARD
 * es decorativo y se mantiene siempre.
 */
public final class BorderRequiresBackgroundTest {

	@Test
	public void frameMatrixOnlyBothOn() {
		for (EffectsVisualStyle style : EffectsVisualStyle.values()) {
			assertFalse(EffectsFramePolicy.drawFrame(false, false), style + " bg-off border-off");
			assertFalse(EffectsFramePolicy.drawFrame(false, true), style + " bg-off border-on (la regla nueva)");
			assertFalse(EffectsFramePolicy.drawFrame(true, false), style + " bg-on border-off");
			assertTrue(EffectsFramePolicy.drawFrame(true, true), style + " bg-on border-on");
		}
		EffectsConfig cfg = new EffectsConfig();
		cfg.showEffectBackground = false;
		cfg.showEffectBorder = true;
		assertFalse(EffectsFramePolicy.drawFrame(cfg), "defaults → sin marco dibujado");
		cfg.showEffectBackground = true;
		assertTrue(EffectsFramePolicy.drawFrame(cfg), "con fondo el borde reaparece");
	}

	@Test
	public void borderValueSurvivesBackgroundToggling() {
		EffectsConfig cfg = new EffectsConfig();
		cfg.showEffectBorder = true;
		cfg.showEffectBackground = true;
		// apagar el fondo NO debe borrar ni forzar el borde guardado
		cfg.showEffectBackground = false;
		assertFalse(EffectsFramePolicy.drawFrame(cfg));
		assertTrue(cfg.showEffectBorder, "el valor de borde debe conservarse guardado");
		// y volver a encenderlo lo restaura
		cfg.showEffectBackground = true;
		assertTrue(EffectsFramePolicy.drawFrame(cfg));
	}

	@Test
	public void borderControlEnabledOnlyWithBackground() {
		assertTrue(EffectsFramePolicy.borderControlEnabled(true));
		assertFalse(EffectsFramePolicy.borderControlEnabled(false));
	}

	@Test
	public void roundTripKeepsBothFlags(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		HudConfig cfg = mgr.getConfig().copy();
		cfg.effects.showEffectBackground = false;
		cfg.effects.showEffectBorder = true;
		assertTrue(mgr.applyAndSaveNow(cfg));
		String raw = Files.readString(file);
		assertTrue(raw.contains("\"showEffectBorder\": true"), "el valor debe seguir serializado");
		ConfigManager again = new ConfigManager(file);
		again.load();
		assertFalse(again.getConfig().effects.showEffectBackground);
		assertTrue(again.getConfig().effects.showEffectBorder);
	}

	@Test
	public void rendererUsesFramePolicyForAllStyles() throws Exception {
		// La capa de dibujo delega en la política (compacto, tarjeta, minimal,
		// potion HUD normal y mini comparten drawRow/drawPotionHudRow).
		Path renderer = Path.of("src/client/java/dev/effecttimer/client/hud/EffectsHudRenderer.java");
		if (!Files.exists(renderer)) {
			renderer = Path.of("..", "src/client/java/dev/effecttimer/client/hud/EffectsHudRenderer.java");
		}
		String src = Files.readString(renderer);
		assertTrue(src.contains("EffectsFramePolicy.drawFrame(config)"),
				"drawRow debe leer el borde a través de la política");
		assertFalse(src.contains("boolean drawBorder = config.showEffectBorder;"),
				"no debe quedar el antiguo borde independiente");
	}

	@Test
	public void screenGatesBorderControlOnBackground() throws Exception {
		// El control de «Borde de marco» debe quedar inactivo + tooltip
		// «Requiere activar el fondo…» cuando el fondo esta apagado.
		Path screen = Path.of("src/client/java/dev/effecttimer/client/screen/EffectsConfigScreen.java");
		if (!Files.exists(screen)) {
			screen = Path.of("..", "src/client/java/dev/effecttimer/client/screen/EffectsConfigScreen.java");
		}
		String src = Files.readString(screen);
		assertTrue(src.contains("borderBtn.active = EffectsFramePolicy.borderControlEnabled(effects.showEffectBackground);"),
				"el boton de borde debe deshabilitarse segun la politica");
		assertTrue(src.contains("borderBtn.setTooltip(Tooltip.create(t(\"effecttimer.tooltip.effects.border_requires_background\")));"),
				"y ganar el tooltip especifico cuando esta gris");
	}

	@Test
	public void presetsRemainConsistentWithRule() {
		// "Vanilla compacto" = defaults (bg off, border on): con la regla nueva
		// su marco deja de dibujarse (cambio de aspecto REPORTADO, no error).
		HudConfig vanilla = PresetCatalog.createPreset(PresetId.VANILLA_COMPACT);
		assertFalse(EffectsFramePolicy.drawFrame(vanilla.effects));
		// "Lectura ampliada" (bg off + border on) → mismo caso.
		HudConfig expanded = PresetCatalog.createPreset(PresetId.EXPANDED_READOUT);
		assertFalse(EffectsFramePolicy.drawFrame(expanded.effects), "reportar en la entrega");
		// "Texto compacto" nunca pidió borde → sin cambio.
		HudConfig compactText = PresetCatalog.createPreset(PresetId.COMPACT_TEXT);
		assertFalse(compactText.effects.showEffectBorder);
	}
}

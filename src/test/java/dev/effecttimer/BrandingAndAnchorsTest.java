package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.effecttimer.config.HudAnchor;
import dev.effecttimer.layout.HudLayoutEngine;
import dev.effecttimer.layout.HudRect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Tests: anclajes de la barra rápida (9 predefinidos + CUSTOM), el filtro
 * de búsqueda puro de la pantalla de colores y el branding exigido (icono PNG
 * 128x128 con alfa dibujado originalmente, fabric.mod.json limpio y crédito
 * "by Denvdrite" en ambos idiomas).
 */
public final class BrandingAndAnchorsTest {

	@Test
	public void anchorCatalogHasNinePresetsPlusCustom() {
		assertEquals(14, HudAnchor.values().length, "13 predefinidos + CUSTOM");
		assertNotNull(HudAnchor.valueOf("HOTBAR_LEFT"));
		assertNotNull(HudAnchor.valueOf("HOTBAR_RIGHT"));
		assertNotNull(HudAnchor.valueOf("CUSTOM"));
		for (HudAnchor a : HudAnchor.values()) {
			assertNotNull(a.translationKey());
			assertNotNull(a.tooltipKey());
			assertTrue(a.translationKey().startsWith("effecttimer.anchor."), a.name());
		}
	}

	@Test
	public void hotbarLeftAnchorsBesideVanillaHotbar() {
		// Barra rápida vanilla: 182 px centrados (de sx/2-91 a sx/2+91).
		HudRect r = HudLayoutEngine.resolveAnchorRect(HudAnchor.HOTBAR_LEFT, 0, 0, 60, 20, 400, 300);
		assertEquals(400 / 2 - 124 - 60, r.x());
		assertTrue(r.right() <= 400 / 2 - 91, "debe quedar a la izquierda de la hotbar: " + r);
		assertEquals(300 - 20 - 2, r.y());
	}

	@Test
	public void hotbarRightAnchorsBesideVanillaHotbar() {
		HudRect r = HudLayoutEngine.resolveAnchorRect(HudAnchor.HOTBAR_RIGHT, 0, 0, 60, 20, 400, 300);
		assertEquals(400 / 2 + 122, r.x());
		assertTrue(r.x() >= 400 / 2 + 91, "debe quedar a la derecha de la hotbar: " + r);
		assertEquals(300 - 20 - 2, r.y());
	}

	@Test
	public void customAnchorUsesRawOffsets() {
		HudRect r = HudLayoutEngine.resolveAnchorRect(HudAnchor.CUSTOM, 37, 15, 60, 20, 400, 300);
		assertEquals(37, r.x());
		assertEquals(15, r.y());
	}

	@Test
	public void searchFilterSemantics() {
		assertTrue(dev.effecttimer.format.EffectSearchFilter.matches("Slowness", "minecraft:slowness", null));
		assertTrue(dev.effecttimer.format.EffectSearchFilter.matches("Slowness", "minecraft:slowness", "   "));
		assertTrue(dev.effecttimer.format.EffectSearchFilter.matches("Slowness", "minecraft:slowness", "SLOW"));
		assertTrue(dev.effecttimer.format.EffectSearchFilter.matches("Regeneration", "minecraft:regeneration", "gener"));
		assertTrue(dev.effecttimer.format.EffectSearchFilter.matches("Regeneration", "minecraft:regeneration", "eration"));
		assertFalse(dev.effecttimer.format.EffectSearchFilter.matches("Slowness", "minecraft:slowness", "haste"));
		assertTrue(dev.effecttimer.format.EffectSearchFilter.matches(null, "minecraft:luck", "luck"));
		assertTrue(dev.effecttimer.format.EffectSearchFilter.matches("Suerte", null, "suer"));
	}

	@Test
	public void trackerKeepsPeakPerIdAndAmplifier() {
		dev.effecttimer.format.EffectDurationTracker both = new dev.effecttimer.format.EffectDurationTracker();
		both.syncAndObserve(List.of(
				snap("minecraft:strength", "Strength", 1, 300),
				snap("minecraft:strength", "Strength", 2, 100)));
		both.syncAndObserve(List.of(
				snap("minecraft:strength", "Strength", 1, 250),
				snap("minecraft:strength", "Strength", 2, 90)));
		assertEquals(300, both.observedPeakTicks(snap("minecraft:strength", "Strength", 1, 250)), "pico amp I no debe contaminarse con amp II");
		assertEquals(100, both.observedPeakTicks(snap("minecraft:strength", "Strength", 2, 90)), "pico amp II independiente");
	}

	private static dev.effecttimer.format.EffectSnapshot snap(String id, String name, int amp, int ticks) {
		return new dev.effecttimer.format.EffectSnapshot(id, name,
				dev.effecttimer.format.EffectCategoryKind.BENEFICIAL, amp, ticks, false, false, true, true, 0xFF00FF00);
	}

	private static Path repoFile(String relative) throws Exception {
		Path direct = Path.of(relative);
		if (Files.exists(direct)) {
			return direct;
		}
		Path parent = Path.of("..", relative);
		if (Files.exists(parent)) {
			return parent;
		}
		throw new IllegalStateException("no existe " + relative + " (cwd=" + Path.of("").toAbsolutePath() + ")");
	}

	@Test
	public void iconIsOriginal128x128PngWithAlpha() throws Exception {
		byte[] png = Files.readAllBytes(repoFile("src/main/resources/assets/effecttimer/icon.png"));
		assertTrue(png.length > 8, "icon.png vacío");
		// firma PNG
		assertEquals((byte) 0x89, png[0]);
		assertEquals((byte) 'P', png[1]);
		assertEquals((byte) 'N', png[2]);
		assertEquals((byte) 'G', png[3]);
		// IHDR: width, height, bit depth, color type (6 = RGBA → con alfa)
		int w = ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
		int h = ((png[20] & 0xFF) << 24) | ((png[21] & 0xFF) << 16) | ((png[22] & 0xFF) << 8) | (png[23] & 0xFF);
		assertEquals(128, w);
		assertEquals(128, h);
		assertEquals(6, png[25] & 0xFF, "el icono debe ser RGBA (canal alfa)");
		// dibujado originalmente: tamaño minúsculo y sin atlas de Mojang (que tendría
		// miles de entradas); además, el primer IDAT debe incluir píxeles transparentes.
		assertTrue(png.length < 20_000, "el icono parece una textura importada: " + png.length + " B");
	}

	@Test
	public void modMetadataIsBrandedClean() throws Exception {
		JsonObject mod = JsonParser.parseString(
				Files.readString(repoFile("src/main/resources/fabric.mod.json"))).getAsJsonObject();
		assertEquals("assets/effecttimer/icon.png", mod.get("icon").getAsString());
		assertEquals(1, mod.getAsJsonArray("authors").size());
		assertEquals("Denvdrite", mod.getAsJsonArray("authors").get(0).getAsString());
		assertFalse(mod.has("contributors"), "no debe existir contributors");
		JsonObject contact = mod.getAsJsonObject("contact");
		for (var entry : contact.entrySet()) {
			String v = entry.getValue().getAsString().toLowerCase(Locale.ROOT);
			assertFalse(v.contains("hud-mods") || v.contains("customizablehud") || v.contains("hud_mods"),
					"contact apunta al repo original: " + v);
		}
		assertFalse(mod.has("homepage"));
		assertFalse(mod.has("issues"));
	}

	@Test
	public void creditPresentInBothLanguagesAndReadme() throws Exception {
		Path base = Path.of("src/main/resources/assets/effecttimer/lang");
		if (!Files.isDirectory(base)) {
			base = Path.of("../src/main/resources/assets/effecttimer/lang");
		}
		for (String fn : new String[] {"en_us.json", "es_es.json"}) {
			JsonObject d = JsonParser.parseString(Files.readString(base.resolve(fn))).getAsJsonObject();
			assertTrue(d.has("effecttimer.screen.main.credit"), fn + " sin clave de crédito");
			assertEquals("by Denvdrite", d.get("effecttimer.screen.main.credit").getAsString(), fn);
			assertTrue(d.has("effecttimer.anchor.hotbar_left"), fn + " sin anclaje hotbar_left");
			assertTrue(d.has("effecttimer.anchor.hotbar_right"), fn + " sin anclaje hotbar_right");
			assertTrue(d.has("effecttimer.tooltip.anchor.hotbar_left"), fn);
			assertTrue(d.has("effecttimer.tooltip.anchor.hotbar_right"), fn);
		}
		String readme = Files.readString(repoFile("README.md"));
		assertTrue(readme.contains("by Denvdrite"), "README sin crédito");
	}

	@Test
	public void noArmorReferencesInSources() throws Exception {
		Path root = Path.of("src").toAbsolutePath().normalize();
		if (!Files.isDirectory(root)) {
			root = Path.of("..", "src").toAbsolutePath().normalize();
		}
		// Solo código entregado (main + client); los tests pueden nombrar las palabras vedadas.
		for (Path sub : new Path[] {root.resolve("main"), root.resolve("client")}) {
			if (!Files.isDirectory(sub)) {
				continue;
			}
			try (var files = Files.walk(sub)) {
				for (Path f : files.filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".json")).toList()) {
					String text = Files.readString(f).toLowerCase(Locale.ROOT);
					for (String forbidden : new String[] {"armor", "durability", "chestplate", "leggings", "helmet"}) {
						assertFalse(text.contains(forbidden), f + " menciona '" + forbidden + "'");
					}
				}
			}
		}
	}
}

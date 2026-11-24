package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * en "Configurar efectos…" el selector «HUD vanilla» es el
 * PRIMER control de la lista, dentro de una nueva sección «Visibilidad» (junto
 * al toggle global), y queda por encima de «Estilo». El test verifica el orden
 * real de construcción de la pantalla (fuente de init(), única lista de
 * entradas del port) — el orden de widgets en runtime deriva de ella.
 */
public final class OptionsOrderTest {

	private static String read(String rel) throws Exception {
		Path p = Path.of(rel);
		if (!Files.exists(p)) {
			p = Path.of("..", rel);
		}
		return Files.readString(p);
	}

	@Test
	public void vanillaHudSelectorIsFirstControlOfEffectsScreen() throws Exception {
		String src = read("src/client/java/dev/effecttimer/client/screen/EffectsConfigScreen.java");
		int visibility = src.indexOf("effecttimer.section.visibility");
		int vanilla = src.indexOf("effecttimer.effects.vanilla_mode");
		int enabled = src.indexOf("effecttimer.option.enabled");
		int general = src.indexOf("effecttimer.section.general");
		int style = src.indexOf("effecttimer.option.style");
		assertTrue(visibility >= 0 && vanilla >= 0, "falta la sección Visibilidad");
		assertTrue(visibility < vanilla, "el selector vanilla debe abrir la sección");
		assertTrue(vanilla < enabled, "«HUD vanilla» es el PRIMER control (antes que el toggle)");
		assertTrue(enabled < general && general < style, "el resto de opciones queda debajo");
		// y sigue existiendo el dropdown completo de anclajes + el grid rápido
		assertTrue(src.contains("effecttimer.option.anchor"));
		assertTrue(src.contains("AnchorQuickPicker.build"));
	}

	@Test
	public void quickGridHasSixButtonsAndEditorSharesIt() throws Exception {
		int count = 0;
		while (dev.effecttimer.client.screen.AnchorQuickPicker.anchorAt(count) != null) {
			count++;
		}
		assertEquals(6, count, "grilla rapida de 6 botones (3x2)");
		String editor = read("src/client/java/dev/effecttimer/client/screen/HudEditorScreen.java");
		assertTrue(editor.contains("AnchorQuickPicker.buildRow"),
				"el editor visual (H) debe exponer la misma coleccion de 6 (barra)");
	}

	@Test
	public void borderControlFollowsBackground() throws Exception {
		String src = read("src/client/java/dev/effecttimer/client/screen/EffectsConfigScreen.java");
		assertTrue(src.contains("EffectsFramePolicy.borderControlEnabled"),
				"el control de borde debe leer la política");
	}
}

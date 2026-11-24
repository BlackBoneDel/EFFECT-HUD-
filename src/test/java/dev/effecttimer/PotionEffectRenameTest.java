package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * el nombre visible pasa a ser "Potion Effect" en TODOS los
 * sitios visibles, mientras el id de mod, el paquete y el archivo de config
 * siguen siendo "effecttimer" (decisión de compatibilidad).
 */
public final class PotionEffectRenameTest {

	private static Path repo(String relative) throws Exception {
		Path direct = Path.of(relative);
		if (Files.exists(direct)) {
			return direct;
		}
		Path parent = Path.of("..", relative);
		assertTrue(Files.exists(parent), "no existe " + relative);
		return parent;
	}

	@Test
	public void fabricMetadataRenamed() throws Exception {
		JsonObject mod = JsonParser.parseString(
				Files.readString(repo("src/main/resources/fabric.mod.json"))).getAsJsonObject();
		assertEquals("Potion Effect", mod.get("name").getAsString());
		assertEquals("effecttimer", mod.get("id").getAsString());
		assertEquals("Denvdrite", mod.getAsJsonArray("authors").get(0).getAsString());
		assertFalse(mod.get("description").getAsString().contains("Effect Timer"));
	}

	@Test
	public void noVisibleEffectTimerStringInLang() throws Exception {
		for (String fn : new String[] {"en_us.json", "es_es.json"}) {
			JsonObject d = JsonParser.parseString(Files.readString(
					repo("src/main/resources/assets/effecttimer/lang/" + fn))).getAsJsonObject();
			for (String k : d.keySet()) {
				assertFalse(d.get(k).getAsString().contains("Effect Timer"), fn + ":" + k + " aún dice «Effect Timer»");
			}
			assertEquals("Potion Effect", d.get("effecttimer.section.effects").getAsString(), fn);
			assertEquals("Potion Effect", d.get("key.category.effecttimer.general").getAsString(), fn);
		}
	}

	@Test
	public void readmeRenamedAndCredited() throws Exception {
		String readme = Files.readString(repo("README.md"));
		assertTrue(readme.contains("# Potion Effect — by Denvdrite"), "falta encabezado renombrado");
		assertTrue(readme.contains("tools/generate_icon.py"), "falta documentación del script del icono");
	}

	@Test
	public void jarNameIsPotionEffect() throws Exception {
		String props = Files.readString(repo("gradle.properties"));
		assertTrue(props.contains("archives_base_name=potion-effect"), "archivesName no renombrado");
		String wf = Files.readString(repo(".github/workflows/release.yml"));
		assertTrue(wf.contains("'potion-effect-*.jar'"), "workflow sin patrón de jar nuevo");
		assertTrue(wf.contains("Potion Effect ${{ steps.vars.outputs.tag }}"), "workflow sin nombre nuevo");
	}
}

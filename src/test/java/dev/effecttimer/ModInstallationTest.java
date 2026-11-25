package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.junit.jupiter.api.Test;

/**
 * el mod tiene que quedar INSTALADO por Fabric Loader, que es lo que pasa
 * cuando el jugador deja el jar en la carpeta «mods». En los tests el loader
 * arranca solo (fabric-loader-junit, en modo de desarrollo), así que aquí se
 * comprueba que reconoce el mod, que el manifiesto está completo y que el
 * punto de entrada existe en el classpath.
 *
 * Si el manifiesto declarara una dependencia que no está, o una versión sin
 * expandir, el loader no instalaría el mod y este test fallaría.
 */
public final class ModInstallationTest {

	private static final String MOD_ID = "effecttimer";
	private static final String CLIENT_ENTRYPOINT = "dev.effecttimer.client.EffectTimerMod";

	@Test
	public void fabricLoaderInstallsTheMod() {
		FabricLoader loader = FabricLoader.getInstance();
		assertNotNull(loader, "Fabric Loader no está arrancado: falta fabric-loader-junit");

		ModContainer container = container();
		ModMetadata metadata = container.getMetadata();
		assertEquals(MOD_ID, metadata.getId());
		assertEquals("Potion Effect", metadata.getName());
		assertEquals(ModEnvironment.CLIENT, metadata.getEnvironment());
		assertEquals(EnvType.CLIENT, loader.getEnvironmentType(),
				"los tests arrancan el loader en modo cliente");
	}

	@Test
	public void manifestVersionIsExpanded() {
		String version = container().getMetadata().getVersion().getFriendlyString();
		assertFalse(version.contains("${"), "el «version» del manifiesto no está expandido: " + version);
		assertFalse(version.isBlank(), "el «version» del manifiesto está vacío");
	}

	@Test
	public void clientEntrypointIsOnTheClasspath() throws Exception {
		// Se carga SIN inicializar: la inicialización toca la API de
		// Minecraft, que aquí no está arrancada.
		Class<?> entrypoint = Class.forName(CLIENT_ENTRYPOINT, false, getClass().getClassLoader());
		assertEquals(CLIENT_ENTRYPOINT, entrypoint.getName());
		assertEquals("dev.effecttimer.client", entrypoint.getPackageName());
		assertTrue(ClientModInitializer.class.isAssignableFrom(entrypoint),
				"el entrypoint cliente no implementa ClientModInitializer");
	}

	@Test
	public void iconShipsWithTheMod() {
		Optional<Path> icon = container().findPath("assets/effecttimer/icon.png");
		assertTrue(icon.isPresent(), "falta el icono del mod en el classpath");
	}

	private static ModContainer container() {
		FabricLoader loader = FabricLoader.getInstance();
		assertNotNull(loader, "Fabric Loader no está arrancado: falta fabric-loader-junit");
		ModContainer container = loader.getModContainer(MOD_ID)
				.orElseThrow(() -> new AssertionError("Fabric Loader no conoce el mod «" + MOD_ID + "»"));
		return container;
	}
}

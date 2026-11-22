package dev.effecttimer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.effecttimer.client.config.EffectTimerConfig;
import dev.effecttimer.client.hud.EffectTimerHudRenderer;
import dev.effecttimer.client.screen.EffectTimerConfigScreen;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Effect Timer — extracción independiente (standalone) del módulo "Effect Timer /
 * Effects HUD" de Hud Utils (github.com/BlackBoneDel/HUD-MODS).
 *
 * <p>Mod puramente client-side: muestra los temporizadores de pociones/efectos
 * con posición, escala, orientación, color e íconos configurables, y un modo de
 * prueba en vivo en la pantalla de ajustes. No hay mixins: el renderizado se
 * engancha al pipeline oficial de HUD de Fabric tras
 * {@code VanillaHudElements.STATUS_EFFECTS}, igual que el mod padre.</p>
 */
public final class EffectTimerMod implements ClientModInitializer {
	public static final String MOD_ID = "effecttimer";

	private static final System.Logger LOGGER = System.getLogger("EffectTimer");
	private static final long AUTO_SAVE_DEBOUNCE_MS = 450L;

	private static EffectTimerConfig config;
	private static Path configPath;
	private static boolean dirty;
	private static long saveDeadlineMs;

	private static KeyMapping openConfigKey;

	/** Acceso perezoso y sincronizado a la configuración (patrón del ConfigManager original, simplificado). */
	public static EffectTimerConfig getConfig() {
		if (config == null) {
			synchronized (EffectTimerMod.class) {
				if (config == null) {
					configPath = FabricLoader.getInstance().getConfigDir().resolve("effecttimer.json");
					config = EffectTimerConfig.load(configPath);
				}
			}
		}
		return config;
	}

	/** Marca cambios pendientes; el autoguardado con debounce los persiste sin escribir por cada drag. */
	public static synchronized void markDirty() {
		dirty = true;
		saveDeadlineMs = System.currentTimeMillis() + AUTO_SAVE_DEBOUNCE_MS;
	}

	public static synchronized void saveNow() {
		if (config == null || configPath == null) {
			return;
		}
		if (config.save(configPath)) {
			dirty = false;
		} else {
			LOGGER.log(System.Logger.Level.ERROR, "Failed to save Effect Timer config; retry scheduled");
			dirty = true;
			saveDeadlineMs = System.currentTimeMillis() + 5000L;
		}
	}

	@Override
	public void onInitializeClient() {
		getConfig();

		KeyMapping.Category keyCategory = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "general")
		);
		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.effecttimer.open_config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				keyCategory
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveNow());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> saveNow());

		// Enganche oficial del HUD (misma estrategia que el mod original, sin mixins):
		// nuestra capa va justo tras el indicador vanilla de efectos de estado.
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.STATUS_EFFECTS,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
				EffectTimerHudRenderer::renderLive
		);
	}

	private void onEndClientTick(Minecraft client) {
		if (dirty && System.currentTimeMillis() >= saveDeadlineMs) {
			saveNow();
		}

		// Nunca abrir pantallas ni consumir clics de hotkey con una GUI abierta.
		if (client.screen != null) {
			while (openConfigKey.consumeClick()) {
				// drained
			}
			return;
		}

		while (openConfigKey.consumeClick()) {
			client.setScreen(new EffectTimerConfigScreen(null));
			return;
		}
	}
}

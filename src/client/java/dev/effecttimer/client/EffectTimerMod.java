package dev.effecttimer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.effecttimer.client.hud.EffectsHudRenderer;
import dev.effecttimer.client.hud.LiveDataCollector;
import dev.effecttimer.client.hud.VisibilityRules;
import dev.effecttimer.client.i18n.ModI18n;
import dev.effecttimer.client.screen.HudEditorScreen;
import dev.effecttimer.client.screen.MainConfigScreen;
import dev.effecttimer.config.ConfigManager;
import dev.effecttimer.config.HudConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

/**
 * Punto de entrada de Potion Effect.
 * Comportamiento:
 * <ul>
 *   <li>O abre la pantalla principal de configuración;</li>
 *   <li>H abre el editor visual (HUD editor);</li>
 *   <li>una tercera ligación SIN tecla asigna el toggle de activación/desactivación,
 *       que guarda el config al instante y lo anuncia en la action bar;</li>
 *   <li>los tres viven en su propia categoría de teclas "Potion Effect";</li>
 *   <li>el HUD propio se engancha con la API oficial {@code HudElementRegistry}
 *       (sin mixins) y sustituye al HUD vanilla de efectos según el modo
 *       {@code vanillaEffectsMode}.</li>
 * </ul>
 */
public final class EffectTimerMod implements ClientModInitializer {
	public static final String MOD_ID = "effecttimer";

	private static ConfigManager configManager;

	private static KeyMapping openConfigKey;
	private static KeyMapping openEditorKey;
	private static KeyMapping toggleEffectsKey;

	public static synchronized ConfigManager getConfigManager() {
		if (configManager == null) {
			configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("effecttimer.json"));
			configManager.load();
		}
		return configManager;
	}

	public static HudConfig getConfig() {
		return getConfigManager().getConfig();
	}

	@Override
	public void onInitializeClient() {
		getConfigManager();

		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
				Identifier.fromNamespaceAndPath(MOD_ID, "i18n_reloader"),
				(ResourceManagerReloadListener) ModI18n::reloadFromResources
		);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> getConfigManager().flushPendingSaves());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			LiveDataCollector.clearLiveEffectTracking();
			getConfigManager().flushPendingSaves();
		});

		KeyMapping.Category keyCategory = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "general")
		);

		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.effecttimer.open_config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				keyCategory
		));

		openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.effecttimer.open_editor",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				keyCategory
		));

		toggleEffectsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.effecttimer.toggle_effects",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				keyCategory
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);

		// Ocultar el HUD vanilla de efectos solo cuando el usuario lo pide
		// (vanillaEffectsMode = HIDE_VANILLA). No se usan mixins: la API oficial
		// HudElementRegistry.replaceElement ya permite envolver el elemento.
		HudElementRegistry.replaceElement(
				VanillaHudElements.STATUS_EFFECTS,
				vanilla -> (graphics, tickCounter) -> {
					if (VisibilityRules.shouldRenderVanillaEffects(getConfig())) {
						vanilla.render(graphics, tickCounter);
					}
				}
		);

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.STATUS_EFFECTS,
				Identifier.fromNamespaceAndPath(MOD_ID, "effects_hud"),
				EffectsHudRenderer::renderLive
		);
	}

	private void onEndClientTick(Minecraft client) {
		if (!(client.screen instanceof HudEditorScreen editor && editor.isInteracting())) {
			getConfigManager().tickAutoSave();
		}

		if (client.player != null && getConfigManager().consumeRecoveryNotice()) {
			client.player.displayClientMessage(
					ModI18n.text("effecttimer.message.config_recovered"),
					false
			);
		}

		// Nunca procesar los hotkeys del mod con una GUI/pantalla de texto abierta.
		if (client.screen != null) {
			drainClicks();
			return;
		}

		while (openConfigKey.consumeClick()) {
			client.setScreen(new MainConfigScreen(null));
			return;
		}

		while (openEditorKey.consumeClick()) {
			client.setScreen(new HudEditorScreen(null, getConfig().copy(), null));
			return;
		}

		while (toggleEffectsKey.consumeClick()) {
			HudConfig cfg = getConfig();
			cfg.effects.enabled = !cfg.effects.enabled;
			getConfigManager().markDirty();
			getConfigManager().saveNow();
			if (client.player != null) {
				client.player.displayClientMessage(
						ModI18n.text(cfg.effects.enabled
								? "effecttimer.message.effects_enabled"
								: "effecttimer.message.effects_disabled"),
						true
				);
			}
		}
	}

	private static void drainClicks() {
		while (openConfigKey.consumeClick()) {
			// drained
		}
		while (openEditorKey.consumeClick()) {
			// drained
		}
		while (toggleEffectsKey.consumeClick()) {
			// drained
		}
	}
}

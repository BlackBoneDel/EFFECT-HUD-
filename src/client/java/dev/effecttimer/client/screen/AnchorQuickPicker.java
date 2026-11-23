package dev.effecttimer.client.screen;

import dev.effecttimer.config.HudAnchor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Selector rápido «Posiciones rápidas» (v1.2, bloque 3): cuadrícula de 3 filas
 * × 2 columnas — Alta/Media/Baja × Izquierda/Derecha. Compartido por la
 * pantalla "Configurar efectos…" y la barra de herramientas del editor visual.
 * El anclaje activo se marca con el prefijo «» ». La lógica de mapeo es pura
 * ({@link #GRID} / {@link #anchorAt(int)}) y la ejercita {@code SideAnchorsTest}.
 */
public final class AnchorQuickPicker {
	public record Option(HudAnchor anchor, String labelKey) {
	}

	/** Orden del grid: fila = (alta, media, baja), columna = (izquierda, derecha). */
	public static final Option[] GRID = {
			new Option(HudAnchor.SIDE_HIGH_LEFT, "effecttimer.anchor.quick.side_high_left"),
			new Option(HudAnchor.SIDE_HIGH_RIGHT, "effecttimer.anchor.quick.side_high_right"),
			new Option(HudAnchor.CENTER_LEFT, "effecttimer.anchor.quick.center_left"),
			new Option(HudAnchor.CENTER_RIGHT, "effecttimer.anchor.quick.center_right"),
			new Option(HudAnchor.SIDE_LOW_LEFT, "effecttimer.anchor.quick.side_low_left"),
			new Option(HudAnchor.SIDE_LOW_RIGHT, "effecttimer.anchor.quick.side_low_right"),
	};

	public static final int ROWS = 3;
	public static final int COLS = 2;

	private AnchorQuickPicker() {
	}

	public static HudAnchor anchorAt(int index) {
		if (index < 0 || index >= GRID.length) {
			return null;
		}
		return GRID[index].anchor();
	}

	/**
	 * Contrato compartido (Configurar efectos, editor y test): fijar un anclaje
	 * fijo desde el selector rápido siempre deja los desplazamientos en 0 —
	 * mismo comportamiento que al salir de {@code CUSTOM} con el dropdown.
	 */
	public static void applyFixedAnchor(dev.effecttimer.config.EffectsConfig effects, HudAnchor anchor) {
		if (effects == null || anchor == null) {
			return;
		}
		effects.anchor = anchor;
		effects.offsetX = 0;
		effects.offsetY = 0;
	}

	/** Índice del anclaje dentro del grid, o -1 (p. ej. CUSTOM o HOTBAR_*). */
	public static int indexOf(HudAnchor anchor) {
		for (int i = 0; i < GRID.length; i++) {
			if (GRID[i].anchor() == anchor) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Construye los 6 botones colocados en el grid (col = i % 2, row = i / 2).
	 * {@code onPick} recibe el anclaje elegido; la llamada a rebuild/restart de
	 * widgets corresponde a la pantalla contenedora.
	 */
	public static List<Button> build(HudAnchor current, Consumer<HudAnchor> onPick,
			Function<String, Component> tr, int leftX, int columnGap, int topY, int rowGap,
			int buttonWidth, int buttonHeight) {
		List<Button> buttons = new ArrayList<>(GRID.length);
		for (int i = 0; i < GRID.length; i++) {
			Option option = GRID[i];
			int col = i % COLS;
			int row = i / COLS;
			int x = leftX + col * (buttonWidth + columnGap);
			int y = topY + row * (buttonHeight + rowGap);
			boolean active = current == option.anchor();
			Component label = active
					? Component.literal("\u00BB ").append(tr.apply(option.labelKey()))
					: tr.apply(option.labelKey());
			buttons.add(Button.builder(label, btn -> onPick.accept(option.anchor()))
					.bounds(x, y, buttonWidth, buttonHeight)
					.tooltip(Tooltip.create(tr.apply(option.anchor().tooltipKey())))
					.build());
		}
		return buttons;
	}
}

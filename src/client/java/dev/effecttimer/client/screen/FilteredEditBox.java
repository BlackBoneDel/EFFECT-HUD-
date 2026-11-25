package dev.effecttimer.client.screen;

import java.util.function.Predicate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;

/**
 * {@link EditBox} que rechaza la entrada que no cumple un predicado.
 *
 * <p>Minecraft 26.1 eliminó {@code EditBox#setFilter}, así que el filtro se
 * reimplementa aquí validando cada carácter tecleado antes de insertarlo.
 * El texto ya presente en la caja se considera válido siempre (procede del
 * propio mod, no del usuario).
 */
public final class FilteredEditBox extends EditBox {
	private final Predicate<String> filter;

	public FilteredEditBox(
			Font font,
			int x,
			int y,
			int width,
			int height,
			Component message,
			Predicate<String> filter
	) {
		super(font, x, y, width, height, message);
		this.filter = filter;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		String typed = event.codepointAsString();
		if (typed == null || typed.isEmpty()) {
			return super.charTyped(event);
		}
		String candidate = getValue() + typed;
		if (!filter.test(candidate)) {
			// Se consume el carácter pero no se inserta: es el mismo
			// comportamiento que tenía el filtro de 1.21.11.
			return true;
		}
		return super.charTyped(event);
	}
}

package dev.effecttimer.format;

import dev.effecttimer.client.format.LevelNames;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El nivel I se oculta (regla por defecto heredada del mod original: hideLevelOne = true). */
public final class LevelNamesTest {
	@Test
	public void hidesLevelOneLikeVanilla() {
		assertEquals("", LevelNames.suffixFor(0));
		assertEquals("Regeneración", LevelNames.withLevel("Regeneración", 0));
	}

	@Test
	public void formatsHigherLevelsAsRomans() {
		assertEquals(" II", LevelNames.suffixFor(1));
		assertEquals("Velocidad II", LevelNames.withLevel("Velocidad", 1));
		assertEquals("Poison V", LevelNames.withLevel("Poison", 4));
		assertEquals("Haste X", LevelNames.withLevel("Haste", 9));
	}

	@Test
	public void romanConversionCoversEdgeCases() {
		assertEquals("I", LevelNames.roman(1));
		assertEquals("IV", LevelNames.roman(4));
		assertEquals("IX", LevelNames.roman(9));
		assertEquals("XIV", LevelNames.roman(14));
		assertEquals("XL", LevelNames.roman(40));
		assertEquals("CMXCIX", LevelNames.roman(999));
	}

	@Test
	public void unknownOrNegativeAmplifiersClampToOne() {
		assertEquals("", LevelNames.suffixFor(-5));
		assertEquals("Desconocido", LevelNames.withLevel("Desconocido", -5));
		assertEquals("Unknown", LevelNames.withLevel("", 3));
	}
}

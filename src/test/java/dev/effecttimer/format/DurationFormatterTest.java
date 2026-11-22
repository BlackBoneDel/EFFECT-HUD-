package dev.effecttimer.format;

import dev.effecttimer.client.format.DurationFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mantiene el contrato del formateador heredado del módulo Effects HUD original,
 * con el relleno mm:ss que fija la spec (02:15, 00:45, 00:10).
 */
public final class DurationFormatterTest {
	@Test
	public void formatsMinutesAndSecondsZeroPadded() {
		assertEquals("02:15", DurationFormatter.formatTicks(2700, 20.0F, false));
		assertEquals("00:45", DurationFormatter.formatTicks(900, 20.0F, false));
		assertEquals("00:10", DurationFormatter.formatTicks(200, 20.0F, false));
		assertEquals("00:00", DurationFormatter.formatTicks(0, 20.0F, false));
	}

	@Test
	public void formatsHoursWithoutPaddingOnMinutes() {
		assertEquals("1:00:00", DurationFormatter.formatTicks(72000, 20.0F, false));
		// 10 h 02 m 05 s = 36125 s = 722500 ticks a 20 tps
		assertEquals("10:02:05", DurationFormatter.formatTicks(722500, 20.0F, false));
	}

	@Test
	public void infiniteAndNegativeTicksShowInfinity() {
		assertEquals("\u221E", DurationFormatter.formatTicks(-1, 20.0F, false));
		assertEquals("\u221E", DurationFormatter.formatTicks(300, 20.0F, true));
	}

	@Test
	public void respectsCustomTicksPerSecond() {
		// A 40 tps, 2700 ticks son 67.5 s -> "01:07"
		assertEquals("01:07", DurationFormatter.formatTicks(2700, 40.0F, false));
	}

	@Test
	public void fallsBackTo20TpsOnInvalidRates() {
		assertEquals("02:15", DurationFormatter.formatTicks(2700, 0.0F, false));
		assertEquals("02:15", DurationFormatter.formatTicks(2700, Float.NaN, false));
	}
}

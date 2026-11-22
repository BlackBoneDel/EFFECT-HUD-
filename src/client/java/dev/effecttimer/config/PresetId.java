package dev.effecttimer.config;

public enum PresetId {
	VANILLA_COMPACT("effecttimer.preset.vanilla_compact", "effecttimer.tooltip.preset.vanilla_compact"),
	COMPACT_TEXT("effecttimer.preset.compact_text", "effecttimer.tooltip.preset.compact_text"),
	EXPANDED_READOUT("effecttimer.preset.expanded_readout", "effecttimer.tooltip.preset.expanded_readout");

	private final String translationKey;
	private final String tooltipKey;

	PresetId(String translationKey, String tooltipKey) {
		this.translationKey = translationKey;
		this.tooltipKey = tooltipKey;
	}

	public String translationKey() {
		return translationKey;
	}

	public String tooltipKey() {
		return tooltipKey;
	}
}

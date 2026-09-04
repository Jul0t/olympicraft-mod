package fr.olympicraft.game.sumo.kit;

import java.util.Locale;

public enum SumoKitResolution {

    RANDOM,
    DEFAULT,
    CANCEL;

    public static SumoKitResolution from(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }

        try {
            return valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );
        } catch (IllegalArgumentException exception) {
            return DEFAULT;
        }
    }
}
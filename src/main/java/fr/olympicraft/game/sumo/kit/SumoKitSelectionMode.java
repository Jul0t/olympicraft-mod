package fr.olympicraft.game.sumo.kit;

import java.util.Locale;

public enum SumoKitSelectionMode {

    FIXED,
    RANDOM,
    VOTE,
    PLAYER_CHOICE;

    public static SumoKitSelectionMode from(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return FIXED;
        }

        try {
            return valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );
        } catch (IllegalArgumentException exception) {
            return FIXED;
        }
    }

    public boolean usesMenu() {
        return this == VOTE
                || this == PLAYER_CHOICE;
    }

    public boolean commonPreset() {
        return this != PLAYER_CHOICE;
    }
}
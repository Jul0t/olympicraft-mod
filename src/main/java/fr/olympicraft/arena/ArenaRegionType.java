package fr.olympicraft.arena;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ArenaRegionType {

    GAME_BOUNDS(
            "game_bounds",
            "Limites générales"
    ),

    PLAY_AREA(
            "play_area",
            "Zone jouable"
    ),

    VOID(
            "void",
            "Zone d'élimination"
    ),

    SPECTATOR(
            "spectator",
            "Zone spectateur"
    ),

    ISLAND(
            "island",
            "Île Sheep Wars"
    ),

    BOMB_SITE(
            "bomb_site",
            "Site de bombe"
    ),

    TRAP(
            "trap",
            "Piège"
    ),

    FLOOR(
            "floor",
            "Étage TNT Run"
    ),

    SAFE_ZONE(
            "safe_zone",
            "Zone sécurisée"
    ),

    PROTECTED(
            "protected",
            "Zone protégée"
    ),

    POOL(
            "pool",
            "Piscine du Dé à coudre"
    ),

    HIDE_AREA(
            "hide_area",
            "Zone de Cache-cache"
    ),

    SUMO_RING(
            "sumo_ring",
            "Zone de combat Sumo"
    );

    private final String id;
    private final String displayName;

    ArenaRegionType(
            String id,
            String displayName
    ) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ArenaRegionType> fromInput(
            String input
    ) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(input);

        return Arrays.stream(values())
                .filter(type ->
                        type.id.equals(normalized)
                                || normalize(type.name())
                                .equals(normalized)
                                || normalize(type.displayName)
                                .equals(normalized)
                )
                .findFirst();
    }

    private static String normalize(String input) {
        String withoutAccents = Normalizer.normalize(
                input,
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
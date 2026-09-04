package fr.olympicraft.arena;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum GameType {

    DE_A_COUDRE("de_a_coudre", "Dé à coudre"),
    SHEEP_WARS("sheep_wars", "Sheep Wars"),
    SUMO("sumo", "Sumo"),
    HIDE_AND_SEEK("hide_and_seek", "Cache-cache"),
    TNT_RUN("tnt_run", "TNT Run"),
    MURDER_MYSTERY("murder_mystery", "Murder Mystery"),
    TNT_TAG("tnt_tag", "TNT Tag"),
    COPS_N_ROBBERS("cops_n_robbers", "Cops n' Robbers");

    private final String id;
    private final String displayName;

    GameType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<GameType> fromInput(String input) {
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

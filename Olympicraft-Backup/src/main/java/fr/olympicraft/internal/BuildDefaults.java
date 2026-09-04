package fr.olympicraft.internal;

import java.util.List;
import java.util.Map;

public final class BuildDefaults {

    public static final String MOD_ID = "olympicraft";
    public static final String MOD_NAME = "Olympicraft";
    public static final String ROOT_COMMAND = "oc";
    public static final String PLACEHOLDER_PREFIX = "oc";

    public static final List<String> ROOT_COMMAND_ALIASES = List.of(
            "olympicraft"
    );

    public static final Map<String, String> DEFAULT_GAME_NAMES = Map.of(
            "de_a_coudre", "Dé à coudre",
            "sheep_wars", "Sheep Wars",
            "sumo", "Sumo",
            "hide_and_seek", "Cache-cache",
            "tnt_run", "TNT Run",
            "murder_mystery", "Murder Mystery",
            "tnt_tag", "TNT Tag",
            "cops_n_robbers", "Cops n' Robbers"
    );

    public static final int CONFIG_SCHEMA_VERSION = 1;
    public static final int SAVE_SCHEMA_VERSION = 1;

    private BuildDefaults() {
    }
}

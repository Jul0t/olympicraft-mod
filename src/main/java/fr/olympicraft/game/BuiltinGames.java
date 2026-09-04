package fr.olympicraft.game;

import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.arena.RegionRequirement;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BuiltinGames {

    private BuiltinGames() {
    }

    public static void registerAll(GameRegistry registry) {
        registry.register(
                definition(
                        "de_a_coudre",
                        "Dé à coudre",
                        "Saute dans la grille et ferme les cases.",
                        "minecraft:water_bucket",
                        2,
                        24,
                        requirements(
                                required(
                                        ArenaRegionType.POOL,
                                        1,
                                        1
                                ),
                                optionalSingle(
                                        ArenaRegionType.GAME_BOUNDS
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.PROTECTED
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                )
                        )
                )
        );

        registry.register(
                definition(
                        "sheep_wars",
                        "Sheep Wars",
                        "Combat entre îles avec des moutons spéciaux.",
                        "minecraft:sheep_spawn_egg",
                        2,
                        32,
                        requirements(
                                required(
                                        ArenaRegionType.GAME_BOUNDS,
                                        1,
                                        1
                                ),
                                required(
                                        ArenaRegionType.ISLAND,
                                        2,
                                        RegionRequirement.UNLIMITED
                                ),
                                optionalSingle(
                                        ArenaRegionType.PLAY_AREA
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.PROTECTED
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                ),
                                optionalMultiple(
                                        ArenaRegionType.VOID
                                )
                        )
                )
        );

        registry.register(
                definition(
                        "sumo",
                        "Sumo",
                        "Éjecte ton adversaire sans lui infliger de dégâts.",
                        "minecraft:stick",
                        2,
                        64,
                        requirements(
                                required(
                                        ArenaRegionType.SUMO_RING,
                                        1,
                                        1
                                ),
                                required(
                                        ArenaRegionType.VOID,
                                        1,
                                        RegionRequirement.UNLIMITED
                                ),
                                optionalSingle(
                                        ArenaRegionType.GAME_BOUNDS
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                )
                        )
                )
        );

        registry.register(
                definition(
                        "hide_and_seek",
                        "Cache-cache",
                        "Les cachés doivent échapper aux chercheurs.",
                        "minecraft:spyglass",
                        2,
                        64,
                        requirements(
                                required(
                                        ArenaRegionType.HIDE_AREA,
                                        1,
                                        1
                                ),
                                optionalSingle(
                                        ArenaRegionType.GAME_BOUNDS
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                ),
                                optionalMultiple(
                                        ArenaRegionType.PROTECTED
                                )
                        )
                )
        );

        registry.register(
                definition(
                        "tnt_run",
                        "TNT Run",
                        "Survis sur un sol qui disparaît.",
                        "minecraft:tnt",
                        2,
                        64,
                        requirements(
                                required(
                                        ArenaRegionType.FLOOR,
                                        1,
                                        RegionRequirement.UNLIMITED
                                ),
                                optionalSingle(
                                        ArenaRegionType.GAME_BOUNDS
                                ),
                                optionalSingle(
                                        ArenaRegionType.PLAY_AREA
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.VOID
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                )
                        )
                )
        );

        registry.register(
                definition(
                        "murder_mystery",
                        "Murder Mystery",
                        "Découvre le meurtrier avant qu'il ne soit trop tard.",
                        "minecraft:bow",
                        3,
                        64,
                        requirements(
                                required(
                                        ArenaRegionType.GAME_BOUNDS,
                                        1,
                                        1
                                ),
                                optionalSingle(
                                        ArenaRegionType.PLAY_AREA
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.TRAP
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                ),
                                optionalMultiple(
                                        ArenaRegionType.PROTECTED
                                )
                        )
                )
        );

        registry.register(
                definition(
                        "tnt_tag",
                        "TNT Tag",
                        "Transmets la TNT avant la fin du chrono.",
                        "minecraft:gunpowder",
                        2,
                        64,
                        requirements(
                                required(
                                        ArenaRegionType.PLAY_AREA,
                                        1,
                                        1
                                ),
                                optionalSingle(
                                        ArenaRegionType.GAME_BOUNDS
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                ),
                                optionalMultiple(
                                        ArenaRegionType.VOID
                                )
                        )
                )
        );

        registry.register(
                definition(
                        "cops_n_robbers",
                        "Cops n' Robbers",
                        "Pose ou désamorce la bombe en équipe.",
                        "minecraft:crossbow",
                        2,
                        64,
                        requirements(
                                required(
                                        ArenaRegionType.BOMB_SITE,
                                        1,
                                        RegionRequirement.UNLIMITED
                                ),
                                required(
                                        ArenaRegionType.GAME_BOUNDS,
                                        1,
                                        1
                                ),
                                optionalSingle(
                                        ArenaRegionType.PLAY_AREA
                                ),
                                optionalSingle(
                                        ArenaRegionType.SPECTATOR
                                ),
                                optionalMultiple(
                                        ArenaRegionType.SAFE_ZONE
                                ),
                                optionalMultiple(
                                        ArenaRegionType.PROTECTED
                                )
                        )
                )
        );
    }

    private static GameDefinition definition(
            String id,
            String displayName,
            String description,
            String iconItem,
            int minimumPlayers,
            int maximumPlayers,
            Map<ArenaRegionType, RegionRequirement>
                    regionRequirements
    ) {
        return new GameDefinition(
                id,
                new GamePresentation(
                        displayName,
                        description,
                        iconItem,
                        List.of(description)
                ),
                minimumPlayers,
                maximumPlayers,
                regionRequirements,
                context -> {
                    context.requireLobby();
                    context.requireSpectator();
                    context.requireTotalSpawns(2);
                }
        );
    }

    @SafeVarargs
    private static Map<ArenaRegionType, RegionRequirement>
    requirements(
            Map.Entry<
                    ArenaRegionType,
                    RegionRequirement
                    >... entries
    ) {
        EnumMap<ArenaRegionType, RegionRequirement> result =
                new EnumMap<>(ArenaRegionType.class);

        for (Map.Entry<
                ArenaRegionType,
                RegionRequirement
                > entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private static Map.Entry<
            ArenaRegionType,
            RegionRequirement
            > required(
            ArenaRegionType type,
            int minimum,
            int maximum
    ) {
        return Map.entry(
                type,
                new RegionRequirement(minimum, maximum)
        );
    }

    private static Map.Entry<
            ArenaRegionType,
            RegionRequirement
            > optionalSingle(
            ArenaRegionType type
    ) {
        return Map.entry(
                type,
                RegionRequirement.optionalSingle()
        );
    }

    private static Map.Entry<
            ArenaRegionType,
            RegionRequirement
            > optionalMultiple(
            ArenaRegionType type
    ) {
        return Map.entry(
                type,
                RegionRequirement.optionalMultiple()
        );
    }
}
package fr.olympicraft.game.murder.chat;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.test.dummy.DummyParticipant;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class MurderMysterySensitiveNames {

    private static final Pattern COMBINING_MARKS =
            Pattern.compile(
                    "\\p{M}+"
            );

    private static final Pattern NON_ALPHANUMERIC =
            Pattern.compile(
                    "[^a-z0-9]"
            );

    private MurderMysterySensitiveNames() {
    }

    public static List<SensitiveName> collect(
            MinecraftServer server,
            MurderMysteryDictionary dictionary
    ) {
        Set<String> names =
                new LinkedHashSet<>();

        collectOnlinePlayerNames(
                server,
                names
        );

        collectDummyNames(
                names
        );

        return names.stream()
                .map(name ->
                        new SensitiveName(
                                name,
                                normalizeCompact(
                                        name
                                ),
                                dictionary != null
                                        && dictionary.contains(
                                        name
                                )
                        )
                )
                .filter(name ->
                        !name.compact()
                                .isBlank()
                )
                /*
                 * Les noms les plus longs sont contrôlés
                 * en premier.
                 */
                .sorted(
                        Comparator.comparingInt(
                                (
                                        SensitiveName name
                                ) ->
                                        name.compact()
                                                .length()
                        ).reversed()
                )
                .toList();
    }

    private static void collectOnlinePlayerNames(
            MinecraftServer server,
            Set<String> names
    ) {
        if (server == null) {
            return;
        }

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            if (player == null) {
                continue;
            }

            addName(
                    names,
                    player.getGameProfile()
                            .getName()
            );
        }
    }

    private static void collectDummyNames(
            Set<String> names
    ) {
        /*
         * DummyManager.all() demande l'identifiant d'une arène.
         * On parcourt donc toutes les arènes connues.
         */
        for (ArenaDefinition arena :
                Olympicraft.arenas().all()) {
            if (arena == null
                    || arena.id == null
                    || arena.id.isBlank()) {
                continue;
            }

            for (DummyParticipant dummy :
                    Olympicraft.dummies()
                            .all(arena.id)) {
                if (dummy == null) {
                    continue;
                }

                addName(
                        names,
                        dummy.name()
                );
            }
        }
    }

    public static String normalizeReadable(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lowercase =
                value.toLowerCase(
                        Locale.ROOT
                );

        String decomposed =
                Normalizer.normalize(
                        lowercase,
                        Normalizer.Form.NFD
                );

        return COMBINING_MARKS.matcher(
                decomposed
        ).replaceAll("");
    }

    public static String normalizeCompact(
            String value
    ) {
        String readable =
                normalizeReadable(
                        value
                );

        return NON_ALPHANUMERIC.matcher(
                readable
        ).replaceAll("");
    }

    private static void addName(
            Set<String> names,
            String name
    ) {
        if (name == null || name.isBlank()) {
            return;
        }

        names.add(
                name.trim()
        );
    }

    public record SensitiveName(
            String original,
            String compact,
            boolean dictionaryWord
    ) {
    }
}
package fr.olympicraft.game.murder.chat;

import fr.olympicraft.Olympicraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MurderMysteryDictionary {

    private static final String FRENCH_DICTIONARY =
            "/assets/olympicraft/dictionaries/fr_fr.txt";

    private static final String ENGLISH_DICTIONARY =
            "/assets/olympicraft/dictionaries/en_us.txt";

    private static final int MINIMUM_WORD_LENGTH = 4;

    private final Set<String> words =
            new HashSet<>();

    private boolean loaded;

    public synchronized void load() {
        if (loaded) {
            return;
        }

        words.clear();

        loadResource(
                FRENCH_DICTIONARY
        );

        loadResource(
                ENGLISH_DICTIONARY
        );

        loaded = true;

        Olympicraft.LOGGER.info(
                "{} mot(s) chargé(s) dans les dictionnaires "
                        + "du chat Murder Mystery.",
                words.size()
        );
    }

    public boolean contains(
            String word
    ) {
        if (!loaded) {
            load();
        }

        String normalized =
                normalizeWord(
                        word
                );

        return normalized.length()
                >= MINIMUM_WORD_LENGTH
                && words.contains(
                normalized
        );
    }

    public int size() {
        if (!loaded) {
            load();
        }

        return words.size();
    }

    private void loadResource(
            String resourcePath
    ) {
        try (InputStream input =
                     MurderMysteryDictionary.class
                             .getResourceAsStream(
                                     resourcePath
                             )) {
            if (input == null) {
                Olympicraft.LOGGER.warn(
                        "Dictionnaire Murder Mystery "
                                + "introuvable : {}",
                        resourcePath
                );

                return;
            }

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         input,
                                         StandardCharsets.UTF_8
                                 )
                         )) {
                String line;

                while ((line = reader.readLine())
                        != null) {
                    String cleaned =
                            line.trim();

                    if (cleaned.isBlank()
                            || cleaned.startsWith("#")) {
                        continue;
                    }

                    String normalized =
                            normalizeWord(
                                    cleaned
                            );

                    /*
                     * Les mots de moins de quatre caractères
                     * ne sont jamais chargés.
                     */
                    if (normalized.length()
                            >= MINIMUM_WORD_LENGTH) {
                        words.add(
                                normalized
                        );
                    }
                }
            }
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de charger le dictionnaire '{}'.",
                    resourcePath,
                    exception
            );
        }
    }

    public static String normalizeWord(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lowercase =
                value.toLowerCase(
                        Locale.ROOT
                );

        String withoutAccents =
                Normalizer.normalize(
                        lowercase,
                        Normalizer.Form.NFD
                ).replaceAll(
                        "\\p{M}+",
                        ""
                );

        return withoutAccents.replaceAll(
                "[^a-z0-9]",
                ""
        );
    }
}
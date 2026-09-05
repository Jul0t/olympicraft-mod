package fr.olympicraft.game.murder.chat;

import java.util.List;

public final class MurderMysteryNameFilter {

    private static final List<String> REVEAL_PREFIXES =
            List.of(
                    "@",
                    "pseudo ",
                    "joueur ",
                    "player ",
                    "username ",
                    "c'est ",
                    "cest ",
                    "c est ",
                    "it's ",
                    "its "
            );

    private static final List<String> REVEAL_SUFFIXES =
            List.of(
                    " est le meurtrier",
                    " est murder",
                    " est le murder",
                    " is the murderer",
                    " is murderer",
                    " est le detective",
                    " est détective",
                    " is the detective",
                    " est le trouble fete",
                    " est trouble fête"
            );

    public boolean containsForbiddenName(
            String message,
            List<
                    MurderMysterySensitiveNames.SensitiveName
                    > sensitiveNames
    ) {
        if (message == null
                || message.isBlank()
                || sensitiveNames == null
                || sensitiveNames.isEmpty()) {
            return false;
        }

        String readable =
                MurderMysterySensitiveNames
                        .normalizeReadable(
                                message
                        );

        String compact =
                MurderMysterySensitiveNames
                        .normalizeCompact(
                                message
                        );

        for (MurderMysterySensitiveNames.SensitiveName name :
                sensitiveNames) {
            if (name == null
                    || name.compact() == null
                    || name.compact().isBlank()) {
                continue;
            }

            /*
             * Si le pseudo est également un mot courant,
             * il est autorisé dans une phrase normale.
             *
             * Il reste toutefois interdit lorsqu'il est
             * clairement utilisé comme une mention.
             */
            if (name.dictionaryWord()) {
                if (containsExplicitMention(
                        readable,
                        name.original()
                )) {
                    return true;
                }

                continue;
            }

            /*
             * Les pseudos inhabituels sont recherchés dans
             * une version sans espaces ni ponctuation.
             *
             * Les formes suivantes seront donc détectées :
             *
             * Jul0t
             * J-u-l-0-t
             * J u l 0 t
             * J_u_l_0_t
             */
            if (compact.contains(
                    name.compact()
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean containsExplicitMention(
            String readableMessage,
            String originalName
    ) {
        if (readableMessage == null
                || readableMessage.isBlank()
                || originalName == null
                || originalName.isBlank()) {
            return false;
        }

        String normalizedName =
                MurderMysterySensitiveNames
                        .normalizeReadable(
                                originalName
                        )
                        .trim();

        if (normalizedName.isBlank()) {
            return false;
        }

        /*
         * Mentions avec un marqueur placé avant le pseudo.
         *
         * Exemples :
         * @bonjour
         * [bonjour]
         * (bonjour)
         * "bonjour"
         * 'bonjour'
         */
        if (containsMarkedMention(
                readableMessage,
                normalizedName
        )) {
            return true;
        }

        /*
         * Exemples :
         *
         * joueur bonjour
         * pseudo bonjour
         * c'est bonjour
         */
        for (String prefix :
                REVEAL_PREFIXES) {
            if (readableMessage.contains(
                    prefix + normalizedName
            )) {
                return true;
            }
        }

        /*
         * Exemples :
         *
         * bonjour est le meurtrier
         * bonjour est le détective
         */
        for (String suffix :
                REVEAL_SUFFIXES) {
            if (readableMessage.contains(
                    normalizedName + suffix
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean containsMarkedMention(
            String message,
            String name
    ) {
        return containsMarker(
                message,
                name,
                "@",
                ""
        ) || containsMarker(
                message,
                name,
                "[",
                "]"
        ) || containsMarker(
                message,
                name,
                "(",
                ")"
        ) || containsMarker(
                message,
                name,
                "\"",
                "\""
        ) || containsMarker(
                message,
                name,
                "'",
                "'"
        );
    }

    private boolean containsMarker(
            String message,
            String name,
            String openingMarker,
            String closingMarker
    ) {
        String beginning =
                openingMarker + name;

        int searchFrom = 0;

        while (searchFrom < message.length()) {
            int index =
                    message.indexOf(
                            beginning,
                            searchFrom
                    );

            if (index < 0) {
                return false;
            }

            int nameEnd =
                    index + beginning.length();

            /*
             * Empêche de détecter "@chat" dans "@chateau".
             */
            boolean validCharacterAfterName =
                    nameEnd >= message.length()
                            || !isNameCharacter(
                            message.charAt(
                                    nameEnd
                            )
                    );

            if (!validCharacterAfterName) {
                searchFrom = index + 1;
                continue;
            }

            /*
             * Pour @pseudo, aucun marqueur de fermeture
             * n'est nécessaire.
             */
            if (closingMarker.isEmpty()) {
                return true;
            }

            int cursor = nameEnd;

            /*
             * Autorise les espaces avant le marqueur fermant :
             *
             * [bonjour ]
             * "bonjour "
             */
            while (cursor < message.length()
                    && Character.isWhitespace(
                    message.charAt(cursor)
            )) {
                cursor++;
            }

            if (message.startsWith(
                    closingMarker,
                    cursor
            )) {
                return true;
            }

            searchFrom = index + 1;
        }

        return false;
    }

    private boolean isNameCharacter(
            char character
    ) {
        return Character.isLetterOrDigit(
                character
        ) || character == '_';
    }
}
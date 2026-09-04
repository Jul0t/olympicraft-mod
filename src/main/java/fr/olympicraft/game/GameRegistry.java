package fr.olympicraft.game;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class GameRegistry {

    private final Map<String, GameDefinition> definitions =
            new LinkedHashMap<>();

    private boolean locked;

    public synchronized void register(
            GameDefinition definition
    ) {
        if (locked) {
            throw new IllegalStateException(
                    "Le registre des jeux est verrouillé."
            );
        }

        String id = normalizeId(definition.id());

        if (definitions.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Le jeu '" + id + "' est déjà enregistré."
            );
        }

        definitions.put(id, definition);
    }

    public synchronized Optional<GameDefinition> find(
            String input
    ) {
        String normalized = normalizeId(input);

        GameDefinition direct = definitions.get(normalized);

        if (direct != null) {
            return Optional.of(direct);
        }

        return definitions.values()
                .stream()
                .filter(definition ->
                        normalizeId(definition.displayName())
                                .equals(normalized)
                )
                .findFirst();
    }

    public synchronized Collection<GameDefinition> all() {
        return definitions.values()
                .stream()
                .sorted(Comparator.comparing(GameDefinition::id))
                .toList();
    }

    public synchronized List<String> idsStartingWith(
            String input
    ) {
        String normalized = normalizeId(input);

        return definitions.keySet()
                .stream()
                .filter(id -> id.startsWith(normalized))
                .sorted()
                .toList();
    }

    public synchronized boolean contains(String id) {
        return definitions.containsKey(normalizeId(id));
    }

    public synchronized int size() {
        return definitions.size();
    }

    public synchronized void lock() {
        locked = true;
    }

    public synchronized boolean isLocked() {
        return locked;
    }

    public static String normalizeId(String input) {
        if (input == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(
                input,
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[_-]+|[_-]+$", "");
    }
}

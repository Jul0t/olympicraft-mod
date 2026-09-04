package fr.olympicraft.match.runtime;

import fr.olympicraft.arena.ArenaDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class GameRuntimeRegistry {

    private final Map<String, GameRuntimeFactory> factories =
            new LinkedHashMap<>();

    private boolean locked;

    public synchronized void register(
            String gameId,
            GameRuntimeFactory factory
    ) {
        if (locked) {
            throw new IllegalStateException(
                    "Le registre des runtimes est verrouillé."
            );
        }

        String normalized = normalize(gameId);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "L'identifiant du jeu ne peut pas être vide."
            );
        }

        if (factory == null) {
            throw new IllegalArgumentException(
                    "La factory du runtime ne peut pas être nulle."
            );
        }

        if (factories.putIfAbsent(
                normalized,
                factory
        ) != null) {
            throw new IllegalArgumentException(
                    "Un runtime est déjà enregistré pour "
                            + normalized
                            + "."
            );
        }
    }

    public synchronized GameRuntime create(
            MinecraftServer server,
            ArenaDefinition arena
    ) {
        if (arena == null) {
            return new GenericGameRuntime();
        }

        GameRuntimeFactory factory =
                factories.get(
                        normalize(arena.gameType)
                );

        if (factory == null) {
            return new GenericGameRuntime();
        }

        GameRuntime runtime =
                factory.create(server, arena);

        return runtime == null
                ? new GenericGameRuntime()
                : runtime;
    }

    public synchronized boolean contains(
            String gameId
    ) {
        return factories.containsKey(
                normalize(gameId)
        );
    }

    public synchronized int size() {
        return factories.size();
    }

    public synchronized void lock() {
        locked = true;
    }

    public boolean locked() {
        return locked;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }
}
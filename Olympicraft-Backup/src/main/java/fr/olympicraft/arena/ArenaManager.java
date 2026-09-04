package fr.olympicraft.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.olympicraft.Olympicraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ArenaManager {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Map<String, ArenaDefinition> arenas =
            new LinkedHashMap<>();

    private MinecraftServer server;
    private Path storageDirectory;

    public synchronized void attachServer(
            MinecraftServer server
    ) {
        this.server = server;

        storageDirectory = server.getWorldPath(
                        net.minecraft.world.level.storage.LevelResource.ROOT
                ).resolve("olympicraft")
                .resolve("arenas");

        Olympicraft.LOGGER.info(
                "Dossier des arènes : {}",
                storageDirectory.toAbsolutePath()
        );

        loadAll();
    }
    
    public synchronized void detachServer() {
        saveAll();

        arenas.clear();
        storageDirectory = null;
        server = null;
    }

    public synchronized boolean isAttached() {
        return server != null && storageDirectory != null;
    }

    public synchronized Collection<ArenaDefinition> all() {
        return List.copyOf(arenas.values());
    }

    public synchronized Optional<ArenaDefinition> find(
            String input
    ) {
        return Optional.ofNullable(
                arenas.get(normalizeId(input))
        );
    }

    public synchronized CreateResult create(
            String requestedName,
            GameType gameType,
            ServerPlayer creator
    ) {
        if (!isAttached()) {
            return CreateResult.failure(
                    "Aucun monde n'est attaché."
            );
        }

        String id = normalizeId(requestedName);

        if (id.isBlank()) {
            return CreateResult.failure(
                    "Le nom ne contient aucun caractère valide."
            );
        }

        if (id.length() > 48) {
            return CreateResult.failure(
                    "L'identifiant ne peut pas dépasser 48 caractères."
            );
        }

        if (arenas.containsKey(id)) {
            return CreateResult.failure(
                    "Une arène portant cet identifiant existe déjà."
            );
        }

        ArenaDefinition arena = new ArenaDefinition(
                id,
                requestedName.trim(),
                gameType,
                creator.getUUID(),
                server.getWorldData()
                        .getLevelName()
        );

        arena.lobby = ArenaPosition.from(
                creator.serverLevel(),
                creator.getX(),
                creator.getY(),
                creator.getZ(),
                creator.getYRot(),
                creator.getXRot()
        );

        arena.spectator = ArenaPosition.from(
                creator.serverLevel(),
                creator.getX(),
                creator.getY(),
                creator.getZ(),
                creator.getYRot(),
                creator.getXRot()
        );

        arenas.put(id, arena);

        if (!save(arena)) {
            arenas.remove(id);

            return CreateResult.failure(
                    "L'arène n'a pas pu être enregistrée."
            );
        }

        return CreateResult.success(arena);
    }

    public synchronized boolean setEnabled(
            ArenaDefinition arena,
            boolean enabled
    ) {
        if (enabled && !arena.validate().valid()) {
            return false;
        }

        arena.enabled = enabled;
        arena.touch();

        return save(arena);
    }

    public synchronized boolean delete(
            ArenaDefinition arena
    ) {
        if (storageDirectory == null) {
            return false;
        }

        Path file = file(arena.id);

        try {
            Files.deleteIfExists(file);
            arenas.remove(arena.id);
            return true;
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de supprimer l'arène {}.",
                    arena.id,
                    exception
            );

            return false;
        }
    }

    public synchronized void loadAll() {
        arenas.clear();

        if (storageDirectory == null) {
            return;
        }

        try {
            Files.createDirectories(storageDirectory);

            try (var files = Files.list(storageDirectory)) {
                for (Path path : files
                        .filter(file ->
                                file.getFileName()
                                        .toString()
                                        .endsWith(".json")
                        )
                        .sorted()
                        .toList()) {
                    load(path).ifPresent(arena ->
                            arenas.put(arena.id, arena)
                    );
                }
            }

            Olympicraft.LOGGER.info(
                    "{} arène(s) chargée(s) depuis {}.",
                    arenas.size(),
                    storageDirectory.toAbsolutePath()
            );
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de charger les arènes.",
                    exception
            );
        }
    }

    public synchronized void saveAll() {
        for (ArenaDefinition arena : arenas.values()) {
            save(arena);
        }
    }

    private Optional<ArenaDefinition> load(Path path) {
        try (Reader reader = Files.newBufferedReader(
                path,
                StandardCharsets.UTF_8
        )) {
            ArenaDefinition arena =
                    gson.fromJson(reader, ArenaDefinition.class);

            if (arena == null) {
                return Optional.empty();
            }

            arena.id = normalizeId(arena.id);

            if (arena.id.isBlank()) {
                Olympicraft.LOGGER.warn(
                        "Arène ignorée car son identifiant est vide : {}",
                        path
                );

                return Optional.empty();
            }

            if (arena.spawns == null) {
                arena.spawns = new LinkedHashMap<>();
            }

            if (arena.properties == null) {
                arena.properties = new LinkedHashMap<>();
            }

            return Optional.of(arena);
        } catch (Exception exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de lire l'arène {}.",
                    path,
                    exception
            );

            return Optional.empty();
        }
    }

    private boolean save(ArenaDefinition arena) {
        if (storageDirectory == null) {
            return false;
        }

        try {
            Files.createDirectories(storageDirectory);

            arena.touch();

            Path target = file(arena.id);
            Path temporary = target.resolveSibling(
                    target.getFileName() + ".tmp"
            );

            try (Writer writer = Files.newBufferedWriter(
                    temporary,
                    StandardCharsets.UTF_8
            )) {
                gson.toJson(arena, writer);
            }

            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicFailure) {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible d'enregistrer l'arène {}.",
                    arena.id,
                    exception
            );

            return false;
        }
    }

    private Path file(String arenaId) {
        return storageDirectory.resolve(arenaId + ".json");
    }

    public static String normalizeId(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                input,
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[_-]+|[_-]+$", "");
    }

    public List<String> idsStartingWith(String input) {
        String normalized = normalizeId(input);

        return arenas.keySet()
                .stream()
                .filter(id -> id.startsWith(normalized))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public record CreateResult(
            boolean successful,
            ArenaDefinition arena,
            String error
    ) {
        public static CreateResult success(
                ArenaDefinition arena
        ) {
            return new CreateResult(true, arena, "");
        }

        public static CreateResult failure(
                String error
        ) {
            return new CreateResult(false, null, error);
        }
    }
}

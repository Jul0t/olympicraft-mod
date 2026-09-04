package fr.olympicraft.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.game.GameDefinition;
import fr.olympicraft.game.GameRegistry;

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

    public synchronized boolean setLobby(
            ArenaDefinition arena,
            ServerPlayer player
    ) {
        arena.lobby = positionOf(player);
        arena.touch();

        return save(arena);
    }

    public synchronized int addSpawn(
            ArenaDefinition arena,
            String requestedGroup,
            ServerPlayer player
    ) {
        String group = normalizeGroup(requestedGroup);

        if (group.isBlank()) {
            return -1;
        }

        List<ArenaPosition> positions =
                arena.spawns.computeIfAbsent(
                        group,
                        ignored -> new ArrayList<>()
                );

        positions.add(positionOf(player));
        arena.touch();

        if (!save(arena)) {
            positions.remove(positions.size() - 1);

            if (positions.isEmpty()) {
                arena.spawns.remove(group);
            }

            return -1;
        }

        /*
         * Index public : 1, 2, 3...
         */
        return positions.size();
    }

    public synchronized boolean removeSpawn(
            ArenaDefinition arena,
            String requestedGroup,
            int publicIndex
    ) {
        String group = normalizeGroup(requestedGroup);
        List<ArenaPosition> positions = arena.spawns.get(group);

        if (positions == null || positions.isEmpty()) {
            return false;
        }

        int internalIndex = publicIndex - 1;

        if (internalIndex < 0
                || internalIndex >= positions.size()) {
            return false;
        }

        ArenaPosition removed = positions.remove(internalIndex);

        if (positions.isEmpty()) {
            arena.spawns.remove(group);
        }

        arena.touch();

        if (!save(arena)) {
            List<ArenaPosition> restored =
                    arena.spawns.computeIfAbsent(
                            group,
                            ignored -> new ArrayList<>()
                    );

            restored.add(
                    Math.min(internalIndex, restored.size()),
                    removed
            );

            return false;
        }

        return true;
    }

    public synchronized Optional<ArenaPosition> spawn(
            ArenaDefinition arena,
            String requestedGroup,
            int publicIndex
    ) {
        String group = normalizeGroup(requestedGroup);
        List<ArenaPosition> positions = arena.spawns.get(group);

        if (positions == null) {
            return Optional.empty();
        }

        int internalIndex = publicIndex - 1;

        if (internalIndex < 0
                || internalIndex >= positions.size()) {
            return Optional.empty();
        }

        return Optional.of(positions.get(internalIndex));
    }

    public synchronized List<String> spawnGroupsStartingWith(
            ArenaDefinition arena,
            String input
    ) {
        String normalized = normalizeGroup(input);

        return arena.spawns.keySet()
                .stream()
                .filter(group -> group.startsWith(normalized))
                .sorted()
                .toList();
    }

    private ArenaPosition positionOf(ServerPlayer player) {
        return ArenaPosition.from(
                player.serverLevel(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    public static String normalizeGroup(String input) {
        return normalizeId(input);
    }

    public synchronized boolean setSpectator(
            ArenaDefinition arena,
            ServerPlayer player
    ) {
        arena.spectator = positionOf(player);
        arena.touch();

        return save(arena);
    }

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
            GameDefinition game,
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
                game.id(),
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

    public record RegionResult(
            boolean successful,
            ArenaRegion region,
            String error
    ) {

        public static RegionResult success(
                ArenaRegion region
        ) {
            return new RegionResult(
                    true,
                    region,
                    ""
            );
        }

        public static RegionResult failure(
                String error
        ) {
            return new RegionResult(
                    false,
                    null,
                    error
            );
        }
    }

    public synchronized boolean setEnabled(
            ArenaDefinition arena,
            boolean enabled
    ) {
        if (enabled) {
            GameDefinition game = games.find(arena.gameType)
                    .orElse(null);

            if (game == null) {
                return false;
            }

            if (!game.validateArena(arena).valid()) {
                return false;
            }
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
            JsonElement rootElement = JsonParser.parseReader(reader);

            if (!rootElement.isJsonObject()) {
                Olympicraft.LOGGER.warn(
                        "Arène ignorée : le fichier {} ne contient "
                                + "pas un objet JSON.",
                        path
                );

                return Optional.empty();
            }

            JsonObject root = rootElement.getAsJsonObject();

            migrateSpawnFormat(root, path);

            ArenaDefinition arena = gson.fromJson(
                    root,
                    ArenaDefinition.class
            );

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

            if (arena.regions == null) {
                arena.regions = new LinkedHashMap<>();
            }

            arena.regions.entrySet().removeIf(entry ->
                    entry.getKey() == null
                            || entry.getKey().isBlank()
                            || entry.getValue() == null
            );


            arena.spawns.entrySet().removeIf(entry ->
                    entry.getKey() == null
                            || entry.getKey().isBlank()
                            || entry.getValue() == null
            );

            if (arena.properties == null) {
                arena.properties = new LinkedHashMap<>();
            }

            if (!games.contains(arena.gameType)) {
                Olympicraft.LOGGER.warn(
                        "L'arène {} utilise un jeu non enregistré : {}.",
                        arena.id,
                        arena.gameType
                );

                arena.enabled = false;
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
    private final GameRegistry games;

    public ArenaManager(GameRegistry games) {
        this.games = games;
    }
    private void migrateSpawnFormat(
            JsonObject root,
            Path path
    ) {
        JsonElement spawnsElement = root.get("spawns");

        if (spawnsElement == null
                || !spawnsElement.isJsonObject()) {
            return;
        }

        JsonObject spawnsObject =
                spawnsElement.getAsJsonObject();

        JsonObject migratedSpawns = new JsonObject();
        boolean migrated = false;

        for (Map.Entry<String, JsonElement> entry :
                spawnsObject.entrySet()) {
            JsonElement value = entry.getValue();

            if (value == null || value.isJsonNull()) {
                continue;
            }

            if (value.isJsonArray()) {
                migratedSpawns.add(entry.getKey(), value);
                continue;
            }

            if (value.isJsonObject()) {
                com.google.gson.JsonArray array =
                        new com.google.gson.JsonArray();

                array.add(value);
                migratedSpawns.add(entry.getKey(), array);
                migrated = true;
                continue;
            }

            Olympicraft.LOGGER.warn(
                    "Spawn ignoré dans {} : groupe '{}' invalide.",
                    path,
                    entry.getKey()
            );
        }

        if (migrated) {
            root.add("spawns", migratedSpawns);

            Olympicraft.LOGGER.info(
                    "Ancien format de spawns migré pour {}.",
                    path.getFileName()
            );
        }
    }
    public synchronized RegionResult addRegion(
            ArenaDefinition arena,
            String requestedId,
            ArenaRegionType type,
            ArenaSelectionManager.Selection selection
    ) {
        String regionId = normalizeId(requestedId);

        if (regionId.isBlank()) {
            return RegionResult.failure(
                    "Le nom de la région est invalide."
            );
        }

        if (!selection.complete()) {
            return RegionResult.failure(
                    "La sélection est incomplète ou utilise "
                            + "plusieurs dimensions."
            );
        }

        if (arena.regions.containsKey(regionId)) {
            return RegionResult.failure(
                    "Une région portant cet identifiant existe déjà."
            );
        }

        ArenaRegion region;

        try {
            region = new ArenaRegion(
                    regionId,
                    type,
                    selection.first(),
                    selection.second()
            );
        } catch (IllegalArgumentException exception) {
            return RegionResult.failure(exception.getMessage());
        }

        arena.regions.put(regionId, region);
        arena.touch();

        if (!save(arena)) {
            arena.regions.remove(regionId);

            return RegionResult.failure(
                    "La région n'a pas pu être enregistrée."
            );
        }

        return RegionResult.success(region);
    }
    public synchronized RegionResult redefineRegion(
            ArenaDefinition arena,
            String requestedId,
            ArenaSelectionManager.Selection selection
    ) {
        String regionId = normalizeId(requestedId);
        ArenaRegion current = arena.regions.get(regionId);

        if (current == null) {
            return RegionResult.failure(
                    "Cette région n'existe pas."
            );
        }

        if (!selection.complete()) {
            return RegionResult.failure(
                    "La sélection est incomplète ou invalide."
            );
        }

        ArenaRegionType type = current.resolvedType();

        if (type == null) {
            return RegionResult.failure(
                    "Le type de cette région est invalide."
            );
        }

        ArenaRegion replacement;

        try {
            replacement = new ArenaRegion(
                    regionId,
                    type,
                    selection.first(),
                    selection.second()
            );
        } catch (IllegalArgumentException exception) {
            return RegionResult.failure(exception.getMessage());
        }

        arena.regions.put(regionId, replacement);
        arena.touch();

        if (!save(arena)) {
            arena.regions.put(regionId, current);
            return RegionResult.failure(
                    "La nouvelle région n'a pas pu être enregistrée."
            );
        }

        return RegionResult.success(replacement);
    }
    public synchronized boolean removeRegion(
            ArenaDefinition arena,
            String requestedId
    ) {
        String regionId = normalizeId(requestedId);
        ArenaRegion removed = arena.regions.remove(regionId);

        if (removed == null) {
            return false;
        }

        arena.touch();

        if (!save(arena)) {
            arena.regions.put(regionId, removed);
            return false;
        }

        return true;
    }
    public synchronized List<String> regionIdsStartingWith(
            ArenaDefinition arena,
            String input
    ) {
        String normalized = normalizeId(input);

        return arena.regions.keySet()
                .stream()
                .filter(id -> id.startsWith(normalized))
                .sorted()
                .toList();
    }
    public synchronized String nextRegionId(
            ArenaDefinition arena,
            ArenaRegionType type,
            RegionRequirement requirement
    ) {
        String base = type.id();

        /*
         * Une région limitée à une seule occurrence
         * utilise directement le nom du type.
         */
        if (requirement != null
                && requirement.maximum() == 1) {
            return base;
        }

        /*
         * Les régions multiples utilisent :
         * island_1, island_2, floor_1...
         */
        int index = 1;

        while (arena.regions.containsKey(
                base + "_" + index
        )) {
            index++;
        }

        return base + "_" + index;
    }
}

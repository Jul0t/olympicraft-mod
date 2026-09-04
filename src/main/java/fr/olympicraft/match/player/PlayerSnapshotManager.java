package fr.olympicraft.match.player;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.config.ConfigPaths;
import fr.olympicraft.config.OlympicraftConfigManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerSnapshotManager {

    private static final DateTimeFormatter ARCHIVE_DATE =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd_HH-mm-ss-SSS"
            );

    private final OlympicraftConfigManager configs;

    private final Map<UUID, PlayerSnapshot> snapshots =
            new LinkedHashMap<>();

    private MinecraftServer server;

    public PlayerSnapshotManager(
            OlympicraftConfigManager configs
    ) {
        this.configs = configs;
    }

    public synchronized void attachServer(
            MinecraftServer server
    ) {
        this.server = server;

        try {
            createDirectories();
            loadPendingSnapshots();
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de charger les snapshots "
                            + "de joueurs en attente.",
                    exception
            );
        }
    }

    public synchronized void detachServer() {
        this.server = null;
    }

    public synchronized SaveResult save(
            ServerPlayer player
    ) {
        if (server == null) {
            return SaveResult.failure(
                    "Le gestionnaire de snapshots "
                            + "n'est pas attaché au serveur."
            );
        }

        UUID playerId = player.getUUID();

        if (snapshots.containsKey(playerId)
                || Files.exists(
                ConfigPaths.pendingPlayerSnapshot(
                        playerId
                )
        )) {
            return SaveResult.failure(
                    "Une sauvegarde existe déjà pour ce joueur."
            );
        }

        PlayerSnapshot snapshot =
                PlayerSnapshot.capture(player);

        try {
            writePending(snapshot);
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible d'enregistrer le snapshot du "
                            + "joueur '{}'.",
                    player.getGameProfile().getName(),
                    exception
            );

            return SaveResult.failure(
                    "La sauvegarde de sécurité n'a pas "
                            + "pu être écrite sur le disque."
            );
        }

        snapshots.put(playerId, snapshot);

        return SaveResult.success(snapshot);
    }

    public synchronized RestoreResult restore(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (server == null || player == null) {
            return RestoreResult.failure(
                    "Le serveur ou le joueur est introuvable."
            );
        }

        UUID playerId =
                player.getUUID();

        PlayerSnapshot snapshot =
                snapshots.get(playerId);

        if (snapshot == null) {
            snapshot = loadPending(playerId);

            if (snapshot != null) {
                snapshots.put(
                        playerId,
                        snapshot
                );
            }
        }

        if (snapshot == null) {
            return RestoreResult.failure(
                    "Aucune sauvegarde n'existe pour ce joueur."
            );
        }

        if (!snapshot.restore(server, player)) {
            return RestoreResult.failure(
                    "La sauvegarde n'a pas pu être restaurée."
            );
        }

        /*
         * À partir d'ici, l'état du joueur est restauré.
         * Le snapshot peut être retiré de la mémoire.
         */
        snapshots.remove(playerId);

        try {
            archivePending(playerId);
        } catch (IOException exception) {
            /*
             * Un problème d'archivage ne doit pas transformer
             * une restauration réussie en échec.
             */
            Olympicraft.LOGGER.error(
                    "L'état du joueur '{}' a été restauré, "
                            + "mais son snapshot n'a pas pu être archivé.",
                    player.getGameProfile().getName(),
                    exception
            );

            try {
                Files.deleteIfExists(
                        ConfigPaths.pendingPlayerSnapshot(
                                playerId
                        )
                );
            } catch (IOException deletionException) {
                Olympicraft.LOGGER.error(
                        "Impossible de supprimer le snapshot "
                                + "déjà restauré du joueur '{}'.",
                        player.getGameProfile().getName(),
                        deletionException
                );
            }
        }

        return RestoreResult.success(
                snapshot
        );
    }

    public synchronized Optional<PlayerSnapshot> find(
            UUID playerId
    ) {
        PlayerSnapshot snapshot =
                snapshots.get(playerId);

        if (snapshot != null) {
            return Optional.of(snapshot);
        }

        snapshot = loadPending(playerId);

        if (snapshot != null) {
            snapshots.put(playerId, snapshot);
        }

        return Optional.ofNullable(snapshot);
    }

    public synchronized boolean contains(
            UUID playerId
    ) {
        return snapshots.containsKey(playerId)
                || Files.exists(
                ConfigPaths.pendingPlayerSnapshot(
                        playerId
                )
        );
    }

    public synchronized boolean discard(
            UUID playerId
    ) {
        PlayerSnapshot removed =
                snapshots.remove(playerId);

        Path pending =
                ConfigPaths.pendingPlayerSnapshot(
                        playerId
                );

        if (Files.notExists(pending)) {
            return removed != null;
        }

        try {
            archivePending(playerId);
            return true;
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible d'archiver le snapshot abandonné "
                            + "du joueur {}.",
                    playerId,
                    exception
            );

            return false;
        }
    }

    public synchronized int size() {
        return snapshots.size();
    }

    public synchronized void clearMemory() {
        snapshots.clear();
    }

    private void createDirectories()
            throws IOException {
        Files.createDirectories(
                ConfigPaths.pendingPlayerSnapshots()
        );

        Files.createDirectories(
                ConfigPaths.playerSnapshotArchives()
        );
    }

    private void loadPendingSnapshots()
            throws IOException {
        snapshots.clear();

        if (Files.notExists(
                ConfigPaths.pendingPlayerSnapshots()
        )) {
            return;
        }

        try (var files = Files.list(
                ConfigPaths.pendingPlayerSnapshots()
        )) {
            for (Path file : files
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .endsWith(".dat")
                    )
                    .toList()) {
                try {
                    CompoundTag root =
                            NbtIo.readCompressed(
                                    file,
                                    NbtAccounter.unlimitedHeap()
                            );

                    PlayerSnapshot snapshot =
                            PlayerSnapshot.load(
                                    root,
                                    server.registryAccess()
                            );

                    snapshots.put(
                            snapshot.playerId(),
                            snapshot
                    );
                } catch (Exception exception) {
                    Olympicraft.LOGGER.error(
                            "Le snapshot '{}' est illisible. "
                                    + "Il est conservé sur disque.",
                            file.toAbsolutePath(),
                            exception
                    );
                }
            }
        }

        Olympicraft.LOGGER.info(
                "{} snapshot(s) de joueur en attente "
                        + "chargé(s).",
                snapshots.size()
        );
    }

    private PlayerSnapshot loadPending(
            UUID playerId
    ) {
        if (server == null) {
            return null;
        }

        Path pending =
                ConfigPaths.pendingPlayerSnapshot(
                        playerId
                );

        if (Files.notExists(pending)) {
            return null;
        }

        try {
            CompoundTag root =
                    NbtIo.readCompressed(
                            pending,
                            NbtAccounter.unlimitedHeap()
                    );

            return PlayerSnapshot.load(
                    root,
                    server.registryAccess()
            );
        } catch (Exception exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de lire le snapshot du joueur {}.",
                    playerId,
                    exception
            );

            return null;
        }
    }

    private void writePending(
            PlayerSnapshot snapshot
    ) throws IOException {
        createDirectories();

        Path destination =
                ConfigPaths.pendingPlayerSnapshot(
                        snapshot.playerId()
                );

        Path temporary =
                destination.resolveSibling(
                        destination.getFileName()
                                + ".tmp"
                );

        CompoundTag root =
                snapshot.save(
                        server.registryAccess()
                );

        NbtIo.writeCompressed(
                root,
                temporary
        );

        try {
            Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException atomicFailure) {
            Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void archivePending(
            UUID playerId
    ) throws IOException {
        Path pending =
                ConfigPaths.pendingPlayerSnapshot(
                        playerId
                );

        if (Files.notExists(pending)) {
            return;
        }

        Path archiveDirectory =
                ConfigPaths
                        .playerSnapshotArchiveDirectory(
                                playerId
                        );

        Files.createDirectories(
                archiveDirectory
        );

        String timestamp =
                LocalDateTime.now()
                        .format(ARCHIVE_DATE);

        Path archive =
                archiveDirectory.resolve(
                        timestamp + ".dat"
                );

        try {
            Files.move(
                    pending,
                    archive,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException atomicFailure) {
            Files.move(
                    pending,
                    archive,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        cleanupArchives(archiveDirectory);
    }

    private void cleanupArchives(
            Path archiveDirectory
    ) throws IOException {
        int limit = Math.max(
                0,
                configs.general()
                        .playerSnapshotArchiveLimit
        );

        if (limit == 0
                || Files.notExists(archiveDirectory)) {
            return;
        }

        try (var files =
                     Files.list(archiveDirectory)) {
            var ordered = files
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .endsWith(".dat")
                    )
                    .sorted()
                    .toList();

            int amountToDelete =
                    ordered.size() - limit;

            for (int index = 0;
                 index < amountToDelete;
                 index++) {
                Files.deleteIfExists(
                        ordered.get(index)
                );
            }
        }
    }

    public record SaveResult(
            boolean successful,
            String error,
            PlayerSnapshot snapshot
    ) {
        public static SaveResult success(
                PlayerSnapshot snapshot
        ) {
            return new SaveResult(
                    true,
                    null,
                    snapshot
            );
        }

        public static SaveResult failure(
                String error
        ) {
            return new SaveResult(
                    false,
                    error,
                    null
            );
        }
    }

    public record RestoreResult(
            boolean successful,
            String error,
            PlayerSnapshot snapshot
    ) {
        public static RestoreResult success(
                PlayerSnapshot snapshot
        ) {
            return new RestoreResult(
                    true,
                    null,
                    snapshot
            );
        }

        public static RestoreResult failure(
                String error
        ) {
            return new RestoreResult(
                    false,
                    error,
                    null
            );
        }
    }
    public synchronized java.util.Set<UUID> playerIds() {
        return java.util.Set.copyOf(
                snapshots.keySet()
        );
    }
}
package fr.olympicraft.config;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.UUID;

public final class ConfigPaths {

    private ConfigPaths() {
    }

    public static Path root() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("olympicraft");
    }

    public static Path general() {
        return root().resolve(
                "general.json5"
        );
    }

    public static Path messages() {
        return root().resolve(
                "messages.json5"
        );
    }

    public static Path games() {
        return root().resolve(
                "games"
        );
    }

    public static Path sumo() {
        return games().resolve(
                "sumo.json5"
        );
    }

    public static Path murderMystery() {
        return games().resolve(
                "murder_mystery.json5"
        );
    }

    public static Path backups() {
        return root().resolve(
                "backups"
        );
    }

    public static Path playerSnapshots() {
        return root().resolve(
                "player-snapshots"
        );
    }

    public static Path pendingPlayerSnapshots() {
        return playerSnapshots().resolve(
                "pending"
        );
    }

    public static Path playerSnapshotArchives() {
        return playerSnapshots().resolve(
                "archives"
        );
    }

    public static Path pendingPlayerSnapshot(
            UUID playerId
    ) {
        return pendingPlayerSnapshots().resolve(
                playerId + ".dat"
        );
    }

    public static Path playerSnapshotArchiveDirectory(
            UUID playerId
    ) {
        return playerSnapshotArchives().resolve(
                playerId.toString()
        );
    }
}
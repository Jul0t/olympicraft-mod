package fr.olympicraft.config;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ConfigPaths {

    private ConfigPaths() {
    }

    public static Path root() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("olympicraft");

    }

    public static Path general() {
        return root().resolve("general.json5");
    }

    public static Path messages() {
        return root().resolve("messages.json5");
    }

    public static Path games() {
        return root().resolve("games");
    }

    public static Path backups() {
        return root().resolve("backups");
    }
}

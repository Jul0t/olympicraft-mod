package fr.olympicraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import fr.olympicraft.Olympicraft;
import fr.olympicraft.config.model.GeneralConfig;
import fr.olympicraft.config.model.MessageConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class OlympicraftConfigManager {

    private static final DateTimeFormatter BACKUP_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private GeneralConfig general = new GeneralConfig();
    private MessageConfig messages = new MessageConfig();

    private boolean loaded;

    public synchronized boolean loadAll() {
        try {
            Files.createDirectories(ConfigPaths.root());
            Files.createDirectories(ConfigPaths.games());
            Files.createDirectories(ConfigPaths.backups());

            general = load(
                    ConfigPaths.general(),
                    GeneralConfig.class,
                    new GeneralConfig()
            );

            messages = load(
                    ConfigPaths.messages(),
                    MessageConfig.class,
                    new MessageConfig()
            );

            general.validate();
            messages.validate();

            saveAllInternal(false);

            loaded = true;

            Olympicraft.LOGGER.info(
                    "Configurations Olympicraft chargées depuis {}.",
                    ConfigPaths.root().toAbsolutePath()
            );

            return true;
        } catch (IOException | JsonParseException exception) {
            loaded = false;

            Olympicraft.LOGGER.error(
                    "Impossible de charger les configurations.",
                    exception
            );

            return false;
        }
    }

    public synchronized boolean saveAll() {
        try {
            saveAllInternal(true);
            loaded = true;
            return true;
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible d'enregistrer les configurations.",
                    exception
            );

            return false;
        }
    }

    private void saveAllInternal(
            boolean createBackup
    ) throws IOException {
        general.validate();
        messages.validate();

        save(
                ConfigPaths.general(),
                general,
                createBackup && general.createBackups
        );

        save(
                ConfigPaths.messages(),
                messages,
                createBackup && general.createBackups
        );
    }

    private <T extends ConfigFile> T load(
            Path path,
            Class<T> type,
            T defaults
    ) throws IOException {
        if (Files.notExists(path)) {
            save(path, defaults, false);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(
                path,
                StandardCharsets.UTF_8
        )) {
            T loadedConfig = gson.fromJson(reader, type);

            if (loadedConfig == null) {
                return defaults;
            }

            loadedConfig.validate();
            return loadedConfig;
        }
    }

    private void save(
            Path path,
            Object value,
            boolean createBackup
    ) throws IOException {
        Files.createDirectories(path.getParent());

        if (createBackup && Files.exists(path)) {
            backup(path);
        }

        Path temporary = path.resolveSibling(
                path.getFileName() + ".tmp"
        );

        try (Writer writer = Files.newBufferedWriter(
                temporary,
                StandardCharsets.UTF_8
        )) {
            gson.toJson(value, writer);
        }

        try {
            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException atomicFailure) {
            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void backup(Path source) throws IOException {
        String timestamp = LocalDateTime.now()
                .format(BACKUP_DATE);

        Path backupDirectory = ConfigPaths.backups()
                .resolve(timestamp);

        Files.createDirectories(backupDirectory);

        Files.copy(
                source,
                backupDirectory.resolve(source.getFileName()),
                StandardCopyOption.REPLACE_EXISTING
        );

        cleanupOldBackups();
    }

    private void cleanupOldBackups() throws IOException {
        int limit = Math.max(0, general.backupLimit);

        if (limit == 0 || Files.notExists(ConfigPaths.backups())) {
            return;
        }

        try (var directories = Files.list(ConfigPaths.backups())) {
            var ordered = directories
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();

            int amountToDelete = ordered.size() - limit;

            for (int index = 0; index < amountToDelete; index++) {
                deleteDirectory(ordered.get(index));
            }
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths
                    .sorted((first, second) ->
                            second.compareTo(first))
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public GeneralConfig general() {
        return general;
    }

    public MessageConfig messages() {
        return messages;
    }

    public boolean isLoaded() {
        return loaded;
    }
}

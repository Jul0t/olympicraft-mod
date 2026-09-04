package fr.olympicraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.config.model.GeneralConfig;
import fr.olympicraft.config.model.MessageConfig;
import fr.olympicraft.config.model.game.MurderMysteryConfig;
import fr.olympicraft.config.model.game.SumoConfig;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public final class OlympicraftConfigManager {

    private static final DateTimeFormatter BACKUP_DATE =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd_HH-mm-ss"
            );

    private final Gson gson =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

    private GeneralConfig general =
            new GeneralConfig();

    private MessageConfig messages =
            new MessageConfig();

    private SumoConfig sumo =
            new SumoConfig();

    private MurderMysteryConfig murderMystery =
            new MurderMysteryConfig();

    private boolean loaded;

    public synchronized boolean loadAll() {
        try {
            createDirectories();

            general =
                    loadOrCreateGeneral();

            messages =
                    loadOrCreateMessages();

            sumo =
                    loadOrCreateSumo();

            murderMystery =
                    loadOrCreateMurderMystery();

            general.validate();
            messages.validate();
            sumo.validate();
            murderMystery.validate();

            /*
             * Les fichiers existants ne sont pas réécrits lors
             * du chargement. Leurs commentaires JSON5 restent
             * donc intacts.
             */
            loaded = true;

            Olympicraft.LOGGER.info(
                    "Configurations Olympicraft chargées depuis {}.",
                    ConfigPaths.root()
                            .toAbsolutePath()
            );

            return true;
        } catch (IOException
                 | JsonParseException exception) {
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
            createDirectories();

            general.validate();
            messages.validate();
            sumo.validate();
            murderMystery.validate();

            saveGeneral();
            saveMessages();
            saveSumo();
            saveMurderMystery();

            loaded = true;

            return true;
        } catch (IOException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible d'enregistrer "
                            + "les configurations.",
                    exception
            );

            return false;
        }
    }

    private GeneralConfig loadOrCreateGeneral()
            throws IOException {
        Path path =
                ConfigPaths.general();

        if (Files.notExists(path)) {
            GeneralConfig defaults =
                    new GeneralConfig();

            defaults.validate();

            Json5ConfigWriter.writeGeneral(
                    path,
                    defaults
            );

            return defaults;
        }

        return load(
                path,
                GeneralConfig.class,
                new GeneralConfig()
        );
    }

    private MessageConfig loadOrCreateMessages()
            throws IOException {
        Path path =
                ConfigPaths.messages();

        if (Files.notExists(path)) {
            MessageConfig defaults =
                    new MessageConfig();

            defaults.validate();

            Json5ConfigWriter.writeMessages(
                    path,
                    defaults,
                    gson
            );

            return defaults;
        }

        return load(
                path,
                MessageConfig.class,
                new MessageConfig()
        );
    }

    private SumoConfig loadOrCreateSumo()
            throws IOException {
        Path path =
                ConfigPaths.sumo();

        if (Files.notExists(path)) {
            SumoConfig defaults =
                    new SumoConfig();

            defaults.validate();

            Json5ConfigWriter.writeSumo(
                    path,
                    defaults,
                    gson
            );

            return defaults;
        }

        return load(
                path,
                SumoConfig.class,
                new SumoConfig()
        );
    }

    private MurderMysteryConfig
    loadOrCreateMurderMystery()
            throws IOException {
        Path path =
                ConfigPaths.murderMystery();

        if (Files.notExists(path)) {
            MurderMysteryConfig defaults =
                    new MurderMysteryConfig();

            defaults.validate();

            Json5ConfigWriter.writeMurderMystery(
                    path,
                    defaults,
                    gson
            );

            return defaults;
        }

        return load(
                path,
                MurderMysteryConfig.class,
                new MurderMysteryConfig()
        );
    }

    private <T extends ConfigFile> T load(
            Path path,
            Class<T> type,
            T defaults
    ) throws IOException {
        try (Reader reader =
                     Files.newBufferedReader(
                             path,
                             StandardCharsets.UTF_8
                     )) {
            T loadedConfig =
                    gson.fromJson(
                            reader,
                            type
                    );

            if (loadedConfig == null) {
                defaults.validate();
                return defaults;
            }

            loadedConfig.validate();

            return loadedConfig;
        }
    }

    private void saveGeneral()
            throws IOException {
        Path path =
                ConfigPaths.general();

        backupIfEnabled(path);

        Json5ConfigWriter.writeGeneral(
                path,
                general
        );
    }

    private void saveMessages()
            throws IOException {
        Path path =
                ConfigPaths.messages();

        backupIfEnabled(path);

        Json5ConfigWriter.writeMessages(
                path,
                messages,
                gson
        );
    }

    private void saveSumo()
            throws IOException {
        Path path =
                ConfigPaths.sumo();

        backupIfEnabled(path);

        Json5ConfigWriter.writeSumo(
                path,
                sumo,
                gson
        );
    }

    private void saveMurderMystery()
            throws IOException {
        Path path =
                ConfigPaths.murderMystery();

        backupIfEnabled(path);

        Json5ConfigWriter.writeMurderMystery(
                path,
                murderMystery,
                gson
        );
    }

    private void backupIfEnabled(
            Path path
    ) throws IOException {
        if (general.createBackups
                && Files.exists(path)) {
            backup(path);
        }
    }

    private void createDirectories()
            throws IOException {
        Files.createDirectories(
                ConfigPaths.root()
        );

        Files.createDirectories(
                ConfigPaths.games()
        );

        Files.createDirectories(
                ConfigPaths.backups()
        );

        Files.createDirectories(
                ConfigPaths.pendingPlayerSnapshots()
        );

        Files.createDirectories(
                ConfigPaths.playerSnapshotArchives()
        );
    }

    private void backup(
            Path source
    ) throws IOException {
        String timestamp =
                LocalDateTime.now()
                        .format(BACKUP_DATE);

        Path backupDirectory =
                ConfigPaths.backups()
                        .resolve(timestamp);

        Files.createDirectories(
                backupDirectory
        );

        Files.copy(
                source,
                backupDirectory.resolve(
                        source.getFileName()
                ),
                StandardCopyOption.REPLACE_EXISTING
        );

        cleanupOldBackups();
    }

    private void cleanupOldBackups()
            throws IOException {
        int limit =
                Math.max(
                        0,
                        general.backupLimit
                );

        if (limit == 0
                || Files.notExists(
                ConfigPaths.backups()
        )) {
            return;
        }

        try (var directories =
                     Files.list(
                             ConfigPaths.backups()
                     )) {
            var ordered =
                    directories
                            .filter(
                                    Files::isDirectory
                            )
                            .sorted()
                            .toList();

            int amountToDelete =
                    ordered.size() - limit;

            for (int index = 0;
                 index < amountToDelete;
                 index++) {
                deleteDirectory(
                        ordered.get(index)
                );
            }
        }
    }

    private void deleteDirectory(
            Path directory
    ) throws IOException {
        try (var paths =
                     Files.walk(directory)) {
            for (Path path :
                    paths.sorted(
                                    Comparator.reverseOrder()
                            )
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

    public SumoConfig sumo() {
        return sumo;
    }

    public MurderMysteryConfig murderMystery() {
        return murderMystery;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
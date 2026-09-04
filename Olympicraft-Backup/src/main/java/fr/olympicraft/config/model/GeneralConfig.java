package fr.olympicraft.config.model;

import fr.olympicraft.config.ConfigFile;
import fr.olympicraft.internal.BuildDefaults;

public final class GeneralConfig implements ConfigFile {

    public int schemaVersion =
            BuildDefaults.CONFIG_SCHEMA_VERSION;

    public boolean enabled = true;

    public String locale = "fr_fr";

    public boolean testModeAllowed = true;

    public boolean createBackups = true;

    public boolean saveOnModification = true;

    public boolean logConfigurationChanges = true;

    public int backupLimit = 10;

    public int saveIntervalSeconds = 60;

    public Points points = new Points();

    public Gui gui = new Gui();

    public Sessions sessions = new Sessions();

    public static final class Points {

        public boolean enabled = true;

        public boolean allowNegativeScores = false;

        public int decimals = 0;
    }

    public static final class Gui {

        public boolean enabled = true;

        public boolean playSounds = true;

        public boolean confirmDestructiveActions = true;

        public boolean preferEnhancedClientGui = true;
    }

    public static final class Sessions {

        public boolean restoreInventory = true;

        public boolean restorePosition = true;

        public boolean restoreGameMode = true;

        public boolean restoreExperience = true;

        public boolean restoreEffects = true;

        public boolean restoreHealthAndFood = true;

        public int disconnectGraceSeconds = 30;
    }

    @Override
    public int schemaVersion() {
        return schemaVersion;
    }

    @Override
    public void schemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @Override
    public void validate() {
        if (locale == null || locale.isBlank()) {
            locale = "fr_fr";
        }

        backupLimit = Math.max(0, backupLimit);
        saveIntervalSeconds = Math.max(5, saveIntervalSeconds);

        if (points == null) {
            points = new Points();
        }

        if (gui == null) {
            gui = new Gui();
        }

        if (sessions == null) {
            sessions = new Sessions();
        }

        points.decimals = Math.max(
                0,
                Math.min(4, points.decimals)
        );

        sessions.disconnectGraceSeconds = Math.max(
                0,
                sessions.disconnectGraceSeconds
        );
    }
}

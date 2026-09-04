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

    public int playerSnapshotArchiveLimit = 10;

    public Points points = new Points();

    public Gui gui = new Gui();

    public Sessions sessions = new Sessions();

    public ArenaEditor arenaEditor = new ArenaEditor();

    public Dummy dummy = new Dummy();

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

    public static final class ArenaEditor {

        /*
         * Type d'objet utilisé comme baguette.
         * Nous commencerons avec une hache en bois.
         */
        public String wandItem = "minecraft:spectral_arrow";

        public boolean preventBlockBreaking = true;

        public boolean preventBlockUse = true;

        public boolean showSelectionAfterClick = true;

        public int selectionPreviewSeconds = 10;

        public int regionPreviewSeconds = 15;

        public int maximumPreviewSeconds = 300;

        /*
         * Nombre maximal approximatif de particules envoyées
         * pendant chaque actualisation.
         */
        public int maximumParticlesPerRefresh = 500;

        /*
         * Une actualisation toutes les 10 ticks = deux fois
         * par seconde.
         */
        public int refreshIntervalTicks = 10;

        /*
         * Maj + clic gauche prépare l'assistant de création.
         */
        public boolean shiftLeftClickOpensCreationAssistant = true;
    }

    public static final class Dummy {

        public boolean enabled = true;

        /*
         * Recul horizontal appliqué au dummy lorsqu'il est frappé.
         */
        public double horizontalKnockback = 0.85D;

        /*
         * Recul vertical appliqué au dummy.
         */
        public double verticalKnockback = 0.35D;

        /*
         * Multiplicateur supplémentaire lorsque l'attaquant
         * sprinte au moment du coup.
         */
        public double sprintMultiplier = 1.25D;

        public boolean trollOutfitEnabled = true;

        public String helmetItem =
                "minecraft:player_head";

        public String chestItem =
                "minecraft:diamond_chestplate";

        public String legsItem =
                "minecraft:diamond_leggings";

        public String bootsItem =
                "minecraft:diamond_boots";
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

        if (arenaEditor == null) {
            arenaEditor = new ArenaEditor();
        }

        if (dummy == null) {
            dummy = new Dummy();
        }

        dummy.horizontalKnockback =
                Math.clamp(
                        dummy.horizontalKnockback,
                        0.0D,
                        5.0D
                );

        dummy.verticalKnockback =
                Math.clamp(
                        dummy.verticalKnockback,
                        0.0D,
                        2.0D
                );

        dummy.sprintMultiplier =
                Math.clamp(
                        dummy.sprintMultiplier,
                        0.1D,
                        5.0D
                );

        if (dummy.helmetItem == null
                || dummy.helmetItem.isBlank()) {
            dummy.helmetItem =
                    "minecraft:player_head";
        }

        if (dummy.chestItem == null
                || dummy.chestItem.isBlank()) {
            dummy.chestItem =
                    "minecraft:diamond_chestplate";
        }

        if (dummy.legsItem == null
                || dummy.legsItem.isBlank()) {
            dummy.legsItem =
                    "minecraft:diamond_leggings";
        }

        if (dummy.bootsItem == null
                || dummy.bootsItem.isBlank()) {
            dummy.bootsItem =
                    "minecraft:diamond_boots";
        }

        if (arenaEditor.wandItem == null
                || arenaEditor.wandItem.isBlank()) {
            arenaEditor.wandItem =
                    "minecraft:spectral_arrow";
        }

        arenaEditor.selectionPreviewSeconds =
                Math.clamp(
                        arenaEditor.selectionPreviewSeconds
                        ,
                        1,
                        300);

        arenaEditor.regionPreviewSeconds =
                Math.clamp(
                        arenaEditor.regionPreviewSeconds
                        ,
                        1,
                        300);

        arenaEditor.maximumPreviewSeconds =
                Math.clamp(
                        arenaEditor.maximumPreviewSeconds
                        ,
                        10,
                        3600);

        arenaEditor.maximumParticlesPerRefresh =
                Math.clamp(
                        arenaEditor.maximumParticlesPerRefresh
                        ,
                        24,
                        5000);

        arenaEditor.refreshIntervalTicks =
                Math.clamp(
                        arenaEditor.refreshIntervalTicks
                        ,
                        1,
                        100);

        points.decimals = Math.clamp(points.decimals,
                0, 4);

        sessions.disconnectGraceSeconds = Math.max(
                0,
                sessions.disconnectGraceSeconds
        );

        playerSnapshotArchiveLimit =
                Math.clamp(
                        playerSnapshotArchiveLimit,
                        0,
                        100
                );

    }
}

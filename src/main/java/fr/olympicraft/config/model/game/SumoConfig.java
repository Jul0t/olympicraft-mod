package fr.olympicraft.config.model.game;

import fr.olympicraft.config.ConfigFile;
import fr.olympicraft.internal.BuildDefaults;

import java.util.ArrayList;
import java.util.List;

public final class SumoConfig implements ConfigFile {

    public int schemaVersion =
            BuildDefaults.CONFIG_SCHEMA_VERSION;

    public boolean enabled = true;

    public Players players = new Players();
    public Countdown countdown = new Countdown();
    public KitSelection kitSelection =
            new KitSelection();
    public Round round = new Round();
    public Protection protection = new Protection();
    public Dummy dummy = new Dummy();
    public Victory victory = new Victory();

    public List<KitPreset> kitPresets =
            createDefaultKitPresets();

    public static final class KitPreset {

        public String id = "CLASSIC";

        public boolean enabled = true;

        public String displayNameMessageKey =
                "";

        public String descriptionMessageKey =
                "";

        public String iconItem =
                "minecraft:stick";

        public String glassItem =
                "minecraft:blue_stained_glass_pane";

        public List<KitItem> items =
                new ArrayList<>();
    }

    public static final class Players {

        public int minimum = 2;
        public int maximum = 64;
    }

    public static final class Countdown {

        public int seconds = 10;

        public boolean bossbarEnabled = true;
        public boolean soundsEnabled = true;

        public int finalSoundsFromSeconds = 5;

        public String bossbarColor = "blue";
        public String bossbarOverlay = "progress";

        public String countdownSound =
                "minecraft:block.note_block.hat";

        public String finishSound =
                "minecraft:entity.player.levelup";

        public float volume = 0.8F;
        public float pitch = 1.2F;
    }

    public static final class Round {

        public int durationSeconds = 60;

        public boolean overtimeEnabled = true;

        public int overtimeDamageIntervalSeconds = 5;

        public float overtimeDamage = 2.0F;

        public int endingDurationSeconds = 3;

        /*
         * Annonce affichée une seule fois lorsque le temps normal
         * se termine.
         */
        public boolean overtimeAnnouncementEnabled = true;

        /*
         * Son vanilla joué au début de la prolongation.
         */
        public boolean overtimeSoundEnabled = true;

        public String overtimeSound =
                "minecraft:entity.elder_guardian.curse";

        public float overtimeSoundVolume = 1.0F;

        public float overtimeSoundPitch = 1.0F;

        /*
         * Préparation de la musique personnalisée des clients
         * possédant Olympicraft.
         */
        public boolean enhancedClientOvertimeSoundEnabled = true;

        public String enhancedClientOvertimeSound =
                "olympicraft:sumo.overtime";
    }

    public static final class Protection {

        public boolean cancelNormalDamage = true;

        public boolean resistanceEnabled = true;
        public int resistanceAmplifier = 4;

        public boolean preventHunger = true;
        public boolean preventFire = true;
        public boolean preventFallDamage = true;
    }

    public static final class Dummy {

        public boolean enabled = true;

        public double horizontalKnockback = 0.85D;
        public double verticalKnockback = 0.35D;
        public double sprintMultiplier = 1.25D;

        public String defaultName = "Dummy_%index%";

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

    public static final class Victory {

        public boolean titleEnabled = true;
        public boolean defeatTitleEnabled = true;

        public boolean soundsEnabled = true;
        public boolean defeatSoundEnabled = true;

        public boolean particlesEnabled = true;
        public boolean fireworksEnabled = false;

        public int fadeInTicks = 10;
        public int stayTicks = 60;
        public int fadeOutTicks = 20;

        /*
         * Son de secours pour les joueurs vanilla vainqueurs.
         */
        public String victorySound =
                "minecraft:ui.toast.challenge_complete";

        public float victorySoundVolume = 1.0F;
        public float victorySoundPitch = 1.0F;

        /*
         * Son de secours pour le joueur vaincu.
         */
        public String defeatSound =
                "minecraft:entity.wither.death";

        public float defeatSoundVolume = 0.7F;
        public float defeatSoundPitch = 0.7F;

        public boolean enhancedClientMusicEnabled = true;

        public String enhancedClientSound =
                "olympicraft:music.sumo_victory";

        public boolean enhancedClientDefeatMusicEnabled = true;

        public String enhancedClientDefeatSound =
                "olympicraft:music.sumo_defeat";
    }

    public static final class KitItem {

        public String id = "item";

        public boolean enabled = true;

        public String item = "minecraft:stick";

        public int slot = 0;
        public int amount = 1;

        public String nameMessageKey = "";

        public List<String> loreMessageKeys =
                new ArrayList<>();

        public int knockbackLevel = 0;

        public boolean unbreakable = true;

        public boolean preventDrop = true;
        public boolean preventMove = false;
    }

    public static final class KitSelection {

        /*
         * FIXED, RANDOM, VOTE ou PLAYER_CHOICE.
         */
        public String mode = "FIXED";

        /*
         * Preset imposé lorsque le mode est FIXED.
         */
        public String fixedPreset = "CLASSIC";

        /*
         * Preset utilisé lorsqu'un choix est absent
         * ou lorsqu'une résolution DEFAULT est demandée.
         */
        public String defaultPreset = "CLASSIC";

        /*
         * Presets disponibles pour RANDOM, VOTE
         * et PLAYER_CHOICE.
         */
        public List<String> allowedPresets =
                new ArrayList<>(
                        List.of(
                                "CLASSIC",
                                "VANILLA",
                                "CHAOS"
                        )
                );

        /*
         * Durée totale du décompte avant le combat.
         *
         * Cette valeur reprend countdown.seconds.
         * Elle n'est pas dupliquée ici.
         */

        /*
         * Le GUI est fermé lorsque cette durée reste
         * avant le début du combat.
         *
         * Exemple :
         * countdown.seconds = 15
         * closeMenuBeforeStartSeconds = 5
         *
         * Le menu reste donc disponible pendant 10 secondes,
         * puis les cinq dernières secondes servent de marge.
         */
        public int closeMenuBeforeStartSeconds = 5;

        /*
         * RANDOM, CANCEL ou DEFAULT.
         */
        public String tieResolution = "RANDOM";

        /*
         * RANDOM, CANCEL ou DEFAULT.
         */
        public String noVoteResolution = "DEFAULT";

        /*
         * Permet à un joueur de modifier son vote
         * tant que le GUI est encore ouvert.
         */
        public boolean allowVoteChange = true;

        /*
         * Affichage des choix des autres joueurs.
         */
        public boolean showOtherPlayerChoices = true;

        /*
         * Nombre maximal de noms affichés dans
         * la description d'un preset.
         */
        public int maximumDisplayedVoterNames = 8;

        /*
         * En PLAYER_CHOICE, preset attribué aux dummies :
         * DEFAULT, RANDOM ou MOST_SELECTED.
         */
        public String dummyPresetMode = "DEFAULT";
    }

    private static List<KitPreset>
    createDefaultKitPresets() {
        List<KitPreset> presets =
                new ArrayList<>();

        /*
         * ---------------------------------------------------------
         * CLASSIC
         * ---------------------------------------------------------
         */

        KitPreset classic =
                new KitPreset();

        classic.id = "CLASSIC";
        classic.enabled = true;

        classic.displayNameMessageKey =
                "sumo.kit.classic.name";

        classic.descriptionMessageKey =
                "sumo.kit.classic.description";

        classic.iconItem =
                "minecraft:stick";

        classic.glassItem =
                "minecraft:blue_stained_glass_pane";

        KitItem classicStick =
                new KitItem();

        classicStick.id =
                "classic_stick";

        classicStick.item =
                "minecraft:stick";

        classicStick.slot = 0;
        classicStick.amount = 1;

        classicStick.nameMessageKey =
                "sumo.item.knockback_stick.name";

        classicStick.loreMessageKeys =
                new ArrayList<>(
                        List.of(
                                "sumo.item.knockback_stick.lore.1"
                        )
                );

        classicStick.knockbackLevel = 2;
        classicStick.unbreakable = true;
        classicStick.preventDrop = true;
        classicStick.preventMove = false;

        classic.items.add(classicStick);
        presets.add(classic);

        /*
         * ---------------------------------------------------------
         * VANILLA
         * ---------------------------------------------------------
         */

        KitPreset vanilla =
                new KitPreset();

        vanilla.id = "VANILLA";
        vanilla.enabled = true;

        vanilla.displayNameMessageKey =
                "sumo.kit.vanilla.name";

        vanilla.descriptionMessageKey =
                "sumo.kit.vanilla.description";

        vanilla.iconItem =
                "minecraft:air";

        vanilla.glassItem =
                "minecraft:white_stained_glass_pane";

        /*
         * Aucun objet : combat à mains nues.
         */
        presets.add(vanilla);

        /*
         * ---------------------------------------------------------
         * CHAOS
         * ---------------------------------------------------------
         */

        KitPreset chaos =
                new KitPreset();

        chaos.id = "CHAOS";
        chaos.enabled = true;

        chaos.displayNameMessageKey =
                "sumo.kit.chaos.name";

        chaos.descriptionMessageKey =
                "sumo.kit.chaos.description";

        chaos.iconItem =
                "minecraft:wind_charge";

        chaos.glassItem =
                "minecraft:purple_stained_glass_pane";

        KitItem chaosStick =
                new KitItem();

        chaosStick.id =
                "chaos_stick";

        chaosStick.item =
                "minecraft:stick";

        chaosStick.slot = 0;
        chaosStick.amount = 1;

        chaosStick.nameMessageKey =
                "sumo.item.chaos_stick.name";

        chaosStick.loreMessageKeys =
                new ArrayList<>(
                        List.of(
                                "sumo.item.chaos_stick.lore.1"
                        )
                );

        chaosStick.knockbackLevel = 1;
        chaosStick.unbreakable = true;
        chaosStick.preventDrop = true;
        chaosStick.preventMove = false;

        KitItem windCharges =
                new KitItem();

        windCharges.id =
                "wind_charges";

        windCharges.item =
                "minecraft:wind_charge";

        windCharges.slot = 1;
        windCharges.amount = 2;

        windCharges.nameMessageKey =
                "sumo.item.wind_charge.name";

        windCharges.loreMessageKeys =
                new ArrayList<>(
                        List.of(
                                "sumo.item.wind_charge.lore.1"
                        )
                );

        windCharges.knockbackLevel = 0;
        windCharges.unbreakable = false;
        windCharges.preventDrop = true;
        windCharges.preventMove = false;

        chaos.items.add(chaosStick);
        chaos.items.add(windCharges);

        presets.add(chaos);

        return presets;
    }

    @Override
    public int schemaVersion() {
        return schemaVersion;
    }

    @Override
    public void schemaVersion(
            int schemaVersion
    ) {
        this.schemaVersion = schemaVersion;
    }

    @Override
    public void validate() {
        players = players == null
                ? new Players()
                : players;

        countdown = countdown == null
                ? new Countdown()
                : countdown;

        kitSelection = kitSelection == null
                ? new KitSelection()
                : kitSelection;

        round = round == null
                ? new Round()
                : round;

        protection = protection == null
                ? new Protection()
                : protection;

        dummy = dummy == null
                ? new Dummy()
                : dummy;

        victory = victory == null
                ? new Victory()
                : victory;

        if (kitPresets == null
                || kitPresets.isEmpty()) {
            kitPresets =
                    createDefaultKitPresets();
        }

        players.minimum = Math.clamp(
                players.minimum,
                1,
                64
        );

        players.maximum = Math.clamp(
                players.maximum,
                players.minimum,
                256
        );

        countdown.seconds = Math.clamp(
                countdown.seconds,
                0,
                300
        );

        countdown.finalSoundsFromSeconds =
                Math.clamp(
                        countdown.finalSoundsFromSeconds,
                        0,
                        Math.max(
                                countdown.seconds,
                                1
                        )
                );

        countdown.volume = Math.clamp(
                countdown.volume,
                0.0F,
                4.0F
        );

        countdown.pitch = Math.clamp(
                countdown.pitch,
                0.1F,
                2.0F
        );

        kitSelection.mode =
                normalizeSelectionMode(
                        kitSelection.mode
                );

        kitSelection.fixedPreset =
                normalizePresetId(
                        kitSelection.fixedPreset,
                        "CLASSIC"
                );

        kitSelection.defaultPreset =
                normalizePresetId(
                        kitSelection.defaultPreset,
                        "CLASSIC"
                );

        kitSelection.closeMenuBeforeStartSeconds =
                Math.clamp(
                        kitSelection
                                .closeMenuBeforeStartSeconds,
                        0,
                        countdown.seconds
                );

        kitSelection.maximumDisplayedVoterNames =
                Math.clamp(
                        kitSelection
                                .maximumDisplayedVoterNames,
                        0,
                        100
                );

        kitSelection.tieResolution =
                normalizeResolution(
                        kitSelection.tieResolution
                );

        kitSelection.noVoteResolution =
                normalizeResolution(
                        kitSelection.noVoteResolution
                );

        if (kitSelection.allowedPresets == null
                || kitSelection.allowedPresets.isEmpty()) {
            kitSelection.allowedPresets =
                    new ArrayList<>(
                            List.of(
                                    "CLASSIC",
                                    "VANILLA",
                                    "CHAOS"
                            )
                    );
        } else {
            List<String> normalizedPresets =
                    new ArrayList<>();

            for (String preset :
                    kitSelection.allowedPresets) {
                String normalized =
                        normalizePresetId(
                                preset,
                                ""
                        );

                if (!normalized.isBlank()
                        && !normalizedPresets
                        .contains(normalized)) {
                    normalizedPresets.add(
                            normalized
                    );
                }
            }

            if (normalizedPresets.isEmpty()) {
                normalizedPresets.add(
                        "CLASSIC"
                );
            }

            kitSelection.allowedPresets =
                    normalizedPresets;
        }

        for (int presetIndex = 0;
             presetIndex < kitPresets.size();
             presetIndex++) {
            KitPreset preset =
                    kitPresets.get(presetIndex);

            if (preset == null) {
                preset = new KitPreset();

                preset.id =
                        "PRESET_" + presetIndex;

                kitPresets.set(
                        presetIndex,
                        preset
                );
            }

            preset.id =
                    normalizePresetId(
                            preset.id,
                            "PRESET_" + presetIndex
                    );

            preset.iconItem =
                    validItemId(
                            preset.iconItem,
                            "minecraft:stick"
                    );

            preset.glassItem =
                    validItemId(
                            preset.glassItem,
                            "minecraft:gray_stained_glass_pane"
                    );

            if (preset.displayNameMessageKey == null) {
                preset.displayNameMessageKey = "";
            }

            if (preset.descriptionMessageKey == null) {
                preset.descriptionMessageKey = "";
            }

            if (preset.items == null) {
                preset.items =
                        new ArrayList<>();
            }

            for (int itemIndex = 0;
                 itemIndex < preset.items.size();
                 itemIndex++) {
                KitItem item =
                        preset.items.get(itemIndex);

                if (item == null) {
                    item = new KitItem();

                    preset.items.set(
                            itemIndex,
                            item
                    );
                }

                validateKitItem(
                        item,
                        presetIndex,
                        itemIndex
                );
            }
        }

        round.durationSeconds = Math.clamp(
                round.durationSeconds,
                10,
                3600
        );

        round.overtimeDamageIntervalSeconds =
                Math.clamp(
                        round.overtimeDamageIntervalSeconds,
                        1,
                        60
                );

        round.overtimeDamage = Math.clamp(
                round.overtimeDamage,
                0.0F,
                100.0F
        );

        if (round.overtimeSound == null
                || round.overtimeSound.isBlank()) {
            round.overtimeSound =
                    "minecraft:entity.elder_guardian.curse";
        }

        round.overtimeSoundVolume =
                Math.clamp(
                        round.overtimeSoundVolume,
                        0.0F,
                        4.0F
                );

        round.overtimeSoundPitch =
                Math.clamp(
                        round.overtimeSoundPitch,
                        0.1F,
                        2.0F
                );

        if (round.enhancedClientOvertimeSound == null
                || round.enhancedClientOvertimeSound.isBlank()) {
            round.enhancedClientOvertimeSound =
                    "olympicraft:sumo.overtime";
        }

        round.endingDurationSeconds =
                Math.clamp(
                        round.endingDurationSeconds,
                        0,
                        60
                );

        protection.resistanceAmplifier =
                Math.clamp(
                        protection.resistanceAmplifier,
                        0,
                        255
                );

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

        if (dummy.defaultName == null
                || dummy.defaultName.isBlank()) {
            dummy.defaultName =
                    "Dummy_%index%";
        }

        dummy.helmetItem = validItemId(
                dummy.helmetItem,
                "minecraft:player_head"
        );

        dummy.chestItem = validItemId(
                dummy.chestItem,
                "minecraft:diamond_chestplate"
        );

        dummy.legsItem = validItemId(
                dummy.legsItem,
                "minecraft:diamond_leggings"
        );

        dummy.bootsItem = validItemId(
                dummy.bootsItem,
                "minecraft:diamond_boots"
        );

        victory.fadeInTicks = Math.clamp(
                victory.fadeInTicks,
                0,
                200
        );

        victory.stayTicks = Math.clamp(
                victory.stayTicks,
                1,
                1200
        );

        victory.fadeOutTicks = Math.clamp(
                victory.fadeOutTicks,
                0,
                200
        );

        if (victory.enhancedClientSound == null
                || victory.enhancedClientSound.isBlank()) {
            victory.enhancedClientSound =
                    "olympicraft:music.sumo_victory";
        }

        if (victory.victorySound == null
                || victory.victorySound.isBlank()) {
            victory.victorySound =
                    "minecraft:ui.toast.challenge_complete";
        }

        if (victory.defeatSound == null
                || victory.defeatSound.isBlank()) {
            victory.defeatSound =
                    "minecraft:entity.wither.death";
        }

        victory.victorySoundVolume =
                Math.clamp(
                        victory.victorySoundVolume,
                        0.0F,
                        4.0F
                );

        victory.victorySoundPitch =
                Math.clamp(
                        victory.victorySoundPitch,
                        0.1F,
                        2.0F
                );

        victory.defeatSoundVolume =
                Math.clamp(
                        victory.defeatSoundVolume,
                        0.0F,
                        4.0F
                );

        victory.defeatSoundPitch =
                Math.clamp(
                        victory.defeatSoundPitch,
                        0.1F,
                        2.0F
                );

        if (victory.enhancedClientDefeatSound == null
                || victory.enhancedClientDefeatSound.isBlank()) {
            victory.enhancedClientDefeatSound =
                    "olympicraft:music.sumo_defeat";
        }
    }

    private static String validItemId(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
    private static void validateKitItem(
            KitItem item,
            int presetIndex,
            int itemIndex
    ) {
        item.id = item.id == null
                || item.id.isBlank()
                ? "item_"
                + presetIndex
                + "_"
                + itemIndex
                : item.id.trim();

        item.item = validItemId(
                item.item,
                "minecraft:stick"
        );

        item.slot = Math.clamp(
                item.slot,
                0,
                40
        );

        item.amount = Math.clamp(
                item.amount,
                1,
                99
        );

        item.knockbackLevel =
                Math.clamp(
                        item.knockbackLevel,
                        0,
                        10
                );

        if (item.nameMessageKey == null) {
            item.nameMessageKey = "";
        }

        if (item.loreMessageKeys == null) {
            item.loreMessageKeys =
                    new ArrayList<>();
        }
    }

    private static String normalizeSelectionMode(
            String value
    ) {
        if (value == null) {
            return "FIXED";
        }

        return switch (value.trim()
                .toUpperCase()) {
            case "RANDOM" -> "RANDOM";
            case "VOTE" -> "VOTE";
            case "PLAYER_CHOICE" ->
                    "PLAYER_CHOICE";
            default -> "FIXED";
        };
    }

    private static String normalizeResolution(
            String value
    ) {
        if (value == null) {
            return "DEFAULT";
        }

        return switch (value.trim()
                .toUpperCase()) {
            case "RANDOM" -> "RANDOM";
            case "CANCEL" -> "CANCEL";
            default -> "DEFAULT";
        };
    }

    private static String normalizePresetId(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim()
                .toUpperCase();
    }
}
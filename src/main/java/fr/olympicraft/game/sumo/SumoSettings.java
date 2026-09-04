package fr.olympicraft.game.sumo;

import fr.olympicraft.config.model.game.SumoConfig;

import java.util.List;

public final class SumoSettings {

    private final SumoConfig config;

    public SumoSettings(
            SumoConfig config
    ) {
        this.config = config == null
                ? new SumoConfig()
                : config;

        this.config.validate();
    }

    public SumoConfig config() {
        return config;
    }

    /*
     * -----------------------------------------------------------------
     * Joueurs
     * -----------------------------------------------------------------
     */

    public int minimumPlayers() {
        return config.players.minimum;
    }

    public int maximumPlayers() {
        return config.players.maximum;
    }

    /*
     * -----------------------------------------------------------------
     * Compte à rebours
     * -----------------------------------------------------------------
     */

    public int countdownSeconds() {
        return config.countdown.seconds;
    }

    /*
     * -----------------------------------------------------------------
     * Manche et prolongation
     * -----------------------------------------------------------------
     */

    public int roundDurationSeconds() {
        return config.round.durationSeconds;
    }

    public boolean overtimeEnabled() {
        return config.round.overtimeEnabled;
    }

    public int overtimeDamageIntervalSeconds() {
        return config.round
                .overtimeDamageIntervalSeconds;
    }

    public float overtimeDamage() {
        return config.round.overtimeDamage;
    }

    public boolean overtimeAnnouncementEnabled() {
        return config.round
                .overtimeAnnouncementEnabled;
    }

    public boolean overtimeSoundEnabled() {
        return config.round
                .overtimeSoundEnabled;
    }

    public String overtimeSound() {
        return config.round.overtimeSound;
    }

    public float overtimeSoundVolume() {
        return config.round
                .overtimeSoundVolume;
    }

    public float overtimeSoundPitch() {
        return config.round
                .overtimeSoundPitch;
    }

    public int endingDurationSeconds() {
        return config.round
                .endingDurationSeconds;
    }

    /*
     * -----------------------------------------------------------------
     * Protection
     * -----------------------------------------------------------------
     */

    public boolean cancelNormalDamage() {
        return config.protection
                .cancelNormalDamage;
    }

    public boolean resistanceEnabled() {
        return config.protection
                .resistanceEnabled;
    }

    public int resistanceAmplifier() {
        return config.protection
                .resistanceAmplifier;
    }

    public boolean preventHunger() {
        return config.protection
                .preventHunger;
    }

    public boolean preventFire() {
        return config.protection
                .preventFire;
    }

    public boolean preventFallDamage() {
        return config.protection
                .preventFallDamage;
    }

    /*
     * -----------------------------------------------------------------
     * Dummy
     * -----------------------------------------------------------------
     */

    public SumoConfig.Dummy dummy() {
        return config.dummy;
    }

    /*
     * -----------------------------------------------------------------
     * Kit
     * -----------------------------------------------------------------
     */

    public SumoConfig.KitSelection kitSelection() {
        return config.kitSelection;
    }

    public String kitSelectionMode() {
        return config.kitSelection.mode;
    }

    public int closeMenuBeforeStartSeconds() {
        return config.kitSelection
                .closeMenuBeforeStartSeconds;
    }

    public List<SumoConfig.KitPreset>
    enabledKitPresets() {
        return config.kitPresets
                .stream()
                .filter(preset -> preset != null)
                .filter(preset -> preset.enabled)
                .toList();
    }

    public SumoConfig.KitPreset findKitPreset(
            String requestedId
    ) {
        if (requestedId == null
                || requestedId.isBlank()) {
            return null;
        }

        return enabledKitPresets()
                .stream()
                .filter(preset ->
                        preset.id.equalsIgnoreCase(
                                requestedId
                        )
                )
                .findFirst()
                .orElse(null);
    }

    public SumoConfig.KitPreset defaultKitPreset() {
        SumoConfig.KitPreset selected =
                findKitPreset(
                        config.kitSelection
                                .defaultPreset
                );

        if (selected != null) {
            return selected;
        }

        return enabledKitPresets()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public SumoConfig.KitPreset fixedKitPreset() {
        SumoConfig.KitPreset selected =
                findKitPreset(
                        config.kitSelection
                                .fixedPreset
                );

        if (selected != null) {
            return selected;
        }

        return defaultKitPreset();
    }

    /*
     * -----------------------------------------------------------------
     * Victoire et défaite
     * -----------------------------------------------------------------
     */

    public SumoConfig.Victory victory() {
        return config.victory;
    }
}
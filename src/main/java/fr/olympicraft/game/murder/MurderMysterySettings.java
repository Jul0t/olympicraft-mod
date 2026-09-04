package fr.olympicraft.game.murder;

import fr.olympicraft.config.model.game.MurderMysteryConfig;

public final class MurderMysterySettings {

    private final MurderMysteryConfig config;

    public MurderMysterySettings(
            MurderMysteryConfig config
    ) {
        this.config = config == null
                ? new MurderMysteryConfig()
                : config;

        this.config.validate();
    }

    public MurderMysteryConfig config() {
        return config;
    }

    public boolean enabled() {
        return config.enabled;
    }

    public int minimumPlayers() {
        return config.players.minimum;
    }

    public int maximumPlayers() {
        return config.players.maximum;
    }

    public boolean allowDummies() {
        return config.players.allowDummies;
    }

    public int countdownSeconds() {
        return config.countdown.seconds;
    }

    public int roundDurationSeconds() {
        return config.round.durationSeconds;
    }

    public int preparationSeconds() {
        return config.round.preparationSeconds;
    }

    public int endingDurationSeconds() {
        return config.round.endingDurationSeconds;
    }

    public boolean endWhenMurdererDies() {
        return config.round.endWhenMurdererDies;
    }

    public boolean murdererWinsAtTimeLimit() {
        return config.round.murdererWinsAtTimeLimit;
    }

    public int murdererAmount() {
        return config.roles.murdererAmount;
    }

    public int detectiveAmount() {
        return config.detective.enabled
                ? config.roles.detectiveAmount
                : 0;
    }

    public boolean troublemakerEnabled() {
        return config.troublemaker.enabled;
    }

    public int troublemakerChancePercent() {
        return config.troublemaker
                .appearanceChancePercent;
    }

    public int troublemakerMinimumPlayers() {
        return config.troublemaker.minimumPlayers;
    }

    public boolean announceTroublemakerPresence() {
        return config.troublemaker
                .announcePresence;
    }

    public boolean normalDamageEnabled() {
        return config.combat.normalDamageEnabled;
    }
    ///////////////////////
    // Chat de proximité //
    ///////////////////////

    public boolean proximityChatEnabled() {
        return config.proximityChat.enabled;
    }

    public double proximityChatHorizontalRange() {
        return config.proximityChat
                .horizontalRange;
    }

    public double proximityChatVerticalRange() {
        return config.proximityChat
                .verticalRange;
    }

    public boolean proximityChatRequireSameDimension() {
        return config.proximityChat
                .requireSameDimension;
    }

    public boolean spectatorsCanHearLiving() {
        return config.proximityChat
                .spectatorsCanHearLiving;
    }

    public boolean deadPlayersCanTalkTogether() {
        return config.proximityChat
                .deadPlayersCanTalkTogether;
    }
}
package fr.olympicraft.config.model.game;

import fr.olympicraft.config.ConfigFile;
import fr.olympicraft.internal.BuildDefaults;

import java.util.ArrayList;
import java.util.List;

public final class MurderMysteryConfig
        implements ConfigFile {

    public int schemaVersion =
            BuildDefaults.CONFIG_SCHEMA_VERSION;

    public boolean enabled = true;

    public Players players =
            new Players();

    public Countdown countdown =
            new Countdown();

    public Round round =
            new Round();

    public Roles roles =
            new Roles();

    public Anonymity anonymity =
            new Anonymity();

    public ProximityChat proximityChat =
            new ProximityChat();

    public Investigation investigation =
            new Investigation();

    public Combat combat =
            new Combat();

    public Detective detective =
            new Detective();

    public Murderer murderer =
            new Murderer();

    public Troublemaker troublemaker =
            new Troublemaker();

    public Ranked ranked =
            new Ranked();

    ///////////////////////
    // Joueurs           //
    ///////////////////////

    public static final class Players {

        public int minimum = 4;

        public int maximum = 24;

        /*
         * Les dummies pourront servir aux tests techniques,
         * mais seront refusés plus tard dans les parties ranked.
         */
        public boolean allowDummies = true;
    }

    ///////////////////////
    // Compte à rebours  //
    ///////////////////////

    public static final class Countdown {

        public int seconds = 15;

        public boolean bossbarEnabled = true;

        public boolean soundsEnabled = true;

        public int finalSoundsFromSeconds = 5;

        public String bossbarColor = "purple";

        public String bossbarOverlay = "progress";

        public String countdownSound =
                "minecraft:block.note_block.hat";

        public String finishSound =
                "minecraft:entity.player.levelup";

        public float volume = 0.8F;

        public float pitch = 1.0F;
    }

    ///////////////////////
    // Partie            //
    ///////////////////////

    public static final class Round {

        /*
         * Trente minutes par défaut.
         */
        public int durationSeconds = 1800;

        /*
         * Durée pendant laquelle les joueurs peuvent explorer
         * avant que le Meurtrier puisse attaquer.
         */
        public int preparationSeconds = 90;

        /*
         * Durée de l'écran de fin avant la restauration
         * des joueurs.
         */
        public int endingDurationSeconds = 8;

        public boolean endWhenMurdererDies = true;

        public boolean murdererWinsAtTimeLimit = true;

        public boolean announceRemainingTime = true;

        public List<Integer> announcedRemainingSeconds =
                new ArrayList<>(
                        List.of(
                                900,
                                600,
                                300,
                                120,
                                60,
                                30,
                                10
                        )
                );
    }

    ///////////////////////
    // Rôles             //
    ///////////////////////

    public static final class Roles {

        public int murdererAmount = 1;

        public int detectiveAmount = 1;

        /*
         * Les joueurs qui ne reçoivent aucun rôle spécial
         * deviennent automatiquement Innocents.
         */
        public boolean revealRoleOnDeath = false;

        public boolean revealMurdererAtEnd = true;
    }

    ///////////////////////
    // Anonymisation     //
    ///////////////////////

    public static final class Anonymity {

        public boolean enabled = true;

        public boolean replacePlayerNames = true;

        public boolean replaceSkins = true;

        public boolean replaceTabNames = true;

        public boolean replaceChatNames = true;

        public boolean hideRealNamesInMessages = true;

        public boolean randomizeEveryMatch = true;

        public boolean useNeutralAliasesOnly = true;

        public List<String> aliases =
                new ArrayList<>(
                        List.of(
                                "Alex",
                                "Alix",
                                "Andrea",
                                "Camille",
                                "Charlie",
                                "Claude",
                                "Dominique",
                                "Eden",
                                "Lou",
                                "Morgan",
                                "Noa",
                                "Sacha",
                                "Sam",
                                "Val",
                                "Yaël",
                                "Avery",
                                "Billie",
                                "Casey",
                                "Dana",
                                "Emery",
                                "Jamie",
                                "Jordan",
                                "Robin",
                                "Taylor"
                        )
                );
    }

    ///////////////////////
    // Chat de proximité //
    ///////////////////////

    public static final class ProximityChat {

        public boolean enabled = true;

        public double horizontalRange = 14.0D;

        public double verticalRange = 8.0D;

        public boolean requireSameDimension = true;

        public boolean spectatorsCanHearLiving = false;

        public boolean deadPlayersCanTalkTogether = true;
    }

    ///////////////////////
    // Enquête           //
    ///////////////////////

    public static final class Investigation {

        public boolean enabled = true;

        public double corpseDetectionRange = 2.5D;

        public int requiredSeconds = 5;

        public boolean interruptWhenMoving = true;

        public boolean interruptWhenDamaged = true;

        public boolean cluesBecomeLessPrecise = true;

        public int recentCorpseSeconds = 60;

        public int oldCorpseSeconds = 300;

        public boolean differentTroublemakerClues = true;

        public boolean revealRolesDirectly = false;
    }

    ///////////////////////
    // Combat            //
    ///////////////////////

    public static final class Combat {

        public boolean normalDamageEnabled = false;

        public boolean fallDamageEnabled = false;

        public boolean fireDamageEnabled = false;

        public boolean hungerEnabled = false;

        /*
         * Nombre maximal de blessures avant la mort.
         * Cette mécanique sera ajoutée après les cadavres.
         */
        public int maximumWounds = 3;
    }

    ///////////////////////
    // Détective         //
    ///////////////////////

    public static final class Detective {

        public boolean enabled = true;

        public String weaponItem =
                "minecraft:bow";

        public int weaponSlot = 0;

        public int startingArrows = 1;

        public boolean bowDropsOnDeath = false;

        /*
         * L'arc sera trouvé en enquêtant sur le cadavre,
         * et non lâché directement sur le sol.
         */
        public boolean bowDiscoverableOnCorpse = true;

        public int bowDiscoveryChancePercent = 100;

        public boolean finderBecomesDetective = false;

        public boolean wrongShotKillsShooter = true;
    }

    ///////////////////////
    // Meurtrier         //
    ///////////////////////

    public static final class Murderer {

        public String weaponItem =
                "minecraft:iron_sword";

        public int weaponSlot = 0;

        /*
         * La lame n'est pas encore distribuée dans cette
         * première fondation.
         */
        public int attackCooldownSeconds = 12;

        public int initialAttackDelaySeconds = 90;

        public boolean weaponHiddenWhenUnused = true;

        public int weaponVisibleTicks = 10;

        public boolean directAttackOneShots = false;

        public int directAttackWounds = 1;

        public Poison poison =
                new Poison();
    }

    ///////////////////////
    // Poison            //
    ///////////////////////

    public static final class Poison {

        public boolean enabled = true;

        public int cooldownSeconds = 120;

        public int activationDelaySeconds = 30;

        public float damage = 12.0F;

        public boolean canKill = false;

        public boolean victimKnowsImmediately = false;

        public boolean leavesSpecialClues = true;
    }

    ///////////////////////
    // Trouble-fête      //
    ///////////////////////

    public static final class Troublemaker {

        public boolean enabled = true;

        public int appearanceChancePercent = 25;

        public int minimumPlayers = 7;

        public boolean announcePresence = true;

        public boolean identityRemainsSecret = true;

        public boolean canInvestigate = true;

        public boolean useAmbiguousRoleClues = true;

        public boolean mustKillInnocent = true;

        public boolean mustKillDetective = true;

        public boolean mustKillMurderer = true;

        /*
         * Si le Meurtrier meurt avant que les autres objectifs
         * soient remplis, le Trouble-fête perd.
         */
        public boolean losesIfMurdererDiesTooEarly = true;
    }

    ///////////////////////
    // Ranked            //
    ///////////////////////

    public static final class Ranked {

        /*
         * Le ranked n'est pas encore actif.
         * Les valeurs sont préparées pour la suite.
         */
        public boolean enabled = false;

        public boolean enhancedClientRequired = true;

        public int minimumRealPlayers = 7;

        public boolean allowDummies = false;

        public int innocentVictoryPoints = 20;

        public int detectiveVictoryPoints = 25;

        public int murdererVictoryPoints = 35;

        public int troublemakerVictoryPoints = 60;

        public int defeatPoints = -10;

        public int voluntaryLeavePoints = -20;
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

        round = round == null
                ? new Round()
                : round;

        roles = roles == null
                ? new Roles()
                : roles;

        anonymity = anonymity == null
                ? new Anonymity()
                : anonymity;

        proximityChat = proximityChat == null
                ? new ProximityChat()
                : proximityChat;

        investigation = investigation == null
                ? new Investigation()
                : investigation;

        combat = combat == null
                ? new Combat()
                : combat;

        detective = detective == null
                ? new Detective()
                : detective;

        murderer = murderer == null
                ? new Murderer()
                : murderer;

        murderer.poison = murderer.poison == null
                ? new Poison()
                : murderer.poison;

        troublemaker = troublemaker == null
                ? new Troublemaker()
                : troublemaker;

        ranked = ranked == null
                ? new Ranked()
                : ranked;

        players.minimum = Math.clamp(
                players.minimum,
                3,
                64
        );

        players.maximum = Math.clamp(
                players.maximum,
                players.minimum,
                64
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

        round.durationSeconds = Math.clamp(
                round.durationSeconds,
                60,
                7200
        );

        round.preparationSeconds = Math.clamp(
                round.preparationSeconds,
                0,
                Math.max(
                        0,
                        round.durationSeconds - 1
                )
        );

        round.endingDurationSeconds =
                Math.clamp(
                        round.endingDurationSeconds,
                        0,
                        60
                );

        if (round.announcedRemainingSeconds == null) {
            round.announcedRemainingSeconds =
                    new ArrayList<>();
        }

        List<Integer> announcements =
                new ArrayList<>();

        for (Integer seconds :
                round.announcedRemainingSeconds) {
            if (seconds == null) {
                continue;
            }

            int normalized =
                    Math.clamp(
                            seconds,
                            1,
                            round.durationSeconds
                    );

            if (!announcements.contains(normalized)) {
                announcements.add(normalized);
            }
        }

        announcements.sort(
                java.util.Comparator.reverseOrder()
        );

        round.announcedRemainingSeconds =
                announcements;

        roles.murdererAmount = Math.clamp(
                roles.murdererAmount,
                1,
                8
        );

        roles.detectiveAmount = Math.clamp(
                roles.detectiveAmount,
                0,
                8
        );

        if (anonymity.aliases == null
                || anonymity.aliases.isEmpty()) {
            anonymity.aliases =
                    new Anonymity().aliases;
        }

        List<String> aliases =
                new ArrayList<>();

        for (String alias :
                anonymity.aliases) {
            if (alias == null
                    || alias.isBlank()) {
                continue;
            }

            String normalized =
                    alias.trim();

            boolean alreadyPresent =
                    aliases.stream()
                            .anyMatch(value ->
                                    value.equalsIgnoreCase(
                                            normalized
                                    )
                            );

            if (!alreadyPresent) {
                aliases.add(normalized);
            }
        }

        if (aliases.isEmpty()) {
            aliases.addAll(
                    new Anonymity().aliases
            );
        }

        anonymity.aliases = aliases;

        proximityChat.horizontalRange =
                Math.clamp(
                        proximityChat.horizontalRange,
                        1.0D,
                        128.0D
                );

        proximityChat.verticalRange =
                Math.clamp(
                        proximityChat.verticalRange,
                        1.0D,
                        128.0D
                );

        investigation.corpseDetectionRange =
                Math.clamp(
                        investigation.corpseDetectionRange,
                        0.5D,
                        10.0D
                );

        investigation.requiredSeconds =
                Math.clamp(
                        investigation.requiredSeconds,
                        1,
                        60
                );

        investigation.recentCorpseSeconds =
                Math.clamp(
                        investigation.recentCorpseSeconds,
                        1,
                        3600
                );

        investigation.oldCorpseSeconds =
                Math.clamp(
                        investigation.oldCorpseSeconds,
                        investigation.recentCorpseSeconds,
                        7200
                );

        combat.maximumWounds = Math.clamp(
                combat.maximumWounds,
                1,
                10
        );

        detective.weaponItem =
                validItemId(
                        detective.weaponItem,
                        "minecraft:bow"
                );

        detective.weaponSlot = Math.clamp(
                detective.weaponSlot,
                0,
                8
        );

        detective.startingArrows = Math.clamp(
                detective.startingArrows,
                0,
                64
        );

        detective.bowDiscoveryChancePercent =
                Math.clamp(
                        detective
                                .bowDiscoveryChancePercent,
                        0,
                        100
                );

        murderer.weaponItem =
                validItemId(
                        murderer.weaponItem,
                        "minecraft:iron_sword"
                );

        murderer.weaponSlot = Math.clamp(
                murderer.weaponSlot,
                0,
                8
        );

        murderer.attackCooldownSeconds =
                Math.clamp(
                        murderer.attackCooldownSeconds,
                        0,
                        300
                );

        murderer.initialAttackDelaySeconds =
                Math.clamp(
                        murderer.initialAttackDelaySeconds,
                        0,
                        round.durationSeconds
                );

        murderer.weaponVisibleTicks =
                Math.clamp(
                        murderer.weaponVisibleTicks,
                        1,
                        200
                );

        murderer.directAttackWounds =
                Math.clamp(
                        murderer.directAttackWounds,
                        1,
                        combat.maximumWounds
                );

        murderer.poison.cooldownSeconds =
                Math.clamp(
                        murderer.poison.cooldownSeconds,
                        1,
                        600
                );

        murderer.poison.activationDelaySeconds =
                Math.clamp(
                        murderer.poison
                                .activationDelaySeconds,
                        1,
                        600
                );

        murderer.poison.damage =
                Math.clamp(
                        murderer.poison.damage,
                        0.0F,
                        100.0F
                );

        troublemaker.appearanceChancePercent =
                Math.clamp(
                        troublemaker
                                .appearanceChancePercent,
                        0,
                        100
                );

        troublemaker.minimumPlayers =
                Math.clamp(
                        troublemaker.minimumPlayers,
                        3,
                        64
                );

        ranked.minimumRealPlayers =
                Math.clamp(
                        ranked.minimumRealPlayers,
                        2,
                        64
                );
    }

    private static String validItemId(
            String value,
            String fallback
    ) {
        if (value == null
                || value.isBlank()) {
            return fallback;
        }

        return value.trim()
                .toLowerCase();
    }
}
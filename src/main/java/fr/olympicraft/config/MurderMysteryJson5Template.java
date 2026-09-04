package fr.olympicraft.config;

import com.google.gson.Gson;

import fr.olympicraft.config.model.game.MurderMysteryConfig;

public final class MurderMysteryJson5Template {

    private MurderMysteryJson5Template() {
    }

    public static String create(
            MurderMysteryConfig config,
            Gson gson
    ) {
        return """
                {
                  ///////////////////////////
                  // Murder Mystery        //
                  ///////////////////////////

                  "schemaVersion": %d, // Version interne de la configuration.
                  "enabled": %s, // Active ou désactive le Murder Mystery.

                  ///////////////////////////
                  // Joueurs               //
                  ///////////////////////////

                  "players": {
                    "minimum": %d, // Nombre minimal de participants.
                    "maximum": %d, // Nombre maximal de participants.
                    "allowDummies": %s // Autorise les dummies pour les tests non ranked.
                  },

                  ///////////////////////////
                  // Compte à rebours      //
                  ///////////////////////////

                  "countdown": {
                    "seconds": %d, // Durée du compte à rebours avant la partie.
                    "bossbarEnabled": %s, // Affiche la bossbar du compte à rebours.
                    "soundsEnabled": %s, // Active les sons du compte à rebours.
                    "finalSoundsFromSeconds": %d, // Joue un son à partir de ce nombre de secondes.
                    "bossbarColor": %s, // Couleur de la bossbar.
                    "bossbarOverlay": %s, // Apparence de la progression de la bossbar.
                    "countdownSound": %s, // Son joué pendant le compte à rebours.
                    "finishSound": %s, // Son joué au lancement de la partie.
                    "volume": %s, // Volume des sons du compte à rebours.
                    "pitch": %s // Hauteur des sons du compte à rebours.
                  },

                  ///////////////////////////
                  // Partie                //
                  ///////////////////////////

                  "round": {
                    "durationSeconds": %d, // Durée maximale de la partie, 1800 = 30 minutes.
                    "preparationSeconds": %d, // Temps sans attaque au début de la partie.
                    "endingDurationSeconds": %d, // Durée de l'écran de fin.
                    "endWhenMurdererDies": %s, // Termine la partie lorsque le Meurtrier meurt.
                    "murdererWinsAtTimeLimit": %s, // Donne la victoire au Meurtrier à la fin du temps.
                    "announceRemainingTime": %s, // Annonce certains temps restants.
                    "announcedRemainingSeconds": %s // Secondes restantes provoquant une annonce.
                  },

                  ///////////////////////////
                  // Rôles                 //
                  ///////////////////////////

                  "roles": {
                    "murdererAmount": %d, // Nombre de Meurtriers.
                    "detectiveAmount": %d, // Nombre de Détectives.
                    "revealRoleOnDeath": %s, // Révèle ou non le rôle d'un joueur mort.
                    "revealMurdererAtEnd": %s // Révèle le Meurtrier à la fin.
                  },

                  ///////////////////////////
                  // Anonymisation         //
                  ///////////////////////////

                  "anonymity": {
                    "enabled": %s, // Active les identités anonymes.
                    "replacePlayerNames": %s, // Remplace les pseudonymes visibles.
                    "replaceSkins": %s, // Remplace les skins pendant la partie.
                    "replaceTabNames": %s, // Remplace les noms dans le TAB.
                    "replaceChatNames": %s, // Remplace les noms dans le chat.
                    "hideRealNamesInMessages": %s, // Masque les vrais noms dans les messages.
                    "randomizeEveryMatch": %s, // Redistribue les identités à chaque partie.
                    "useNeutralAliasesOnly": %s, // Utilise uniquement des alias neutres.
                    "aliases": %s // Liste des alias pouvant être attribués.
                  },

                  ///////////////////////////
                  // Chat de proximité     //
                  ///////////////////////////

                  "proximityChat": {
                    "enabled": %s, // Active le chat écrit de proximité.
                    "horizontalRange": %s, // Portée horizontale du chat.
                    "verticalRange": %s, // Portée verticale du chat.
                    "requireSameDimension": %s, // Exige que les joueurs soient dans le même monde.
                    "spectatorsCanHearLiving": %s, // Autorise les morts à écouter les vivants.
                    "deadPlayersCanTalkTogether": %s // Autorise un chat séparé entre morts.
                  },

                  ///////////////////////////
                  // Enquête               //
                  ///////////////////////////

                  "investigation": {
                    "enabled": %s, // Active l'enquête autour des cadavres.
                    "corpseDetectionRange": %s, // Distance maximale pour examiner un corps.
                    "requiredSeconds": %d, // Durée nécessaire pour terminer une analyse.
                    "interruptWhenMoving": %s, // Interrompt l'analyse si le joueur s'éloigne.
                    "interruptWhenDamaged": %s, // Interrompt l'analyse si le joueur est blessé.
                    "cluesBecomeLessPrecise": %s, // Réduit la précision des vieux indices.
                    "recentCorpseSeconds": %d, // Durée pendant laquelle un corps est récent.
                    "oldCorpseSeconds": %d, // Durée après laquelle un corps est considéré ancien.
                    "differentTroublemakerClues": %s, // Donne des indices différents au Trouble-fête.
                    "revealRolesDirectly": %s // Autorise un indice à révéler directement un rôle.
                  },

                  ///////////////////////////
                  // Combat                //
                  ///////////////////////////

                  "combat": {
                    "normalDamageEnabled": %s, // Autorise les dégâts Minecraft normaux.
                    "fallDamageEnabled": %s, // Autorise les dégâts de chute.
                    "fireDamageEnabled": %s, // Autorise les dégâts de feu.
                    "hungerEnabled": %s, // Autorise la perte de nourriture.
                    "maximumWounds": %d // Nombre de blessures provoquant la mort.
                  },

                  ///////////////////////////
                  // Détective             //
                  ///////////////////////////

                  "detective": {
                    "enabled": %s, // Active le rôle Détective.
                    "weaponItem": %s, // Objet utilisé comme arme du Détective.
                    "weaponSlot": %d, // Emplacement initial de l'arme.
                    "startingArrows": %d, // Nombre initial de flèches.
                    "bowDropsOnDeath": %s, // Fait tomber directement l'arc sur le sol.
                    "bowDiscoverableOnCorpse": %s, // Permet de trouver l'arc en enquêtant.
                    "bowDiscoveryChancePercent": %d, // Probabilité de découvrir l'arc.
                    "finderBecomesDetective": %s, // Transforme le découvreur en Détective.
                    "wrongShotKillsShooter": %s // Tue le tireur s'il touche un innocent.
                  },

                  ///////////////////////////
                  // Meurtrier             //
                  ///////////////////////////

                  "murderer": {
                    "weaponItem": %s, // Objet utilisé comme lame.
                    "weaponSlot": %d, // Emplacement de la lame.
                    "attackCooldownSeconds": %d, // Cooldown entre deux attaques.
                    "initialAttackDelaySeconds": %d, // Délai avant la première attaque.
                    "weaponHiddenWhenUnused": %s, // Cache la lame lorsqu'elle n'est pas utilisée.
                    "weaponVisibleTicks": %d, // Durée d'affichage de la lame en ticks.
                    "directAttackOneShots": %s, // Autorise une attaque directe à tuer immédiatement.
                    "directAttackWounds": %d, // Blessures causées par une attaque directe.

                    "poison": {
                      "enabled": %s, // Active le poison à retardement.
                      "cooldownSeconds": %d, // Cooldown du poison.
                      "activationDelaySeconds": %d, // Délai avant les effets du poison.
                      "damage": %s, // Vie retirée lors de l'activation.
                      "canKill": %s, // Autorise le poison à tuer directement.
                      "victimKnowsImmediately": %s, // Prévient immédiatement la victime.
                      "leavesSpecialClues": %s // Ajoute des indices chimiques sur le corps.
                    }
                  },

                  ///////////////////////////
                  // Trouble-fête          //
                  ///////////////////////////

                  "troublemaker": {
                    "enabled": %s, // Active le rôle Trouble-fête.
                    "appearanceChancePercent": %d, // Probabilité d'apparition du rôle.
                    "minimumPlayers": %d, // Joueurs nécessaires pour permettre son apparition.
                    "announcePresence": %s, // Annonce sa présence sans révéler son identité.
                    "identityRemainsSecret": %s, // Conserve son identité secrète.
                    "canInvestigate": %s, // Autorise le Trouble-fête à enquêter.
                    "useAmbiguousRoleClues": %s, // Mélange les indices Meurtrier et Détective.
                    "mustKillInnocent": %s, // Exige l'élimination d'un Innocent.
                    "mustKillDetective": %s, // Exige l'élimination du Détective.
                    "mustKillMurderer": %s, // Exige l'élimination du Meurtrier.
                    "losesIfMurdererDiesTooEarly": %s // Fait perdre le rôle si le Meurtrier meurt trop tôt.
                  },

                  ///////////////////////////
                  // Ranked                //
                  ///////////////////////////

                  "ranked": {
                    "enabled": %s, // Active plus tard les parties classées.
                    "enhancedClientRequired": %s, // Exige le client Enhanced en ranked.
                    "minimumRealPlayers": %d, // Nombre minimal de vrais joueurs.
                    "allowDummies": %s, // Autorise les dummies en ranked.
                    "innocentVictoryPoints": %d, // Points pour une victoire Innocent.
                    "detectiveVictoryPoints": %d, // Points pour une victoire Détective.
                    "murdererVictoryPoints": %d, // Points pour une victoire Meurtrier.
                    "troublemakerVictoryPoints": %d, // Points pour une victoire Trouble-fête.
                    "defeatPoints": %d, // Points appliqués après une défaite.
                    "voluntaryLeavePoints": %d // Points appliqués après un abandon.
                  }
                }
                """.formatted(
                config.schemaVersion,
                config.enabled,

                config.players.minimum,
                config.players.maximum,
                config.players.allowDummies,

                config.countdown.seconds,
                config.countdown.bossbarEnabled,
                config.countdown.soundsEnabled,
                config.countdown.finalSoundsFromSeconds,
                quote(config.countdown.bossbarColor),
                quote(config.countdown.bossbarOverlay),
                quote(config.countdown.countdownSound),
                quote(config.countdown.finishSound),
                config.countdown.volume,
                config.countdown.pitch,

                config.round.durationSeconds,
                config.round.preparationSeconds,
                config.round.endingDurationSeconds,
                config.round.endWhenMurdererDies,
                config.round.murdererWinsAtTimeLimit,
                config.round.announceRemainingTime,
                gson.toJson(
                        config.round
                                .announcedRemainingSeconds
                ),

                config.roles.murdererAmount,
                config.roles.detectiveAmount,
                config.roles.revealRoleOnDeath,
                config.roles.revealMurdererAtEnd,

                config.anonymity.enabled,
                config.anonymity.replacePlayerNames,
                config.anonymity.replaceSkins,
                config.anonymity.replaceTabNames,
                config.anonymity.replaceChatNames,
                config.anonymity.hideRealNamesInMessages,
                config.anonymity.randomizeEveryMatch,
                config.anonymity.useNeutralAliasesOnly,
                gson.toJson(config.anonymity.aliases),

                config.proximityChat.enabled,
                config.proximityChat.horizontalRange,
                config.proximityChat.verticalRange,
                config.proximityChat.requireSameDimension,
                config.proximityChat.spectatorsCanHearLiving,
                config.proximityChat.deadPlayersCanTalkTogether,

                config.investigation.enabled,
                config.investigation.corpseDetectionRange,
                config.investigation.requiredSeconds,
                config.investigation.interruptWhenMoving,
                config.investigation.interruptWhenDamaged,
                config.investigation.cluesBecomeLessPrecise,
                config.investigation.recentCorpseSeconds,
                config.investigation.oldCorpseSeconds,
                config.investigation
                        .differentTroublemakerClues,
                config.investigation.revealRolesDirectly,

                config.combat.normalDamageEnabled,
                config.combat.fallDamageEnabled,
                config.combat.fireDamageEnabled,
                config.combat.hungerEnabled,
                config.combat.maximumWounds,

                config.detective.enabled,
                quote(config.detective.weaponItem),
                config.detective.weaponSlot,
                config.detective.startingArrows,
                config.detective.bowDropsOnDeath,
                config.detective.bowDiscoverableOnCorpse,
                config.detective.bowDiscoveryChancePercent,
                config.detective.finderBecomesDetective,
                config.detective.wrongShotKillsShooter,

                quote(config.murderer.weaponItem),
                config.murderer.weaponSlot,
                config.murderer.attackCooldownSeconds,
                config.murderer.initialAttackDelaySeconds,
                config.murderer.weaponHiddenWhenUnused,
                config.murderer.weaponVisibleTicks,
                config.murderer.directAttackOneShots,
                config.murderer.directAttackWounds,

                config.murderer.poison.enabled,
                config.murderer.poison.cooldownSeconds,
                config.murderer.poison
                        .activationDelaySeconds,
                config.murderer.poison.damage,
                config.murderer.poison.canKill,
                config.murderer.poison
                        .victimKnowsImmediately,
                config.murderer.poison
                        .leavesSpecialClues,

                config.troublemaker.enabled,
                config.troublemaker
                        .appearanceChancePercent,
                config.troublemaker.minimumPlayers,
                config.troublemaker.announcePresence,
                config.troublemaker.identityRemainsSecret,
                config.troublemaker.canInvestigate,
                config.troublemaker
                        .useAmbiguousRoleClues,
                config.troublemaker.mustKillInnocent,
                config.troublemaker.mustKillDetective,
                config.troublemaker.mustKillMurderer,
                config.troublemaker
                        .losesIfMurdererDiesTooEarly,

                config.ranked.enabled,
                config.ranked.enhancedClientRequired,
                config.ranked.minimumRealPlayers,
                config.ranked.allowDummies,
                config.ranked.innocentVictoryPoints,
                config.ranked.detectiveVictoryPoints,
                config.ranked.murdererVictoryPoints,
                config.ranked.troublemakerVictoryPoints,
                config.ranked.defeatPoints,
                config.ranked.voluntaryLeavePoints
        );
    }

    private static String quote(
            String value
    ) {
        if (value == null) {
            return "\"\"";
        }

        return '"'
                + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + '"';
    }
}
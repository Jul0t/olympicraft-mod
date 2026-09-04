package fr.olympicraft.config;

import com.google.gson.Gson;
import fr.olympicraft.config.model.game.SumoConfig;

public final class SumoJson5Template {

    private SumoJson5Template() {
    }

    public static String create(
            SumoConfig config,
            Gson gson
    ) {
        StringBuilder result =
                new StringBuilder();

        result.append("""
                {
                  ///////////////////////////
                  // Configuration du Sumo //
                  ///////////////////////////

                  "schemaVersion": %d, // Version interne de la configuration.
                  "enabled": %s, // Active ou désactive le mini-jeu Sumo.

                  ///////////////////////////
                  // Joueurs               //
                  ///////////////////////////

                  "players": {
                    "minimum": %d, // Nombre minimal de combattants.
                    "maximum": %d // Nombre maximal de participants.
                  },

                  ///////////////////////////
                  // Compte à rebours      //
                  ///////////////////////////

                  "countdown": {
                    "seconds": %d, // Durée totale avant le combat.
                    "bossbarEnabled": %s, // Affiche la barre du décompte.
                    "soundsEnabled": %s, // Active les sons du décompte.
                    "finalSoundsFromSeconds": %d, // Joue un son à partir de cette seconde.
                    "bossbarColor": %s, // Couleur : pink, blue, red, green, yellow, purple ou white.
                    "bossbarOverlay": %s, // Style : progress, notched_6, notched_10, notched_12 ou notched_20.
                    "countdownSound": %s, // Son joué pendant le décompte.
                    "finishSound": %s, // Son joué au lancement du combat.
                    "volume": %s, // Volume compris entre 0.0 et 4.0.
                    "pitch": %s // Hauteur comprise entre 0.1 et 2.0.
                  },

                  ///////////////////////////
                  // Sélection des kits    //
                  ///////////////////////////

                  "kitSelection": {
                    // FIXED = kit imposé par fixedPreset.
                    // RANDOM = kit commun choisi au hasard.
                    // VOTE = les joueurs votent pour un kit commun.
                    // PLAYER_CHOICE = chaque joueur choisit son kit.
                    "mode": %s,

                    "fixedPreset": %s, // Kit imposé avec le mode FIXED.
                    "defaultPreset": %s, // Kit utilisé lorsqu'aucun choix n'est disponible.
                """.formatted(
                config.schemaVersion,
                config.enabled,

                config.players.minimum,
                config.players.maximum,

                config.countdown.seconds,
                config.countdown.bossbarEnabled,
                config.countdown.soundsEnabled,
                config.countdown.finalSoundsFromSeconds,
                json(gson, config.countdown.bossbarColor),
                json(gson, config.countdown.bossbarOverlay),
                json(gson, config.countdown.countdownSound),
                json(gson, config.countdown.finishSound),
                config.countdown.volume,
                config.countdown.pitch,

                json(gson, config.kitSelection.mode),
                json(gson, config.kitSelection.fixedPreset),
                json(gson, config.kitSelection.defaultPreset)
        ));

        result.append("""
                    "allowedPresets": %s, // Kits disponibles pour RANDOM, VOTE et PLAYER_CHOICE.
                    "closeMenuBeforeStartSeconds": %d, // Ferme le GUI ce nombre de secondes avant le départ.

                    // RANDOM choisit parmi les kits à égalité.
                    // DEFAULT utilise defaultPreset.
                    // CANCEL annule la partie.
                    "tieResolution": %s,

                    // RANDOM choisit un kit autorisé.
                    // DEFAULT utilise defaultPreset.
                    // CANCEL annule la partie.
                    "noVoteResolution": %s,

                    "allowVoteChange": %s, // Autorise le changement de vote.
                    "showOtherPlayerChoices": %s, // Affiche les choix des autres joueurs.
                    "maximumDisplayedVoterNames": %d, // Nombre maximal de noms affichés.

                    // DEFAULT, RANDOM ou MOST_SELECTED.
                    "dummyPresetMode": %s
                  },

                  ///////////////////////////
                  // Manche et prolongation//
                  ///////////////////////////

                  "round": {
                    "durationSeconds": %d, // Durée normale d'une manche.
                    "overtimeEnabled": %s, // Active la prolongation.
                    "overtimeDamageIntervalSeconds": %d, // Intervalle des dégâts.
                    "overtimeDamage": %s, // Vie retirée à chaque intervalle.
                    "endingDurationSeconds": %d, // Temps d'affichage de la fin.
                    "overtimeAnnouncementEnabled": %s, // Affiche le titre de prolongation.
                    "overtimeSoundEnabled": %s, // Joue le son de prolongation.
                    "overtimeSound": %s, // Son vanilla de prolongation.
                    "overtimeSoundVolume": %s, // Volume du son.
                    "overtimeSoundPitch": %s, // Hauteur du son.
                    "enhancedClientOvertimeSoundEnabled": %s, // Active le son Enhanced.
                    "enhancedClientOvertimeSound": %s // Identifiant du son Enhanced.
                  },

                  ///////////////////////////
                  // Protection            //
                  ///////////////////////////

                  "protection": {
                    "cancelNormalDamage": %s, // Bloque les dégâts normaux.
                    "resistanceEnabled": %s, // Applique Résistance aux combattants.
                    "resistanceAmplifier": %d, // Niveau de Résistance, de 0 à 255.
                    "preventHunger": %s, // Empêche la perte de nourriture.
                    "preventFire": %s, // Empêche le feu.
                    "preventFallDamage": %s // Empêche les dégâts de chute.
                  },

                  ///////////////////////////
                  // Dummies               //
                  ///////////////////////////

                  "dummy": {
                    "enabled": %s, // Active les dummies pour le Sumo.
                    "horizontalKnockback": %s, // Force horizontale du recul.
                    "verticalKnockback": %s, // Force verticale du recul.
                    "sprintMultiplier": %s, // Multiplicateur lorsque l'attaquant sprinte.
                    "defaultName": %s, // Nom automatique ; %%index%% est remplacé.
                    "trollOutfitEnabled": %s, // Active l'apparence de troll.
                    "helmetItem": %s, // Objet placé sur la tête.
                    "chestItem": %s, // Objet placé sur le torse.
                    "legsItem": %s, // Objet placé sur les jambes.
                    "bootsItem": %s // Objet placé sur les pieds.
                  },

                  ///////////////////////////
                  // Victoire et défaite   //
                  ///////////////////////////

                  "victory": {
                    "titleEnabled": %s, // Affiche le titre de victoire.
                    "defeatTitleEnabled": %s, // Affiche le titre de défaite.
                    "soundsEnabled": %s, // Active le son de victoire.
                    "defeatSoundEnabled": %s, // Active le son de défaite.
                    "particlesEnabled": %s, // Active les particules du vainqueur.
                    "fireworksEnabled": %s, // Active les feux d'artifice.
                    "fadeInTicks": %d, // Durée d'apparition du titre en ticks.
                    "stayTicks": %d, // Durée d'affichage du titre en ticks.
                    "fadeOutTicks": %d, // Durée de disparition du titre.
                    "victorySound": %s, // Son vanilla de victoire.
                    "victorySoundVolume": %s, // Volume du son de victoire.
                    "victorySoundPitch": %s, // Hauteur du son de victoire.
                    "defeatSound": %s, // Son vanilla de défaite.
                    "defeatSoundVolume": %s, // Volume du son de défaite.
                    "defeatSoundPitch": %s, // Hauteur du son de défaite.
                    "enhancedClientMusicEnabled": %s, // Active la musique Enhanced.
                    "enhancedClientSound": %s, // Identifiant Enhanced de victoire.
                    "enhancedClientDefeatMusicEnabled": %s, // Active la musique Enhanced de défaite.
                    "enhancedClientDefeatSound": %s // Identifiant Enhanced de défaite.
                  },

                  ///////////////////////////
                  // Presets de kits       //
                  ///////////////////////////

                  "kitPresets": [
                """.formatted(
                gson.toJson(
                        config.kitSelection.allowedPresets
                ),
                config.kitSelection
                        .closeMenuBeforeStartSeconds,
                json(gson, config.kitSelection.tieResolution),
                json(gson, config.kitSelection.noVoteResolution),
                config.kitSelection.allowVoteChange,
                config.kitSelection.showOtherPlayerChoices,
                config.kitSelection
                        .maximumDisplayedVoterNames,
                json(gson, config.kitSelection.dummyPresetMode),

                config.round.durationSeconds,
                config.round.overtimeEnabled,
                config.round
                        .overtimeDamageIntervalSeconds,
                config.round.overtimeDamage,
                config.round.endingDurationSeconds,
                config.round.overtimeAnnouncementEnabled,
                config.round.overtimeSoundEnabled,
                json(gson, config.round.overtimeSound),
                config.round.overtimeSoundVolume,
                config.round.overtimeSoundPitch,
                config.round
                        .enhancedClientOvertimeSoundEnabled,
                json(
                        gson,
                        config.round.enhancedClientOvertimeSound
                ),

                config.protection.cancelNormalDamage,
                config.protection.resistanceEnabled,
                config.protection.resistanceAmplifier,
                config.protection.preventHunger,
                config.protection.preventFire,
                config.protection.preventFallDamage,

                config.dummy.enabled,
                config.dummy.horizontalKnockback,
                config.dummy.verticalKnockback,
                config.dummy.sprintMultiplier,
                json(gson, config.dummy.defaultName),
                config.dummy.trollOutfitEnabled,
                json(gson, config.dummy.helmetItem),
                json(gson, config.dummy.chestItem),
                json(gson, config.dummy.legsItem),
                json(gson, config.dummy.bootsItem),

                config.victory.titleEnabled,
                config.victory.defeatTitleEnabled,
                config.victory.soundsEnabled,
                config.victory.defeatSoundEnabled,
                config.victory.particlesEnabled,
                config.victory.fireworksEnabled,
                config.victory.fadeInTicks,
                config.victory.stayTicks,
                config.victory.fadeOutTicks,
                json(gson, config.victory.victorySound),
                config.victory.victorySoundVolume,
                config.victory.victorySoundPitch,
                json(gson, config.victory.defeatSound),
                config.victory.defeatSoundVolume,
                config.victory.defeatSoundPitch,
                config.victory.enhancedClientMusicEnabled,
                json(gson, config.victory.enhancedClientSound),
                config.victory
                        .enhancedClientDefeatMusicEnabled,
                json(
                        gson,
                        config.victory.enhancedClientDefeatSound
                )
        ));

        appendPresets(result, config, gson);

        result.append("""
                  ]
                }
                """);

        return result.toString();
    }

    private static void appendPresets(
            StringBuilder result,
            SumoConfig config,
            Gson gson
    ) {
        for (int presetIndex = 0;
             presetIndex < config.kitPresets.size();
             presetIndex++) {
            SumoConfig.KitPreset preset =
                    config.kitPresets.get(presetIndex);

            result.append("""
                    {
                      "id": %s, // Identifiant technique du preset.
                      "enabled": %s, // Active ou désactive le preset.
                      "displayNameMessageKey": %s, // Clé du nom dans messages.json5.
                      "descriptionMessageKey": %s, // Clé de la description.
                      "iconItem": %s, // Objet affiché dans le GUI.
                      "glassItem": %s, // Vitre utilisée pour les votes.
                      "items": [
                    """.formatted(
                    json(gson, preset.id),
                    preset.enabled,
                    json(
                            gson,
                            preset.displayNameMessageKey
                    ),
                    json(
                            gson,
                            preset.descriptionMessageKey
                    ),
                    json(gson, preset.iconItem),
                    json(gson, preset.glassItem)
            ));

            appendItems(
                    result,
                    preset,
                    gson
            );

            result.append("      ]\n")
                    .append("    }");

            if (presetIndex + 1
                    < config.kitPresets.size()) {
                result.append(',');
            }

            result.append('\n');
        }
    }

    private static void appendItems(
            StringBuilder result,
            SumoConfig.KitPreset preset,
            Gson gson
    ) {
        for (int itemIndex = 0;
             itemIndex < preset.items.size();
             itemIndex++) {
            SumoConfig.KitItem item =
                    preset.items.get(itemIndex);

            result.append("""
                            {
                              "id": %s, // Identifiant technique de l'objet.
                              "enabled": %s, // Active ou désactive cet objet.
                              "item": %s, // Objet Minecraft réellement donné.
                              "slot": %d, // Emplacement dans l'inventaire.
                              "amount": %d, // Quantité donnée.
                              "nameMessageKey": %s, // Clé du nom dans messages.json5.
                              "loreMessageKeys": %s, // Clés des lignes de description.
                              "knockbackLevel": %d, // Niveau de Recul ; 0 désactive l'enchantement.
                              "unbreakable": %s, // Rend l'objet incassable.
                              "preventDrop": %s, // Empêche de jeter l'objet.
                              "preventMove": %s // Empêche de déplacer l'objet.
                            }
                    """.formatted(
                    json(gson, item.id),
                    item.enabled,
                    json(gson, item.item),
                    item.slot,
                    item.amount,
                    json(gson, item.nameMessageKey),
                    gson.toJson(item.loreMessageKeys),
                    item.knockbackLevel,
                    item.unbreakable,
                    item.preventDrop,
                    item.preventMove
            ));

            if (itemIndex + 1
                    < preset.items.size()) {
                int end = result.length();

                while (end > 0
                        && Character.isWhitespace(
                        result.charAt(end - 1)
                )) {
                    end--;
                }

                result.insert(end, ',');
            }
        }
    }

    private static String json(
            Gson gson,
            String value
    ) {
        return gson.toJson(
                value == null ? "" : value
        );
    }
}

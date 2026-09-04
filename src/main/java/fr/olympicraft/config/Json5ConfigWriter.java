package fr.olympicraft.config;

import com.google.gson.Gson;
import fr.olympicraft.config.model.GeneralConfig;
import fr.olympicraft.config.model.MessageConfig;
import fr.olympicraft.config.model.game.SumoConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Json5ConfigWriter {

    private Json5ConfigWriter() {
    }

    public static void writeGeneral(
            Path path,
            GeneralConfig config
    ) throws IOException {
        String content = """
                {
                  ///////////////////////////
                  // Configuration générale //
                  ///////////////////////////

                  "schemaVersion": %d, // Version interne de la configuration.
                  "enabled": %s, // Active ou désactive Olympicraft.
                  "locale": %s, // Langue utilisée par défaut, par exemple fr_fr.
                  "testModeAllowed": %s, // Autorise l'activation du mode test.
                  "createBackups": %s, // Crée une copie avant chaque sauvegarde.
                  "saveOnModification": %s, // Sauvegarde après une modification.
                  "logConfigurationChanges": %s, // Écrit les changements dans les logs.
                  "backupLimit": %d, // Nombre maximal de dossiers de sauvegarde.
                  "saveIntervalSeconds": %d, // Intervalle de sauvegarde en secondes.
                  "playerSnapshotArchiveLimit": %d, // Archives conservées par joueur.

                  ///////////////////////////
                  // Points                //
                  ///////////////////////////

                  "points": {
                    "enabled": %s, // Active le système de points.
                    "allowNegativeScores": %s, // Autorise les scores négatifs.
                    "decimals": %d // Nombre de décimales, entre 0 et 4.
                  },

                  ///////////////////////////
                  // Interfaces            //
                  ///////////////////////////

                  "gui": {
                    "enabled": %s, // Active les interfaces Olympicraft.
                    "playSounds": %s, // Joue les sons des interfaces.
                    "confirmDestructiveActions": %s, // Demande confirmation avant une suppression.
                    "preferEnhancedClientGui": %s // Préfère le GUI Enhanced si disponible.
                  },

                  ///////////////////////////
                  // Sessions de joueurs   //
                  ///////////////////////////

                  "sessions": {
                    "restoreInventory": %s, // Restaure l'inventaire après une partie.
                    "restorePosition": %s, // Restaure l'ancienne position.
                    "restoreGameMode": %s, // Restaure l'ancien mode de jeu.
                    "restoreExperience": %s, // Restaure l'expérience.
                    "restoreEffects": %s, // Restaure les effets de potion.
                    "restoreHealthAndFood": %s, // Restaure la vie et la nourriture.
                    "disconnectGraceSeconds": %d // Délai de reconnexion en secondes.
                  },

                  ///////////////////////////
                  // Éditeur d'arènes      //
                  ///////////////////////////

                  "arenaEditor": {
                    "wandItem": %s, // Objet utilisé comme baguette de sélection.
                    "preventBlockBreaking": %s, // Bloque la casse avec la baguette.
                    "preventBlockUse": %s, // Bloque l'utilisation normale de la baguette.
                    "showSelectionAfterClick": %s, // Affiche la sélection après un clic.
                    "selectionPreviewSeconds": %d, // Durée d'affichage d'une sélection.
                    "regionPreviewSeconds": %d, // Durée d'affichage d'une région.
                    "maximumPreviewSeconds": %d, // Durée maximale d'un aperçu.
                    "maximumParticlesPerRefresh": %d, // Nombre maximal de particules par actualisation.
                    "refreshIntervalTicks": %d, // Intervalle d'actualisation en ticks.

                    // true permet d'ouvrir l'assistant de création avec
                    // Maj + clic gauche en utilisant la baguette.
                    "shiftLeftClickOpensCreationAssistant": %s
                  },

                  ///////////////////////////
                  // Dummies généraux      //
                  ///////////////////////////

                  "dummy": {
                    "enabled": %s, // Active les dummies.
                    "horizontalKnockback": %s, // Force horizontale du recul.
                    "verticalKnockback": %s, // Force verticale du recul.
                    "sprintMultiplier": %s, // Multiplicateur appliqué en sprint.
                    "trollOutfitEnabled": %s, // Active l'équipement de troll.
                    "helmetItem": %s, // Objet placé sur la tête.
                    "chestItem": %s, // Objet placé sur le torse.
                    "legsItem": %s, // Objet placé sur les jambes.
                    "bootsItem": %s // Objet placé sur les pieds.
                  }
                }
                """.formatted(
                config.schemaVersion,
                config.enabled,
                quote(config.locale),
                config.testModeAllowed,
                config.createBackups,
                config.saveOnModification,
                config.logConfigurationChanges,
                config.backupLimit,
                config.saveIntervalSeconds,
                config.playerSnapshotArchiveLimit,

                config.points.enabled,
                config.points.allowNegativeScores,
                config.points.decimals,

                config.gui.enabled,
                config.gui.playSounds,
                config.gui.confirmDestructiveActions,
                config.gui.preferEnhancedClientGui,

                config.sessions.restoreInventory,
                config.sessions.restorePosition,
                config.sessions.restoreGameMode,
                config.sessions.restoreExperience,
                config.sessions.restoreEffects,
                config.sessions.restoreHealthAndFood,
                config.sessions.disconnectGraceSeconds,

                quote(config.arenaEditor.wandItem),
                config.arenaEditor.preventBlockBreaking,
                config.arenaEditor.preventBlockUse,
                config.arenaEditor.showSelectionAfterClick,
                config.arenaEditor.selectionPreviewSeconds,
                config.arenaEditor.regionPreviewSeconds,
                config.arenaEditor.maximumPreviewSeconds,
                config.arenaEditor.maximumParticlesPerRefresh,
                config.arenaEditor.refreshIntervalTicks,
                config.arenaEditor
                        .shiftLeftClickOpensCreationAssistant,

                config.dummy.enabled,
                config.dummy.horizontalKnockback,
                config.dummy.verticalKnockback,
                config.dummy.sprintMultiplier,
                config.dummy.trollOutfitEnabled,
                quote(config.dummy.helmetItem),
                quote(config.dummy.chestItem),
                quote(config.dummy.legsItem),
                quote(config.dummy.bootsItem)
        );

        write(path, content);
    }

    public static void writeMessages(
            Path path,
            MessageConfig config,
            Gson gson
    ) throws IOException {
        StringBuilder content =
                new StringBuilder();

        content.append("""
            {
              ///////////////////////////
              // Messages Olympicraft  //
              ///////////////////////////

              "schemaVersion": %d, // Version interne de la configuration.
              "prefix": %s, // Préfixe placé devant les messages.

              "messages": {
            """.formatted(
                config.schemaVersion,
                quote(config.prefix)
        ));

        int index = 0;

        for (Map.Entry<String, String> entry :
                config.messages.entrySet()) {
            content.append("    ")
                    .append(quote(entry.getKey()))
                    .append(": ")
                    .append(
                            gson.toJson(
                                    entry.getValue()
                            )
                    );

            if (index + 1
                    < config.messages.size()) {
                content.append(',');
            }

            content.append('\n');
            index++;
        }

        content.append("""
              }
            }
            """);

        write(
                path,
                content.toString()
        );
    }

    public static void writeSumo(
            Path path,
            SumoConfig config,
            Gson gson
    ) throws IOException {
        /*
         * La configuration Sumo possède des listes imbriquées.
         * Une version documentée complète sera générée par blocs
         * afin que chaque paramètre reste lisible.
         */
        String content = SumoJson5Template.create(
                config,
                gson
        );

        write(path, content);
    }

    private static String quote(String value) {
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

    private static void write(
            Path path,
            String content
    ) throws IOException {
        Files.createDirectories(
                path.getParent()
        );

        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8
        );
    }
}
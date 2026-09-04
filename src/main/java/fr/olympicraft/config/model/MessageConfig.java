package fr.olympicraft.config.model;

import fr.olympicraft.config.ConfigFile;
import fr.olympicraft.internal.BuildDefaults;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MessageConfig implements ConfigFile {

    public int schemaVersion =
            BuildDefaults.CONFIG_SCHEMA_VERSION;

    public String prefix =
            "<dark_gray>[<aqua>Olympicraft</aqua>]</dark_gray> ";

    public Map<String, String> messages =
            createDefaultMessages();

    private static Map<String, String> createDefaultMessages() {
        Map<String, String> values =
                new LinkedHashMap<>();

        values.put(
                "state.enabled",
                "<green>activé</green>"
        );
        values.put(
                "state.disabled",
                "<red>désactivé</red>"
        );
        values.put(
                "state.attached",
                "<green>attaché</green>"
        );
        values.put(
                "state.detached",
                "<red>détaché</red>"
        );
        values.put(
                "state.loaded",
                "<green>chargée</green>"
        );
        values.put(
                "state.not_loaded",
                "<red>non chargée</red>"
        );

        values.put(
                "command.help.header",
                "<aqua>──────────── Olympicraft ────────────</aqua>"
        );
        values.put(
                "command.help.line",
                "<aqua>%command%</aqua> <dark_gray>—</dark_gray> "
                        + "<gray>%description%</gray>"
        );
        values.put(
                "command.help.help",
                "Affiche l'aide d'Olympicraft."
        );
        values.put(
                "command.help.status",
                "Affiche l'état du mod."
        );
        values.put(
                "command.help.menu",
                "Ouvre le menu principal."
        );
        values.put(
                "command.help.admin",
                "Ouvre le menu d'administration."
        );

        values.put(
                "command.help.config_reload",
                "Recharge les configurations."
        );
        values.put(
                "command.help.config_save",
                "Enregistre les configurations."
        );
        values.put(
                "command.help.config_status",
                "Affiche l'état des configurations."
        );
        values.put(
                "command.config.reload.success",
                "Les configurations ont été rechargées."
        );
        values.put(
                "command.config.reload.failure",
                "Le rechargement a échoué. Consulte les journaux."
        );
        values.put(
                "command.config.save.success",
                "Les configurations ont été enregistrées."
        );
        values.put(
                "command.config.save.failure",
                "L'enregistrement a échoué. Consulte les journaux."
        );
        values.put(
                "command.config.status",
                "Configuration : %state%"
        );

        values.put(
                "command.status.name",
                "Nom : <aqua>%name%</aqua>"
        );
        values.put(
                "command.status.command",
                "Commande principale : <aqua>/%command%</aqua>"
        );
        values.put(
                "command.status.server",
                "Serveur logique : %state%"
        );
        values.put(
                "command.status.test_mode",
                "Mode test : %state%"
        );
        values.put(
                "command.status.games",
                "Mini-jeux prévus : <aqua>%count%</aqua>"
        );

        values.put(
                "command.help.test_enable",
                "Active le mode de test."
        );
        values.put(
                "command.help.test_disable",
                "Désactive le mode de test."
        );
        values.put(
                "command.help.test_status",
                "Affiche l'état du mode de test."
        );
        values.put(
                "command.test.not_allowed",
                "Le mode test est désactivé dans la configuration."
        );
        values.put(
                "command.test.enabled",
                "Le mode test est maintenant activé."
        );
        values.put(
                "command.test.already_enabled",
                "Le mode test est déjà activé."
        );
        values.put(
                "command.test.disabled",
                "Le mode test est maintenant désactivé."
        );
        values.put(
                "command.test.already_disabled",
                "Le mode test est déjà désactivé."
        );
        values.put(
                "command.test.status",
                "Mode test : %state%"
        );

        values.put(
                "command.help.arena_list",
                "Affiche les arènes du monde."
        );
        values.put(
                "command.help.arena_create",
                "Crée une arène à ta position."
        );
        values.put(
                "command.help.arena_setlobby",
                "Définit le lobby d'une arène."
        );
        values.put(
                "command.help.arena_setspectator",
                "Définit la position spectateur."
        );
        values.put(
                "command.help.arena_addspawn",
                "Ajoute un spawn à un groupe."
        );
        values.put(
                "command.help.arena_removespawn",
                "Supprime un spawn d'un groupe."
        );
        values.put(
                "command.help.arena_listspawns",
                "Affiche les spawns d'une arène."
        );
        values.put(
                "command.help.arena_tp",
                "Téléporte vers une position d'arène."
        );

        values.put(
                "command.help.game_join",
                "Rejoint une partie."
        );
        values.put(
                "command.help.game_leave",
                "Quitte la partie actuelle."
        );
        values.put(
                "command.help.game_spectate",
                "Observe une partie."
        );
        values.put(
                "command.help.game_list",
                "Affiche les parties actives."
        );
        values.put(
                "command.help.game_status",
                "Affiche l'état d'une partie."
        );
        values.put(
                "command.help.game_start",
                "Lance le décompte d'une partie."
        );
        values.put(
                "command.help.game_stop",
                "Arrête une partie."
        );

        values.put(
                "game.countdown.bossbar",
                "<aqua>Démarrage dans "
                        + "<yellow>%seconds%</yellow> seconde(s)</aqua>"
        );
        values.put(
                "game.countdown.started",
                "<green><bold>C'est parti !</bold></green>"
        );
        values.put(
                "game.countdown.cancelled",
                "<yellow>Le démarrage a été annulé.</yellow>"
        );

        values.put(
                "dummy.added",
                "<green>Dummy ajouté : "
                        + "<aqua>%dummy%</aqua>.</green>"
        );
        values.put(
                "dummy.removed",
                "<green>Dummy supprimé : "
                        + "<aqua>%dummy%</aqua>.</green>"
        );
        values.put(
                "dummy.list.empty",
                "<yellow>Aucun dummy dans l'arène "
                        + "<aqua>%arena%</aqua>.</yellow>"
        );
        values.put(
                "dummy.list.header",
                "<aqua>Dummies de l'arène %arena% :</aqua>"
        );
        values.put(
                "dummy.list.entry",
                "<gray>- <aqua>%dummy%</aqua></gray>"
        );

        values.put(
                "sumo.duel.started",
                "<aqua>%fighter1%</aqua> "
                        + "<gray>affronte</gray> "
                        + "<red>%fighter2%</red> <yellow>!</yellow>"
        );
        values.put(
                "sumo.duel.not_enough_fighters",
                "<red>Pas assez de combattants pour le duel.</red>"
        );
        values.put(
                "sumo.overtime.started",
                "<red><bold>PROLONGATION !</bold></red>"
        );
        values.put(
                "sumo.overtime.damage",
                "<yellow>Les combattants perdent désormais "
                        + "de la vie.</yellow>"
        );
        values.put(
                "sumo.victory.chat",
                "<gold><bold>%winner%</bold></gold> "
                        + "<yellow>remporte le duel !</yellow>"
        );
        values.put(
                "sumo.victory.title",
                "<gold>✦ </gold><yellow><bold>VICTOIRE !"
                        + "</bold></yellow><gold> ✦</gold>"
        );
        values.put(
                "sumo.victory.subtitle",
                "<aqua><bold>%winner%</bold></aqua> "
                        + "<white>remporte le duel de Sumo</white>"
        );
        values.put(
                "sumo.draw",
                "<yellow>Le duel se termine sans gagnant.</yellow>"
        );
        values.put(
                "sumo.ending",
                "<gray>Fin du duel de Sumo.</gray>"
        );
        values.put(
                "sumo.item.knockback_stick.name",
                "<aqua><bold>Bâton de Sumo</bold></aqua>"
        );
        values.put(
                "sumo.item.knockback_stick.lore.1",
                "<gray>Éjecte ton adversaire du ring.</gray>"
        );
        values.put(
                "sumo.overtime.started",
                "<dark_red><bold>⚠ PROLONGATION ⚠</bold></dark_red>"
        );

        values.put(
                "sumo.overtime.subtitle",
                "<yellow>Les combattants perdront "
                        + "<red>%damage%</red> points de vie "
                        + "toutes les <gold>%seconds%</gold> secondes.</yellow>"
        );

        values.put(
                "sumo.defeat.title",
                "<dark_red><bold>DÉFAITE</bold></dark_red>"
        );

        values.put(
                "sumo.defeat.subtitle",
                "<red>%winner%</red> "
                        + "<gray>remporte le duel.</gray>"
        );

        /*
         * ---------------------------------------------------------
         * SUMO - SÉLECTION DES KITS
         * ---------------------------------------------------------
         */

        values.put(
                "sumo.kit.classic.name",
                "<aqua>Classique</aqua>"
        );

        values.put(
                "sumo.kit.classic.description",
                "<gray>Une partie de Sumo classique "
                        + "avec un bâton Recul II.</gray>"
        );

        values.put(
                "sumo.kit.vanilla.name",
                "<green>Vanilla</green>"
        );

        values.put(
                "sumo.kit.vanilla.description",
                "<gray>Affrontez votre adversaire "
                        + "à mains nues !</gray>"
        );

        values.put(
                "sumo.kit.chaos.name",
                "<red>CHAOS !</red>"
        );

        values.put(
                "sumo.kit.chaos.description",
                "<gray>Un bâton Recul I et "
                        + "des charges de vent !</gray>"
        );

        /*
         * ---------------------------------------------------------
         * SUMO - OBJETS DES KITS
         * ---------------------------------------------------------
         */

        values.put(
                "sumo.item.knockback_stick.name",
                "<aqua>Bâton de Sumo</aqua>"
        );

        values.put(
                "sumo.item.knockback_stick.lore.1",
                "<gray>Repoussez votre adversaire "
                        + "hors de l'arène.</gray>"
        );

        values.put(
                "sumo.item.chaos_stick.name",
                "<red>Bâton du Chaos</red>"
        );

        values.put(
                "sumo.item.chaos_stick.lore.1",
                "<gray>Recul I — moins puissant, "
                        + "mais toujours dangereux.</gray>"
        );

        values.put(
                "sumo.item.wind_charge.name",
                "<light_purple>Charges de vent</light_purple>"
        );

        values.put(
                "sumo.item.wind_charge.lore.1",
                "<gray>Utilisez-les pour propulser "
                        + "vos adversaires.</gray>"
        );

        return values;
    }

    public String value(String key) {
        return messages.getOrDefault(
                key,
                "<red>Message inconnu : " + key + "</red>"
        );
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
        if (prefix == null) {
            prefix = "";
        }

        if (messages == null) {
            messages = new LinkedHashMap<>();
        }

        Map<String, String> defaults = createDefaultMessages();

        defaults.forEach((key, value) ->
                messages.putIfAbsent(key, value)
        );

        messages.replaceAll((key, value) ->
                value == null ? defaults.getOrDefault(key, "") : value
        );
        /*
         * Nettoyage des anciennes fausses sections qui avaient
         * été enregistrées comme des messages JSON5.
         */
        messages.entrySet().removeIf(entry -> {
            String key = entry.getKey();

            return key != null
                    && key.startsWith("######### ")
                    && key.endsWith(" #########");
        });
    }
}

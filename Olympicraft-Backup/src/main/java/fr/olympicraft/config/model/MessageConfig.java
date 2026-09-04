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
        Map<String, String> values = new LinkedHashMap<>();

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
                "command.help.arena_list",
                "Affiche les arènes du monde."
        );

        values.put(
                "command.help.arena_create",
                "Crée une arène à ta position."
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
    }
}

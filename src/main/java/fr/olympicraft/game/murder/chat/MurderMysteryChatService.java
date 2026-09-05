package fr.olympicraft.game.murder.chat;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.config.model.game.MurderMysteryConfig;
import fr.olympicraft.game.murder.MurderMysteryParticipant;
import fr.olympicraft.game.murder.MurderMysteryRuntime;
import fr.olympicraft.match.GameInstance;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class MurderMysteryChatService {

    private boolean registered;

    public void register() {
        if (registered) {
            return;
        }
        dictionary.load();
        registered = true;

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (message, sender, parameters) ->
                        handleChatMessage(
                                message.signedContent(),
                                message.decoratedContent(),
                                sender
                        )
        );
    }

    /*
     * Retourne true si Minecraft doit diffuser normalement
     * le message.
     *
     * Retourne false lorsque le message a été traité par le
     * chat de proximité du Murder Mystery.
     */
    private boolean handleChatMessage(
            String rawContent,
            Component content,
            ServerPlayer sender
    ) {
        if (sender == null) {
            return true;
        }

        GameInstance instance =
                Olympicraft.matches()
                        .findByPlayer(
                                sender.getUUID()
                        )
                        .orElse(null);

        if (instance == null
                || !(instance.runtime()
                instanceof MurderMysteryRuntime runtime)) {
            return true;
        }

        MurderMysteryConfig.ProximityChat config =
                Olympicraft.configs()
                        .murderMystery()
                        .proximityChat;

        if (config == null || !config.enabled) {
            return true;
        }

        MurderMysteryParticipant senderParticipant =
                runtime.participant(
                        sender.getUUID()
                );

        /*
         * Le joueur peut être dans le lobby de la partie alors
         * que les identités n'ont pas encore été distribuées.
         *
         * Dans ce cas, le chat normal reste actif.
         */
        if (senderParticipant == null) {
            return true;
        }

        List<MurderMysterySensitiveNames.SensitiveName>
                sensitiveNames =
                MurderMysterySensitiveNames.collect(
                        sender.getServer(),
                        dictionary
                );

        if (nameFilter.containsForbiddenName(
                rawContent,
                sensitiveNames
        )) {
            sender.sendSystemMessage(
                    Component.literal(
                            "[Olympicraft] Ton message peut révéler "
                                    + "l'identité réelle d'un joueur. "
                                    + "Utilise uniquement les alias."
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );

            return false;
        }

        Component formattedMessage =
                createMessage(
                        senderParticipant,
                        content
                );

        int recipientAmount = 0;

        for (ServerPlayer recipient :
                instance.onlineParticipants()) {
            if (!canReceive(
                    sender,
                    senderParticipant,
                    recipient,
                    runtime,
                    config
            )) {
                continue;
            }

            recipient.sendSystemMessage(
                    formattedMessage
            );

            recipientAmount++;
        }

        /*
         * Cette sécurité devrait rarement être utile car
         * l'auteur peut toujours entendre son propre message.
         */
        if (recipientAmount == 0) {
            sender.sendSystemMessage(
                    formattedMessage
            );
        }

        /*
         * Empêche Minecraft d'envoyer ensuite le message normal
         * avec le véritable pseudonyme à tout le serveur.
         */
        return false;
    }

    private boolean canReceive(
            ServerPlayer sender,
            MurderMysteryParticipant senderParticipant,
            ServerPlayer recipient,
            MurderMysteryRuntime runtime,
            MurderMysteryConfig.ProximityChat config
    ) {
        if (recipient == null) {
            return false;
        }

        /*
         * Le joueur voit toujours son propre message.
         */
        if (sender.getUUID()
                .equals(recipient.getUUID())) {
            return true;
        }

        MurderMysteryParticipant recipientParticipant =
                runtime.participant(
                        recipient.getUUID()
                );

        /*
         * Pour cette première version, seuls les véritables
         * participants du Murder Mystery reçoivent ce chat.
         */
        if (recipientParticipant == null) {
            return false;
        }

        /*
         * Les morts peuvent parler entre eux si l'option est
         * activée, indépendamment de leur distance.
         */
        if (!senderParticipant.alive()
                && !recipientParticipant.alive()) {
            return config.deadPlayersCanTalkTogether;
        }

        /*
         * Un mort ne parle jamais à un vivant.
         */
        if (!senderParticipant.alive()
                && recipientParticipant.alive()) {
            return false;
        }

        /*
         * Les spectateurs ou joueurs morts ne doivent pas
         * écouter les participants vivants si l'option est
         * désactivée.
         */
        if (senderParticipant.alive()
                && !recipientParticipant.alive()
                && !config.spectatorsCanHearLiving) {
            return false;
        }

        if (config.requireSameDimension
                && sender.serverLevel()
                != recipient.serverLevel()) {
            return false;
        }

        double horizontalRange =
                Math.max(
                        0.0D,
                        config.horizontalRange
                );

        double verticalRange =
                Math.max(
                        0.0D,
                        config.verticalRange
                );

        double deltaX =
                sender.getX()
                        - recipient.getX();

        double deltaZ =
                sender.getZ()
                        - recipient.getZ();

        double horizontalDistanceSquared =
                deltaX * deltaX
                        + deltaZ * deltaZ;

        if (horizontalDistanceSquared
                > horizontalRange
                * horizontalRange) {
            return false;
        }

        double verticalDistance =
                Math.abs(
                        sender.getY()
                                - recipient.getY()
                );

        return verticalDistance <= verticalRange;
    }

    private Component createMessage(
            MurderMysteryParticipant sender,
            Component content
    ) {
        String alias =
                sender.alias();

        if (alias == null || alias.isBlank()) {
            alias = "Inconnu";
        }

        return Component.empty()
                .append(
                        Component.literal(
                                "[Proximité] "
                        ).withStyle(
                                ChatFormatting.DARK_GRAY
                        )
                )
                .append(
                        Component.literal(
                                alias
                        ).withStyle(
                                ChatFormatting.GOLD
                        )
                )
                .append(
                        Component.literal(
                                " : "
                        ).withStyle(
                                ChatFormatting.GRAY
                        )
                )
                .append(
                        content.copy()
                                .withStyle(
                                        ChatFormatting.WHITE
                                )
                );
    }
    private final MurderMysteryDictionary dictionary =
            new MurderMysteryDictionary();

    private final MurderMysteryNameFilter nameFilter =
            new MurderMysteryNameFilter();
}
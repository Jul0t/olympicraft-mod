package fr.olympicraft.gui.menu.murder;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.game.murder.MurderMysteryRuntime;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import fr.olympicraft.match.GameInstance;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.UUID;

public final class MurderMysteryReconnectMenu
        implements GuiMenu {

    private final String arenaId;
    private final UUID playerId;

    public MurderMysteryReconnectMenu(
            String arenaId,
            UUID playerId
    ) {
        this.arenaId = arenaId;
        this.playerId = playerId;
    }

    @Override
    public Component title() {
        return Component.literal(
                "Reprendre la partie"
        );
    }

    @Override
    public int rows() {
        return 3;
    }

    @Override
    public void render(
            ServerPlayer player,
            GuiSession session,
            GuiManager manager
    ) {
        fill(session);

        GameInstance instance =
                Olympicraft.matches()
                        .findByArena(
                                arenaId
                        )
                        .orElse(null);

        MurderMysteryRuntime runtime =
                instance != null
                        && instance.runtime()
                        instanceof MurderMysteryRuntime murder
                        ? murder
                        : null;

        boolean reconnectAvailable =
                runtime != null
                        && runtime.canReconnect(
                        playerId
                );

        int remainingSeconds =
                reconnectAvailable
                        ? runtime.reconnectSecondsRemaining(
                        playerId
                )
                        : 0;

        ////////////////////////
        // Informations       //
        ////////////////////////

        session.setItem(
                4,
                GuiItemFactory.item(
                        reconnectAvailable
                                ? Items.CLOCK
                                : Items.BARRIER,
                        reconnectAvailable
                                ? "Partie toujours active"
                                : "Reconnexion indisponible",
                        reconnectAvailable
                                ? ChatFormatting.GOLD
                                : ChatFormatting.RED,
                        reconnectAvailable
                                ? "Ta place et ton rôle sont réservés."
                                : "La partie est terminée ou le délai a expiré.",
                        "Temps restant : "
                                + formatTime(
                                remainingSeconds
                        )
                ),
                null
        );

        //////////////////////////
        // Réintégrer la partie //
        //////////////////////////

        session.setItem(
                11,
                GuiItemFactory.item(
                        reconnectAvailable
                                ? Items.LIME_CONCRETE
                                : Items.GRAY_CONCRETE,
                        "Réintégrer la partie",
                        reconnectAvailable
                                ? ChatFormatting.GREEN
                                : ChatFormatting.DARK_GRAY,
                        reconnectAvailable
                                ? "Reprends ton rôle, ton alias"
                                : "Cette action n'est plus disponible.",
                        reconnectAvailable
                                ? "et ta progression."
                                : ""
                ),
                reconnectAvailable
                        ? (clickedPlayer, context) ->
                        handleReconnect(
                                clickedPlayer
                        )
                        : null
        );

        //////////////////////////
        // Abandonner la partie //
        //////////////////////////

        session.setItem(
                15,
                GuiItemFactory.item(
                        reconnectAvailable
                                ? Items.RED_CONCRETE
                                : Items.GRAY_CONCRETE,
                        "Abandonner",
                        reconnectAvailable
                                ? ChatFormatting.RED
                                : ChatFormatting.DARK_GRAY,
                        reconnectAvailable
                                ? "Cela compte comme une défaite."
                                : "Cette action n'est plus disponible.",
                        reconnectAvailable
                                ? "En ranked, une pénalité sera appliquée."
                                : ""
                ),
                reconnectAvailable
                        ? (clickedPlayer, context) ->
                        handleAbandon(
                                clickedPlayer
                        )
                        : null
        );

        ////////////////////////
        // Actualiser         //
        ////////////////////////

        session.setItem(
                22,
                GuiItemFactory.item(
                        Items.COMPASS,
                        "Actualiser",
                        ChatFormatting.AQUA,
                        "Actualise le temps restant",
                        "et l'état de la partie."
                ),
                (clickedPlayer, context) ->
                        manager.refresh(
                                clickedPlayer
                        )
        );
    }

    private void handleReconnect(
            ServerPlayer player
    ) {
        /*
         * L'action est exécutée directement par
         * GuiScreenHandler pendant le clic.
         *
         * Il ne faut pas fermer le menu avant d'avoir traité
         * la reconnexion, sinon la session du GUI pourrait être
         * supprimée avant l'exécution complète.
         */
        var result =
                Olympicraft.matches()
                        .acceptMurderMysteryReconnect(
                                player,
                                arenaId
                        );

        Component message =
                Component.literal(
                        result.successful()
                                ? result.message()
                                : "Impossible de reprendre : "
                                + result.message()
                ).withStyle(
                        result.successful()
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                );

        player.sendSystemMessage(
                message
        );

        if (result.successful()) {
            player.closeContainer();
            return;
        }

        /*
         * Si la reconnexion échoue, le menu reste ouvert
         * et son contenu est actualisé.
         */
        Olympicraft.gui().refresh(
                player
        );
    }

    private void handleAbandon(
            ServerPlayer player
    ) {
        /*
         * Le forfait est enregistré avant la fermeture du menu.
         * Cela évite de perdre la session ou l'action en cours.
         */
        var result =
                Olympicraft.matches()
                        .declineMurderMysteryReconnect(
                                player,
                                arenaId
                        );

        Component message =
                Component.literal(
                        result.message()
                ).withStyle(
                        result.successful()
                                ? ChatFormatting.YELLOW
                                : ChatFormatting.RED
                );

        player.sendSystemMessage(
                message
        );

        if (result.successful()) {
            player.closeContainer();
            return;
        }

        Olympicraft.gui().refresh(
                player
        );
    }

    private void fill(
            GuiSession session
    ) {
        for (int slot = 0;
             slot < session.container()
                     .getContainerSize();
             slot++) {
            session.setItem(
                    slot,
                    GuiItemFactory.filler(),
                    null
            );
        }
    }

    private static String formatTime(
            int seconds
    ) {
        int safeSeconds =
                Math.max(
                        0,
                        seconds
                );

        return String.format(
                "%02d:%02d",
                safeSeconds / 60,
                safeSeconds % 60
        );
    }
}
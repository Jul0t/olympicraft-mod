package fr.olympicraft.gui.menu;

import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public final class OlympicraftMainMenu
        implements GuiMenu {

    @Override
    public Component title() {
        return Component.literal("Olympicraft");
    }

    @Override
    public void render(
            ServerPlayer player,
            GuiSession session,
            GuiManager manager
    ) {
        fill(session);

        session.setItem(
                20,
                GuiItemFactory.item(
                        Items.NETHER_STAR,
                        "Jouer",
                        ChatFormatting.GREEN,
                        "Les files d'attente seront ajoutées",
                        "avec le moteur de sessions."
                ),
                (clickedPlayer, context) ->
                        manager.messages().sendInfo(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                "Le système de parties sera "
                                        + "ajouté prochainement."
                        )
        );

        session.setItem(
                22,
                GuiItemFactory.item(
                        Items.COMPASS,
                        "Arènes",
                        ChatFormatting.AQUA,
                        "Consulter et administrer les arènes."
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new ArenaListMenu(0)
                        )
        );

        session.setItem(
                24,
                GuiItemFactory.item(
                        Items.BOOK,
                        "Statistiques",
                        ChatFormatting.GOLD,
                        "Les statistiques seront ajoutées",
                        "avec le moteur de points."
                ),
                (clickedPlayer, context) ->
                        manager.messages().sendInfo(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                "Les statistiques seront "
                                        + "ajoutées prochainement."
                        )
        );

        if (player.hasPermissions(2)) {
            session.setItem(
                    31,
                    GuiItemFactory.item(
                            Items.COMMAND_BLOCK,
                            "Administration",
                            ChatFormatting.RED,
                            "Accéder aux outils administratifs."
                    ),
                    (clickedPlayer, context) ->
                            manager.open(
                                    clickedPlayer,
                                    new ArenaListMenu(0)
                            )
            );
        }

        session.setItem(
                49,
                GuiItemFactory.item(
                        Items.BARRIER,
                        "Fermer",
                        ChatFormatting.RED
                ),
                (clickedPlayer, context) ->
                        clickedPlayer.closeContainer()
        );
    }

    private void fill(GuiSession session) {
        for (int slot = 0;
             slot < session.container().getContainerSize();
             slot++) {
            session.setItem(
                    slot,
                    GuiItemFactory.filler(),
                    null
            );
        }
    }
}
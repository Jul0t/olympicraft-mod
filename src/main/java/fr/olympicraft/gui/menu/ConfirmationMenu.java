package fr.olympicraft.gui.menu;

import fr.olympicraft.gui.GuiAction;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public final class ConfirmationMenu
        implements GuiMenu {

    private final String title;
    private final String description;
    private final GuiAction confirmAction;
    private final GuiMenu returnMenu;

    public ConfirmationMenu(
            String title,
            String description,
            GuiAction confirmAction,
            GuiMenu returnMenu
    ) {
        this.title = title;
        this.description = description;
        this.confirmAction = confirmAction;
        this.returnMenu = returnMenu;
    }

    @Override
    public Component title() {
        return Component.literal(title);
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

        session.setItem(
                13,
                GuiItemFactory.item(
                        Items.PAPER,
                        "Confirmation requise",
                        ChatFormatting.YELLOW,
                        description,
                        "",
                        "Cette action peut être irréversible."
                ),
                null
        );

        session.setItem(
                11,
                GuiItemFactory.item(
                        Items.LIME_CONCRETE,
                        "Confirmer",
                        ChatFormatting.GREEN,
                        "Clique pour confirmer l'action."
                ),
                confirmAction
        );

        session.setItem(
                15,
                GuiItemFactory.item(
                        Items.RED_CONCRETE,
                        "Annuler",
                        ChatFormatting.RED,
                        "Retourne au menu précédent."
                ),
                (clickedPlayer, context) ->
                        manager.openNextTick(
                                clickedPlayer,
                                returnMenu
                        )
        );

        session.setItem(
                22,
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
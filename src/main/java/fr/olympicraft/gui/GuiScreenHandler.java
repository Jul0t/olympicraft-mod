package fr.olympicraft.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class GuiScreenHandler extends ChestMenu {

    private final GuiManager manager;
    private final GuiSession session;

    private boolean actionRunning;

    public GuiScreenHandler(
            int containerId,
            Inventory playerInventory,
            GuiManager manager,
            GuiSession session
    ) {
        super(
                menuType(session.menu().rows()),
                containerId,
                playerInventory,
                session.container(),
                session.menu().rows()
        );

        this.manager = manager;
        this.session = session;
    }

    private static MenuType<ChestMenu> menuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;

            default -> throw new IllegalArgumentException(
                    "Nombre de lignes GUI invalide : "
                            + rows
                            + ". La valeur doit être comprise "
                            + "entre 1 et 6."
            );
        };
    }

    @Override
    public void clicked(
            int slotId,
            int button,
            ClickType clickType,
            Player player
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int guiSize =
                session.container().getContainerSize();

        /*
         * Les emplacements compris entre 0 et guiSize - 1
         * appartiennent au GUI Olympicraft.
         */
        if (slotId >= 0 && slotId < guiSize) {
            if (actionRunning) {
                return;
            }

            GuiAction action = session.action(slotId);

            if (action == null) {
                return;
            }

            boolean shiftClick =
                    clickType == ClickType.QUICK_MOVE;

            actionRunning = true;

            fr.olympicraft.Olympicraft.LOGGER.info(
                    "Clic GUI reçu : joueur={}, menu={}, slot={}",
                    serverPlayer.getGameProfile().getName(),
                    session.menu()
                            .getClass()
                            .getSimpleName(),
                    slotId
            );

            try {
                action.execute(
                        serverPlayer,
                        new GuiContext(
                                manager,
                                session,
                                slotId,
                                button,
                                shiftClick
                        )
                );
            } finally {
                actionRunning = false;
            }

            return;
        }

        /*
         * Les clics dans l'inventaire du joueur restent
         * bloqués pour empêcher l'insertion d'objets dans
         * le menu Olympicraft.
         */
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (player instanceof ServerPlayer serverPlayer) {
            manager.close(
                    serverPlayer,
                    session
            );
        }
    }
    public GuiSession session() {
        return session;
    }
}
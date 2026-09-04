package fr.olympicraft.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuiSession {

    private final UUID playerId;
    private final GuiMenu menu;
    private final SimpleContainer container;
    private final Map<Integer, GuiAction> actions =
            new HashMap<>();

    public GuiSession(
            ServerPlayer player,
            GuiMenu menu
    ) {
        this.playerId = player.getUUID();
        this.menu = menu;
        this.container = new SimpleContainer(menu.size());
    }

    public UUID playerId() {
        return playerId;
    }

    public GuiMenu menu() {
        return menu;
    }

    public SimpleContainer container() {
        return container;
    }

    public void clear() {
        container.clearContent();
        actions.clear();
    }

    public void setItem(
            int slot,
            ItemStack item,
            GuiAction action
    ) {
        if (slot < 0 || slot >= container.getContainerSize()) {
            throw new IndexOutOfBoundsException(
                    "Emplacement GUI invalide : " + slot
            );
        }

        container.setItem(slot, item);

        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
    }

    public GuiAction action(int slot) {
        return actions.get(slot);
    }
}

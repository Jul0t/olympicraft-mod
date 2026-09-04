package fr.olympicraft.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface GuiMenu {

    Component title();

    default int rows() {
        return 6;
    }

    default int size() {
        return rows() * 9;
    }

    void render(
            ServerPlayer player,
            GuiSession session,
            GuiManager manager
    );
}

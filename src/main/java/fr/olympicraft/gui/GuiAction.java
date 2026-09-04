package fr.olympicraft.gui;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface GuiAction {

    void execute(
            ServerPlayer player,
            GuiContext context
    );
}

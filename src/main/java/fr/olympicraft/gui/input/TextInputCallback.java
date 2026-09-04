package fr.olympicraft.gui.input;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface TextInputCallback {

    void onSubmit(
            ServerPlayer player,
            String value
    );
}
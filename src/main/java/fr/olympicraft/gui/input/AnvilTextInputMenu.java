package fr.olympicraft.gui.input;

import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AnvilTextInputMenu {

    private final Component title;
    private final String placeholder;
    private final String initialValue;
    private final TextInputCallback callback;
    private final GuiMenu cancelMenu;

    public AnvilTextInputMenu(
            Component title,
            String placeholder,
            String initialValue,
            TextInputCallback callback,
            GuiMenu cancelMenu
    ) {
        this.title = title == null
                ? Component.literal("Saisir un texte")
                : title;

        this.placeholder = placeholder == null
                ? ""
                : placeholder;

        this.initialValue = initialValue == null
                ? ""
                : initialValue;

        this.callback = callback;
        this.cancelMenu = cancelMenu;
    }

    public Component title() {
        return title;
    }

    public String placeholder() {
        return placeholder;
    }

    public String initialValue() {
        return initialValue;
    }

    public void submit(
            ServerPlayer player,
            String value
    ) {
        if (callback == null) {
            return;
        }

        callback.onSubmit(
                player,
                value == null
                        ? ""
                        : value.trim()
        );
    }

    public void cancel(
            GuiManager guiManager,
            ServerPlayer player
    ) {
        if (guiManager == null
                || player == null
                || cancelMenu == null) {
            return;
        }

        guiManager.openNextTick(
                player,
                cancelMenu
        );
    }
}
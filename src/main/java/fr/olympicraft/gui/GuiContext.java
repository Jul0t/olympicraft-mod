package fr.olympicraft.gui;

public record GuiContext(
        GuiManager manager,
        GuiSession session,
        int slot,
        int button,
        boolean shiftClick
) {
}

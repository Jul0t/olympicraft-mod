package fr.olympicraft.game;

import java.util.List;

public record GamePresentation(
        String displayName,
        String description,
        String iconItem,
        List<String> lore
) {

    public GamePresentation {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom d'affichage d'un jeu ne peut pas être vide."
            );
        }

        description = description == null ? "" : description;
        iconItem = normalizeIcon(iconItem);
        lore = lore == null ? List.of() : List.copyOf(lore);
    }

    private static String normalizeIcon(String iconItem) {
        if (iconItem == null || iconItem.isBlank()) {
            return "minecraft:barrier";
        }

        return iconItem.trim().toLowerCase();
    }
}

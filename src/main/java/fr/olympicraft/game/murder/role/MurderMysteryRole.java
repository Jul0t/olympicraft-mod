package fr.olympicraft.game.murder.role;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum MurderMysteryRole {

    INNOCENT(
            "innocent",
            "Innocent",
            ChatFormatting.GREEN
    ),

    DETECTIVE(
            "detective",
            "Détective",
            ChatFormatting.AQUA
    ),

    MURDERER(
            "murderer",
            "Meurtrier",
            ChatFormatting.RED
    ),

    TROUBLEMAKER(
            "troublemaker",
            "Trouble-fête",
            ChatFormatting.LIGHT_PURPLE
    );

    private final String id;

    private final String displayName;

    private final ChatFormatting color;

    MurderMysteryRole(
            String id,
            String displayName,
            ChatFormatting color
    ) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting color() {
        return color;
    }

    public Component displayComponent() {
        return Component.literal(
                displayName
        ).withStyle(color);
    }

    public boolean innocentCamp() {
        return this == INNOCENT
                || this == DETECTIVE;
    }

    public boolean independent() {
        return this == TROUBLEMAKER;
    }

    public boolean armedRole() {
        return this == DETECTIVE
                || this == MURDERER
                || this == TROUBLEMAKER;
    }
}
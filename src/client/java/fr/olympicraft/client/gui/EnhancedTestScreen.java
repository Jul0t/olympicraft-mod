package fr.olympicraft.client.gui;

import fr.olympicraft.client.sound.OlympicraftClientSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class EnhancedTestScreen
        extends Screen {

    private static final ResourceLocation LOGO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "olympicraft",
                    "textures/gui/enhanced/logo.png"
            );

    private static final int LOGO_TEXTURE_WIDTH = 256;
    private static final int LOGO_TEXTURE_HEIGHT = 64;

    private static final int LOGO_DISPLAY_WIDTH = 256;
    private static final int LOGO_DISPLAY_HEIGHT = 64;

    private final String message;

    public EnhancedTestScreen(
            String title,
            String message
    ) {
        super(
                Component.literal(
                        title == null
                                ? "Olympicraft Enhanced"
                                : title
                )
        );

        this.message =
                message == null
                        ? ""
                        : message;
    }

    @Override
    protected void init() {
        int buttonWidth = 180;
        int buttonHeight = 20;

        int left =
                width / 2 - buttonWidth / 2;

        int firstY =
                height / 2 + 45;

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Tester le son Enhanced"
                                ),
                                button ->
                                        OlympicraftClientSounds
                                                .playEnhancedTest()
                        )
                        .bounds(
                                left,
                                firstY,
                                buttonWidth,
                                buttonHeight
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Fermer"
                                ),
                                button -> onClose()
                        )
                        .bounds(
                                left,
                                firstY + 26,
                                buttonWidth,
                                buttonHeight
                        )
                        .build()
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        /*
         * Dessine d'abord le fond et les boutons.
         */
        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        int centerX = width / 2;

        int logoX =
                centerX
                        - LOGO_DISPLAY_WIDTH / 2;

        int logoY =
                height / 2 - 105;

        /*
         * Affichage du logo personnalisé.
         */
        graphics.blit(
                LOGO_TEXTURE,
                logoX,
                logoY,
                0.0F,
                0.0F,
                LOGO_DISPLAY_WIDTH,
                LOGO_DISPLAY_HEIGHT,
                LOGO_TEXTURE_WIDTH,
                LOGO_TEXTURE_HEIGHT
        );

        graphics.drawCenteredString(
                font,
                title,
                centerX,
                height / 2 - 32,
                0xFF55FFFF
        );

        graphics.drawCenteredString(
                font,
                Component.literal(
                        "Client Enhanced détecté ✓"
                ),
                centerX,
                height / 2 - 10,
                0xFF55FF55
        );

        graphics.drawCenteredString(
                font,
                Component.literal(message),
                centerX,
                height / 2 + 10,
                0xFFFFFFFF
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
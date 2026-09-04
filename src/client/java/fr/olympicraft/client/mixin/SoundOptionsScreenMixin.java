package fr.olympicraft.client.mixin;

import fr.olympicraft.client.gui.widget.OlympicraftSoundSlider;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin
        extends Screen {

    @Unique
    private OlympicraftSoundSlider
            olympicraft$soundSlider;

    protected SoundOptionsScreenMixin(
            Component title
    ) {
        super(title);
    }

    @Inject(
            method = "addOptions",
            at = @At("TAIL")
    )
    private void olympicraft$addSoundSlider(
            CallbackInfo callback
    ) {
        olympicraft$soundSlider =
                new OlympicraftSoundSlider(
                        0,
                        0,
                        150,
                        20
                );

        /*
         * Le Mixin étend Screen, il peut donc appeler
         * directement la méthode protected.
         */
        addRenderableWidget(
                olympicraft$soundSlider
        );

        olympicraft$placeSoundSlider();
    }

    /*
     * Cette méthode utilise toujours les dimensions actuelles
     * de l'écran. Le bouton reste donc centré et responsive.
     */
    @Unique
    private void olympicraft$placeSoundSlider() {
        if (olympicraft$soundSlider == null) {
            return;
        }

        int buttonWidth = 150;
        int horizontalGap = 10;

        /*
         * Colonne droite de la grille Minecraft.
         */
        int x =
                this.width / 2
                        + horizontalGap / 2;

        /*
         * Ligne située juste au-dessus du bouton Terminé.
         */
        int y =
                this.height - 54;

        olympicraft$soundSlider.setX(x);
        olympicraft$soundSlider.setY(y);
        olympicraft$soundSlider.setWidth(
                buttonWidth
        );
    }
}
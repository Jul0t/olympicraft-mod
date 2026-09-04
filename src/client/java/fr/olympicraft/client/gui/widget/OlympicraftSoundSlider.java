package fr.olympicraft.client.gui.widget;

import fr.olympicraft.client.config.OlympicraftClientConfig;
import fr.olympicraft.client.sound.OlympicraftClientSounds;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public final class OlympicraftSoundSlider
        extends AbstractSliderButton {

    public OlympicraftSoundSlider(
            int x,
            int y,
            int width,
            int height
    ) {
        super(
                x,
                y,
                width,
                height,
                Component.empty(),
                OlympicraftClientConfig
                        .soundVolume()
        );

        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int percentage =
                (int) Math.round(
                        value * 100.0D
                );

        Component displayedValue =
                percentage <= 0
                        ? Component.translatable(
                        "options.off"
                )
                        : Component.literal(
                        percentage + "%"
                );

        setMessage(
                Component.translatable(
                        "options.olympicraft.sound_volume"
                ).append(
                        ": "
                ).append(
                        displayedValue
                )
        );
    }

    @Override
    protected void applyValue() {
        OlympicraftClientConfig.soundVolume(
                value
        );

        /*
         * Applique immédiatement le nouveau volume
         * aux boucles Enhanced actives.
         */
        OlympicraftClientSounds
                .refreshLoopVolumes();
    }
}
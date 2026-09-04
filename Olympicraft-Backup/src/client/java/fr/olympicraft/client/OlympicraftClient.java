package fr.olympicraft.client;

import fr.olympicraft.Olympicraft;
import net.fabricmc.api.ClientModInitializer;

public final class OlympicraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Olympicraft.LOGGER.info(
                "Initialisation des fonctionnalités client d'Olympicraft."
        );
    }
}

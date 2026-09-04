package fr.olympicraft.match.runtime;

import fr.olympicraft.game.murder.MurderMysteryRuntime;
import fr.olympicraft.game.sumo.SumoRuntime;

public final class BuiltinGameRuntimes {

    private BuiltinGameRuntimes() {
    }

    public static void registerAll(
            GameRuntimeRegistry registry
    ) {
        registry.register(
                "sumo",
                SumoRuntime::new
        );

        registry.register(
                "murder_mystery",
                MurderMysteryRuntime::new
        );
    }
}
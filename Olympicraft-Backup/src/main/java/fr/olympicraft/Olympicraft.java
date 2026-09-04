package fr.olympicraft;

import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.command.OlympicraftCommands;
import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.internal.BuildDefaults;
import fr.olympicraft.message.MessageService;
import fr.olympicraft.test.TestModeManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Olympicraft implements ModInitializer {

    public static final Logger LOGGER =
            LoggerFactory.getLogger(BuildDefaults.MOD_NAME);

    /*
     * Instances uniques utilisées dans tout Olympicraft.
     *
     * Il est important que la même instance ARENAS soit :
     * - attachée au serveur ;
     * - transmise aux commandes.
     */
    private static final OlympicraftConfigManager CONFIGS =
            new OlympicraftConfigManager();

    private static final MessageService MESSAGES =
            new MessageService(CONFIGS);

    private static final TestModeManager TEST_MODE =
            new TestModeManager();

    private static final ArenaManager ARENAS =
            new ArenaManager();

    @Override
    public void onInitialize() {
        LOGGER.info("Initialisation d'Olympicraft.");

        if (!CONFIGS.loadAll()) {
            LOGGER.error(
                    "Les configurations n'ont pas pu être "
                            + "chargées correctement."
            );
        }

        registerCommands();
        registerServerLifecycle();

        LOGGER.info("Olympicraft est initialisé.");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        OlympicraftCommands.register(
                                dispatcher,
                                CONFIGS,
                                MESSAGES,
                                TEST_MODE,
                                ARENAS
                        )
        );
    }

    private void registerServerLifecycle() {
        /*
         * SERVER_STARTED est appelé aussi bien :
         * - sur un serveur Fabric dédié ;
         * - dans le serveur intégré d'un monde solo.
         */
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TEST_MODE.attachServer(server);
            ARENAS.attachServer(server);

            LOGGER.info(
                    "Olympicraft est attaché au serveur logique."
            );

            LOGGER.info(
                    "Gestionnaire d'arènes attaché : {}.",
                    ARENAS.isAttached()
            );
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            /*
             * L'arène est enregistrée et détachée avant que
             * le serveur ne soit entièrement arrêté.
             */
            ARENAS.detachServer();
            TEST_MODE.detachServer();
            CONFIGS.saveAll();

            LOGGER.info(
                    "Arrêt et nettoyage d'Olympicraft."
            );
        });
    }

    public static OlympicraftConfigManager configs() {
        return CONFIGS;
    }

    public static MessageService messages() {
        return MESSAGES;
    }

    public static TestModeManager testMode() {
        return TEST_MODE;
    }

    public static ArenaManager arenas() {
        return ARENAS;
    }
}
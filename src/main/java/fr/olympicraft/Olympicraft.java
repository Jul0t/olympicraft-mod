package fr.olympicraft;

import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.arena.SelectionService;
import fr.olympicraft.arena.editor.ArenaEditorManager;
import fr.olympicraft.command.OlympicraftCommands;
import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.game.BuiltinGames;
import fr.olympicraft.game.GameRegistry;
import fr.olympicraft.game.murder.chat.MurderMysteryChatService;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.integration.worldedit.WorldEditSelectionProvider;
import fr.olympicraft.internal.BuildDefaults;
import fr.olympicraft.match.GameInstanceManager;
import fr.olympicraft.message.MessageService;
import fr.olympicraft.test.TestModeManager;
import fr.olympicraft.test.dummy.DummyManager;
import fr.olympicraft.test.dummy.DummyCombatHandler;
import fr.olympicraft.match.player.PlayerMatchService;
import fr.olympicraft.match.player.PlayerSnapshotManager;
import fr.olympicraft.match.runtime.BuiltinGameRuntimes;
import fr.olympicraft.match.runtime.GameRuntimeRegistry;
import fr.olympicraft.match.GameInstance;
import fr.olympicraft.enhanced.EnhancedClientManager;
import fr.olympicraft.network.OlympicraftPayloads;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Olympicraft implements ModInitializer {

    public static final Logger LOGGER =
            LoggerFactory.getLogger(BuildDefaults.MOD_NAME);

    private static final OlympicraftConfigManager CONFIGS =
            new OlympicraftConfigManager();

    private static final MessageService MESSAGES =
            new MessageService(CONFIGS);

    private static final TestModeManager TEST_MODE =
            new TestModeManager();

    private static final DummyManager DUMMIES =
            new DummyManager(TEST_MODE);

    private static final GameRegistry GAMES =
            new GameRegistry();

    private static final ArenaManager ARENAS =
            new ArenaManager(GAMES);

    private static final PlayerSnapshotManager SNAPSHOTS =
            new PlayerSnapshotManager(CONFIGS);

    private static final PlayerMatchService MATCH_PLAYERS =
            new PlayerMatchService(SNAPSHOTS);

    private static final GameRuntimeRegistry RUNTIMES =
            new GameRuntimeRegistry();

    private static final GameInstanceManager MATCHES =
            new GameInstanceManager(
                    ARENAS,
                    MATCH_PLAYERS,
                    RUNTIMES,
                    TEST_MODE,
                    DUMMIES
            );

    private static final DummyCombatHandler DUMMY_COMBAT =
            new DummyCombatHandler(
                    DUMMIES,
                    MATCHES,
                    CONFIGS
            );

    private static final ArenaSelectionManager SELECTIONS =
            new ArenaSelectionManager();

    private static final WorldEditSelectionProvider WORLD_EDIT =
            new WorldEditSelectionProvider();

    private static final SelectionService SELECTION_SERVICE =
            new SelectionService(
                    SELECTIONS,
                    WORLD_EDIT
            );

    private static final ArenaEditorManager ARENA_EDITOR =
            new ArenaEditorManager(
                    SELECTIONS,
                    CONFIGS,
                    MESSAGES
            );

    private static final GuiManager GUI =
            new GuiManager(
                    ARENAS,
                    GAMES,
                    MESSAGES
            );

    private static final EnhancedClientManager ENHANCED_CLIENTS =
            new EnhancedClientManager();

    private static final MurderMysteryChatService
            MURDER_MYSTERY_CHAT =
            new MurderMysteryChatService();

    @Override
    public void onInitialize() {
        LOGGER.info("Initialisation d'Olympicraft.");

        OlympicraftPayloads.register();
        ENHANCED_CLIENTS.registerReceiver();

        if (!CONFIGS.loadAll()) {
            LOGGER.error(
                    "Les configurations d'Olympicraft "
                            + "n'ont pas pu être chargées."
            );
        }

        BuiltinGames.registerAll(GAMES);
        GAMES.lock();

        BuiltinGameRuntimes.registerAll(RUNTIMES);
        RUNTIMES.lock();

        LOGGER.info(
                "{} runtime(s) de mini-jeu enregistré(s).",
                RUNTIMES.size()
        );

        registerDamageProtection();

        MURDER_MYSTERY_CHAT.register();

        ARENA_EDITOR.registerEvents();

        DUMMY_COMBAT.register();

        ARENA_EDITOR
                .creationLauncher()
                .attachGui(GUI);

        LOGGER.info(
                "{} mini-jeu(x) enregistré(s).",
                GAMES.size()
        );

        LOGGER.info(
                "Intégration WorldEdit : {}.",
                WORLD_EDIT.available()
                        ? "disponible"
                        : "absente"
        );

        registerCommands();
        registerServerLifecycle();
        registerServerTicks();
        registerConnections();

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
                                ARENAS,
                                GAMES,
                                MATCHES,
                                MATCH_PLAYERS,
                                SELECTION_SERVICE,
                                ARENA_EDITOR,
                                GUI
                        )
        );
    }

    private void registerServerTicks() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            MATCHES.tick();
            ARENA_EDITOR.tick(server);
        });
    }

    private void registerConnections() {
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> {
                    MATCHES.handleDisconnect(
                            handler.player
                    );

                    ENHANCED_CLIENTS.handleDisconnect(
                            handler.player
                    );
                }
        );

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) ->
                        MATCHES.handleJoin(
                                handler.player
                        )
        );
    }

    private void registerServerLifecycle() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TEST_MODE.attachServer(server);
            ARENAS.attachServer(server);

            /*
             * Les snapshots doivent être chargés avant que
             * PlayerMatchService récupère la liste des
             * restaurations en attente.
             */
            SNAPSHOTS.attachServer(server);
            MATCH_PLAYERS.attachServer(server);

            DUMMIES.attachServer(server);
            MATCHES.attachServer(server);

            LOGGER.info(
                    "Olympicraft est attaché au serveur logique."
            );

            LOGGER.info(
                    "Gestionnaire d'arènes attaché : {}.",
                    ARENAS.isAttached()
            );

            LOGGER.info(
                    "Gestionnaire de parties attaché : {}.",
                    MATCHES.isAttached()
            );

            LOGGER.info(
                    "{} restauration(s) de joueur en attente.",
                    SNAPSHOTS.playerIds().size()
            );
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            /*
             * La fermeture des parties doit être effectuée
             * pendant que les snapshots et le serveur sont
             * encore accessibles.
             */
            try {
                MATCHES.detachServer();
            } catch (Exception exception) {
                LOGGER.error(
                        "Erreur pendant la fermeture des parties.",
                        exception
                );
            }

            try {
                DUMMIES.detachServer();
            } catch (Exception exception) {
                LOGGER.error(
                        "Erreur pendant le nettoyage des dummies.",
                        exception
                );
            }

            MATCH_PLAYERS.detachServer();
            SNAPSHOTS.detachServer();

            SELECTIONS.clearAll();
            ARENA_EDITOR.clearAll();
            GUI.closeAll();

            ARENAS.detachServer();
            TEST_MODE.detachServer();

            CONFIGS.saveAll();
            ENHANCED_CLIENTS.clear();

            LOGGER.info(
                    "Arrêt et nettoyage d'Olympicraft."
            );
        });
    }

    private void registerDamageProtection() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> {
                    if (!(entity instanceof ServerPlayer player)) {
                        return true;
                    }

                    GameInstance instance =
                            MATCHES.findByPlayer(
                                    player.getUUID()
                            ).orElse(null);

                    if (instance == null) {
                        return true;
                    }

                    return !instance.preventsDamage(
                            player,
                            source,
                            amount
                    );
                }
        );
    }

    public static OlympicraftConfigManager configs() {
        return CONFIGS;
    }

    public static EnhancedClientManager enhancedClients() {return ENHANCED_CLIENTS;}

    public static MessageService messages() {
        return MESSAGES;
    }

    public static TestModeManager testMode() {
        return TEST_MODE;
    }

    public static ArenaManager arenas() {
        return ARENAS;
    }

    public static GameRegistry games() {
        return GAMES;
    }

    public static GameInstanceManager matches() {
        return MATCHES;
    }

    public static ArenaSelectionManager selections() {
        return SELECTIONS;
    }

    public static SelectionService selectionService() {
        return SELECTION_SERVICE;
    }

    public static ArenaEditorManager arenaEditor() {
        return ARENA_EDITOR;
    }

    public static GuiManager gui() {
        return GUI;
    }

    public static PlayerSnapshotManager snapshots() {
        return SNAPSHOTS;
    }

    public static PlayerMatchService matchPlayers() {
        return MATCH_PLAYERS;
    }

    public static GameRuntimeRegistry runtimes() {
        return RUNTIMES;
    }

    public static DummyManager dummies() {
        return DUMMIES;
    }

}
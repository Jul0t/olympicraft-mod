package fr.olympicraft.gui;
import fr.olympicraft.gui.input.AnvilTextInputHandler;
import fr.olympicraft.gui.input.AnvilTextInputMenu;

import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.game.GameRegistry;
import fr.olympicraft.message.MessageService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiManager {

    private final ArenaManager arenas;
    private final GameRegistry games;
    private final MessageService messages;
    private final fr.olympicraft.gui.flow.region.RegionCreationFlow regionCreationFlow;

    private final Map<UUID, GuiSession> sessions =
            new ConcurrentHashMap<>();

    public GuiManager(
            ArenaManager arenas,
            GameRegistry games,
            MessageService messages
    ) {
        this.arenas = arenas;
        this.games = games;
        this.messages = messages;
        this.regionCreationFlow =
                new fr.olympicraft.gui.flow.region.RegionCreationFlow(
                        arenas,
                        games
                );
    }

    public ArenaManager arenas() {
        return arenas;
    }

    public GameRegistry games() {
        return games;
    }

    public MessageService messages() {
        return messages;
    }

    public fr.olympicraft.gui.flow.region.RegionCreationFlow
    regionCreationFlow() {
        return regionCreationFlow;
    }


    /**
     * Ouvre immédiatement un menu.
     *
     * Cette méthode convient pour une commande, mais lorsqu'un
     * clic dans un GUI ouvre un autre GUI, il faut utiliser
     * openNextTick(...) afin de laisser Minecraft fermer
     * correctement l'ancien conteneur.
     */
    public void open(
            ServerPlayer player,
            GuiMenu menu
    ) {
        GuiSession session =
                new GuiSession(player, menu);

        menu.render(
                player,
                session,
                this
        );

        sessions.put(
                player.getUUID(),
                session
        );

        player.openMenu(
                new MenuProvider() {

                    @Override
                    public Component getDisplayName() {
                        return menu.title();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(
                            int containerId,
                            Inventory playerInventory,
                            Player ignoredPlayer
                    ) {
                        return new GuiScreenHandler(
                                containerId,
                                playerInventory,
                                GuiManager.this,
                                session
                        );
                    }
                }
        );
    }

    /**
     * Ferme l'ancien menu, puis ouvre le nouveau au tick suivant.
     * Cette méthode doit être utilisée par les boutons de GUI.
     */
    public void openNextTick(
            ServerPlayer player,
            GuiMenu menu
    ) {
        player.getServer().execute(() ->
                open(player, menu)
        );
    }

    public void refresh(ServerPlayer player) {
        GuiSession session =
                sessions.get(player.getUUID());

        if (session == null) {
            return;
        }

        session.clear();

        session.menu().render(
                player,
                session,
                this
        );

        player.containerMenu.broadcastChanges();
    }

    /**
     * Retire uniquement la session correspondant réellement au
     * menu qui vient d'être fermé.
     *
     * Si une nouvelle session a déjà été installée, elle n'est
     * pas supprimée.
     */
    public void close(
            ServerPlayer player,
            GuiSession closedSession
    ) {
        sessions.remove(
                player.getUUID(),
                closedSession
        );
    }

    public void closeAll() {
        sessions.clear();
    }
    public void openTextInput(
            ServerPlayer player,
            AnvilTextInputMenu input
    ) {
        player.getServer().execute(() ->
                player.openMenu(
                        new MenuProvider() {
                            @Override
                            public Component getDisplayName() {
                                return input.title();
                            }

                            @Override
                            public AbstractContainerMenu createMenu(
                                    int containerId,
                                    Inventory playerInventory,
                                    Player ignoredPlayer
                            ) {
                                return new AnvilTextInputHandler(
                                        containerId,
                                        playerInventory,
                                        GuiManager.this,
                                        input
                                );
                            }
                        }
                )
        );
    }
    public GuiSession session(
            UUID playerId
    ) {
        if (playerId == null) {
            return null;
        }

        return sessions.get(playerId);
    }

    public boolean isOpen(
            ServerPlayer player,
            Class<? extends GuiMenu> menuType
    ) {
        if (player == null || menuType == null) {
            return false;
        }

        GuiSession session =
                sessions.get(player.getUUID());

        return session != null
                && menuType.isInstance(
                session.menu()
        );
    }
}
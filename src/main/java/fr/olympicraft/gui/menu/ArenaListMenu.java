package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.game.GameDefinition;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

public final class ArenaListMenu
        implements GuiMenu {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final int requestedPage;

    public ArenaListMenu(int requestedPage) {
        this.requestedPage = Math.max(
                0,
                requestedPage
        );
    }

    @Override
    public Component title() {
        return Component.literal("Arènes Olympicraft");
    }

    @Override
    public void render(
            ServerPlayer player,
            GuiSession session,
            GuiManager manager
    ) {
        fill(session);

        List<ArenaDefinition> arenas =
                manager.arenas()
                        .all()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        arena -> arena.id
                                )
                        )
                        .toList();

        int pageCount = Math.max(
                1,
                (int) Math.ceil(
                        arenas.size()
                                / (double) CONTENT_SLOTS.length
                )
        );

        int page = Math.min(
                requestedPage,
                pageCount - 1
        );

        int startIndex =
                page * CONTENT_SLOTS.length;

        for (int index = 0;
             index < CONTENT_SLOTS.length;
             index++) {
            int arenaIndex = startIndex + index;

            if (arenaIndex >= arenas.size()) {
                break;
            }

            ArenaDefinition arena =
                    arenas.get(arenaIndex);

            GameDefinition game =
                    manager.games()
                            .find(arena.gameType)
                            .orElse(null);

            String gameName = game == null
                    ? arena.gameType
                    : game.displayName();

            Item icon = arena.enabled
                    ? Items.LIME_CONCRETE
                    : Items.RED_CONCRETE;

            session.setItem(
                    CONTENT_SLOTS[index],
                    GuiItemFactory.item(
                            icon,
                            arena.displayName,
                            arena.enabled
                                    ? ChatFormatting.GREEN
                                    : ChatFormatting.RED,
                            "ID : " + arena.id,
                            "Jeu : " + gameName,
                            "État : "
                                    + (
                                    arena.enabled
                                            ? "activée"
                                            : "désactivée"
                            ),
                            "Spawns : "
                                    + arena.totalSpawnCount(),
                            "Régions : "
                                    + arena.regions.size(),
                            "",
                            "Clique pour modifier."
                    ),
                    (clickedPlayer, context) ->
                            manager.open(
                                    clickedPlayer,
                                    new ArenaEditorMenu(
                                            arena.id,
                                            page
                                    )
                            )
            );
        }

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.COMPASS,
                        "Liste des arènes",
                        ChatFormatting.AQUA,
                        "Page " + (page + 1)
                                + "/" + pageCount,
                        "Total : " + arenas.size()
                ),
                null
        );

        if (page > 0) {
            session.setItem(
                    45,
                    GuiItemFactory.item(
                            Items.ARROW,
                            "Page précédente",
                            ChatFormatting.YELLOW
                    ),
                    (clickedPlayer, context) ->
                            manager.open(
                                    clickedPlayer,
                                    new ArenaListMenu(
                                            page - 1
                                    )
                            )
            );
        }

        if (page + 1 < pageCount) {
            session.setItem(
                    53,
                    GuiItemFactory.item(
                            Items.ARROW,
                            "Page suivante",
                            ChatFormatting.YELLOW
                    ),
                    (clickedPlayer, context) ->
                            manager.open(
                                    clickedPlayer,
                                    new ArenaListMenu(
                                            page + 1
                                    )
                            )
            );
        }

        session.setItem(
                48,
                GuiItemFactory.item(
                        Items.SPECTRAL_ARROW,
                        "Baguette de régions",
                        ChatFormatting.AQUA,
                        "Reçoit la baguette d'édition."
                ),
                (clickedPlayer, context) -> {
                    fr.olympicraft.Olympicraft
                            .arenaEditor()
                            .wand()
                            .giveWand(clickedPlayer);
                }
        );

        session.setItem(
                49,
                GuiItemFactory.item(
                        Items.BARRIER,
                        "Fermer",
                        ChatFormatting.RED
                ),
                (clickedPlayer, context) ->
                        clickedPlayer.closeContainer()
        );

        session.setItem(
                50,
                GuiItemFactory.item(
                        Items.OAK_DOOR,
                        "Retour",
                        ChatFormatting.YELLOW
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new OlympicraftMainMenu()
                        )
        );
    }

    private void fill(GuiSession session) {
        for (int slot = 0;
             slot < session.container().getContainerSize();
             slot++) {
            session.setItem(
                    slot,
                    GuiItemFactory.filler(),
                    null
            );
        }
    }
}

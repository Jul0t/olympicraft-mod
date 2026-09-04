package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.game.GameDefinition;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

public final class RegionArenaChoiceMenu
        implements GuiMenu {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final ArenaSelectionManager.Selection selection;
    private final int requestedPage;

    public RegionArenaChoiceMenu(
            ArenaSelectionManager.Selection selection,
            int requestedPage
    ) {
        this.selection = selection.normalized();
        this.requestedPage = Math.max(0, requestedPage);
    }

    @Override
    public Component title() {
        return Component.literal(
                "Choisir une arène"
        );
    }

    @Override
    public void render(
            ServerPlayer player,
            GuiSession session,
            GuiManager manager
    ) {
        fill(session);

        List<ArenaDefinition> arenas =
                manager.arenas().all()
                        .stream()
                        .filter(arena ->
                                arena.regions != null
                        )
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

        int startIndex = page * CONTENT_SLOTS.length;

        for (int index = 0;
             index < CONTENT_SLOTS.length;
             index++) {
            int arenaIndex = startIndex + index;

            if (arenaIndex >= arenas.size()) {
                break;
            }

            ArenaDefinition arena = arenas.get(arenaIndex);

            GameDefinition game = manager.games()
                    .find(arena.gameType)
                    .orElse(null);

            session.setItem(
                    CONTENT_SLOTS[index],
                    GuiItemFactory.item(
                            Items.COMPASS,
                            arena.displayName,
                            ChatFormatting.AQUA,
                            "ID : " + arena.id,
                            "Jeu : "
                                    + (
                                    game == null
                                            ? arena.gameType
                                            : game.displayName()
                            ),
                            "Régions : "
                                    + arena.regions.size(),
                            "",
                            "Clique pour sélectionner."
                    ),
                    (clickedPlayer, context) ->
                            manager.openNextTick(
                                    clickedPlayer,
                                    new RegionTypeChoiceMenu(
                                            arena.id,
                                            selection,
                                            page
                                    )
                            )
            );
        }

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.MAP,
                        "Création d'une région",
                        ChatFormatting.AQUA,
                        "Sélection valide",
                        "Volume : "
                                + selection.estimatedVolume()
                                + " blocs",
                        "",
                        "Choisis l'arène concernée."
                ),
                null
        );

        session.setItem(
                49,
                GuiItemFactory.item(
                        Items.BARRIER,
                        "Annuler",
                        ChatFormatting.RED
                ),
                (clickedPlayer, context) ->
                        clickedPlayer.closeContainer()
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
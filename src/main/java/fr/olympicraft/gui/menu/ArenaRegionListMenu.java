package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaRegion;
import fr.olympicraft.arena.ArenaRegionType;
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

public final class ArenaRegionListMenu
        implements GuiMenu {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final String arenaId;
    private final int requestedPage;
    private final int arenaListReturnPage;

    public ArenaRegionListMenu(
            String arenaId,
            int requestedPage,
            int arenaListReturnPage
    ) {
        this.arenaId = arenaId;
        this.requestedPage = Math.max(0, requestedPage);
        this.arenaListReturnPage =
                Math.max(0, arenaListReturnPage);
    }

    @Override
    public Component title() {
        return Component.literal(
                "Régions : " + arenaId
        );
    }

    @Override
    public void render(
            ServerPlayer player,
            GuiSession session,
            GuiManager manager
    ) {
        fill(session);

        ArenaDefinition arena = manager.arenas()
                .find(arenaId)
                .orElse(null);

        if (arena == null) {
            manager.messages().sendError(
                    player.createCommandSourceStack(),
                    "Cette arène n'existe plus."
            );
            player.closeContainer();
            return;
        }

        List<ArenaRegion> regions = arena.regions
                .values()
                .stream()
                .sorted(Comparator.comparing(
                        region -> region.id
                ))
                .toList();

        int pageCount = Math.max(
                1,
                (int) Math.ceil(
                        regions.size()
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
            int regionIndex = startIndex + index;

            if (regionIndex >= regions.size()) {
                break;
            }

            ArenaRegion region = regions.get(regionIndex);
            ArenaRegionType type = region.resolvedType();

            String typeName = type == null
                    ? region.type
                    : type.displayName();

            session.setItem(
                    CONTENT_SLOTS[index],
                    GuiItemFactory.item(
                            icon(type),
                            region.id,
                            ChatFormatting.AQUA,
                            "Type : " + typeName,
                            "Volume : "
                                    + region.volume()
                                    + " blocs",
                            "Dimension : "
                                    + region.dimension,
                            "",
                            "Clique pour modifier."
                    ),
                    (clickedPlayer, context) ->
                            manager.open(
                                    clickedPlayer,
                                    new ArenaRegionEditorMenu(
                                            arena.id,
                                            region.id,
                                            page,
                                            arenaListReturnPage
                                    )
                            )
            );
        }

        GameDefinition game = manager.games()
                .find(arena.gameType)
                .orElse(null);

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.MAP,
                        "Régions de " + arena.displayName,
                        ChatFormatting.AQUA,
                        "Page " + (page + 1)
                                + "/" + pageCount,
                        "Régions : " + regions.size(),
                        "Jeu : "
                                + (
                                game == null
                                        ? arena.gameType
                                        : game.displayName()
                        )
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
                                    new ArenaRegionListMenu(
                                            arena.id,
                                            page - 1,
                                            arenaListReturnPage
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
                                    new ArenaRegionListMenu(
                                            arena.id,
                                            page + 1,
                                            arenaListReturnPage
                                    )
                            )
            );
        }

        session.setItem(
                47,
                GuiItemFactory.item(
                        Items.SPECTRAL_ARROW,
                        "Baguette de régions",
                        ChatFormatting.AQUA,
                        "Reçoit la baguette d'édition.",
                        "",
                        "Maj + clic gauche prépare",
                        "l'assistant de création."
                ),
                (clickedPlayer, context) ->
                        fr.olympicraft.Olympicraft
                                .arenaEditor()
                                .wand()
                                .giveWand(clickedPlayer)
        );

        session.setItem(
                48,
                GuiItemFactory.item(
                        Items.ENDER_EYE,
                        "Afficher toutes les régions",
                        ChatFormatting.LIGHT_PURPLE,
                        "Les contours ne seront visibles",
                        "que pour toi."
                ),
                (clickedPlayer, context) -> {
                    if (arena.regions.isEmpty()) {
                        manager.messages().sendWarning(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                "Cette arène ne possède "
                                        + "aucune région."
                        );
                        return;
                    }

                    int seconds =
                            fr.olympicraft.Olympicraft
                                    .configs()
                                    .general()
                                    .arenaEditor
                                    .regionPreviewSeconds;

                    fr.olympicraft.Olympicraft
                            .arenaEditor()
                            .visualizations()
                            .showRegions(
                                    clickedPlayer,
                                    arena.regions
                                            .values()
                                            .stream()
                                            .toList(),
                                    seconds
                            );

                    manager.messages().sendSuccess(
                            clickedPlayer
                                    .createCommandSourceStack(),
                            arena.regions.size()
                                    + " région(s) affichée(s) "
                                    + "pendant "
                                    + seconds
                                    + " seconde(s)."
                    );
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
                        "Retour à l'arène",
                        ChatFormatting.YELLOW
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new ArenaEditorMenu(
                                        arena.id,
                                        arenaListReturnPage
                                )
                        )
        );
    }

    private Item icon(ArenaRegionType type) {
        if (type == null) {
            return Items.BARRIER;
        }

        return switch (type) {
            case GAME_BOUNDS -> Items.BARRIER;
            case PLAY_AREA -> Items.GRASS_BLOCK;
            case VOID -> Items.BLACK_CONCRETE;
            case SPECTATOR -> Items.ENDER_EYE;
            case ISLAND -> Items.GRASS_BLOCK;
            case BOMB_SITE -> Items.TNT;
            case TRAP -> Items.TRIPWIRE_HOOK;
            case FLOOR -> Items.SAND;
            case SAFE_ZONE -> Items.LIME_STAINED_GLASS;
            case PROTECTED -> Items.SHIELD;
            case POOL -> Items.WATER_BUCKET;
            case HIDE_AREA -> Items.OAK_LEAVES;
            case SUMO_RING -> Items.SLIME_BLOCK;
        };
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
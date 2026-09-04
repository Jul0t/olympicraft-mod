package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ArenaSpawnListMenu
        implements GuiMenu {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final String arenaId;
    private final String group;
    private final int requestedPage;
    private final int groupListReturnPage;
    private final int arenaListReturnPage;

    public ArenaSpawnListMenu(
            String arenaId,
            String group,
            int requestedPage,
            int groupListReturnPage,
            int arenaListReturnPage
    ) {
        this.arenaId = arenaId;
        this.group = group;
        this.requestedPage = Math.max(0, requestedPage);
        this.groupListReturnPage =
                Math.max(0, groupListReturnPage);
        this.arenaListReturnPage =
                Math.max(0, arenaListReturnPage);
    }

    @Override
    public Component title() {
        return Component.literal(
                "Spawns : " + group
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

        List<ArenaPosition> positions =
                arena.spawnGroup(group);

        int pageCount = Math.max(
                1,
                (int) Math.ceil(
                        positions.size()
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
            int spawnIndex = startIndex + index;

            if (spawnIndex >= positions.size()) {
                break;
            }

            ArenaPosition position =
                    positions.get(spawnIndex);

            int publicIndex = spawnIndex + 1;

            session.setItem(
                    CONTENT_SLOTS[index],
                    GuiItemFactory.item(
                            Items.ENDER_PEARL,
                            "Spawn #" + publicIndex,
                            ChatFormatting.AQUA,
                            position.formatted(),
                            "",
                            "Clic gauche : se téléporter.",
                            "Clic droit : supprimer."
                    ),
                    (clickedPlayer, context) -> {
                        if (context.button() == 1) {
                            manager.open(
                                    clickedPlayer,
                                    new ConfirmationMenu(
                                            "Supprimer le spawn",
                                            "Supprimer "
                                                    + group
                                                    + " #"
                                                    + publicIndex
                                                    + " ?",
                                            (confirmedPlayer,
                                             confirmationContext) -> {
                                                if (!manager.arenas()
                                                        .removeSpawn(
                                                                arena,
                                                                group,
                                                                publicIndex
                                                        )) {
                                                    manager.messages()
                                                            .sendError(
                                                                    confirmedPlayer
                                                                            .createCommandSourceStack(),
                                                                    "Le spawn n'a "
                                                                            + "pas pu être supprimé."
                                                            );
                                                    return;
                                                }

                                                manager.messages()
                                                        .sendSuccess(
                                                                confirmedPlayer
                                                                        .createCommandSourceStack(),
                                                                "Spawn "
                                                                        + group
                                                                        + " #"
                                                                        + publicIndex
                                                                        + " supprimé."
                                                        );

                                                manager.openNextTick(
                                                        confirmedPlayer,
                                                        new ArenaSpawnListMenu(
                                                                arena.id,
                                                                group,
                                                                page,
                                                                groupListReturnPage,
                                                                arenaListReturnPage
                                                        )
                                                );
                                            },
                                            new ArenaSpawnListMenu(
                                                    arena.id,
                                                    group,
                                                    page,
                                                    groupListReturnPage,
                                                    arenaListReturnPage
                                            )
                                    )
                            );

                            return;
                        }

                        if (!position.teleport(
                                clickedPlayer.getServer(),
                                clickedPlayer
                        )) {
                            manager.messages().sendError(
                                    clickedPlayer
                                            .createCommandSourceStack(),
                                    "La dimension cible "
                                            + "est introuvable."
                            );
                        }
                    }
            );
        }

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.TARGET,
                        "Groupe : " + group,
                        ChatFormatting.AQUA,
                        "Page " + (page + 1)
                                + "/" + pageCount,
                        "Spawns : " + positions.size()
                ),
                null
        );

        session.setItem(
                47,
                GuiItemFactory.item(
                        Items.LIME_DYE,
                        "Ajouter un spawn ici",
                        ChatFormatting.GREEN,
                        "Ajoute ta position actuelle",
                        "au groupe " + group + "."
                ),
                (clickedPlayer, context) -> {
                    int publicIndex =
                            manager.arenas().addSpawn(
                                    arena,
                                    group,
                                    clickedPlayer
                            );

                    if (publicIndex < 1) {
                        manager.messages().sendError(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                "Le spawn n'a pas pu "
                                        + "être enregistré."
                        );
                        return;
                    }

                    manager.messages().sendSuccess(
                            clickedPlayer
                                    .createCommandSourceStack(),
                            "Spawn " + group
                                    + " #"
                                    + publicIndex
                                    + " ajouté."
                    );

                    manager.refresh(clickedPlayer);
                }
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
                                    new ArenaSpawnListMenu(
                                            arena.id,
                                            group,
                                            page - 1,
                                            groupListReturnPage,
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
                                    new ArenaSpawnListMenu(
                                            arena.id,
                                            group,
                                            page + 1,
                                            groupListReturnPage,
                                            arenaListReturnPage
                                    )
                            )
            );
        }

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
                        "Retour aux groupes",
                        ChatFormatting.YELLOW
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new ArenaSpawnGroupListMenu(
                                        arena.id,
                                        groupListReturnPage,
                                        arenaListReturnPage
                                )
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
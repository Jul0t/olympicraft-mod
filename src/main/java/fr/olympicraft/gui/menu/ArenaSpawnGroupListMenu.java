package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import fr.olympicraft.gui.input.AnvilTextInputMenu;
import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.gui.GuiMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ArenaSpawnGroupListMenu
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

    public ArenaSpawnGroupListMenu(
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
                "Groupes : " + arenaId
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

        List<Map.Entry<String, List<ArenaPosition>>> groups =
                arena.spawns.entrySet()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        Map.Entry::getKey
                                )
                        )
                        .toList();

        int pageCount = Math.max(
                1,
                (int) Math.ceil(
                        groups.size()
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
            int groupIndex = startIndex + index;

            if (groupIndex >= groups.size()) {
                break;
            }

            Map.Entry<String, List<ArenaPosition>> entry =
                    groups.get(groupIndex);

            String group = entry.getKey();
            int amount = entry.getValue().size();

            session.setItem(
                    CONTENT_SLOTS[index],
                    GuiItemFactory.item(
                            Items.ARMOR_STAND,
                            group,
                            ChatFormatting.AQUA,
                            "Spawns : " + amount,
                            "",
                            "Clique pour voir les positions."
                    ),
                    (clickedPlayer, context) ->
                            manager.open(
                                    clickedPlayer,
                                    new ArenaSpawnListMenu(
                                            arena.id,
                                            group,
                                            0,
                                            page,
                                            arenaListReturnPage
                                    )
                            )
            );
        }

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.TARGET,
                        "Groupes de spawns",
                        ChatFormatting.AQUA,
                        "Page " + (page + 1)
                                + "/" + pageCount,
                        "Groupes : " + groups.size(),
                        "Total : "
                                + arena.totalSpawnCount()
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
                                    new ArenaSpawnGroupListMenu(
                                            arena.id,
                                            page - 1,
                                            arenaListReturnPage
                                    )
                            )
            );
            session.setItem(
                    47,
                    GuiItemFactory.item(
                            Items.LIME_DYE,
                            "Créer un groupe",
                            ChatFormatting.GREEN,
                            "Saisis son nom dans une enclume.",
                            "Ta position actuelle deviendra",
                            "le premier spawn du groupe."
                    ),
                    (clickedPlayer, context) -> {
                        GuiMenu returnMenu =
                                new ArenaSpawnGroupListMenu(
                                        arena.id,
                                        page,
                                        arenaListReturnPage
                                );

                        manager.openTextInput(
                                clickedPlayer,
                                new AnvilTextInputMenu(
                                        Component.literal(
                                                "Nom du groupe"
                                        ),
                                        "",
                                        "players",
                                        (submittedPlayer, submittedName) -> {
                                            String group =
                                                    ArenaManager.normalizeGroup(
                                                            submittedName
                                                    );

                                            if (group.isBlank()) {
                                                manager.messages().sendError(
                                                        submittedPlayer
                                                                .createCommandSourceStack(),
                                                        "Le nom du groupe "
                                                                + "est invalide."
                                                );

                                                manager.openNextTick(
                                                        submittedPlayer,
                                                        returnMenu
                                                );

                                                return;
                                            }

                                            if (arena.spawns.containsKey(group)) {
                                                manager.messages().sendError(
                                                        submittedPlayer
                                                                .createCommandSourceStack(),
                                                        "Ce groupe existe déjà."
                                                );

                                                manager.openNextTick(
                                                        submittedPlayer,
                                                        returnMenu
                                                );

                                                return;
                                            }

                                            int index = manager.arenas()
                                                    .addSpawn(
                                                            arena,
                                                            group,
                                                            submittedPlayer
                                                    );

                                            if (index < 1) {
                                                manager.messages().sendError(
                                                        submittedPlayer
                                                                .createCommandSourceStack(),
                                                        "Le groupe n'a pas "
                                                                + "pu être créé."
                                                );

                                                return;
                                            }

                                            manager.messages().sendSuccess(
                                                    submittedPlayer
                                                            .createCommandSourceStack(),
                                                    "Groupe " + group
                                                            + " créé avec "
                                                            + "le spawn #1."
                                            );

                                            manager.openNextTick(
                                                    submittedPlayer,
                                                    new ArenaSpawnListMenu(
                                                            arena.id,
                                                            group,
                                                            0,
                                                            page,
                                                            arenaListReturnPage
                                                    )
                                            );
                                        },
                                        returnMenu
                                )
                        );
                    }
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
                                    new ArenaSpawnGroupListMenu(
                                            arena.id,
                                            page + 1,
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
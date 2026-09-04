package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaValidationResult;
import fr.olympicraft.game.GameDefinition;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public final class ArenaEditorMenu
        implements GuiMenu {

    private final String arenaId;
    private final int returnPage;

    public ArenaEditorMenu(
            String arenaId,
            int returnPage
    ) {
        this.arenaId = arenaId;
        this.returnPage = Math.max(
                0,
                returnPage
        );
    }

    @Override
    public Component title() {
        return Component.literal(
                "Arène : " + arenaId
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

        GameDefinition game = manager.games()
                .find(arena.gameType)
                .orElse(null);

        ArenaValidationResult validation =
                game == null
                        ? arena.validateBase()
                        : game.validateArena(arena);

        String gameName = game == null
                ? arena.gameType
                : game.displayName();

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.NAME_TAG,
                        arena.displayName,
                        ChatFormatting.AQUA,
                        "ID : " + arena.id,
                        "Jeu : " + gameName,
                        "Spawns : "
                                + arena.totalSpawnCount(),
                        "Régions : "
                                + arena.regions.size()
                ),
                null
        );

        session.setItem(
                20,
                GuiItemFactory.item(
                        arena.enabled
                                ? Items.LIME_DYE
                                : Items.GRAY_DYE,
                        arena.enabled
                                ? "Arène activée"
                                : "Activer l'arène",
                        arena.enabled
                                ? ChatFormatting.GREEN
                                : ChatFormatting.YELLOW,
                        arena.enabled
                                ? "Clique pour désactiver."
                                : "Clique pour activer."
                ),
                (clickedPlayer, context) -> {
                    boolean target = !arena.enabled;

                    if (!manager.arenas()
                            .setEnabled(arena, target)) {
                        manager.messages().sendError(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                "L'état de l'arène n'a pas "
                                        + "pu être modifié. "
                                        + "Vérifie sa validation."
                        );

                        return;
                    }

                    manager.refresh(clickedPlayer);
                }
        );

        session.setItem(
                22,
                GuiItemFactory.item(
                        validation.valid()
                                ? Items.LIME_CONCRETE
                                : Items.RED_CONCRETE,
                        validation.valid()
                                ? "Arène valide"
                                : "Arène invalide",
                        validation.valid()
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED,
                        "Erreurs : "
                                + validation.errors().size(),
                        "Avertissements : "
                                + validation.warnings().size(),
                        "",
                        "Les détails sont disponibles",
                        "avec /oc arena validate "
                                + arena.id
                ),
                null
        );

        session.setItem(
                24,
                GuiItemFactory.item(
                        Items.ENDER_PEARL,
                        "Téléporter au lobby",
                        ChatFormatting.LIGHT_PURPLE,
                        arena.lobby == null
                                ? "Lobby non défini."
                                : arena.lobby.formatted()
                ),
                (clickedPlayer, context) -> {
                    if (arena.lobby == null) {
                        manager.messages().sendError(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                "Le lobby n'est pas défini."
                        );
                        return;
                    }

                    boolean success = arena.lobby.teleport(
                            clickedPlayer.getServer(),
                            clickedPlayer
                    );

                    if (!success) {
                        manager.messages().sendError(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                "La dimension cible "
                                        + "est introuvable."
                        );
                    }
                }
        );

        session.setItem(
                29,
                GuiItemFactory.item(
                        Items.RESPAWN_ANCHOR,
                        "Définir le lobby ici",
                        ChatFormatting.YELLOW,
                        "Enregistre ta position actuelle."
                ),
                (clickedPlayer, context) -> {
                    if (!manager.arenas().setLobby(
                            arena,
                            clickedPlayer
                    )) {
                        manager.messages().sendError(
                                clickedPlayer.createCommandSourceStack(),
                                "Le lobby n'a pas pu être enregistré."
                        );

                        return;
                    }

                    manager.messages().sendSuccess(
                            clickedPlayer.createCommandSourceStack(),
                            "Le lobby de l'arène "
                                    + arena.id
                                    + " a été défini à ta position."
                    );

                    manager.messages().sendInfo(
                            clickedPlayer.createCommandSourceStack(),
                            "Position : "
                                    + arena.lobby.formatted()
                    );

                    manager.refresh(clickedPlayer);
                }
        );

        session.setItem(
                31,
                GuiItemFactory.item(
                        Items.ENDER_EYE,
                        "Définir le point spectateur",
                        ChatFormatting.YELLOW,
                        "Enregistre ta position actuelle."
                ),
                (clickedPlayer, context) -> {
                    if (!manager.arenas().setSpectator(
                            arena,
                            clickedPlayer
                    )) {
                        manager.messages().sendError(
                                clickedPlayer.createCommandSourceStack(),
                                "Le point spectateur n'a pas pu "
                                        + "être enregistré."
                        );

                        return;
                    }

                    manager.messages().sendSuccess(
                            clickedPlayer.createCommandSourceStack(),
                            "Le point spectateur de l'arène "
                                    + arena.id
                                    + " a été défini à ta position."
                    );

                    manager.messages().sendInfo(
                            clickedPlayer.createCommandSourceStack(),
                            "Position : "
                                    + arena.spectator.formatted()
                    );

                    manager.refresh(clickedPlayer);
                }
        );

        session.setItem(
                33,
                GuiItemFactory.item(
                        Items.SPECTRAL_ARROW,
                        "Baguette de régions",
                        ChatFormatting.AQUA,
                        "Reçoit la baguette Olympicraft."
                ),
                (clickedPlayer, context) ->
                        fr.olympicraft.Olympicraft
                                .arenaEditor()
                                .wand()
                                .giveWand(clickedPlayer)
        );

        session.setItem(
                39,
                GuiItemFactory.item(
                        Items.ARMOR_STAND,
                        "Spawns",
                        ChatFormatting.GOLD,
                        "Groupes : "
                                + arena.spawns.size(),
                        "Positions : "
                                + arena.totalSpawnCount(),
                        "",
                        "Clique pour gérer les spawns."
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new ArenaSpawnGroupListMenu(
                                        arena.id,
                                        0,
                                        returnPage
                                )
                        )
        );
        session.setItem(
                41,
                GuiItemFactory.item(
                        Items.MAP,
                        "Régions",
                        ChatFormatting.GOLD,
                        "Régions : "
                                + arena.regions.size(),
                        "",
                        "Clique pour gérer les régions."
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new ArenaRegionListMenu(
                                        arena.id,
                                        0,
                                        returnPage
                                )
                        )
        );

        session.setItem(
                45,
                GuiItemFactory.item(
                        Items.ARROW,
                        "Retour aux arènes",
                        ChatFormatting.YELLOW
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new ArenaListMenu(returnPage)
                        )
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
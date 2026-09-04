package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.arena.ArenaRegion;
import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public final class ArenaRegionEditorMenu
        implements GuiMenu {

    private final String arenaId;
    private final String regionId;
    private final int regionListReturnPage;
    private final int arenaListReturnPage;

    public ArenaRegionEditorMenu(
            String arenaId,
            String regionId,
            int regionListReturnPage,
            int arenaListReturnPage
    ) {
        this.arenaId = arenaId;
        this.regionId = regionId;
        this.regionListReturnPage =
                Math.max(0, regionListReturnPage);
        this.arenaListReturnPage =
                Math.max(0, arenaListReturnPage);
    }

    @Override
    public Component title() {
        return Component.literal(
                "Région : " + regionId
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

        ArenaRegion region = arena.region(regionId);

        if (region == null) {
            manager.messages().sendError(
                    player.createCommandSourceStack(),
                    "Cette région n'existe plus."
            );

            manager.open(
                    player,
                    new ArenaRegionListMenu(
                            arena.id,
                            regionListReturnPage,
                            arenaListReturnPage
                    )
            );
            return;
        }

        ArenaRegionType type = region.resolvedType();

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.MAP,
                        region.id,
                        ChatFormatting.AQUA,
                        "Type : "
                                + (
                                type == null
                                        ? region.type
                                        : type.displayName()
                        ),
                        "Volume : "
                                + region.volume()
                                + " blocs",
                        "Limites :",
                        region.formattedBounds()
                ),
                null
        );

        session.setItem(
                20,
                GuiItemFactory.item(
                        Items.ENDER_EYE,
                        "Afficher le contour",
                        ChatFormatting.LIGHT_PURPLE,
                        "Visible uniquement pour toi."
                ),
                (clickedPlayer, context) -> {
                    int seconds =
                            fr.olympicraft.Olympicraft
                                    .configs()
                                    .general()
                                    .arenaEditor
                                    .regionPreviewSeconds;

                    fr.olympicraft.Olympicraft
                            .arenaEditor()
                            .visualizations()
                            .showRegion(
                                    clickedPlayer,
                                    region,
                                    seconds
                            );

                    manager.messages().sendSuccess(
                            clickedPlayer
                                    .createCommandSourceStack(),
                            "Contour de " + region.id
                                    + " affiché pendant "
                                    + seconds
                                    + " seconde(s)."
                    );
                }
        );

        session.setItem(
                22,
                GuiItemFactory.item(
                        Items.ENDER_PEARL,
                        "Téléporter à la région",
                        ChatFormatting.LIGHT_PURPLE,
                        "Téléporte au-dessus de son centre."
                ),
                (clickedPlayer, context) -> {
                    ArenaPosition position =
                            new ArenaPosition(
                                    region.dimension,
                                    region.center().getX()
                                            + 0.5,
                                    region.maxY + 1.0,
                                    region.center().getZ()
                                            + 0.5,
                                    clickedPlayer.getYRot(),
                                    clickedPlayer.getXRot()
                            );

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

        session.setItem(
                24,
                GuiItemFactory.item(
                        Items.SPECTRAL_ARROW,
                        "Redéfinir avec la sélection",
                        ChatFormatting.YELLOW,
                        "Utilise la sélection Olympicraft",
                        "ou WorldEdit actuellement active."
                ),
                (clickedPlayer, context) -> {
                    var selectionResult =
                            fr.olympicraft.Olympicraft
                                    .selectionService()
                                    .resolve(clickedPlayer);

                    if (!selectionResult.successful()) {
                        manager.messages().sendError(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                selectionResult.error()
                        );
                        return;
                    }

                    var result = manager.arenas()
                            .redefineRegion(
                                    arena,
                                    region.id,
                                    selectionResult.selection()
                            );

                    if (!result.successful()) {
                        manager.messages().sendError(
                                clickedPlayer
                                        .createCommandSourceStack(),
                                result.error()
                        );
                        return;
                    }

                    manager.messages().sendSuccess(
                            clickedPlayer
                                    .createCommandSourceStack(),
                            "La région " + region.id
                                    + " a été redéfinie."
                    );

                    manager.refresh(clickedPlayer);
                }
        );

        session.setItem(
                31,
                GuiItemFactory.item(
                        Items.LAVA_BUCKET,
                        "Supprimer la région",
                        ChatFormatting.RED,
                        "Une confirmation sera demandée."
                ),
                (clickedPlayer, context) ->
                        manager.openNextTick(
                                clickedPlayer,
                                new ConfirmationMenu(
                                        "Supprimer la région",
                                        "Supprimer "
                                                + region.id
                                                + " de "
                                                + arena.id
                                                + " ?",
                                        (confirmedPlayer,
                                         confirmationContext) -> {
                                            if (!manager.arenas()
                                                    .removeRegion(
                                                            arena,
                                                            region.id
                                                    )) {
                                                manager.messages()
                                                        .sendError(
                                                                confirmedPlayer
                                                                        .createCommandSourceStack(),
                                                                "La région n'a "
                                                                        + "pas pu être supprimée."
                                                        );
                                                return;
                                            }

                                            manager.messages()
                                                    .sendSuccess(
                                                            confirmedPlayer
                                                                    .createCommandSourceStack(),
                                                            "La région "
                                                                    + region.id
                                                                    + " a été supprimée."
                                                    );

                                            manager.open(
                                                    confirmedPlayer,
                                                    new ArenaRegionListMenu(
                                                            arena.id,
                                                            regionListReturnPage,
                                                            arenaListReturnPage
                                                    )
                                            );
                                        },
                                        new ArenaRegionEditorMenu(
                                                arena.id,
                                                region.id,
                                                regionListReturnPage,
                                                arenaListReturnPage
                                        )
                                )
                        )
        );

        session.setItem(
                45,
                GuiItemFactory.item(
                        Items.ARROW,
                        "Retour aux régions",
                        ChatFormatting.YELLOW
                ),
                (clickedPlayer, context) ->
                        manager.open(
                                clickedPlayer,
                                new ArenaRegionListMenu(
                                        arena.id,
                                        regionListReturnPage,
                                        arenaListReturnPage
                                )
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
package fr.olympicraft.gui.menu;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.arena.RegionRequirement;
import fr.olympicraft.game.GameDefinition;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;
import fr.olympicraft.gui.flow.region.RegionCreationRequest;
import fr.olympicraft.gui.input.AnvilTextInputMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

public final class RegionTypeChoiceMenu
        implements GuiMenu {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final String arenaId;
    private final ArenaSelectionManager.Selection selection;
    private final int arenaChoiceReturnPage;

    public RegionTypeChoiceMenu(
            String arenaId,
            ArenaSelectionManager.Selection selection,
            int arenaChoiceReturnPage
    ) {
        this.arenaId = arenaId;
        this.selection = selection.normalized();
        this.arenaChoiceReturnPage =
                Math.max(0, arenaChoiceReturnPage);
    }

    @Override
    public Component title() {
        return Component.literal(
                "Choisir le type"
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
            player.closeContainer();
            return;
        }

        GameDefinition game = manager.games()
                .find(arena.gameType)
                .orElse(null);

        if (game == null) {
            manager.messages().sendError(
                    player.createCommandSourceStack(),
                    "Le jeu de cette arène "
                            + "n'est pas enregistré."
            );
            player.closeContainer();
            return;
        }

        List<ArenaRegionType> types =
                game.allowedRegionTypes()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        ArenaRegionType::id
                                )
                        )
                        .toList();

        for (int index = 0;
             index < Math.min(
                     CONTENT_SLOTS.length,
                     types.size()
             );
             index++) {
            ArenaRegionType type = types.get(index);
            RegionRequirement requirement =
                    game.regionRequirement(type);

            long current =
                    game.regionCount(arena, type);

            boolean available =
                    game.canAddRegion(arena, type);

            String maximum = requirement.unlimited()
                    ? "∞"
                    : String.valueOf(
                    requirement.maximum()
            );

            session.setItem(
                    CONTENT_SLOTS[index],
                    GuiItemFactory.item(
                            icon(type),
                            type.displayName(),
                            available
                                    ? ChatFormatting.AQUA
                                    : ChatFormatting.DARK_GRAY,
                            "Actuelles : "
                                    + current
                                    + "/"
                                    + maximum,
                            requirement.minimum() > 0
                                    ? "Obligatoire"
                                    : "Facultatif",
                            "",
                            available
                                    ? "Clique pour continuer."
                                    : "Limite atteinte."
                    ),
                    available
                            ? (clickedPlayer, context) ->
                            openNameInput(
                                    clickedPlayer,
                                    manager,
                                    arena,
                                    game,
                                    type
                            )
                            : null
            );
        }

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.NAME_TAG,
                        arena.displayName,
                        ChatFormatting.AQUA,
                        "Jeu : " + game.displayName(),
                        "Volume : "
                                + selection.estimatedVolume()
                                + " blocs"
                ),
                null
        );

        session.setItem(
                45,
                GuiItemFactory.item(
                        Items.ARROW,
                        "Retour",
                        ChatFormatting.YELLOW
                ),
                (clickedPlayer, context) ->
                        manager.openNextTick(
                                clickedPlayer,
                                new RegionArenaChoiceMenu(
                                        selection,
                                        arenaChoiceReturnPage
                                )
                        )
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

    private void openNameInput(
            ServerPlayer player,
            GuiManager manager,
            ArenaDefinition arena,
            GameDefinition game,
            ArenaRegionType type
    ) {
        RegionRequirement requirement =
                game.regionRequirement(type);

        String automaticName =
                manager.arenas().nextRegionId(
                        arena,
                        type,
                        requirement
                );

        GuiMenu returnMenu =
                new RegionTypeChoiceMenu(
                        arena.id,
                        selection,
                        arenaChoiceReturnPage
                );

        AnvilTextInputMenu input =
                new AnvilTextInputMenu(
                        Component.literal(
                                "Nom de la région"
                        ),
                        "",
                        automaticName,
                        (submittedPlayer, submittedName) -> {
                            RegionCreationRequest request =
                                    new RegionCreationRequest(
                                            submittedPlayer.getUUID(),
                                            arena.id,
                                            type,
                                            submittedName,
                                            selection
                                    );

                            var result = manager
                                    .regionCreationFlow()
                                    .create(request);

                            if (!result.successful()) {
                                manager.messages().sendError(
                                        submittedPlayer
                                                .createCommandSourceStack(),
                                        result.error()
                                );

                                manager.openNextTick(
                                        submittedPlayer,
                                        returnMenu
                                );

                                return;
                            }

                            manager.messages().sendSuccess(
                                    submittedPlayer
                                            .createCommandSourceStack(),
                                    "La région "
                                            + result.region().id
                                            + " a été créée."
                            );

                            manager.openNextTick(
                                    submittedPlayer,
                                    new ArenaRegionEditorMenu(
                                            arena.id,
                                            result.region().id,
                                            0,
                                            0
                                    )
                            );
                        },
                        returnMenu
                );

        manager.openTextInput(
                player,
                input
        );
    }

    private Item icon(ArenaRegionType type) {
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
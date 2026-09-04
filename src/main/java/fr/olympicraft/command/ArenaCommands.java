package fr.olympicraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.arena.ArenaValidationResult;
import fr.olympicraft.arena.ArenaBlockPosition;
import fr.olympicraft.arena.ArenaRegion;
import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.arena.RegionRequirement;
import fr.olympicraft.arena.SelectionResult;
import fr.olympicraft.arena.SelectionService;
import fr.olympicraft.arena.editor.ArenaEditorManager;
import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.message.MessageService;
import fr.olympicraft.game.GameDefinition;
import fr.olympicraft.game.GameRegistry;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.menu.ArenaEditorMenu;
import fr.olympicraft.gui.menu.ArenaListMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import java.util.concurrent.CompletableFuture;
import java.util.Locale;
import java.util.List;
import java.util.Map;

public final class ArenaCommands {

    private ArenaCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create(
            ArenaManager arenas,
            GameRegistry games,
            SelectionService selectionService,
            ArenaEditorManager arenaEditor,
            OlympicraftConfigManager configs,
            GuiManager gui,
            MessageService messages
    ) {
        LiteralArgumentBuilder<CommandSourceStack> arenaRoot =
                Commands.literal("arena")
                        .requires(source -> source.hasPermission(2));

        /*
         * /oc arena list
         */
        arenaRoot.then(
                Commands.literal("list")
                        .executes(context ->
                                list(
                                        context.getSource(),
                                        arenas,
                                        games,
                                        messages
                                )
                        )
        );

        /*
         * /oc arena create <name> <game>
         */
        arenaRoot.then(
                Commands.literal("create")
                        .then(
                                Commands.argument(
                                                "name",
                                                StringArgumentType.word()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "game",
                                                                StringArgumentType.word()
                                                        )
                                                        .suggests(
                                                                (context, builder) -> {
                                                                    String remaining =
                                                                            builder.getRemaining()
                                                                                    .toLowerCase(
                                                                                            Locale.ROOT
                                                                                    );

                                                                    for (GameDefinition game :
                                                                            games.all()) {
                                                                        if (game.id()
                                                                                .startsWith(
                                                                                        remaining
                                                                                )) {
                                                                            builder.suggest(
                                                                                    game.id(),
                                                                                    Component.literal(
                                                                                            game.displayName()
                                                                                    )
                                                                            );
                                                                        }
                                                                    }

                                                                    return builder.buildFuture();
                                                                }
                                                        )
                                                        .executes(context ->
                                                                createArena(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        games,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "name"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "game"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );

        /*
         * /oc arena info <arena>
         */
        arenaRoot.then(
                Commands.literal("info")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                info(
                                                        context.getSource(),
                                                        arenas,
                                                        games,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );

        /*
         * /oc arena enable <arena>
         */
        arenaRoot.then(
                Commands.literal("enable")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                setEnabled(
                                                        context.getSource(),
                                                        arenas,
                                                        games,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        ),
                                                        true
                                                )
                                        )
                        )
        );

        /*
         * /oc arena disable <arena>
         */
        arenaRoot.then(
                Commands.literal("disable")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                setEnabled(
                                                        context.getSource(),
                                                        arenas,
                                                        games,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        ),
                                                        false
                                                )
                                        )
                        )
        );

        /*
         * /oc arena delete <arena>
         * /oc arena delete <arena> confirm
         */
        arenaRoot.then(
                Commands.literal("delete")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context -> {
                                            String arenaId =
                                                    StringArgumentType.getString(
                                                            context,
                                                            "arena"
                                                    );

                                            messages.sendWarning(
                                                    context.getSource(),
                                                    "Confirme avec /oc arena delete "
                                                            + arenaId
                                                            + " confirm"
                                            );

                                            return 0;
                                        })
                                        .then(
                                                Commands.literal("confirm")
                                                        .executes(context ->
                                                                delete(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("setlobby")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                setLobby(
                                                        context.getSource(),
                                                        arenas,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("setspectator")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                setSpectator(
                                                        context.getSource(),
                                                        arenas,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("addspawn")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                Commands.argument(
                                                                "group",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(context ->
                                                                addSpawn(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "group"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("removespawn")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                spawnGroupArgument(arenas)
                                                        .then(
                                                                Commands.argument(
                                                                                "index",
                                                                                IntegerArgumentType.integer(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(context ->
                                                                                removeSpawn(
                                                                                        context.getSource(),
                                                                                        arenas,
                                                                                        messages,
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "arena"
                                                                                        ),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "group"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "index"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("listspawns")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                listSpawns(
                                                        context.getSource(),
                                                        arenas,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("tp")
                        .then(
                                arenaArgument(arenas)

                                        .then(
                                                Commands.literal("lobby")
                                                        .executes(context ->
                                                                teleportToNamedPosition(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        "lobby"
                                                                )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("spectator")
                                                        .executes(context ->
                                                                teleportToNamedPosition(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        "spectator"
                                                                )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("spawn")
                                                        .then(
                                                                spawnGroupArgument(
                                                                        arenas
                                                                )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "index",
                                                                                                IntegerArgumentType.integer(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(context ->
                                                                                                teleportToSpawn(
                                                                                                        context.getSource(),
                                                                                                        arenas,
                                                                                                        messages,
                                                                                                        StringArgumentType.getString(
                                                                                                                context,
                                                                                                                "arena"
                                                                                                        ),
                                                                                                        StringArgumentType.getString(
                                                                                                                context,
                                                                                                                "group"
                                                                                                        ),
                                                                                                        IntegerArgumentType.getInteger(
                                                                                                                context,
                                                                                                                "index"
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("pos1")
                        .executes(context ->
                                setSelectionPosition(
                                        context.getSource(),
                                        selectionService.nativeSelections(),
                                        messages,
                                        true
                                )
                        )
        );
        arenaRoot.then(
                Commands.literal("pos2")
                        .executes(context ->
                                setSelectionPosition(
                                        context.getSource(),
                                        selectionService.nativeSelections(),
                                        messages,
                                        false
                                )
                        )
        );
        arenaRoot.then(
                Commands.literal("selection")
                        .executes(context ->
                                showSelection(
                                        context.getSource(),
                                        selectionService.nativeSelections(),
                                        messages
                                )
                        )

                        .then(
                                Commands.literal("clear")
                                        .executes(context ->
                                                clearSelection(
                                                        context.getSource(),
                                                        selectionService.nativeSelections(),
                                                        messages
                                                )
                                        )
                        )

                        .then(
                                Commands.literal("importworldedit")
                                        .executes(context ->
                                                importWorldEditSelection(
                                                        context.getSource(),
                                                        selectionService,
                                                        messages
                                                )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("validate")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                validateArena(
                                                        context.getSource(),
                                                        arenas,
                                                        games,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );
        arenaRoot.then(
                Commands.literal("wand")
                        .executes(context ->
                                giveWand(
                                        context.getSource(),
                                        arenaEditor,
                                        messages
                                )
                        )
        );
        arenaRoot.then(
                Commands.literal("menu")
                        .executes(context ->
                                openArenaListMenu(
                                        context.getSource(),
                                        gui,
                                        messages
                                )
                        )
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                openArenaEditorMenu(
                                                        context.getSource(),
                                                        gui,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );
        LiteralArgumentBuilder<CommandSourceStack> regionRoot =
                Commands.literal("region");
        regionRoot.then(
                Commands.literal("add")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                Commands.argument(
                                                                "type",
                                                                StringArgumentType.word()
                                                        )

                                                        .suggests(
                                                                (context, builder) ->
                                                                        suggestRegionTypes(
                                                                                context,
                                                                                builder,
                                                                                arenas,
                                                                                games
                                                                        )
                                                        )

                                                        /*
                                                         * Sans nom explicite :
                                                         *
                                                         * /oc arena region add
                                                         * <arène> <type>
                                                         */
                                                        .executes(context ->
                                                                addRegion(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        games,
                                                                        selectionService,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "type"
                                                                        ),
                                                                        null
                                                                )
                                                        )

                                                        /*
                                                         * Avec nom explicite :
                                                         *
                                                         * /oc arena region add
                                                         * <arène> <type> <nom>
                                                         */
                                                        .then(
                                                                Commands.argument(
                                                                                "name",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(context ->
                                                                                addRegion(
                                                                                        context.getSource(),
                                                                                        arenas,
                                                                                        games,
                                                                                        selectionService,
                                                                                        messages,
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "arena"
                                                                                        ),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "type"
                                                                                        ),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "name"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );

        regionRoot.then(
                Commands.literal("list")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                listRegions(
                                                        context.getSource(),
                                                        arenas,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );
        regionRoot.then(
                Commands.literal("info")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                regionArgument(arenas)
                                                        .executes(context ->
                                                                regionInfo(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "region"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );
        regionRoot.then(
                Commands.literal("redefine")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                regionArgument(arenas)
                                                        .executes(context ->
                                                                redefineRegion(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        selectionService,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "region"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );
        regionRoot.then(
                Commands.literal("remove")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                regionArgument(arenas)
                                                        .executes(context ->
                                                                removeRegion(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "region"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );
        regionRoot.then(
                Commands.literal("tp")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                regionArgument(arenas)
                                                        .executes(context ->
                                                                teleportToRegion(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "region"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );
        regionRoot.then(
                Commands.literal("show")
                        .then(
                                arenaArgument(arenas)
                                        .then(
                                                regionArgument(arenas)
                                                        .executes(context ->
                                                                showRegion(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        arenaEditor,
                                                                        configs,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "region"
                                                                        ),
                                                                        configs.general()
                                                                                .arenaEditor
                                                                                .regionPreviewSeconds
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "seconds",
                                                                                IntegerArgumentType.integer(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(context ->
                                                                                showRegion(
                                                                                        context.getSource(),
                                                                                        arenas,
                                                                                        arenaEditor,
                                                                                        configs,
                                                                                        messages,
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "arena"
                                                                                        ),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "region"
                                                                                        ),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "seconds"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
        regionRoot.then(
                Commands.literal("showall")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                showAllRegions(
                                                        context.getSource(),
                                                        arenas,
                                                        arenaEditor,
                                                        configs,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );
        regionRoot.then(
                Commands.literal("hide")
                        .executes(context ->
                                hideRegions(
                                        context.getSource(),
                                        arenaEditor,
                                        messages
                                )
                        )
        );
        arenaRoot.then(regionRoot);

        return arenaRoot;
    }


    private static com.mojang.brigadier.builder
            .RequiredArgumentBuilder<CommandSourceStack, String>
    arenaArgument(ArenaManager arenas) {
        return Commands.argument(
                        "arena",
                        StringArgumentType.word()
                )
                .suggests((context, builder) -> {
                    for (String id : arenas.idsStartingWith(
                            builder.getRemaining()
                    )) {
                        builder.suggest(id);
                    }

                    return builder.buildFuture();
                });
    }
    private static com.mojang.brigadier.builder
            .RequiredArgumentBuilder<CommandSourceStack, String>
    spawnGroupArgument(ArenaManager arenas) {
        return Commands.argument(
                        "group",
                        StringArgumentType.word()
                )
                .suggests((context, builder) -> {
                    String arenaInput;

                    try {
                        arenaInput = StringArgumentType.getString(
                                context,
                                "arena"
                        );
                    } catch (IllegalArgumentException exception) {
                        return builder.buildFuture();
                    }

                    ArenaDefinition arena = arenas.find(arenaInput)
                            .orElse(null);

                    if (arena == null) {
                        return builder.buildFuture();
                    }

                    for (String group :
                            arenas.spawnGroupsStartingWith(
                                    arena,
                                    builder.getRemaining()
                            )) {
                        builder.suggest(group);
                    }

                    return builder.buildFuture();
                });
    }
    private static com.mojang.brigadier.builder
            .RequiredArgumentBuilder<CommandSourceStack, String>
    regionArgument(ArenaManager arenas) {
        return Commands.argument(
                        "region",
                        StringArgumentType.word()
                )
                .suggests((context, builder) -> {
                    String arenaInput;

                    try {
                        arenaInput = StringArgumentType.getString(
                                context,
                                "arena"
                        );
                    } catch (IllegalArgumentException exception) {
                        return builder.buildFuture();
                    }

                    ArenaDefinition arena = arenas.find(arenaInput)
                            .orElse(null);

                    if (arena == null) {
                        return builder.buildFuture();
                    }

                    for (String regionId :
                            arenas.regionIdsStartingWith(
                                    arena,
                                    builder.getRemaining()
                            )) {
                        builder.suggest(regionId);
                    }

                    return builder.buildFuture();
                });
    }

    private static CompletableFuture<Suggestions>
    suggestRegionTypes(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder,
            ArenaManager arenas,
            GameRegistry games
    ) {
        String arenaInput;

        try {
            arenaInput = StringArgumentType.getString(
                    context,
                    "arena"
            );
        } catch (IllegalArgumentException exception) {
            return builder.buildFuture();
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            return builder.buildFuture();
        }

        GameDefinition game = games.find(arena.gameType)
                .orElse(null);

        if (game == null) {
            return builder.buildFuture();
        }

        String remaining = builder.getRemaining()
                .toLowerCase(Locale.ROOT);

        for (ArenaRegionType type :
                game.allowedRegionTypes()) {

            if (!type.id().startsWith(remaining)) {
                continue;
            }

            /*
             * Un type qui a atteint son maximum n'est plus proposé.
             */
            if (!game.canAddRegion(arena, type)) {
                continue;
            }

            RegionRequirement requirement =
                    game.regionRequirement(type);

            String tooltip = type.displayName();

            if (requirement != null
                    && !requirement.unlimited()) {
                long current = game.regionCount(
                        arena,
                        type
                );

                tooltip += " ("
                        + current
                        + "/"
                        + requirement.maximum()
                        + ")";
            }

            builder.suggest(
                    type.id(),
                    Component.literal(tooltip)
            );
        }

        return builder.buildFuture();
    }

    private static int list(
            CommandSourceStack source,
            ArenaManager arenas,
            GameRegistry games,
            MessageService messages
    ) {
        if (arenas.all().isEmpty()) {
            messages.sendWarning(
                    source,
                    "Aucune arène n'est configurée dans ce monde."
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Arènes Olympicraft")
                        .withStyle(ChatFormatting.AQUA),
                false
        );

        for (ArenaDefinition arena : arenas.all()) {
            String gameName = games.find(arena.gameType)
                    .map(GameDefinition::displayName)
                    .orElse(arena.gameType);


            Component line = Component.empty()
                    .append(Component.literal(arena.id)
                            .withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" — ")
                            .withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(gameName)
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(
                                    arena.enabled
                                            ? " [activée]"
                                            : " [désactivée]"
                            )
                            .withStyle(
                                    arena.enabled
                                            ? ChatFormatting.GREEN
                                            : ChatFormatting.RED
                            ));

            source.sendSuccess(() -> line, false);
        }

        return arenas.all().size();
    }

    private static int createArena(
            CommandSourceStack source,
            ArenaManager arenas,
            GameRegistry games,
            MessageService messages,
            String name,
            String gameInput
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande doit être exécutée par un joueur."
            );
            return 0;
        }

        GameDefinition game = games.find(gameInput)
                .orElse(null);

        if (game == null) {
            messages.sendError(
                    source,
                    "Mini-jeu inconnu. Valeurs disponibles : "
                            + String.join(
                            ", ",
                            games.all()
                                    .stream()
                                    .map(GameDefinition::id)
                                    .toList()
                    )
            );

            return 0;
        }

        ArenaManager.CreateResult result =
                arenas.create(name, game, player);

        if (!result.successful()) {
            messages.sendError(source, result.error());
            return 0;
        }

        messages.sendSuccess(
                source,
                "Arène créée : "
                        + result.arena().displayName
                        + " (" + result.arena().id + "), jeu : "
                        + game.displayName()
                        + "."
        );

        messages.sendInfo(
                source,
                "Le lobby et le point spectateur ont été placés "
                        + "à ta position actuelle."
        );

        return 1;
    }

    private static int info(
            CommandSourceStack source,
            ArenaManager arenas,
            GameRegistry games,
            MessageService messages,
            String input
    ) {
        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );
            return 0;
        }

        GameDefinition game = games.find(arena.gameType)
                .orElse(null);

        ArenaValidationResult validation;

        if (game == null) {
            validation = arena.validateBase();
            validation.error(
                    "Le mini-jeu n'est pas enregistré : "
                            + arena.gameType
            );
        } else {
            validation = game.validateArena(arena);
        }

        messages.sendInfo(
                source,
                "Identifiant : " + arena.id
        );

        messages.sendInfo(
                source,
                "Nom : " + arena.displayName
        );

        messages.sendInfo(
                source,
                "Jeu : " + (
                        game == null
                                ? arena.gameType
                                : game.displayName()
                )
        );

        messages.sendInfo(
                source,
                "État : " + (
                        arena.enabled
                                ? "activée"
                                : "désactivée"
                )
        );

        messages.sendInfo(
                source,
                "Monde : " + arena.worldId
        );

        messages.sendInfo(
                source,
                "Lobby : " + (
                        arena.lobby == null
                                ? "non défini"
                                : arena.lobby.formatted()
                )
        );

        messages.sendInfo(
                source,
                "Spectateur : " + (
                        arena.spectator == null
                                ? "non défini"
                                : arena.spectator.formatted()
                )
        );

        messages.sendInfo(
                source,
                "Validation : " + (
                        validation.valid()
                                ? "valide"
                                : "invalide"
                )
        );
        messages.sendInfo(
                source,
                "Groupes de spawns : "
                        + arena.spawns.size()
        );

        messages.sendInfo(
                source,
                "Nombre total de spawns : "
                        + arena.totalSpawnCount()
        );
        messages.sendInfo(
                source,
                "Nombre de régions : "
                        + arena.regions.size()
        );

        for (String error : validation.errors()) {
            messages.sendError(source, error);
        }

        for (String warning : validation.warnings()) {
            messages.sendWarning(source, warning);
        }

        return 1;
    }

    private static int setEnabled(
            CommandSourceStack source,
            ArenaManager arenas,
            GameRegistry games,
            MessageService messages,
            String input,
            boolean enabled
    ) {
        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );
            return 0;
        }

        if (arena.enabled == enabled) {
            messages.sendWarning(
                    source,
                    "Cette arène est déjà "
                            + (enabled
                            ? "activée."
                            : "désactivée.")
            );

            return 0;
        }

        GameDefinition game = games.find(arena.gameType)
                .orElse(null);

        if (game == null) {
            messages.sendError(
                    source,
                    "Le mini-jeu de cette arène n'est pas enregistré."
            );
            return 0;
        }

        ArenaValidationResult validation =
                game.validateArena(arena);

        if (enabled && !validation.valid()) {
            messages.sendError(
                    source,
                    "L'arène ne peut pas être activée."
            );

            for (String error : validation.errors()) {
                messages.sendError(source, error);
            }

            return 0;
        }

        if (!arenas.setEnabled(arena, enabled)) {
            messages.sendError(
                    source,
                    "L'état de l'arène n'a pas pu être enregistré."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "L'arène " + arena.id + " est maintenant "
                        + (enabled
                        ? "activée."
                        : "désactivée.")
        );

        return 1;
    }

    private static int delete(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String input
    ) {
        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );
            return 0;
        }

        if (!arenas.delete(arena)) {
            messages.sendError(
                    source,
                    "L'arène n'a pas pu être supprimée."
            );
            return 0;
        }

        messages.sendSuccess(
                source,
                "L'arène " + arena.id + " a été supprimée."
        );

        return 1;
    }
    private static ServerPlayer requirePlayer(
            CommandSourceStack source,
            MessageService messages
    ) {
        try {
            return source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande doit être exécutée par un joueur."
            );

            return null;
        }
    }
    private static int setLobby(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String input
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        if (!arenas.setLobby(arena, player)) {
            messages.sendError(
                    source,
                    "Le lobby n'a pas pu être enregistré."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Le lobby de l'arène " + arena.id
                        + " a été défini à ta position."
        );

        return 1;
    }
    private static int setSpectator(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String input
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        if (!arenas.setSpectator(arena, player)) {
            messages.sendError(
                    source,
                    "Le point spectateur n'a pas pu être enregistré."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Le point spectateur de l'arène "
                        + arena.id
                        + " a été défini à ta position."
        );

        return 1;
    }
    private static int addSpawn(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput,
            String groupInput
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        String group = ArenaManager.normalizeGroup(groupInput);

        if (group.isBlank()) {
            messages.sendError(
                    source,
                    "Le nom du groupe de spawns est invalide."
            );

            return 0;
        }

        int index = arenas.addSpawn(
                arena,
                group,
                player
        );

        if (index < 1) {
            messages.sendError(
                    source,
                    "Le spawn n'a pas pu être enregistré."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Spawn " + group + " #" + index
                        + " ajouté à ta position."
        );

        return 1;
    }
    private static int removeSpawn(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput,
            String groupInput,
            int index
    ) {
        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        String group = ArenaManager.normalizeGroup(groupInput);

        if (!arenas.removeSpawn(arena, group, index)) {
            messages.sendError(
                    source,
                    "Ce groupe ou cet index de spawn n'existe pas."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Spawn " + group + " #" + index
                        + " supprimé."
        );

        return 1;
    }
    private static int listSpawns(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String input
    ) {
        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        if (arena.spawns.isEmpty()) {
            messages.sendWarning(
                    source,
                    "Cette arène ne possède aucun spawn."
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Spawns de " + arena.id
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        int total = 0;

        for (Map.Entry<String, List<ArenaPosition>> entry :
                arena.spawns.entrySet()) {
            String group = entry.getKey();
            List<ArenaPosition> positions = entry.getValue();

            messages.sendInfo(
                    source,
                    "Groupe " + group + " : "
                            + positions.size() + " spawn(s)"
            );

            for (int index = 0;
                 index < positions.size();
                 index++) {
                ArenaPosition position = positions.get(index);

                messages.sendInfo(
                        source,
                        "  #" + (index + 1)
                                + " — "
                                + position.formatted()
                );

                total++;
            }
        }

        return total;
    }
    private static int teleportToNamedPosition(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput,
            String positionType
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        ArenaPosition position;

        if (positionType.equals("lobby")) {
            position = arena.lobby;
        } else {
            position = arena.spectator;
        }

        if (position == null) {
            messages.sendError(
                    source,
                    "Cette position n'est pas définie."
            );

            return 0;
        }

        if (!position.teleport(
                source.getServer(),
                player
        )) {
            messages.sendError(
                    source,
                    "La dimension cible est introuvable."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Téléportation vers "
                        + positionType
                        + " de l'arène "
                        + arena.id
                        + "."
        );

        return 1;
    }
    private static int teleportToSpawn(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput,
            String groupInput,
            int index
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        ArenaPosition position = arenas.spawn(
                arena,
                groupInput,
                index
        ).orElse(null);

        if (position == null) {
            messages.sendError(
                    source,
                    "Ce spawn n'existe pas."
            );

            return 0;
        }

        if (!position.teleport(
                source.getServer(),
                player
        )) {
            messages.sendError(
                    source,
                    "La dimension cible est introuvable."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Téléportation vers le spawn "
                        + ArenaManager.normalizeGroup(groupInput)
                        + " #" + index
                        + " de l'arène "
                        + arena.id
                        + "."
        );

        return 1;
    }
    private static int setSelectionPosition(
            CommandSourceStack source,
            ArenaSelectionManager selections,
            MessageService messages,
            boolean first
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        /*
         * La position sélectionnée est le bloc situé aux pieds
         * du joueur. La baguette permettra ensuite de cliquer
         * directement sur les blocs.
         */
        BlockPos blockPosition = player.blockPosition();

        ArenaBlockPosition position =
                ArenaBlockPosition.from(
                        player.serverLevel(),
                        blockPosition
                );

        if (first) {
            selections.setFirst(player, position);
        } else {
            selections.setSecond(player, position);
        }

        messages.sendSuccess(
                source,
                "Position " + (first ? "1" : "2")
                        + " définie : "
                        + position.formatted()
                        + "."
        );

        return 1;
    }
    private static int showSelection(
            CommandSourceStack source,
            ArenaSelectionManager selections,
            MessageService messages
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        ArenaSelectionManager.Selection selection =
                selections.get(player).orElse(null);

        if (selection == null) {
            messages.sendWarning(
                    source,
                    "Tu n'as aucune sélection active."
            );

            return 0;
        }

        messages.sendInfo(
                source,
                "Premier coin : "
                        + (
                        selection.first() == null
                                ? "non définie"
                                : selection.first().formatted()
                )
        );

        messages.sendInfo(
                source,
                "Second coin : "
                        + (
                        selection.second() == null
                                ? "non définie"
                                : selection.second().formatted()
                )
        );

        if (selection.hasDifferentDimensions()) {
            messages.sendError(
                    source,
                    "Les positions sont dans des dimensions différentes."
            );

            return 0;
        }

        if (selection.complete()) {
            messages.sendInfo(
                    source,
                    "Volume : "
                            + selection.estimatedVolume()
                            + " bloc(s)."
            );
        }

        return 1;
    }
    private static int clearSelection(
            CommandSourceStack source,
            ArenaSelectionManager selections,
            MessageService messages
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        if (!selections.clear(player)) {
            messages.sendWarning(
                    source,
                    "Tu n'avais aucune sélection active."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Ta sélection a été effacée."
        );

        return 1;
    }

    private static int importWorldEditSelection(
            CommandSourceStack source,
            SelectionService selectionService,
            MessageService messages
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        SelectionResult result =
                selectionService.importWorldEdit(player);

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "La sélection WorldEdit a été importée."
        );

        messages.sendInfo(
                source,
                "Coin minimum : "
                        + result.selection()
                        .first()
                        .formatted()
        );

        messages.sendInfo(
                source,
                "Coin maximum : "
                        + result.selection()
                        .second()
                        .formatted()
        );

        return 1;
    }

    private static int addRegion(
            CommandSourceStack source,
            ArenaManager arenas,
            GameRegistry games,
            SelectionService selectionService,
            MessageService messages,
            String arenaInput,
            String typeInput,
            String requestedName
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        GameDefinition game = games.find(arena.gameType)
                .orElse(null);

        if (game == null) {
            messages.sendError(
                    source,
                    "Le mini-jeu de cette arène "
                            + "n'est pas enregistré."
            );

            return 0;
        }

        ArenaRegionType type =
                ArenaRegionType.fromInput(typeInput)
                        .orElse(null);

        if (type == null) {
            messages.sendError(
                    source,
                    "Ce type de région n'existe pas."
            );

            return 0;
        }

        /*
         * Validation réelle, même si le joueur a écrit
         * manuellement un type non proposé.
         */
        if (!game.allowsRegionType(type)) {
            messages.sendError(
                    source,
                    "Le type '" + type.id()
                            + "' n'est pas autorisé pour "
                            + game.displayName()
                            + "."
            );

            messages.sendInfo(
                    source,
                    "Types autorisés : "
                            + String.join(
                            ", ",
                            game.allowedRegionTypes()
                                    .stream()
                                    .map(ArenaRegionType::id)
                                    .sorted()
                                    .toList()
                    )
            );

            return 0;
        }

        /*
         * Vérification du maximum autorisé.
         */
        if (!game.canAddRegion(arena, type)) {
            messages.sendError(
                    source,
                    "Le nombre maximal de régions '"
                            + type.id()
                            + "' est déjà atteint."
            );

            return 0;
        }

        /*
         * Sélection Olympicraft prioritaire, puis WorldEdit.
         */
        SelectionResult selection =
                selectionService.resolve(player);

        if (!selection.successful()) {
            messages.sendError(
                    source,
                    selection.error()
            );

            return 0;
        }

        RegionRequirement requirement =
                game.regionRequirement(type);

        String regionId = requestedName;

        /*
         * Si aucun nom n'a été fourni :
         * - type unique : sumo_ring
         * - type multiple : island_1, island_2...
         */
        if (regionId == null || regionId.isBlank()) {
            regionId = arenas.nextRegionId(
                    arena,
                    type,
                    requirement
            );
        }

        ArenaManager.RegionResult result =
                arenas.addRegion(
                        arena,
                        regionId,
                        type,
                        selection.selection()
                );

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Région " + result.region().id
                        + " créée avec le type "
                        + type.displayName()
                        + "."
        );

        messages.sendInfo(
                source,
                "Sélection utilisée : "
                        + selection.sourceDisplayName()
                        + "."
        );

        messages.sendInfo(
                source,
                "Limites : "
                        + result.region().formattedBounds()
        );

        return 1;
    }
    private static int listRegions(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput
    ) {
        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        if (arena.regions.isEmpty()) {
            messages.sendWarning(
                    source,
                    "Cette arène ne possède aucune région."
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Régions de " + arena.id
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        for (ArenaRegion region : arena.regions.values()) {
            ArenaRegionType type = region.resolvedType();

            messages.sendInfo(
                    source,
                    region.id
                            + " — "
                            + (
                            type == null
                                    ? region.type
                                    : type.displayName()
                    )
                            + " — "
                            + region.volume()
                            + " bloc(s)"
            );
        }

        return arena.regions.size();
    }
    private static int regionInfo(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput,
            String regionInput
    ) {
        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        ArenaRegion region = arena.region(regionInput);

        if (region == null) {
            messages.sendError(
                    source,
                    "Cette région n'existe pas."
            );

            return 0;
        }

        ArenaRegionType type = region.resolvedType();

        messages.sendInfo(source, "Identifiant : " + region.id);
        messages.sendInfo(
                source,
                "Type : "
                        + (
                        type == null
                                ? region.type
                                : type.displayName()
                )
        );
        messages.sendInfo(
                source,
                "Limites : " + region.formattedBounds()
        );
        messages.sendInfo(
                source,
                "Volume : " + region.volume() + " bloc(s)"
        );

        return 1;
    }
    private static int redefineRegion(
            CommandSourceStack source,
            ArenaManager arenas,
            SelectionService selectionService,
            MessageService messages,
            String arenaInput,
            String regionInput
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        ArenaRegion current = arena.region(regionInput);

        if (current == null) {
            messages.sendError(
                    source,
                    "Cette région n'existe pas."
            );

            return 0;
        }

        SelectionResult selection =
                selectionService.resolve(player);

        if (!selection.successful()) {
            messages.sendError(
                    source,
                    selection.error()
            );

            return 0;
        }

        ArenaManager.RegionResult result =
                arenas.redefineRegion(
                        arena,
                        regionInput,
                        selection.selection()
                );

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "La région " + result.region().id
                        + " a été redéfinie."
        );

        messages.sendInfo(
                source,
                "Sélection utilisée : "
                        + selection.sourceDisplayName()
                        + "."
        );

        return 1;
    }
    private static int removeRegion(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput,
            String regionInput
    ) {
        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        if (!arenas.removeRegion(arena, regionInput)) {
            messages.sendError(
                    source,
                    "Cette région n'existe pas ou n'a pas pu être supprimée."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "La région "
                        + ArenaManager.normalizeId(regionInput)
                        + " a été supprimée."
        );

        return 1;
    }
    private static int teleportToRegion(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String arenaInput,
            String regionInput
    ) {
        ServerPlayer player = requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        ArenaRegion region = arena.region(regionInput);

        if (region == null) {
            messages.sendError(
                    source,
                    "Cette région n'existe pas."
            );

            return 0;
        }

        ArenaPosition position = new ArenaPosition(
                region.dimension,
                region.center().getX() + 0.5,
                region.maxY + 1.0,
                region.center().getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        if (!position.teleport(
                source.getServer(),
                player
        )) {
            messages.sendError(
                    source,
                    "La dimension de la région est introuvable."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Téléportation vers la région "
                        + region.id
                        + "."
        );

        return 1;
    }
    private static int validateArena(
            CommandSourceStack source,
            ArenaManager arenas,
            GameRegistry games,
            MessageService messages,
            String arenaInput
    ) {
        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        GameDefinition game = games.find(arena.gameType)
                .orElse(null);

        if (game == null) {
            messages.sendError(
                    source,
                    "Le mini-jeu de cette arène n'est pas enregistré."
            );

            return 0;
        }

        ArenaValidationResult validation =
                game.validateArena(arena);

        if (validation.valid()) {
            messages.sendSuccess(
                    source,
                    "L'arène est valide pour "
                            + game.displayName()
                            + "."
            );
        } else {
            messages.sendError(
                    source,
                    "L'arène n'est pas valide."
            );
        }

        for (String error : validation.errors()) {
            messages.sendError(source, error);
        }

        for (String warning : validation.warnings()) {
            messages.sendWarning(source, warning);
        }

        return validation.valid() ? 1 : 0;
    }
    private static int giveWand(
            CommandSourceStack source,
            ArenaEditorManager arenaEditor,
            MessageService messages
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        arenaEditor.wand().giveWand(player);
        return 1;
    }
    private static int showRegion(
            CommandSourceStack source,
            ArenaManager arenas,
            ArenaEditorManager arenaEditor,
            OlympicraftConfigManager configs,
            MessageService messages,
            String arenaInput,
            String regionInput,
            int requestedSeconds
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        ArenaRegion region = arena.region(regionInput);

        if (region == null) {
            messages.sendError(
                    source,
                    "Cette région n'existe pas."
            );

            return 0;
        }

        int seconds = Math.max(
                1,
                Math.min(
                        requestedSeconds,
                        configs.general()
                                .arenaEditor
                                .maximumPreviewSeconds
                )
        );

        arenaEditor.visualizations()
                .showRegion(
                        player,
                        region,
                        seconds
                );

        messages.sendSuccess(
                source,
                "Contour de la région "
                        + region.id
                        + " affiché pendant "
                        + seconds
                        + " seconde(s)."
        );

        messages.sendInfo(
                source,
                "Le contour est visible uniquement pour toi."
        );

        return 1;
    }
    private static int showAllRegions(
            CommandSourceStack source,
            ArenaManager arenas,
            ArenaEditorManager arenaEditor,
            OlympicraftConfigManager configs,
            MessageService messages,
            String arenaInput
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        ArenaDefinition arena = arenas.find(arenaInput)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );

            return 0;
        }

        if (arena.regions.isEmpty()) {
            messages.sendWarning(
                    source,
                    "Cette arène ne possède aucune région."
            );

            return 0;
        }

        int seconds = configs.general()
                .arenaEditor
                .regionPreviewSeconds;

        arenaEditor.visualizations()
                .showRegions(
                        player,
                        arena.regions.values()
                                .stream()
                                .toList(),
                        seconds
                );

        messages.sendSuccess(
                source,
                arena.regions.size()
                        + " région(s) affichée(s) pendant "
                        + seconds
                        + " seconde(s)."
        );

        messages.sendInfo(
                source,
                "Les contours sont visibles uniquement pour toi."
        );

        return arena.regions.size();
    }
    private static int hideRegions(
            CommandSourceStack source,
            ArenaEditorManager arenaEditor,
            MessageService messages
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        if (!arenaEditor.visualizations()
                .hide(player)) {
            messages.sendWarning(
                    source,
                    "Aucun contour n'était affiché."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Les contours ont été masqués."
        );

        return 1;
    }
    private static int openArenaListMenu(
            CommandSourceStack source,
            GuiManager gui,
            MessageService messages
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        gui.open(
                player,
                new ArenaListMenu(0)
        );

        return 1;
    }
    private static int openArenaEditorMenu(
            CommandSourceStack source,
            GuiManager gui,
            MessageService messages,
            String arenaId
    ) {
        ServerPlayer player = requirePlayer(
                source,
                messages
        );

        if (player == null) {
            return 0;
        }

        gui.open(
                player,
                new ArenaEditorMenu(
                        arenaId,
                        0
                )
        );

        return 1;
    }

}

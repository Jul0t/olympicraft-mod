package fr.olympicraft.arena.editor;

import fr.olympicraft.arena.ArenaBlockPosition;
import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.message.MessageService;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaWandService {

    private static final String WAND_NAME =
            "Baguette de régions Olympicraft";

    private static final String WAND_MARKER =
            "olympicraft_region_wand";

    private final ArenaSelectionManager selections;
    private final RegionVisualizationService visualizations;
    private final RegionCreationLauncher creationLauncher;
    private final OlympicraftConfigManager configs;
    private final MessageService messages;

    private static final long LEFT_CLICK_COOLDOWN_TICKS = 6L;

    private final Map<UUID, LeftClickState> lastLeftClicks =
            new ConcurrentHashMap<>();

    public ArenaWandService(
            ArenaSelectionManager selections,
            RegionVisualizationService visualizations,
            RegionCreationLauncher creationLauncher,
            OlympicraftConfigManager configs,
            MessageService messages
    ) {
        this.selections = selections;
        this.visualizations = visualizations;
        this.creationLauncher = creationLauncher;
        this.configs = configs;
        this.messages = messages;
    }

    public void registerEvents() {
        registerLeftClick();
        registerRightClick();
    }

    private void registerLeftClick() {
        AttackBlockCallback.EVENT.register(
                (player, level, hand, position, direction) -> {
                    ItemStack heldItem =
                            player.getItemInHand(hand);

                    /*
                     * Si le joueur ne tient pas la baguette,
                     * Olympicraft laisse Minecraft traiter le clic.
                     */
                    if (!isWand(heldItem)) {
                        return InteractionResult.PASS;
                    }

                    /*
                     * La baguette fonctionne uniquement dans
                     * la main principale.
                     */
                    if (hand != InteractionHand.MAIN_HAND) {
                        return InteractionResult.PASS;
                    }

                    /*
                     * Le callback est aussi déclenché côté client.
                     * SUCCESS empêche l'action vanilla et permet
                     * à l'interaction d'atteindre le serveur.
                     */
                    if (level.isClientSide()) {
                        return InteractionResult.SUCCESS;
                    }

                    if (!(player instanceof ServerPlayer serverPlayer)) {
                        return InteractionResult.PASS;
                    }

                    if (!serverPlayer.hasPermissions(2)) {
                        messages.sendError(
                                serverPlayer.createCommandSourceStack(),
                                "Tu n'as pas la permission "
                                        + "d'utiliser cette baguette."
                        );

                        return InteractionResult.FAIL;
                    }

                    boolean assistantAction =
                            serverPlayer.isShiftKeyDown();

                    /*
                     * AttackBlockCallback peut être déclenché
                     * plusieurs fois lorsque le clic est maintenu.
                     *
                     * Si le même bloc et la même action ont déjà
                     * été traités récemment, on n'envoie pas un
                     * second message.
                     */
                    if (isDuplicateLeftClick(
                            serverPlayer,
                            position,
                            assistantAction
                    )) {
                        return InteractionResult.SUCCESS;
                    }

                    /*
                     * Maj + clic gauche prépare le futur assistant
                     * de création de région.
                     */
                    if (assistantAction) {
                        handleShiftLeftClick(serverPlayer);
                        return InteractionResult.SUCCESS;
                    }

                    /*
                     * Clic gauche normal : premier coin.
                     */
                    setFirst(
                            serverPlayer,
                            position
                    );

                    return InteractionResult.SUCCESS;
                }
        );
    }

    private void registerRightClick() {
        UseBlockCallback.EVENT.register(
                (player, level, hand, hitResult) -> {
                    ItemStack heldItem =
                            player.getItemInHand(hand);

                    if (!isWand(heldItem)) {
                        return InteractionResult.PASS;
                    }

                    if (hand != InteractionHand.MAIN_HAND) {
                        return InteractionResult.PASS;
                    }

                    /*
                     * SUCCESS côté client provoque l'envoi du
                     * paquet d'interaction au serveur.
                     */
                    if (level.isClientSide()) {
                        return InteractionResult.SUCCESS;
                    }

                    if (!(player instanceof ServerPlayer serverPlayer)) {
                        return InteractionResult.PASS;
                    }

                    if (!serverPlayer.hasPermissions(2)) {
                        messages.sendError(
                                serverPlayer.createCommandSourceStack(),
                                "Tu n'as pas la permission "
                                        + "d'utiliser cette baguette."
                        );

                        return InteractionResult.FAIL;
                    }

                    setSecond(
                            serverPlayer,
                            hitResult.getBlockPos()
                    );

                    return InteractionResult.SUCCESS;
                }
        );
    }

    public void giveWand(ServerPlayer player) {
        /*
         * Ne donne pas une seconde baguette si le joueur
         * en possède déjà une.
         */
        for (ItemStack stack : player.getInventory().items) {
            if (isWand(stack)) {
                messages.sendWarning(
                        player.createCommandSourceStack(),
                        "Tu possèdes déjà la baguette de régions."
                );

                return;
            }
        }

        ItemStack wand = new ItemStack(
                Items.SPECTRAL_ARROW,
                1
        );

        wand.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(WAND_NAME)
        );

        CompoundTag customData = new CompoundTag();
        customData.putBoolean(WAND_MARKER, true);

        wand.set(
                DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(
                        customData
                )
        );

        boolean added = player.getInventory().add(wand);

        if (!added) {
            player.drop(wand, false);
        }

        messages.sendSuccess(
                player.createCommandSourceStack(),
                "Baguette de régions ajoutée "
                        + "à ton inventaire."
        );

        messages.sendInfo(
                player.createCommandSourceStack(),
                "Objet : flèche spectrale."
        );

        messages.sendInfo(
                player.createCommandSourceStack(),
                "Clic gauche : premier coin."
        );

        messages.sendInfo(
                player.createCommandSourceStack(),
                "Clic droit : second coin."
        );

        messages.sendInfo(
                player.createCommandSourceStack(),
                "Maj + clic gauche : assistant de création."
        );
    }

    private boolean isDuplicateLeftClick(
            ServerPlayer player,
            BlockPos position,
            boolean assistantAction
    ) {
        long currentTick =
                player.getServer().getTickCount();

        LeftClickState previous =
                lastLeftClicks.get(player.getUUID());

        if (previous != null) {
            boolean sameBlock =
                    previous.position().equals(position);

            boolean sameAction =
                    previous.assistantAction()
                            == assistantAction;

            boolean insideCooldown =
                    currentTick - previous.tick()
                            <= LEFT_CLICK_COOLDOWN_TICKS;

            if (sameBlock
                    && sameAction
                    && insideCooldown) {
                return true;
            }
        }

        lastLeftClicks.put(
                player.getUUID(),
                new LeftClickState(
                        position.immutable(),
                        currentTick,
                        assistantAction
                )
        );

        return false;
    }

    private void setFirst(
            ServerPlayer player,
            BlockPos position
    ) {
        ArenaBlockPosition selected =
                ArenaBlockPosition.from(
                        player.serverLevel(),
                        position
                );

        selections.setFirst(player, selected);

        messages.sendSuccess(
                player.createCommandSourceStack(),
                "Premier coin défini : "
                        + selected.formatted()
                        + "."
        );

        previewSelection(player);
    }

    private void setSecond(
            ServerPlayer player,
            BlockPos position
    ) {
        ArenaBlockPosition selected =
                ArenaBlockPosition.from(
                        player.serverLevel(),
                        position
                );

        selections.setSecond(player, selected);

        messages.sendSuccess(
                player.createCommandSourceStack(),
                "Second coin défini : "
                        + selected.formatted()
                        + "."
        );

        previewSelection(player);
    }

    private void handleShiftLeftClick(
            ServerPlayer player
    ) {
        if (!configs.general()
                .arenaEditor
                .shiftLeftClickOpensCreationAssistant) {
            messages.sendWarning(
                    player.createCommandSourceStack(),
                    "L'assistant de création est désactivé."
            );

            return;
        }

        ArenaSelectionManager.Selection selection =
                selections.get(player).orElse(null);

        creationLauncher.prepare(
                player,
                selection,
                messages
        );
    }

    private void previewSelection(
            ServerPlayer player
    ) {
        if (!configs.general()
                .arenaEditor
                .showSelectionAfterClick) {
            return;
        }

        ArenaSelectionManager.Selection selection =
                selections.get(player).orElse(null);

        if (selection == null || !selection.complete()) {
            return;
        }

        visualizations.showSelection(
                player,
                selection,
                configs.general()
                        .arenaEditor
                        .selectionPreviewSeconds
        );
    }

    private boolean isWand(ItemStack stack) {
        if (stack == null
                || stack.isEmpty()
                || !stack.is(Items.SPECTRAL_ARROW)) {
            return false;
        }

        var customData = stack.get(
                DataComponents.CUSTOM_DATA
        );

        if (customData != null) {
            CompoundTag copiedTag =
                    customData.copyTag();

            if (copiedTag.getBoolean(WAND_MARKER)) {
                return true;
            }
        }

        /*
         * Compatibilité avec les baguettes créées avant
         * l'ajout du marqueur personnalisé.
         */
        Component customName = stack.get(
                DataComponents.CUSTOM_NAME
        );

        return customName != null
                && WAND_NAME.equals(
                customName.getString()
        );
    }
    private record LeftClickState(
            BlockPos position,
            long tick,
            boolean assistantAction
    ) {
    }
    public void clear() {
        lastLeftClicks.clear();
    }

}
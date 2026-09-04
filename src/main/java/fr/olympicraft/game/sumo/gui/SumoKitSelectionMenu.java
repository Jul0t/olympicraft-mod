package fr.olympicraft.game.sumo.gui;

import fr.olympicraft.config.model.game.SumoConfig;
import fr.olympicraft.game.sumo.SumoSettings;
import fr.olympicraft.game.sumo.kit.SumoKitSelectionSession;
import fr.olympicraft.gui.GuiItemFactory;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.GuiMenu;
import fr.olympicraft.gui.GuiSession;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SumoKitSelectionMenu
        implements GuiMenu {

    private static final int[] PRESET_SLOTS = {
            20,
            22,
            24
    };

    private final SumoSettings settings;
    private final SumoKitSelectionSession selection;
    private final MinecraftServer server;

    public SumoKitSelectionMenu(
            SumoSettings settings,
            SumoKitSelectionSession selection,
            MinecraftServer server
    ) {
        this.settings = settings;
        this.selection = selection;
        this.server = server;
    }

    @Override
    public Component title() {
        return Component.literal(
                "Choix du kit Sumo"
        );
    }

    @Override
    public void render(
            ServerPlayer player,
            GuiSession session,
            GuiManager manager
    ) {
        fill(session);

        List<SumoConfig.KitPreset> presets =
                settings.enabledKitPresets()
                        .stream()
                        .filter(this::allowed)
                        .limit(PRESET_SLOTS.length)
                        .toList();

        for (int index = 0;
             index < presets.size();
             index++) {
            SumoConfig.KitPreset preset =
                    presets.get(index);

            int slot =
                    PRESET_SLOTS[index];

            long votes =
                    selection.voteCount(
                            preset.id
                    );

            boolean selected =
                    preset.id.equalsIgnoreCase(
                            selection.choice(
                                    player.getUUID()
                            )
                    );

            List<String> lore =
                    createLore(
                            preset,
                            votes,
                            selected
                    );

            session.setItem(
                    slot,
                    GuiItemFactory.item(
                            resolveItem(
                                    preset.iconItem,
                                    Items.STICK
                            ),
                            displayName(
                                    preset,
                                    votes
                            ),
                            selected
                                    ? ChatFormatting.GREEN
                                    : ChatFormatting.AQUA,
                            lore.toArray(
                                    String[]::new
                            )
                    ),
                    (clickedPlayer, context) -> {
                        if (selection.choose(
                                clickedPlayer.getUUID(),
                                preset.id
                        )) {
                            refreshAllOpenMenus(
                                    manager
                            );
                        }
                    }
            );

            fillVoteIndicators(
                    session,
                    preset,
                    slot,
                    votes
            );
        }

        session.setItem(
                4,
                GuiItemFactory.item(
                        Items.CLOCK,
                        selection.mode().name(),
                        ChatFormatting.GOLD,
                        "Choisis ton kit avant la fermeture.",
                        selection.mode().commonPreset()
                                ? "Le kit gagnant sera commun."
                                : "Chaque joueur conserve son choix."
                ),
                null
        );

        session.setItem(
                49,
                GuiItemFactory.item(
                        Items.BARRIER,
                        "Fermer",
                        ChatFormatting.RED,
                        "Ton vote reste enregistré."
                ),
                (clickedPlayer, context) ->
                        clickedPlayer.closeContainer()
        );
    }

    private List<String> createLore(
            SumoConfig.KitPreset preset,
            long votes,
            boolean selected
    ) {
        List<String> lore =
                new ArrayList<>();

        if (preset.descriptionMessageKey != null
                && !preset.descriptionMessageKey
                .isBlank()) {
            lore.add(
                    fr.olympicraft.Olympicraft
                            .messages()
                            .render(
                                    preset.descriptionMessageKey,
                                    false
                            )
                            .getString()
            );
        }

        lore.add("");
        lore.add(
                "Votes/choix : " + votes
        );

        List<UUID> voters =
                selection.voters(preset.id);

        int maximum =
                settings.kitSelection()
                        .maximumDisplayedVoterNames;

        for (int index = 0;
             index < voters.size()
                     && index < maximum;
             index++) {
            ServerPlayer voter =
                    server.getPlayerList()
                            .getPlayer(
                                    voters.get(index)
                            );

            if (voter != null) {
                lore.add(
                        "• "
                                + voter.getGameProfile()
                                .getName()
                );
            }
        }

        if (voters.size() > maximum) {
            lore.add(
                    "… et "
                            + (voters.size() - maximum)
                            + " autre(s)"
            );
        }

        lore.add("");

        if (selected) {
            lore.add("✓ Ton choix actuel");
        } else {
            lore.add("Clique pour sélectionner");
        }

        return lore;
    }

    private String displayName(
            SumoConfig.KitPreset preset,
            long votes
    ) {
        String name =
                preset.displayNameMessageKey == null
                        || preset.displayNameMessageKey
                        .isBlank()
                        ? preset.id
                        : fr.olympicraft.Olympicraft
                        .messages()
                        .render(
                                preset.displayNameMessageKey,
                                false
                        )
                        .getString();

        return name + " — " + votes;
    }

    private void fillVoteIndicators(
            GuiSession session,
            SumoConfig.KitPreset preset,
            int centerSlot,
            long votes
    ) {
        Item glass =
                resolveItem(
                        preset.glassItem,
                        Items.GRAY_STAINED_GLASS_PANE
                );

        int[] indicatorSlots = {
                centerSlot - 9,
                centerSlot + 9
        };

        for (int index = 0;
             index < indicatorSlots.length;
             index++) {
            String name =
                    index < votes
                            ? "Vote enregistré"
                            : " ";

            session.setItem(
                    indicatorSlots[index],
                    GuiItemFactory.item(
                            glass,
                            name,
                            ChatFormatting.GRAY
                    ),
                    null
            );
        }
    }

    private boolean allowed(
            SumoConfig.KitPreset preset
    ) {
        return settings.kitSelection()
                .allowedPresets
                .stream()
                .anyMatch(value ->
                        value.equalsIgnoreCase(
                                preset.id
                        )
                );
    }

    private Item resolveItem(
            String requestedId,
            Item fallback
    ) {
        ResourceLocation identifier =
                ResourceLocation.tryParse(
                        requestedId
                );

        if (identifier == null) {
            return fallback;
        }

        Item item =
                BuiltInRegistries.ITEM.get(
                        identifier
                );

        if (item == null || item == Items.AIR) {
            return fallback;
        }

        return item;
    }

    private void refreshAllOpenMenus(
            GuiManager manager
    ) {
        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {
            if (manager.isOpen(
                    player,
                    SumoKitSelectionMenu.class
            )) {
                manager.refresh(player);
            }
        }
    }

    private void fill(GuiSession session) {
        for (int slot = 0;
             slot < session.container()
                     .getContainerSize();
             slot++) {
            session.setItem(
                    slot,
                    GuiItemFactory.filler(),
                    null
            );
        }
    }
}
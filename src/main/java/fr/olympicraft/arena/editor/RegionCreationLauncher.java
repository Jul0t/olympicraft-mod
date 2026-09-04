package fr.olympicraft.arena.editor;

import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.menu.RegionArenaChoiceMenu;
import fr.olympicraft.message.MessageService;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionCreationLauncher {

    private final Map<UUID, PendingRegionCreation> pending =
            new ConcurrentHashMap<>();

    private GuiManager gui;

    public void attachGui(GuiManager gui) {
        this.gui = gui;
    }

    public boolean prepare(
            ServerPlayer player,
            ArenaSelectionManager.Selection selection,
            MessageService messages
    ) {
        if (selection == null || !selection.complete()) {
            messages.sendError(
                    player.createCommandSourceStack(),
                    "Définis les deux coins avant d'ouvrir "
                            + "l'assistant de création."
            );

            return false;
        }

        ArenaSelectionManager.Selection normalized =
                selection.normalized();

        PendingRegionCreation creation =
                new PendingRegionCreation(
                        player.getUUID(),
                        normalized,
                        Instant.now()
                );

        pending.put(player.getUUID(), creation);

        if (gui == null) {
            messages.sendError(
                    player.createCommandSourceStack(),
                    "Le gestionnaire de GUI n'est pas prêt."
            );

            return false;
        }

        gui.openNextTick(
                player,
                new RegionArenaChoiceMenu(
                        normalized,
                        0
                )
        );

        return true;
    }

    public Optional<PendingRegionCreation> pending(
            ServerPlayer player
    ) {
        return Optional.ofNullable(
                pending.get(player.getUUID())
        );
    }

    public void clear(ServerPlayer player) {
        pending.remove(player.getUUID());
    }

    public void clearAll() {
        pending.clear();
    }
}
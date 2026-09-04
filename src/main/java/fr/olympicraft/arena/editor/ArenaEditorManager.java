package fr.olympicraft.arena.editor;

import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.message.MessageService;
import net.minecraft.server.MinecraftServer;

public final class ArenaEditorManager {

    private final RegionVisualizationService visualizations =
            new RegionVisualizationService();

    private final RegionCreationLauncher creationLauncher =
            new RegionCreationLauncher();

    private final ArenaWandService wand;

    private final OlympicraftConfigManager configs;

    public ArenaEditorManager(
            ArenaSelectionManager selections,
            OlympicraftConfigManager configs,
            MessageService messages
    ) {
        this.configs = configs;

        this.wand = new ArenaWandService(
                selections,
                visualizations,
                creationLauncher,
                configs,
                messages
        );
    }

    public void registerEvents() {
        wand.registerEvents();
    }

    public void tick(MinecraftServer server) {
        visualizations.tick(
                server,
                configs.general()
                        .arenaEditor
                        .refreshIntervalTicks,
                configs.general()
                        .arenaEditor
                        .maximumParticlesPerRefresh
        );
    }

    public void clearAll() {
        visualizations.clearAll();
        creationLauncher.clearAll();
        wand.clear();
    }

    public RegionVisualizationService visualizations() {
        return visualizations;
    }

    public RegionCreationLauncher creationLauncher() {
        return creationLauncher;
    }

    public ArenaWandService wand() {
        return wand;
    }
}
package fr.olympicraft.arena.editor;

import fr.olympicraft.arena.ArenaBlockPosition;
import fr.olympicraft.arena.ArenaRegion;
import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.arena.ArenaSelectionManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionVisualizationService {

    private static final DustParticleOptions SELECTION_PARTICLE =
            new DustParticleOptions(
                    new Vector3f(0.0F, 1.0F, 1.0F),
                    1.0F
            );

    private static final DustParticleOptions REGION_PARTICLE =
            new DustParticleOptions(
                    new Vector3f(0.2F, 1.0F, 0.2F),
                    1.0F
            );

    private final Map<UUID, Visualization> visualizations =
            new ConcurrentHashMap<>();

    private int elapsedTicks;

    public void showSelection(
            ServerPlayer player,
            ArenaSelectionManager.Selection selection,
            int durationSeconds
    ) {
        if (selection == null || !selection.complete()) {
            return;
        }

        ArenaSelectionManager.Selection normalized =
                selection.normalized();

        ArenaRegion temporary = new ArenaRegion(
                "selection",
                ArenaRegionType.GAME_BOUNDS,
                normalized.first(),
                normalized.second()
        );

        showRegion(
                player,
                temporary,
                durationSeconds,
                true
        );
    }

    public void showRegion(
            ServerPlayer player,
            ArenaRegion region,
            int durationSeconds
    ) {
        showRegion(
                player,
                region,
                durationSeconds,
                false
        );
    }

    private void showRegion(
            ServerPlayer player,
            ArenaRegion region,
            int durationSeconds,
            boolean selection
    ) {
        long expirationTick =
                player.getServer().getTickCount()
                        + Math.max(1, durationSeconds) * 20L;

        visualizations.put(
                player.getUUID(),
                new Visualization(
                        List.of(region),
                        expirationTick,
                        selection
                )
        );
    }

    public void showRegions(
            ServerPlayer player,
            List<ArenaRegion> regions,
            int durationSeconds
    ) {
        if (regions == null || regions.isEmpty()) {
            return;
        }

        long expirationTick =
                player.getServer().getTickCount()
                        + Math.max(1, durationSeconds) * 20L;

        visualizations.put(
                player.getUUID(),
                new Visualization(
                        List.copyOf(regions),
                        expirationTick,
                        false
                )
        );
    }

    public boolean hide(ServerPlayer player) {
        return visualizations.remove(
                player.getUUID()
        ) != null;
    }

    public void clearAll() {
        visualizations.clear();
    }

    public void tick(
            MinecraftServer server,
            int refreshIntervalTicks,
            int maximumParticles
    ) {
        elapsedTicks++;

        if (elapsedTicks < refreshIntervalTicks) {
            return;
        }

        elapsedTicks = 0;

        long currentTick = server.getTickCount();

        visualizations.entrySet().removeIf(entry -> {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(entry.getKey());

            Visualization visualization =
                    entry.getValue();

            if (player == null
                    || currentTick >= visualization.expirationTick()) {
                return true;
            }

            render(
                    player,
                    visualization,
                    maximumParticles
            );

            return false;
        });
    }

    private void render(
            ServerPlayer player,
            Visualization visualization,
            int maximumParticles
    ) {
        int regionCount = Math.max(
                1,
                visualization.regions().size()
        );

        int budgetPerRegion = Math.max(
                24,
                maximumParticles / regionCount
        );

        for (ArenaRegion region :
                visualization.regions()) {
            renderRegion(
                    player,
                    region,
                    visualization.selection(),
                    budgetPerRegion
            );
        }
    }

    private void renderRegion(
            ServerPlayer player,
            ArenaRegion region,
            boolean selection,
            int budget
    ) {
        ServerLevel level = player.serverLevel();

        if (!level.dimension()
                .location()
                .toString()
                .equals(region.dimension)) {
            return;
        }

        List<Point> points = outlinePoints(
                region,
                budget
        );

        DustParticleOptions particle =
                selection
                        ? SELECTION_PARTICLE
                        : REGION_PARTICLE;

        /*
         * Envoi directement à ce joueur uniquement.
         *
         * Aucun broadcast au monde ou aux joueurs proches.
         */
        for (Point point : points) {
            level.sendParticles(
                    player,
                    particle,
                    true,
                    point.x(),
                    point.y(),
                    point.z(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    private List<Point> outlinePoints(
            ArenaRegion region,
            int budget
    ) {
        double minX = region.minX;
        double minY = region.minY;
        double minZ = region.minZ;

        double maxX = region.maxX + 1.0;
        double maxY = region.maxY + 1.0;
        double maxZ = region.maxZ + 1.0;

        double sizeX = Math.max(1.0, maxX - minX);
        double sizeY = Math.max(1.0, maxY - minY);
        double sizeZ = Math.max(1.0, maxZ - minZ);

        double totalEdgeLength =
                4.0 * (sizeX + sizeY + sizeZ);

        double spacing = Math.max(
                0.25,
                totalEdgeLength
                        / Math.max(24, budget)
        );

        List<Point> points = new ArrayList<>();

        addEdge(
                points,
                minX, minY, minZ,
                maxX, minY, minZ,
                spacing
        );

        addEdge(
                points,
                minX, minY, maxZ,
                maxX, minY, maxZ,
                spacing
        );

        addEdge(
                points,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                spacing
        );

        addEdge(
                points,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                spacing
        );

        addEdge(
                points,
                minX, minY, minZ,
                minX, maxY, minZ,
                spacing
        );

        addEdge(
                points,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                spacing
        );

        addEdge(
                points,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                spacing
        );

        addEdge(
                points,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                spacing
        );

        addEdge(
                points,
                minX, minY, minZ,
                minX, minY, maxZ,
                spacing
        );

        addEdge(
                points,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                spacing
        );

        addEdge(
                points,
                minX, maxY, minZ,
                minX, maxY, maxZ,
                spacing
        );

        addEdge(
                points,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                spacing
        );

        if (points.size() <= budget) {
            return points;
        }

        /*
         * Sous-échantillonnage si la région est immense.
         */
        return points.stream()
                .sorted(Comparator.comparingDouble(
                        point -> point.distanceSquared(
                                region.center().getX(),
                                region.center().getY(),
                                region.center().getZ()
                        )
                ))
                .limit(budget)
                .toList();
    }

    private void addEdge(
            List<Point> points,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double spacing
    ) {
        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double deltaZ = endZ - startZ;

        double length = Math.sqrt(
                deltaX * deltaX
                        + deltaY * deltaY
                        + deltaZ * deltaZ
        );

        int steps = Math.max(
                1,
                (int) Math.ceil(length / spacing)
        );

        for (int step = 0; step <= steps; step++) {
            double progress =
                    (double) step / steps;

            points.add(
                    new Point(
                            startX + deltaX * progress,
                            startY + deltaY * progress,
                            startZ + deltaZ * progress
                    )
            );
        }
    }

    private record Visualization(
            List<ArenaRegion> regions,
            long expirationTick,
            boolean selection
    ) {
    }

    private record Point(
            double x,
            double y,
            double z
    ) {

        private double distanceSquared(
                double otherX,
                double otherY,
                double otherZ
        ) {
            double deltaX = x - otherX;
            double deltaY = y - otherY;
            double deltaZ = z - otherZ;

            return deltaX * deltaX
                    + deltaY * deltaY
                    + deltaZ * deltaZ;
        }
    }
}
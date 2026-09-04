package fr.olympicraft.integration.worldedit;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.arena.ArenaBlockPosition;
import fr.olympicraft.arena.ArenaSelectionManager;
import fr.olympicraft.arena.SelectionResult;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class WorldEditSelectionProvider {

    private static final String MOD_ID = "worldedit";

    private static final String FABRIC_ADAPTER_CLASS =
            "com.sk89q.worldedit.fabric.FabricAdapter";

    private static final String WORLD_EDIT_CLASS =
            "com.sk89q.worldedit.WorldEdit";

    public boolean available() {
        return FabricLoader.getInstance()
                .isModLoaded(MOD_ID);
    }

    public SelectionResult selection(
            ServerPlayer player
    ) {
        if (!available()) {
            return SelectionResult.failure(
                    "WorldEdit n'est pas installé."
            );
        }

        try {
            /*
             * Conversion du ServerPlayer Minecraft en joueur
             * WorldEdit :
             *
             * FabricAdapter.adaptPlayer(player)
             */
            Object worldEditPlayer = adaptPlayer(player);

            if (worldEditPlayer == null) {
                return SelectionResult.failure(
                        "Le joueur n'a pas pu être converti "
                                + "pour WorldEdit."
                );
            }

            /*
             * WorldEdit.getInstance()
             *     .getSessionManager()
             *     .get(worldEditPlayer)
             */
            Class<?> worldEditClass =
                    Class.forName(WORLD_EDIT_CLASS);

            Object worldEdit = worldEditClass
                    .getMethod("getInstance")
                    .invoke(null);

            Object sessionManager = worldEditClass
                    .getMethod("getSessionManager")
                    .invoke(worldEdit);

            Method getSessionMethod = findCompatibleMethod(
                    sessionManager.getClass(),
                    "get",
                    worldEditPlayer.getClass()
            );

            if (getSessionMethod == null) {
                return SelectionResult.failure(
                        "La méthode permettant de récupérer "
                                + "la session WorldEdit est introuvable."
                );
            }

            Object localSession = getSessionMethod.invoke(
                    sessionManager,
                    worldEditPlayer
            );

            if (localSession == null) {
                return SelectionResult.failure(
                        "La session WorldEdit du joueur "
                                + "est introuvable."
                );
            }

            /*
             * Récupération du monde de la sélection, qui peut
             * être différent du monde où le joueur se trouve.
             */
            Object selectionWorld = localSession
                    .getClass()
                    .getMethod("getSelectionWorld")
                    .invoke(localSession);

            if (selectionWorld == null) {
                return SelectionResult.failure(
                        "La sélection WorldEdit est incomplète."
                );
            }

            /*
             * localSession.getSelection(selectionWorld)
             */
            Method getSelectionMethod =
                    findCompatibleMethod(
                            localSession.getClass(),
                            "getSelection",
                            selectionWorld.getClass()
                    );

            if (getSelectionMethod == null) {
                return SelectionResult.failure(
                        "La méthode de lecture de la sélection "
                                + "WorldEdit est introuvable."
                );
            }

            Object region = getSelectionMethod.invoke(
                    localSession,
                    selectionWorld
            );

            if (region == null) {
                return SelectionResult.failure(
                        "La sélection WorldEdit est vide."
                );
            }

            Object minimum = region.getClass()
                    .getMethod("getMinimumPoint")
                    .invoke(region);

            Object maximum = region.getClass()
                    .getMethod("getMaximumPoint")
                    .invoke(region);

            int minX = coordinate(minimum, "x", "getX");
            int minY = coordinate(minimum, "y", "getY");
            int minZ = coordinate(minimum, "z", "getZ");

            int maxX = coordinate(maximum, "x", "getX");
            int maxY = coordinate(maximum, "y", "getY");
            int maxZ = coordinate(maximum, "z", "getZ");

            String dimension = resolveDimension(
                    selectionWorld,
                    player
            );

            /*
             * WorldEdit retourne normalement les coins minimum et maximum.
             * Nous normalisons malgré tout chaque axe afin de garantir
             * un résultat cohérent entre les versions et types de sélection.
             */
            int normalizedMinX = Math.min(minX, maxX);
            int normalizedMinY = Math.min(minY, maxY);
            int normalizedMinZ = Math.min(minZ, maxZ);

            int normalizedMaxX = Math.max(minX, maxX);
            int normalizedMaxY = Math.max(minY, maxY);
            int normalizedMaxZ = Math.max(minZ, maxZ);

            ArenaBlockPosition first =
                    new ArenaBlockPosition(
                            dimension,
                            normalizedMinX,
                            normalizedMinY,
                            normalizedMinZ
                    );

            ArenaBlockPosition second =
                    new ArenaBlockPosition(
                            dimension,
                            normalizedMaxX,
                            normalizedMaxY,
                            normalizedMaxZ
                    );

            return SelectionResult.success(
                    new ArenaSelectionManager.Selection(
                            first,
                            second
                    ),
                    SelectionResult.Source.WORLD_EDIT
            );
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause != null
                    && cause.getClass()
                    .getSimpleName()
                    .equals("IncompleteRegionException")) {
                return SelectionResult.failure(
                        "La sélection WorldEdit est incomplète."
                );
            }

            Olympicraft.LOGGER.error(
                    "WorldEdit a refusé la lecture "
                            + "de la sélection.",
                    cause == null ? exception : cause
            );

            return SelectionResult.failure(
                    "La sélection WorldEdit n'a pas pu être lue."
            );
        } catch (ReflectiveOperationException exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de lire la sélection WorldEdit.",
                    exception
            );

            return SelectionResult.failure(
                    "L'intégration WorldEdit n'est pas compatible "
                            + "avec la version actuellement installée."
            );
        } catch (RuntimeException exception) {
            Olympicraft.LOGGER.error(
                    "Erreur pendant la lecture de WorldEdit.",
                    exception
            );

            return SelectionResult.failure(
                    "Une erreur WorldEdit est survenue."
            );
        }
    }

    private Object adaptPlayer(
            ServerPlayer player
    ) throws ReflectiveOperationException {
        Class<?> adapterClass =
                Class.forName(FABRIC_ADAPTER_CLASS);

        /*
         * La méthode est normalement statique :
         *
         * FabricAdapter.adaptPlayer(ServerPlayer)
         *
         * La recherche compatible tolère le type concret
         * employé par les mappings et la version installée.
         */
        Method adaptPlayerMethod =
                findCompatibleMethod(
                        adapterClass,
                        "adaptPlayer",
                        player.getClass()
                );

        if (adaptPlayerMethod == null) {
            /*
             * Fallback pour une éventuelle version utilisant
             * simplement le nom "adapt".
             */
            adaptPlayerMethod = findCompatibleMethod(
                    adapterClass,
                    "adapt",
                    player.getClass()
            );
        }

        if (adaptPlayerMethod == null) {
            throw new NoSuchMethodException(
                    "FabricAdapter.adaptPlayer(ServerPlayer)"
            );
        }

        return adaptPlayerMethod.invoke(
                null,
                player
        );
    }

    private String resolveDimension(
            Object selectionWorld,
            ServerPlayer player
    ) {
        /*
         * Première tentative : récupérer l'identifiant depuis
         * le wrapper FabricWorld de WorldEdit.
         */
        try {
            Method getWorldMethod =
                    selectionWorld.getClass()
                            .getMethod("getWorld");

            Object minecraftWorld =
                    getWorldMethod.invoke(selectionWorld);

            if (minecraftWorld != null) {
                Method dimensionMethod =
                        minecraftWorld.getClass()
                                .getMethod("dimension");

                Object resourceKey =
                        dimensionMethod.invoke(minecraftWorld);

                Method locationMethod =
                        resourceKey.getClass()
                                .getMethod("location");

                Object location =
                        locationMethod.invoke(resourceKey);

                if (location != null) {
                    return location.toString();
                }
            }
        } catch (ReflectiveOperationException ignored) {
            /*
             * Certaines versions de WorldEdit n'exposent pas
             * directement le monde Minecraft.
             */
        }

        /*
         * Deuxième tentative : getName() peut être exploitable
         * si WorldEdit retourne déjà un identifiant namespacé.
         */
        try {
            Object name = selectionWorld
                    .getClass()
                    .getMethod("getName")
                    .invoke(selectionWorld);

            if (name != null) {
                String text = name.toString();

                if (text.contains(":")
                        && !text.contains(" ")) {
                    return text;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        /*
         * Fallback sûr : dimension actuelle du joueur.
         *
         * Cette solution est correcte pour une sélection
         * réalisée dans le monde où se trouve le joueur.
         */
        return player.serverLevel()
                .dimension()
                .location()
                .toString();
    }

    private Method findCompatibleMethod(
            Class<?> owner,
            String name,
            Class<?> argumentType
    ) {
        for (Method method : owner.getMethods()) {
            if (!method.getName().equals(name)
                    || method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType =
                    method.getParameterTypes()[0];

            if (parameterType.isAssignableFrom(
                    argumentType
            )) {
                return method;
            }
        }

        return null;
    }

    private int coordinate(
            Object vector,
            String shortMethod,
            String beanMethod
    ) throws ReflectiveOperationException {
        try {
            Object value = vector.getClass()
                    .getMethod(shortMethod)
                    .invoke(vector);

            return ((Number) value).intValue();
        } catch (NoSuchMethodException ignored) {
            Object value = vector.getClass()
                    .getMethod(beanMethod)
                    .invoke(vector);

            return ((Number) value).intValue();
        }
    }
}
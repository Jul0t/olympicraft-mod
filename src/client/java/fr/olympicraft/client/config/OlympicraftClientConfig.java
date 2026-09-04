package fr.olympicraft.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class OlympicraftClientConfig {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

    private static final Path PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("olympicraft")
                    .resolve("client.json5");

    private static Data data =
            new Data();

    private OlympicraftClientConfig() {
    }

    public static void load() {
        if (Files.notExists(PATH)) {
            data = new Data();
            save();
            return;
        }

        try (Reader reader =
                     Files.newBufferedReader(
                             PATH,
                             StandardCharsets.UTF_8
                     )) {
            Data loaded =
                    GSON.fromJson(
                            reader,
                            Data.class
                    );

            data = loaded == null
                    ? new Data()
                    : loaded;

            validate();
        } catch (Exception exception) {
            data = new Data();

            System.err.println(
                    "[Olympicraft] Impossible de charger "
                            + "la configuration client."
            );

            exception.printStackTrace();
        }
    }

    public static void save() {
        validate();

        try {
            Files.createDirectories(
                    PATH.getParent()
            );

            Path temporary =
                    PATH.resolveSibling(
                            PATH.getFileName() + ".tmp"
                    );

            try (Writer writer =
                         Files.newBufferedWriter(
                                 temporary,
                                 StandardCharsets.UTF_8
                         )) {
                GSON.toJson(
                        data,
                        writer
                );
            }

            try {
                Files.move(
                        temporary,
                        PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicFailure) {
                Files.move(
                        temporary,
                        PATH,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException exception) {
            System.err.println(
                    "[Olympicraft] Impossible d'enregistrer "
                            + "la configuration client."
            );

            exception.printStackTrace();
        }
    }

    public static double soundVolume() {
        return data.soundVolume;
    }

    public static void soundVolume(
            double volume
    ) {
        data.soundVolume =
                Math.clamp(
                        volume,
                        0.0D,
                        1.0D
                );

        save();
    }

    public static float applySoundVolume(
            float requestedVolume
    ) {
        return requestedVolume
                * (float) soundVolume();
    }

    private static void validate() {
        if (data == null) {
            data = new Data();
        }

        data.soundVolume =
                Math.clamp(
                        data.soundVolume,
                        0.0D,
                        1.0D
                );
    }

    private static final class Data {

        /*
         * Volume des sons Olympicraft Enhanced.
         *
         * 0.0 = muet
         * 1.0 = volume maximal
         */
        private double soundVolume = 1.0D;
    }
}
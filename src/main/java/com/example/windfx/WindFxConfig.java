package com.example.windfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings, saved to config/windfx.json */
public class WindFxConfig {

    public enum ParticleMode implements dev.isxander.yacl3.api.NameableEnum {
        LINES("Wind lines (recommended)"),
        LEAVES("Leaves (vanilla flutter)"),
        DUST("Dust flecks");

        private final String label;
        ParticleMode(String label) { this.label = label; }

        @Override
        public Component getDisplayName() { return Component.literal(label); }
    }

    public int configVersion = 2;
    public boolean enabled = true;
    public int particlesPerSecond = 16;   // 0..200
    public float windSpeed = 1.0f;        // multiplier
    public int radius = 20;               // distance band: 0.6x to 2.4x this, in blocks
    public float gustStrength = 1.0f;     // how strongly gusts vary things
    public ParticleMode mode = ParticleMode.LINES;
    public float lineLength = 1.0f;       // wind line length multiplier

    // ---- persistence ----
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("windfx.json");
    private static WindFxConfig instance = new WindFxConfig();

    public static WindFxConfig get() { return instance; }

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                com.google.gson.JsonObject raw = com.google.gson.JsonParser.parseReader(r).getAsJsonObject();
                if (raw.has("configVersion")) {
                    WindFxConfig c = GSON.fromJson(raw, WindFxConfig.class);
                    if (c != null) instance = c;
                } // older config files are replaced with the new defaults
            } catch (Exception e) {
                System.err.println("[windfx] Could not read config, using defaults: " + e);
            }
        }
        instance.clamp();
        save();
    }

    public static void save() {
        instance.clamp();
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            System.err.println("[windfx] Could not save config: " + e);
        }
    }

    private void clamp() {
        particlesPerSecond = Math.max(0, Math.min(200, particlesPerSecond));
        windSpeed = Math.max(0.1f, Math.min(4f, windSpeed));
        radius = Math.max(8, Math.min(40, radius));
        gustStrength = Math.max(0f, Math.min(2f, gustStrength));
        if (mode == null) mode = ParticleMode.LINES;
        lineLength = Math.max(0.3f, Math.min(3f, lineLength));
    }
}

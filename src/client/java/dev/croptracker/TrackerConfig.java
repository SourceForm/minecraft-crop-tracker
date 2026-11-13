package dev.croptracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Lightweight config with no extra mod dependency. */
final class TrackerConfig {
    final boolean hudVisible;
    final int hudX;
    final int hudY;
    final double trackingRadius;

    private TrackerConfig(boolean hudVisible, int hudX, int hudY, double trackingRadius) {
        this.hudVisible = hudVisible;
        this.hudX = hudX;
        this.hudY = hudY;
        this.trackingRadius = trackingRadius;
    }

    static TrackerConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("croptracker.properties");
        Properties properties = new Properties();
        try {
            if (Files.exists(path)) try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            }
            properties.putIfAbsent("hud.visible", "true");
            properties.putIfAbsent("hud.x", "8");
            properties.putIfAbsent("hud.y", "8");
            properties.putIfAbsent("tracking.radius", "12.0");
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "CropTracker settings");
            }
        } catch (IOException ignored) {
            // The defaults keep the tracker usable if a config folder is unavailable.
        }
        return new TrackerConfig(
                Boolean.parseBoolean(properties.getProperty("hud.visible", "true")),
                parseInt(properties, "hud.x", 8),
                parseInt(properties, "hud.y", 8),
                Math.max(1.0, parseDouble(properties, "tracking.radius", 12.0)));
    }

    private static int parseInt(Properties properties, String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key)); } catch (NumberFormatException exception) { return fallback; }
    }

    private static double parseDouble(Properties properties, String key, double fallback) {
        try { return Double.parseDouble(properties.getProperty(key)); } catch (NumberFormatException exception) { return fallback; }
    }
}

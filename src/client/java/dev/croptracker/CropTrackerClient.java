package dev.croptracker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;

/** Client-only entry point. Counts crop drops that the client actually receives. */
public final class CropTrackerClient implements ClientModInitializer {
    private static final SessionTracker SESSION = new SessionTracker();
    private TrackerConfig config;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("croptracker", "category"));

    private static final KeyMapping START = key("key.croptracker.start", InputConstants.KEY_HOME);
    private static final KeyMapping PAUSE = key("key.croptracker.pause", InputConstants.KEY_END);
    private static final KeyMapping FINISH = key("key.croptracker.end", InputConstants.KEY_DELETE);
    private static final KeyMapping EXPORT = key("key.croptracker.export", InputConstants.KEY_INSERT);

    private static KeyMapping key(String translationKey, int keyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                translationKey, InputConstants.Type.KEYSYM, keyCode, CATEGORY));
    }

    @Override
    public void onInitializeClient() {
        config = TrackerConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SESSION.flushPending(client.player, config.trackingRadius);
            handleKeys(client);
        });
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity itemEntity) {
                SESSION.queue(itemEntity);
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("croptracker", "session_hud"), this::renderHud);
    }

    private void handleKeys(Minecraft client) {
        while (START.consumeClick()) {
            SESSION.start();
            message(client, "CropTracker session started.");
        }
        while (PAUSE.consumeClick()) {
            message(client, SESSION.togglePause() ? "CropTracker session paused." : "CropTracker session resumed.");
        }
        while (FINISH.consumeClick()) {
            if (SESSION.finish()) message(client, "CropTracker session ended. Press Insert to copy its report.");
        }
        while (EXPORT.consumeClick()) {
            client.keyboardHandler.setClipboard(SESSION.report());
            message(client, "CropTracker report copied to clipboard.");
        }
    }

    private void renderHud(GuiGraphicsExtractor graphics, Object deltaTracker) {
        if (!config.hudVisible || !SESSION.shouldRender()) return;
        int x = config.hudX;
        int y = config.hudY;
        graphics.fill(x - 4, y - 4, x + 178, y + 42, 0x99000000);
        graphics.text(Minecraft.getInstance().font, SESSION.hudTitle(), x, y, 0xFFFFFFFF, true);
        graphics.text(Minecraft.getInstance().font, SESSION.hudTotals(), x, y + 12, 0xFFFFFFFF, true);
        graphics.text(Minecraft.getInstance().font, SESSION.hudRate(), x, y + 24, 0xFFB7F7A4, true);
    }

    private static void message(Minecraft client, String text) {
        if (client.player != null) client.player.sendSystemMessage(Component.literal(text));
    }
}

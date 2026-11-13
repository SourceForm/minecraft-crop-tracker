package dev.croptracker;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;

/** Keeps only the current session and one completed session in memory. */
final class SessionTracker {
    private enum State { IDLE, RUNNING, PAUSED, FINISHED }

    private final Map<CropType, Integer> counts = new EnumMap<>(CropType.class);
    private final Map<Integer, PendingItem> pendingItems = new HashMap<>();
    private State state = State.IDLE;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant pausedAt;
    private Duration pausedTime = Duration.ZERO;

    void start() {
        counts.clear();
        pendingItems.clear();
        pausedTime = Duration.ZERO;
        startedAt = Instant.now();
        finishedAt = null;
        pausedAt = null;
        state = State.RUNNING;
    }

    boolean togglePause() {
        if (state == State.RUNNING) {
            pausedAt = Instant.now();
            state = State.PAUSED;
            return true;
        }
        if (state == State.PAUSED) {
            pausedTime = pausedTime.plus(Duration.between(pausedAt, Instant.now()));
            pausedAt = null;
            state = State.RUNNING;
        }
        return false;
    }

    boolean finish() {
        if (state != State.RUNNING && state != State.PAUSED) return false;
        if (state == State.PAUSED) pausedTime = pausedTime.plus(Duration.between(pausedAt, Instant.now()));
        finishedAt = Instant.now();
        state = State.FINISHED;
        pausedAt = null;
        pendingItems.clear();
        return true;
    }

    /**
     * Item entities are created before their ItemStack data is normally synchronized.
     * Hold the entity for a few client ticks so short-lived hopper drops still have
     * their item type and count available when we inspect them.
     */
    void queue(ItemEntity entity) {
        if (state == State.RUNNING) pendingItems.putIfAbsent(entity.getId(), new PendingItem(entity));
    }

    void flushPending(LocalPlayer player, double radius) {
        if (state != State.RUNNING || player == null) return;
        Iterator<PendingItem> iterator = pendingItems.values().iterator();
        while (iterator.hasNext()) {
            PendingItem pending = iterator.next();
            var stack = pending.entity.getItem();
            if (!stack.isEmpty()) {
                if (pending.entity.distanceToSqr(player) <= radius * radius) {
                    CropType.from(stack.getItem()).ifPresent(type -> counts.merge(type, stack.getCount(), Integer::sum));
                }
                iterator.remove();
            } else if (--pending.remainingChecks == 0) {
                iterator.remove();
            }
        }
    }

    boolean shouldRender() { return state == State.RUNNING || state == State.PAUSED || state == State.FINISHED; }
    String hudTitle() { return "CropTracker: " + state.name().toLowerCase(Locale.ROOT); }
    String hudTotals() { return "Total drops: " + total() + "  |  " + compactBreakdown(); }
    String hudRate() { return String.format(Locale.ROOT, "Rate: %.0f drops/hour", total() / Math.max(1.0 / 3600.0, activeSeconds() / 3600.0)); }

    String report() {
        StringBuilder result = new StringBuilder("CropTracker session (" + state.name().toLowerCase(Locale.ROOT) + ")\n");
        result.append("Active time: ").append(formatDuration()).append("\n");
        result.append("Total drops: ").append(total()).append("\n");
        for (CropType type : CropType.values()) result.append(type.label()).append(": ").append(counts.getOrDefault(type, 0)).append("\n");
        result.append(String.format(Locale.ROOT, "Drops/hour: %.0f", total() / Math.max(1.0 / 3600.0, activeSeconds() / 3600.0)));
        return result.toString();
    }

    private int total() { return counts.values().stream().mapToInt(Integer::intValue).sum(); }
    private double activeSeconds() {
        if (startedAt == null) return 0;
        Duration elapsed = Duration.between(startedAt, state == State.FINISHED ? finishedAt : Instant.now());
        Duration activePause = pausedTime.plus(state == State.PAUSED ? Duration.between(pausedAt, Instant.now()) : Duration.ZERO);
        return Math.max(0, elapsed.minus(activePause).toSeconds());
    }
    private String compactBreakdown() {
        return counts.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getKey().label() + " " + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse("waiting for crop drops");
    }
    private String formatDuration() { return Duration.ofSeconds((long) activeSeconds()).toString().substring(2).toLowerCase(Locale.ROOT); }

    private static final class PendingItem {
        private final ItemEntity entity;
        private int remainingChecks = 4;

        private PendingItem(ItemEntity entity) {
            this.entity = entity;
        }
    }
}

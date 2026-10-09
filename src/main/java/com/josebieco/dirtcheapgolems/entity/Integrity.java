package com.josebieco.dirtcheapgolems.entity;

/**
 * A golem's integrity (spec §5.4): an int in [0, 100] that starts at 100 and never increases.
 * World-independent so it can be unit tested.
 */
public final class Integrity {
    public static final int MAX = 100;

    private int value;
    private int ticksSinceDecay;

    public Integrity() {
        this(MAX);
    }

    public Integrity(int value) {
        this.value = Math.clamp(value, 0, MAX);
    }

    public int value() {
        return value;
    }

    public boolean isDepleted() {
        return value <= 0;
    }

    /** Lowers the integrity by {@code amount}; non-positive amounts are ignored. */
    public void consume(int amount) {
        if (amount <= 0) return;
        value = Math.max(0, value - amount);
    }

    /**
     * Counts one tick of passive decay. Every {@code intervalTicks} ticks consumes 1 point.
     * @return true on the tick a point was consumed
     */
    public boolean tickPassive(int intervalTicks) {
        if (++ticksSinceDecay < intervalTicks) return false;
        ticksSinceDecay = 0;
        consume(1);
        return true;
    }

    public int ticksSinceDecay() {
        return ticksSinceDecay;
    }

    public void setTicksSinceDecay(int ticks) {
        this.ticksSinceDecay = Math.max(0, ticks);
    }
}

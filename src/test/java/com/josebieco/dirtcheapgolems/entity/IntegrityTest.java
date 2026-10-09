package com.josebieco.dirtcheapgolems.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegrityTest {
    @Test void startsAtMax() { assertEquals(100, new Integrity().value()); }

    @Test void consumeClampsAtZero() {
        var i = new Integrity(5);
        i.consume(34);
        assertEquals(0, i.value());
        assertTrue(i.isDepleted());
    }

    @Test void consumeIgnoresNonPositive() {
        var i = new Integrity();
        i.consume(-10);
        i.consume(0);
        assertEquals(100, i.value());
    }

    @Test void constructorClamps() {
        assertEquals(100, new Integrity(250).value());
        assertEquals(0, new Integrity(-3).value());
    }

    @Test void passiveDecaysOncePerInterval() {
        var i = new Integrity();
        int decays = 0;
        for (int t = 0; t < 1200; t++)
            if (i.tickPassive(600)) decays++;
        assertEquals(2, decays);
        assertEquals(98, i.value());
    }

    @Test void decayTicksRoundTrip() {
        var i = new Integrity();
        for (int t = 0; t < 250; t++) i.tickPassive(600);
        var copy = new Integrity(i.value());
        copy.setTicksSinceDecay(i.ticksSinceDecay());
        assertEquals(250, copy.ticksSinceDecay());
    }

    @Test void crackStages() {
        assertEquals(CrackStage.NONE, CrackStage.forIntegrity(100));
        assertEquals(CrackStage.NONE, CrackStage.forIntegrity(67));
        assertEquals(CrackStage.LOW, CrackStage.forIntegrity(66));
        assertEquals(CrackStage.LOW, CrackStage.forIntegrity(34));
        assertEquals(CrackStage.HIGH, CrackStage.forIntegrity(33));
        assertEquals(CrackStage.HIGH, CrackStage.forIntegrity(1));
    }
}

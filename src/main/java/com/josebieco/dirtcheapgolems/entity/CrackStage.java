package com.josebieco.dirtcheapgolems.entity;

/** Visual crack overlay stage for a given integrity (spec §5.5). */
public enum CrackStage {
    NONE, LOW, HIGH;

    public static CrackStage forIntegrity(int value) {
        if (value >= 67) return NONE;
        if (value >= 34) return LOW;
        return HIGH;
    }
}

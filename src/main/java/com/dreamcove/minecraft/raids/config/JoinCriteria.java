package com.dreamcove.minecraft.raids.config;

public class JoinCriteria {
    private int minimumPartySize;
    private int minimumLevel;

    public int getMinimumPartySize() {
        return minimumPartySize;
    }

    public void setMinimumPartySize(int minimumPartySize) {
        this.minimumPartySize = minimumPartySize;
    }

    public int getMinimumLevel() {
        return minimumLevel;
    }

    public void setMinimumLevel(int minimumLevel) {
        this.minimumLevel = minimumLevel;
    }
}

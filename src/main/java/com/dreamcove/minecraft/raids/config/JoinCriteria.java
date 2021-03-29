package com.dreamcove.minecraft.raids.config;

public class JoinCriteria {
    private int minimumPartySize;
    private int minimumRank;
    private int averagePartyRank;

    public JoinCriteria() {
    }

    public int getAveragePartyRank() {
        return averagePartyRank;
    }

    public void setAveragePartyRank(int averagePartyRank) {
        this.averagePartyRank = averagePartyRank;
    }

    public int getMinimumPartySize() {
        return minimumPartySize;
    }

    public void setMinimumPartySize(int minimumPartySize) {
        this.minimumPartySize = minimumPartySize;
    }

    public int getMinimumRank() {
        return minimumRank;
    }

    public void setMinimumRank(int minimumRank) {
        this.minimumRank = minimumRank;
    }
}

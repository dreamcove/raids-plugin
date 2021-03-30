package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.Party;
import com.dreamcove.minecraft.raids.api.World;
import com.dreamcove.minecraft.raids.config.Raid;

public class RaidManagedWorld extends ManagedWorld {
    public static int STATE_QUEUED = 0;
    public static int STATE_STARTED = 1;
    public static int STATE_CANCELED = 2;
    private final Raid raid;
    private final Party party;
    private int state;

    public RaidManagedWorld(World world, Raid raid, Party party) {
        super(world);
        this.raid = raid;
        this.party = party;
    }

    public Raid getRaid() {
        return raid;
    }

    public Party getParty() {
        return party;
    }

    @Override
    public boolean isExpired() {
        return getState() == STATE_CANCELED || getState() == STATE_STARTED && getWorld().getPlayers().isEmpty();
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}

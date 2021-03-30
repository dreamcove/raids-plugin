package com.dreamcove.minecraft.raids.api;

import java.util.List;
import java.util.UUID;

public abstract class PartyFactory {
    private static PartyFactory instance;

    public static PartyFactory getInstance() {
        return instance;
    }

    public static void setInstance(PartyFactory pInstance) {
        instance = pInstance;
    }

    public abstract Party getParty(UUID partyId);

    public abstract List<Party> getOnlineParties();

    public abstract UUID getPartyForPlayer(UUID playerId);
}

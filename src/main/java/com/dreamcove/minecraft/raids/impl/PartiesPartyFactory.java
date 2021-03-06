package com.dreamcove.minecraft.raids.impl;

import com.alessiodp.parties.api.Parties;
import com.dreamcove.minecraft.raids.api.Party;
import com.dreamcove.minecraft.raids.api.PartyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartiesPartyFactory extends PartyFactory {
    @Override
    public Party getParty(UUID partyId) {
        return new PartiesParty(Parties.getApi().getParty(partyId));
    }

    @Override
    public List<Party> getOnlineParties() {
        return Parties.getApi().getOnlineParties().stream()
                .map(PartiesParty::new)
                .collect(Collectors.toList());
    }

    @Override
    public UUID getPartyForPlayer(UUID playerId) {
        return getOnlineParties().stream()
                .filter(p -> p.getMembers().contains(playerId))
                .map(Party::getId)
                .findFirst()
                .orElse(null);
    }

    private static class PartiesParty implements Party {
        com.alessiodp.parties.api.interfaces.Party party;

        PartiesParty(com.alessiodp.parties.api.interfaces.Party party) {
            this.party = party;
        }

        @Override
        public UUID getId() {
            return party.getId();
        }

        @Override
        public List<UUID> getMembers() {
            return new ArrayList<>(party.getMembers());
        }

        @Override
        public void broadcastMessage(String message) {
            party.broadcastMessage(message, null);
        }

        @Override
        public String getName() {
            return party.getName();
        }
    }
}

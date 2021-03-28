package com.dreamcove.minecraft.raids;

import com.dreamcove.minecraft.raids.api.Party;
import com.dreamcove.minecraft.raids.api.PartyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class TestPartyFactory extends PartyFactory {

    private final List<Party> parties = new ArrayList<Party>();

    @Override
    public boolean arePartiesEnabled() {
        return true;
    }

    @Override
    public Party getParty(UUID partyId) {
        return parties.stream()
                .filter(p -> p.getId().equals(partyId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Party> getOnlineParties() {
        return new ArrayList<>(parties);
    }

    @Override
    public UUID getPartyForPlayer(UUID playerId) {
        return parties.stream()
                .filter(p -> p.getMembers().contains(playerId))
                .map(Party::getId)
                .findFirst()
                .orElse(null);
    }

    public void addParty(TestParty party) {
        parties.add(party);
    }

    public static class TestParty implements Party {

        private final UUID id = UUID.randomUUID();
        private final String name;
        private final List<UUID> members = new ArrayList<>();

        public TestParty(String name) {
            this.name = name;
        }

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public List<UUID> getMembers() {
            return new ArrayList<UUID>(members);
        }

        @Override
        public void broadcastMessage(String message) {
            Logger.getLogger("Party-" + getName()).info(message);
        }

        @Override
        public String getName() {
            return name;
        }

        public void addMember(UUID uuid) {
            members.add(uuid);
        }
    }
}

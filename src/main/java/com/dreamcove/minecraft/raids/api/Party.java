package com.dreamcove.minecraft.raids.api;

import java.util.List;
import java.util.UUID;

public interface Party {
    public UUID getId();

    public List<UUID> getMembers();

    public void broadcastMessage(String message);

    public String getName();
}

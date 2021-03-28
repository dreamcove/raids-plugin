package com.dreamcove.minecraft.raids.api;

import java.util.List;
import java.util.UUID;

public interface Party {
    UUID getId();

    List<UUID> getMembers();

    void broadcastMessage(String message);

    String getName();
}

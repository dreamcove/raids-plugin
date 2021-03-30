package com.dreamcove.minecraft.raids;

public class RaidsException extends Exception {
    public RaidsException(String message) {
        super(message);
    }

    public RaidsException(String message, Throwable t) {
        super(message, t);
    }
}

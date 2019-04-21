package com.pyb.trackme.socket;

public interface IEventListener {
    void onEvent(String event, Object[] args);
}

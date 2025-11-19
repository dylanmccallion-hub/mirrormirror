package com.example.mirrormirrorandroid;

public class MirrorDevice {
    public String id;
    public String name;
    public String ip;
    public boolean online;

    public MirrorDevice(String id, String name, String ip, boolean online) {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.online = online;
    }

    @Override
    public String toString() {
        return name + " (" + (online ? "Online" : "Offline") + ")";
    }
}

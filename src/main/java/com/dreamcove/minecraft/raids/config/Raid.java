package com.dreamcove.minecraft.raids.config;

public class Raid {
    private final JoinCriteria joinCriteria = new JoinCriteria();
    private final RaidSetup onStartup = new RaidSetup();
    private String name;
    private String dungeonName;
    private Point spawnLocation = new Point(0, 0, 0);
    private int joinIn = 15;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDungeonName() {
        return dungeonName;
    }

    public void setDungeonName(String dungeonName) {
        this.dungeonName = dungeonName;
    }

    public Point getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Point spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public JoinCriteria getJoinCriteria() {
        return joinCriteria;
    }

    public int getJoinIn() {
        return joinIn;
    }

    public void setJoinIn(int joinIn) {
        this.joinIn = joinIn;
    }

    public RaidSetup getOnStartup() {
        return onStartup;
    }
}

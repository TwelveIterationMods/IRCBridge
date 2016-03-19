package net.blay09.mods.ircbridge.config;

import net.minecraftforge.common.config.Configuration;

public class BridgeSettings {
    private static final String COMMENT_TOPIC = "Should the topic from IRC be sent to players joining Minecraft?";
    private static final String COMMENT_IRCJOINLEAVE = "Should join and leave messages from IRC be sent to Minecraft?";
    private static final String COMMENT_NICKCHANGE = "Should nick changes from IRC be sent to Minecraft?";
    private static final String COMMENT_MCJOINLEAVE = "Should join and leave messages from Minecraft be sent to IRC?";
    private static final String COMMENT_DEATH = "Should death messages from Minecraft be sent to IRC?";
    private static final String COMMENT_ACHIEVEMENT = "Should achievement messages from Minecraft be sent to IRC?";
    private static final String COMMENT_BROADCAST = "Should broadcast messages (/say) from Minecraft be sent to IRC?";

    private boolean ircTopic;
    private boolean ircJoinLeave;
    private boolean ircNickChange;
    private boolean minecraftJoinLeave;
    private boolean minecraftDeath;
    private boolean minecraftAchievement;
    private boolean minecraftBroadcast;

    public BridgeSettings() {
        reload();
    }

    public boolean isIrcTopic() {
        return ircTopic;
    }

    public boolean isIrcJoinLeave() {
        return ircJoinLeave;
    }

    public boolean isIrcNickChange() {
        return ircNickChange;
    }

    public boolean isMinecraftJoinLeave() {
        return minecraftJoinLeave;
    }

    public boolean isMinecraftDeath() {
        return minecraftDeath;
    }

    public boolean isMinecraftAchievement() {
        return minecraftAchievement;
    }

    public boolean isMinecraftBroadcast() {
        return minecraftBroadcast;
    }

    public void reload() {
        Configuration config = ConfigHandler.getConfig();
        ircTopic = config.getBoolean("Display IRC Topic", "general", true, COMMENT_TOPIC);
        ircJoinLeave = config.getBoolean("Display IRC Joins and Leaves", "general", true, COMMENT_IRCJOINLEAVE);
        ircNickChange = config.getBoolean("Display IRC Nick Changes", "general", true, COMMENT_NICKCHANGE);
        minecraftJoinLeave = config.getBoolean("Display Minecraft Joins and Leaves", "general", true, COMMENT_MCJOINLEAVE);
        minecraftDeath = config.getBoolean("Display Minecraft Deaths", "general", true, COMMENT_DEATH);
        minecraftAchievement = config.getBoolean("Display Minecraft Achievements", "general", true, COMMENT_ACHIEVEMENT);
        minecraftBroadcast = config.getBoolean("Display Minecraft Broadcasts", "general", true, COMMENT_BROADCAST);
    }
}

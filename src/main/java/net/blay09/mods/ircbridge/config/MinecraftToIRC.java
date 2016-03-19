package net.blay09.mods.ircbridge.config;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.blay09.mods.ircbridge.IRCFormatting;
import net.minecraftforge.common.config.Configuration;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftToIRC {

    public enum Type {
        Achievement,
        Broadcast,
        Chat,
        Death,
        Emote,
        Join,
        JoinRenamed, // TODO not functional yet, requires addition to Forge event
        Leave,
        PlayerList,
        NoPlayers
    }

    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("\u00a7(.)");
    private final EnumMap<Type, String> map = Maps.newEnumMap(Type.class);

    public MinecraftToIRC() {
        map.put(Type.Achievement, "%u has just earned the achievement §a[%m]");
        map.put(Type.Broadcast, "[%u] %m");
        map.put(Type.Chat, "<%u> %m");
        map.put(Type.Death, "%m");
        map.put(Type.Emote, "* %u %m");
        map.put(Type.Join, "§e%u joined the game");
        map.put(Type.JoinRenamed, "§e%u (formerly known as %m) joined the game");
        map.put(Type.Leave, "§e%u left the game");
        map.put(Type.PlayerList, "%m players online: %u");
        map.put(Type.NoPlayers, "No players online.");
    }

    public String format(Type type, String message, String user) {
        return map.get(type).replace("%m", message).replace("%u", preventUserPing(user));
    }

    public void reload() {
        Configuration config = ConfigHandler.getConfig();
        for(Type type : Type.values()) {
            map.put(type, config.getString(type.name(), "format.minecraft", map.get(type), "Variables: Username (%u), Message (%m)"));
        }
    }

    private static String preventUserPing(String user) {
        if(ConfigHandler.isPreventUserPing() && user.length() > 1) {
            return user.substring(0, 1) + "\u0081" + user.substring(1);
        }
        return user;
    }

    public static String convertFormatting(String message, boolean stripFormat) {
        Matcher matcher = MC_FORMAT_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while(matcher.find()) {
            if(!stripFormat) {
                ChatFormatting formatting = ChatFormatting.getByChar(matcher.group(1).charAt(0));
                if(formatting.isColor()) {
                    matcher.appendReplacement(sb, IRCFormatting.COLOR.getCharString() + getColorCodeForIRC(formatting));
                } else {
                    switch (formatting) {
                        case BOLD:
                            matcher.appendReplacement(sb, IRCFormatting.BOLD.getCharString());
                            break;
                        case UNDERLINE:
                            matcher.appendReplacement(sb, IRCFormatting.UNDERLINE.getCharString());
                            break;
                        case OBFUSCATED:
                            matcher.appendReplacement(sb, IRCFormatting.SECRET.getCharString());
                            break;
                        case RESET:
                            matcher.appendReplacement(sb, IRCFormatting.RESET.getCharString());
                            break;
                    }
                }
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static int getColorCodeForIRC(ChatFormatting formatting) {
        switch(formatting) {
            case BLACK: return 1; // Black
            case DARK_BLUE: return 2; // Dark Blue
            case DARK_GREEN: return 3; // Dark Green
            case DARK_AQUA: return 12; // Dark Aqua
            case DARK_RED: return 5; // Dark Red
            case DARK_PURPLE: return 5; // Dark Purple
            case GOLD: return 7; // Gold
            case GRAY: return 15; // Gray
            case DARK_GRAY: return 14; // Dark Gray
            case BLUE: return 11; // Blue
            case GREEN: return 9; // Green
            case AQUA: return 10; // Aqua
            case RED: return 4; // Red
            case LIGHT_PURPLE: return 13; // Light Purple
            case YELLOW: return 8; // Yellow
            case WHITE: return 0; // White
        }
        return 99;
    }
}

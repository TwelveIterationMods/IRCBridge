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

public class IRCToMinecraft {

    public enum Type {
        Chat,
        Emote,
        Join,
        Leave,
        NickChange,
        Topic,
        Quit,
        UserList,
        NoUsers
    }

    private static final String MC_FORMAT_CODE = "\u00a7";
    private static final Pattern IRC_FORMAT_PATTERN = Pattern.compile("(\u0003|\u0002|\u001f|\u0016|\u000f)([0-9][0-9]?)?(?:[,][0-9][0-9]?)?");

    private final EnumMap<Type, String> map = Maps.newEnumMap(Type.class);

    public IRCToMinecraft() {
        map.put(Type.Chat, ChatFormatting.GRAY + "%u: %m");
        map.put(Type.Emote, ChatFormatting.GRAY + "%u %m");
        map.put(Type.Join, ChatFormatting.YELLOW + "%u has joined %c");
        map.put(Type.Leave, ChatFormatting.YELLOW + "%u has left %c");
        map.put(Type.NickChange, ChatFormatting.GRAY + "%u is now known as %m");
        map.put(Type.Topic, ChatFormatting.YELLOW + "%m");
        map.put(Type.Quit, ChatFormatting.YELLOW + "%u has quit IRC (%m)");
        map.put(Type.UserList, "[%c] %m users online: %u");
        map.put(Type.NoUsers, "[%c] No users online.");
    }

    public String format(Type type, String message, String channel, String user) {
        return map.get(type).replace("%m", message).replace("%u", user).replace("%c", channel);
    }

    public void reload() {
        Configuration config = ConfigHandler.getConfig();
        for(Type type : Type.values()) {
            map.put(type, config.getString(type.name(), "format.irc", map.get(type), "Variables: Username (%u), Message (%m), Channel (%c)"));
        }
    }

    public static String convertFormatting(String message, boolean stripFormat) {
        Matcher matcher = IRC_FORMAT_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        Set<ChatFormatting> activeFormats = Sets.newHashSet();
        boolean isBold = false;
        boolean isUnderline = false;
        boolean isSecret = false;
        while(matcher.find()) {
            if(!stripFormat) {
                String codeIRC = matcher.group(1);
                if(codeIRC.charAt(0) == IRCFormatting.BOLD.getChar()) {
                    isBold = !isBold;
                    if(isBold) {
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.BOLD.getChar());
                        activeFormats.add(ChatFormatting.BOLD);
                    } else {
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.RESET.getChar());
                        activeFormats.remove(ChatFormatting.BOLD);
                        for(ChatFormatting formatting : activeFormats) {
                            sb.append(MC_FORMAT_CODE).append(formatting.getChar());
                        }
                    }
                } else if(codeIRC.charAt(0) == IRCFormatting.UNDERLINE.getChar()) {
                    isUnderline = !isUnderline;
                    if(isUnderline) {
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.UNDERLINE.getChar());
                        activeFormats.add(ChatFormatting.UNDERLINE);
                    } else {
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.RESET.getChar());
                        activeFormats.remove(ChatFormatting.UNDERLINE);
                        for(ChatFormatting formatting : activeFormats) {
                            sb.append(MC_FORMAT_CODE).append(formatting.getChar());
                        }
                    }
                } else if(codeIRC.charAt(0) == IRCFormatting.SECRET.getChar()) {
                    isSecret = !isSecret;
                    if(isSecret) {
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.OBFUSCATED.getChar());
                        activeFormats.add(ChatFormatting.OBFUSCATED);
                    } else {
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.RESET.getChar());
                        activeFormats.remove(ChatFormatting.OBFUSCATED);
                        for(ChatFormatting formatting : activeFormats) {
                            sb.append(MC_FORMAT_CODE).append(formatting.getChar());
                        }
                    }
                } else if(codeIRC.charAt(0) == IRCFormatting.RESET.getChar()) {
                    matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.RESET.getChar());
                } else if(codeIRC.charAt(0) == IRCFormatting.COLOR.getChar()) {
                    Iterator<ChatFormatting> it = activeFormats.iterator();
                    while(it.hasNext()) {
                        if(it.next().isColor()) {
                            it.remove();
                        }
                    }
                    String colorCode = matcher.group(2);
                    if(colorCode != null && !colorCode.equals("99")) {
                        ChatFormatting colorMC = getColorForMinecraft(Integer.parseInt(colorCode));
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + colorMC.getChar());
                        activeFormats.add(colorMC);
                    } else {
                        matcher.appendReplacement(sb, MC_FORMAT_CODE + ChatFormatting.RESET.getChar());
                        for(ChatFormatting formatting : activeFormats) {
                            sb.append(MC_FORMAT_CODE).append(formatting.getChar());
                        }
                    }
                }
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static ChatFormatting getColorForMinecraft(int codeIRC) {
        switch(codeIRC) {
            case 0: return ChatFormatting.WHITE;
            case 1: return ChatFormatting.BLACK;
            case 2: return ChatFormatting.DARK_BLUE;
            case 3: return ChatFormatting.DARK_GREEN;
            case 4: return ChatFormatting.RED;
            case 5: return ChatFormatting.DARK_RED;
            case 6: return ChatFormatting.DARK_PURPLE;
            case 7: return ChatFormatting.GOLD;
            case 8: return ChatFormatting.YELLOW;
            case 9: return ChatFormatting.GREEN;
            case 10: return ChatFormatting.AQUA;
            case 11: return ChatFormatting.BLUE;
            case 12: return ChatFormatting.DARK_AQUA;
            case 13: return ChatFormatting.LIGHT_PURPLE;
            case 14: return ChatFormatting.DARK_GRAY;
            case 15: return ChatFormatting.GRAY;
        }
        return ChatFormatting.WHITE;
    }
}

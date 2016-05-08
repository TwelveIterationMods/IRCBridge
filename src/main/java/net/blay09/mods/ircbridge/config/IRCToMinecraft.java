package net.blay09.mods.ircbridge.config;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.blay09.mods.ircbridge.IRCFormatting;
import net.minecraft.util.text.TextFormatting;
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

    private static final Pattern IRC_FORMAT_PATTERN = Pattern.compile("(\u0003|\u0002|\u001f|\u0016|\u000f)([0-9][0-9]?)?(?:[,][0-9][0-9]?)?");

    private final EnumMap<Type, String> map = Maps.newEnumMap(Type.class);

    public IRCToMinecraft() {
        map.put(Type.Chat, TextFormatting.GRAY + "\u300b%u: %m");
        map.put(Type.Emote, TextFormatting.GRAY + "\u300b%u %m");
        map.put(Type.Join, TextFormatting.YELLOW + "%u has joined %c");
        map.put(Type.Leave, TextFormatting.YELLOW + "%u has left %c");
        map.put(Type.NickChange, TextFormatting.GRAY + "%u is now known as %m");
        map.put(Type.Topic, TextFormatting.YELLOW + "%m");
        map.put(Type.Quit, TextFormatting.YELLOW + "%u has quit IRC (%m)");
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
        Set<TextFormatting> activeFormats = Sets.newHashSet();
        boolean isBold = false;
        boolean isUnderline = false;
        boolean isSecret = false;
        while(matcher.find()) {
            if(!stripFormat) {
                String codeIRC = matcher.group(1);
                if(codeIRC.charAt(0) == IRCFormatting.BOLD.getChar()) {
                    isBold = !isBold;
                    if(isBold) {
                        matcher.appendReplacement(sb, TextFormatting.BOLD.toString());
                        activeFormats.add(TextFormatting.BOLD);
                    } else {
                        matcher.appendReplacement(sb, TextFormatting.RESET.toString());
                        activeFormats.remove(TextFormatting.BOLD);
                        for(TextFormatting formatting : activeFormats) {
                            sb.append(formatting.toString());
                        }
                    }
                } else if(codeIRC.charAt(0) == IRCFormatting.UNDERLINE.getChar()) {
                    isUnderline = !isUnderline;
                    if(isUnderline) {
                        matcher.appendReplacement(sb, TextFormatting.UNDERLINE.toString());
                        activeFormats.add(TextFormatting.UNDERLINE);
                    } else {
                        matcher.appendReplacement(sb, TextFormatting.RESET.toString());
                        activeFormats.remove(TextFormatting.UNDERLINE);
                        for(TextFormatting formatting : activeFormats) {
                            sb.append(formatting.toString());
                        }
                    }
                } else if(codeIRC.charAt(0) == IRCFormatting.SECRET.getChar()) {
                    isSecret = !isSecret;
                    if(isSecret) {
                        matcher.appendReplacement(sb, TextFormatting.OBFUSCATED.toString());
                        activeFormats.add(TextFormatting.OBFUSCATED);
                    } else {
                        matcher.appendReplacement(sb, TextFormatting.RESET.toString());
                        activeFormats.remove(TextFormatting.OBFUSCATED);
                        for(TextFormatting formatting : activeFormats) {
                            sb.append(formatting.toString());
                        }
                    }
                } else if(codeIRC.charAt(0) == IRCFormatting.RESET.getChar()) {
                    matcher.appendReplacement(sb, TextFormatting.RESET.toString());
                } else if(codeIRC.charAt(0) == IRCFormatting.COLOR.getChar()) {
                    Iterator<TextFormatting> it = activeFormats.iterator();
                    while(it.hasNext()) {
                        if(it.next().isColor()) {
                            it.remove();
                        }
                    }
                    String colorCode = matcher.group(2);
                    if(colorCode != null && !colorCode.equals("99")) {
                        TextFormatting colorMC = getColorForMinecraft(Integer.parseInt(colorCode));
                        matcher.appendReplacement(sb, colorMC.toString());
                        activeFormats.add(colorMC);
                    } else {
                        matcher.appendReplacement(sb, TextFormatting.RESET.toString());
                        for(TextFormatting formatting : activeFormats) {
                            sb.append(formatting.toString());
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

    private static TextFormatting getColorForMinecraft(int codeIRC) {
        switch(codeIRC) {
            case 0: return TextFormatting.WHITE;
            case 1: return TextFormatting.BLACK;
            case 2: return TextFormatting.DARK_BLUE;
            case 3: return TextFormatting.DARK_GREEN;
            case 4: return TextFormatting.RED;
            case 5: return TextFormatting.DARK_RED;
            case 6: return TextFormatting.DARK_PURPLE;
            case 7: return TextFormatting.GOLD;
            case 8: return TextFormatting.YELLOW;
            case 9: return TextFormatting.GREEN;
            case 10: return TextFormatting.AQUA;
            case 11: return TextFormatting.BLUE;
            case 12: return TextFormatting.DARK_AQUA;
            case 13: return TextFormatting.LIGHT_PURPLE;
            case 14: return TextFormatting.DARK_GRAY;
            case 15: return TextFormatting.GRAY;
        }
        return TextFormatting.WHITE;
    }
}

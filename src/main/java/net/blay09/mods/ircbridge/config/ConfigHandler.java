package net.blay09.mods.ircbridge.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.blay09.javairc.IRCConfiguration;
import net.blay09.mods.ircbridge.IRCBridge;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

public class ConfigHandler {

    public static final String COMMENT_CHANNELS = "This is a list of channels to connect to. You can also specify a channel password by adding it to the end of the name, separated by a colon.";
    public static final String COMMENT_ENCODING = "The encoding to use for this IRC connection.";
    public static final String COMMENT_NICK = "The name of this server to use as a nickname in IRC.";
    public static final String COMMENT_SERVER = "The address of the IRC server to connect to.";
    public static final String COMMENT_PORT = "The port of the IRC server to connect to.";
    public static final String COMMENT_REALNAME = "The name to show in WHOIS requests.";
    public static final String COMMENT_USERNAME = "The username for this IRC connection (this is *not* for NickServ)";
    public static final String COMMENT_LOCALADDRESS = "The local address the IRC connection should be bound to.";
    public static final String COMMENT_SECURE = "Set this to true to enable secure connections through SSL.";
    public static final String COMMENT_SELFSIGNED = "Set this to true to accept self-signed certificates in secure connections (required for many IRC servers)";
    public static final String COMMENT_PASSWORD = "The server password for the IRC server (empty for no password).";
    public static final String COMMENT_MESSAGEDELAY = "The delay between messages to prevent accidental flooding.";
    public static final String COMMENT_DISABLEDIFFIEHELLMAN = "Set this to true to disable Diffie Helman for secure connections to work around a bug prior to Java 8";
    public static final String COMMENT_DEBUG = "Set this to true to enable debug mode.";
    public static final String COMMENT_IRCCOMMAND = "The IRC command that will trigger this mapping. Example: !players";
    public static final String COMMENT_MINECRAFTCOMMAND = "The Minecraft command run from this mapping. Example: /ircbridge players";
    public static final String COMMENT_REQUIRETRUSTED = "Are users required to be authenticated with NickServ and added to the trusted users list in order to run this command?";
    public static final String COMMENT_PRIVATERESPONSE = "Should the command response only be sent to the user who ran the command?";
    private static final int DEFAULT_PORT = 6667;

    private static final Set<ChannelEntry> channels = Sets.newHashSet();
    private static final Set<String> trustedUsers = Sets.newHashSet();
    private static final List<CommandMapping> commands = Lists.newArrayList();
    private static Configuration config;
    private static File configFile;
    private static IRCConfigException configException;
    private static boolean preventUserPing;
    private static boolean convertColors;

    public static IRCConfiguration load(File file) {
        ConfigHandler.configFile = file;
        return reload();
    }

    public static IRCConfiguration reload() {
        configException = null;
        channels.clear();
        commands.clear();
        trustedUsers.clear();
        config = new Configuration(configFile);
        config.load();

        preventUserPing = config.getBoolean("Prevent User Ping (experimental)", "general", false, "Should an invisible character be inserted into names coming from Minecraft to prevent pinging IRC users with the same name? This only works correctly on certain IRC clients and thus is disabled by default.");
        convertColors = config.getBoolean("Convert Formatting", "general", true, "Should formatting be converted between Minecraft <-> IRC?");

        String[] trustedUserList = config.getStringList("Trusted Users", "general", new String[0], "A list of NickServ authenticated names that are able to run commands with the requireTrusted flag set to true.");
        Collections.addAll(trustedUsers, trustedUserList);

        ConfigCategory commandsCategory = config.getCategory("commands");
        commandsCategory.setComment("Custom IRC channel commands that will be mapped to Minecraft commands can be defined here.");

        config.getString("IRC Command", "commands.players", "!players", COMMENT_IRCCOMMAND);
        config.getString("Minecraft Command", "commands.players", "/ircbridge players", COMMENT_IRCCOMMAND);
        config.getBoolean("Require Trusted", "commands.players", false, COMMENT_REQUIRETRUSTED);
        config.getBoolean("Private Response", "commands.players", false, COMMENT_PRIVATERESPONSE);

        config.getString("IRC Command", "commands.op", "!op", COMMENT_IRCCOMMAND);
        config.getString("Minecraft Command", "commands.op", "/ircbridge run", COMMENT_IRCCOMMAND);
        config.getBoolean("Require Trusted", "commands.op", true, COMMENT_REQUIRETRUSTED);
        config.getBoolean("Private Response", "commands.op", false, COMMENT_PRIVATERESPONSE);

        for(ConfigCategory command : commandsCategory.getChildren()) {
            String ircCommand = config.getString("IRC Command", command.getQualifiedName(), "", COMMENT_IRCCOMMAND);
            String mcCommand = config.getString("Minecraft Command", command.getQualifiedName(), "", COMMENT_MINECRAFTCOMMAND);
            boolean requireAuth = config.getBoolean("Require Trusted", command.getQualifiedName(), false, COMMENT_REQUIRETRUSTED);
            boolean privateResponse = config.getBoolean("Private Response", command.getQualifiedName(), false, COMMENT_PRIVATERESPONSE);
            if(ircCommand.isEmpty() || mcCommand.isEmpty()) {
                IRCBridge.logger.error("Could not register command mapping '" + command.getName() + "' - missing IRC Command or Minecraft Command property.");
                continue;
            }
            commands.add(new CommandMapping(ircCommand, mcCommand, requireAuth, privateResponse));
        }

        try {
            String[] channelList = config.getStringList("channels", "irc", new String[] {"#ChangeMe"}, COMMENT_CHANNELS);
            for (String channelEntry : channelList) {
                String[] s = channelEntry.split(":");
                if(s[0].equals("#ChangeMe")) {
                    throw new IRCConfigException(IRCConfigException.Type.NoChannels);
                }
                if (s.length == 1) {
                    channels.add(new ChannelEntry(s[0]));
                } else {
                    channels.add(new ChannelEntry(s[0], s[1]));
                }
            }

            IRCConfiguration result = buildConfig(config);
            config.save();
            return result;
        } catch (IRCConfigException e) {
            configException = e;
            config.save();
            return null;
        }
    }

    private static IRCConfiguration buildConfig(Configuration config) throws IRCConfigException {
        Charset encoding;
        try {
            encoding = Charset.forName(config.getString("encoding", "irc.advanced", "UTF-8", COMMENT_ENCODING));
        } catch (IllegalCharsetNameException e) {
            throw new IRCConfigException("Invalid value for option 'encoding': " + e.getMessage());
        } catch (UnsupportedCharsetException e) {
            throw new IRCConfigException("Invalid value for option 'encoding': " + e.getMessage());
        }
        String server = config.getString("server", "irc", "", COMMENT_SERVER);
        if(server.trim().isEmpty()) {
            throw new IRCConfigException(IRCConfigException.Type.NoServer);
        }
        String nick = config.getString("nick", "irc", "ChangeMe", COMMENT_NICK);
        if(nick.trim().isEmpty() || nick.equals("ChangeMe")) {
            throw new IRCConfigException(IRCConfigException.Type.NoNick);
        }
        return IRCConfiguration.builder()
                .server(server)
                .port(config.getInt("port", "irc", DEFAULT_PORT, 0, 65536, COMMENT_PORT))
                .nick(nick)
                .realName(config.getString("realName", "irc.advanced", "IRCBridge", COMMENT_REALNAME))
                .username(config.getString("username", "irc.advanced", "IRCBridge", COMMENT_USERNAME))
                .snapshots(true)
                .localAddress(config.getString("localAddress", "irc.advanced", "", COMMENT_LOCALADDRESS))
                .secure(config.getBoolean("secure", "irc.advanced", false, COMMENT_SECURE))
                .selfSigned(config.getBoolean("selfSigned", "irc.advanced", false, COMMENT_SELFSIGNED))
                .encoding(encoding)
                .password(config.getString("password", "irc.advanced", "", COMMENT_PASSWORD))
                .messageDelay(config.getInt("messageDelay", "irc.advanced", 33, 33, 1000, COMMENT_MESSAGEDELAY))
                .disableDiffieHellman(config.getBoolean("disableDiffieHellman", "irc.advanced", false, COMMENT_DISABLEDIFFIEHELLMAN))
                .debug(config.getBoolean("debug", "general", false, COMMENT_DEBUG))
                .build();
    }

    public static Set<ChannelEntry> getChannels() {
        return channels;
    }

    public static void setServer(String server, String password) {
        int port = DEFAULT_PORT;
        int portIndex = server.indexOf(":");
        if(portIndex != -1) {
            port = Integer.parseInt(server.substring(portIndex + 1));
            server = server.substring(0, portIndex - 1);
        }
        config.get("irc", "server", "", COMMENT_SERVER).set(server);
        config.get("irc", "port", DEFAULT_PORT, COMMENT_PORT).set(port);
        config.get("irc.advanced", "password", "", COMMENT_PASSWORD).set(password != null ? password : "");
        config.save();
    }

    public static void setNick(String nick) {
        config.get("irc", "nick", "").set(nick);
        config.save();
    }

    public static void addChannel(String name, String password) {
        channels.add(new ChannelEntry(name, password));
        updateChannelList();
    }

    public static void removeChannel(String name) {
        Iterator<ChannelEntry> it = channels.iterator();
        while(it.hasNext()) {
            if(it.next().getName().equals(name)) {
                it.remove();
                break;
            }
        }
        updateChannelList();
    }

    private static void updateChannelList() {
        String[] channelList = new String[channels.size()];
        int i = 0;
        for(ChannelEntry channel : channels) {
            channelList[i] = channel.getName() + (channel.getKey() != null ? ":" + channel.getKey() : "");
        }
        config.get("irc", "channels", new String[0], COMMENT_CHANNELS).set(channelList);
        config.save();
    }

    public static ITextComponent getErrorChatComponent() {
        if(configException != null) {
            switch(configException.getType()) {
                case NoChannels:
                    return new TextComponentString("IRC Bridge could not connect because no channels are defined. You can join a channel using '/ircbridge join'.");
                case NoServer:
                    return new TextComponentString("IRC Bridge could not connect because no server is defined. You can set a server using '/ircbridge server'.");
                case NoNick:
                    return new TextComponentString("IRC Bridge could not connect because no nickname is defined. You can set a nick using '/ircbridge nick'.");
                case Other:
                    return new TextComponentString("IRC Bridge could not connect due to a configuration error: " + configException.getMessage());
            }
        }
        return null;
    }

    public static Configuration getConfig() {
        return config;
    }

    public static boolean isPreventUserPing() {
        return preventUserPing;
    }

    public static Collection<CommandMapping> getCommands() {
        return commands;
    }

    public static boolean isConvertColors() {
        return convertColors;
    }

    public static Set<String> getTrustedUsers() {
        return trustedUsers;
    }
}

package net.blay09.mods.ircbridge.handler;

import net.blay09.javairc.IRCAdapter;
import net.blay09.javairc.IRCConnection;
import net.blay09.javairc.IRCMessage;
import net.blay09.javairc.IRCUser;
import net.blay09.mods.ircbridge.IRCBridge;
import net.blay09.mods.ircbridge.config.ChannelEntry;
import net.blay09.mods.ircbridge.config.CommandMapping;
import net.blay09.mods.ircbridge.config.ConfigHandler;
import net.blay09.mods.ircbridge.config.IRCToMinecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class IRCEventHandler extends IRCAdapter {

    private final IRCBridge bridge;
    private final IRCToMinecraft format;

    public IRCEventHandler(IRCBridge bridge, IRCToMinecraft format) {
        this.bridge = bridge;
        this.format = format;
    }

    @Override
    public void onConnected(IRCConnection connection) {
        for(ChannelEntry channel : ConfigHandler.getChannels()) {
            IRCBridge.instance.getConnection().join(channel.getName(), channel.getKey());
        }
    }

    @Override
    public void onConnectionFailed(IRCConnection connection, Exception e) {
        ITextComponent chatComponent = ConfigHandler.getErrorChatComponent();
        if(chatComponent != null) {
            IRCBridge.logger.error(chatComponent.getUnformattedText());
            for(EntityPlayer entityPlayer : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerList()) {
                if(IRCBridge.isOP(entityPlayer)) {
                    entityPlayer.addChatComponentMessage(chatComponent);
                }
            }
        }
    }

    @Override
    public void onUserJoin(IRCConnection connection, IRCMessage message, IRCUser user, String channel) {
        if(bridge.getBridgeSettings().isIrcJoinLeave()) {
            bridge.sendToMC(format.format(IRCToMinecraft.Type.Join, "", channel, user.getNick()));
        }
    }

    @Override
    public void onUserPart(IRCConnection connection, IRCMessage message, IRCUser user, String channel, String quitMessage) {
        if(bridge.getBridgeSettings().isIrcJoinLeave()) {
            bridge.sendToMC(format.format(IRCToMinecraft.Type.Leave, "", channel, user.getNick()));
        }
    }

    @Override
    public void onUserQuit(IRCConnection connection, IRCMessage message, IRCUser user, String quitMessage) {
        if(bridge.getBridgeSettings().isIrcJoinLeave()) {
            bridge.sendToMC(format.format(IRCToMinecraft.Type.Quit, "", "", user.getNick()));
        }
    }

    @Override
    public void onUserNickChange(IRCConnection connection, IRCMessage message, IRCUser user, String nick) {
        if(bridge.getBridgeSettings().isIrcNickChange()) {
            bridge.sendToMC(format.format(IRCToMinecraft.Type.NickChange, user.getNick(), "", nick));
        }
    }

    @Override
    public void onChannelTopic(IRCConnection connection, IRCMessage message, String channel, String topic) {
        if(bridge.getBridgeSettings().isIrcTopic()) {
            bridge.sendToMC(format.format(IRCToMinecraft.Type.Topic, topic, channel, ""));
        }
    }

    @Override
    public void onChannelChat(IRCConnection connection, IRCMessage message, IRCUser user, String channel, String text) {
        if(text.startsWith("\u0001ACTION ") && text.length() > 8) {
            text = text.substring(8, text.length() - 1);
            bridge.sendToMC(format.format(IRCToMinecraft.Type.Emote, text, channel, user.getNick()));
        } else {
            for(CommandMapping command : ConfigHandler.getCommands()) {
                if(text.equals(command.ircCommand) || text.startsWith(command.ircCommand + " ")) {
                    IRCCommandHandler.handleCommand(connection, channel, user, command, text.length() > command.ircCommand.length() ? text.substring(command.ircCommand.length()).split(" ") : new String[0]);
                }
            }
            bridge.sendToMC(format.format(IRCToMinecraft.Type.Chat, text, channel, user.getNick()));
        }
    }

}

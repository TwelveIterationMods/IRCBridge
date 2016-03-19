package net.blay09.mods.ircbridge;

import joptsimple.internal.Strings;
import net.blay09.javairc.snapshot.ChannelSnapshot;
import net.blay09.javairc.snapshot.UserSnapshot;
import net.blay09.mods.ircbridge.config.ChannelEntry;
import net.blay09.mods.ircbridge.config.ConfigHandler;
import net.blay09.mods.ircbridge.config.IRCToMinecraft;
import net.blay09.mods.ircbridge.config.MinecraftToIRC;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CommandIRCBridge extends CommandBase {
    @Override
    public String getCommandName() {
        return "ircbridge";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ircbridge (server|nick|join|leave|reload)";
    }

    public String getCommandUsage(ICommandSender sender, String subCommand) {
        if(subCommand.equals("server")) {
            return "/ircbridge server <address> [password]";
        } else if(subCommand.equals("nick")) {
            return "/ircbridge nick <name>";
        } else if(subCommand.equals("join")) {
            return "/ircbridge join <channel> [password]";
        } else if(subCommand.equals("leave")) {
            return "/ircbridge leave <channel>";
        } else if(subCommand.equals("run")) {
            return "/ircbridge run <command>";
        }
        return getCommandUsage(sender);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length < 1) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        String cmd = args[0];
        if(cmd.equals("server")) {
            if(args.length < 2) {
                throw new WrongUsageException(getCommandUsage(sender, cmd));
            }
            ConfigHandler.setServer(args[1], args.length >= 3 ? args[2] : null);
            IRCBridge.instance.reload();
        } else if(cmd.equals("nick")) {
            ConfigHandler.setNick(args[1]);
            if(IRCBridge.instance.isConnected()) {
                IRCBridge.instance.getConnection().nick(args[1]);
            } else {
                IRCBridge.instance.reload();
            }
        } else if(cmd.equals("join")) {
            ConfigHandler.addChannel(args[1], args.length >= 3 ? args[2] : null);
            if(IRCBridge.instance.isConnected()) {
                IRCBridge.instance.getConnection().join(args[1], args.length >= 3 ? args[2] : null);
            } else {
                IRCBridge.instance.reload();
            }
        } else if(cmd.equals("leave")) {
            ConfigHandler.removeChannel(args[1]);
            if(IRCBridge.instance.isConnected()) {
                IRCBridge.instance.getConnection().part(args[1]);
            } else {
                IRCBridge.instance.reload();
            }
        } else if(cmd.equals("who")) {
            for(ChannelEntry entry : ConfigHandler.getChannels()) {
                ChannelSnapshot channel = IRCBridge.instance.getConnection().getChannelSnapshot(entry.getName());
                if(!channel.getUsers().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for(UserSnapshot user : channel.getUsers()) {
                        if(sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(user.getNick());
                    }
                    sender.addChatMessage(new TextComponentString(IRCBridge.instance.getIrcToMinecraft().format(IRCToMinecraft.Type.UserList, String.valueOf(channel.getUsers().size()), channel.getName(), sb.toString())));
                } else {
                    sender.addChatMessage(new TextComponentString(IRCBridge.instance.getIrcToMinecraft().format(IRCToMinecraft.Type.NoUsers, "", channel.getName(), "")));
                }
            }
        } else if(cmd.equals("players")) {
            List<EntityPlayerMP> playerList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerList();
            if(!playerList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for(EntityPlayerMP entityPlayer : playerList) {
                    if(sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(entityPlayer.getDisplayNameString());
                }
                sender.addChatMessage(new TextComponentString(IRCBridge.instance.getMinecraftToIRC().format(MinecraftToIRC.Type.PlayerList, String.valueOf(playerList.size()), sb.toString())));
            } else {
                sender.addChatMessage(new TextComponentString(IRCBridge.instance.getMinecraftToIRC().format(MinecraftToIRC.Type.NoPlayers, "", "")));
            }
        } else if(cmd.equals("reload")) {
            IRCBridge.instance.reload();
        } else if(cmd.equals("run")) {
            ICommandManager commandManager = FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
            commandManager.executeCommand(sender, StringUtils.join(args, ' ', 1, args.length).trim());
        }
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if(args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "server", "nick", "join", "leave", "reload", "run");
        }
        return super.getTabCompletionOptions(server, sender, args, pos);
    }

}

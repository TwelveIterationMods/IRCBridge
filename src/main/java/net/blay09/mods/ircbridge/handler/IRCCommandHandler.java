package net.blay09.mods.ircbridge.handler;

import com.google.common.collect.Lists;
import joptsimple.internal.Strings;
import net.blay09.javairc.IRCConnection;
import net.blay09.javairc.IRCUser;
import net.blay09.javairc.snapshot.UserSnapshot;
import net.blay09.mods.ircbridge.config.CommandMapping;
import net.blay09.mods.ircbridge.config.ConfigHandler;
import net.minecraft.command.ICommandManager;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Iterator;
import java.util.List;

public class IRCCommandHandler {

    private static final int COMMAND_TIMEOUT = 60;

    private static class QueuedCommand {
        public final ICommandManager commandManager;
        public final IRCUserCommandSender sender;
        public final String[] args;
        public int ticksExisted;

        public QueuedCommand(ICommandManager commandManager, IRCUserCommandSender sender, String[] args) {
            this.commandManager = commandManager;
            this.sender = sender;
            this.args = args;
        }
    }

    private static final List<QueuedCommand> queuedCommands = Lists.newArrayList();

    public static void handleCommand(IRCConnection connection, String channel, IRCUser user, CommandMapping command, String[] args) {
        ICommandManager commandManager = FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
        IRCUserCommandSender sender = new IRCUserCommandSender(connection, channel, user, command);
        if(command.requireTrusted) {
            UserSnapshot userSnapshot = connection.getUserSnapshot(user.getNick());
            if (userSnapshot == null || !ConfigHandler.getTrustedUsers().contains(userSnapshot.getLoginName())) {
                queuedCommands.add(new QueuedCommand(commandManager, sender, args));
                connection.sendRaw("WHOIS " + user.getNick());
                return;
            }
        }
        commandManager.executeCommand(sender, command.mcCommand + " " + Strings.join(args, " "));
    }


    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        Iterator<QueuedCommand> it = queuedCommands.iterator();
        while(it.hasNext()) {
            QueuedCommand qCommand = it.next();
            UserSnapshot userSnapshot = qCommand.sender.getConnection().getUserSnapshot(qCommand.sender.getName());
            String loginName = userSnapshot != null ? userSnapshot.getLoginName() : null;
            if(loginName != null && !loginName.isEmpty()) {
                if(ConfigHandler.getTrustedUsers().contains(loginName)) {
                    qCommand.commandManager.executeCommand(qCommand.sender, qCommand.sender.getCommand() + " " + Strings.join(qCommand.args, " "));
                    it.remove();
                    continue;
                }
                qCommand.ticksExisted = COMMAND_TIMEOUT;
            }
            qCommand.ticksExisted++;
            if(qCommand.ticksExisted > COMMAND_TIMEOUT) {
                TextComponentTranslation chatComponent = new TextComponentTranslation("commands.generic.permission");
                chatComponent.getChatStyle().setColor(TextFormatting.RED);
                qCommand.sender.addChatMessage(chatComponent);
                it.remove();
            }
        }
    }
}

package net.blay09.mods.ircbridge.handler;

import net.blay09.javairc.IRCConnection;
import net.blay09.javairc.IRCUser;
import net.blay09.mods.ircbridge.config.CommandMapping;
import net.blay09.mods.ircbridge.config.ConfigHandler;
import net.blay09.mods.ircbridge.config.MinecraftToIRC;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class IRCUserCommandSender implements ICommandSender {

    private final IRCConnection connection;
    private final String channel;
    private final IRCUser user;
    private final CommandMapping command;

    public IRCUserCommandSender(IRCConnection connection, String channel, IRCUser user, CommandMapping command) {
        this.connection = connection;
        this.channel = channel;
        this.user = user;
        this.command = command;
    }

    @Override
    public String getName() {
        return user.getNick();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    public void addChatMessage(ITextComponent component) {
        String message = component.getFormattedText();
        message = MinecraftToIRC.convertFormatting(message, !ConfigHandler.isConvertColors());
        if(command.privateResponse) {
            connection.message(user.getNick(), message);
        } else {
            connection.message(channel, message);
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public BlockPos getPosition() {
        return BlockPos.ORIGIN;
    }

    @Override
    public Vec3d getPositionVector() {
        return Vec3d.ZERO;
    }

    @Override
    public World getEntityWorld() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld();
    }

    @Override
    public Entity getCommandSenderEntity() {
        return null;
    }

    @Override
    public boolean sendCommandFeedback() {
        return true;
    }

    @Override
    public void setCommandStat(CommandResultStats.Type type, int amount) {}

    @Override
    public MinecraftServer getServer() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public IRCConnection getConnection() {
        return connection;
    }

    public CommandMapping getCommand() {
        return command;
    }
}

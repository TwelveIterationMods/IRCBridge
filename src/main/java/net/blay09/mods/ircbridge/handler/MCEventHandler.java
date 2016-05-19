package net.blay09.mods.ircbridge.handler;

import net.blay09.mods.ircbridge.IRCBridge;
import net.blay09.mods.ircbridge.config.MinecraftToIRC;
import net.minecraft.command.server.CommandBroadcast;
import net.minecraft.command.server.CommandEmote;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AchievementEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.commons.lang3.StringUtils;

public class MCEventHandler {

    private final IRCBridge bridge;
    private final MinecraftToIRC mcToIRC;

    public MCEventHandler(IRCBridge bridge, MinecraftToIRC mcToIRC) {
        this.bridge = bridge;
        this.mcToIRC = mcToIRC;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(bridge.getBridgeSettings().isMinecraftJoinLeave()) {
            bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Join, "", event.player.getDisplayNameString()));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if(bridge.getBridgeSettings().isMinecraftJoinLeave()) {
            bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Leave, "", event.player.getDisplayNameString()));
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if(!event.getCommand().checkPermission(FMLCommonHandler.instance().getMinecraftServerInstance(), event.getSender())) {
            return;
        }
        if(event.getCommand() instanceof CommandBroadcast) {
            if(bridge.getBridgeSettings().isMinecraftBroadcast()) {
                bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Broadcast, StringUtils.join(event.getParameters(), ' '), event.getSender().getDisplayName().getUnformattedText()));
            }
        } else if(event.getCommand() instanceof CommandEmote) {
            bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Emote, StringUtils.join(event.getParameters(), ' '), event.getSender().getDisplayName().getUnformattedText()));
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Chat, event.getMessage(), event.getPlayer().getDisplayNameString()));
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if(bridge.getBridgeSettings().isMinecraftDeath()) {
            if (event.getEntityLiving() instanceof EntityPlayer) {
                bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Death, event.getSource().getDeathMessage(event.getEntityLiving()).getUnformattedText(), ((EntityPlayer) event.getEntityLiving()).getDisplayNameString()));
            }
        }
    }

    @SubscribeEvent
    public void onAchievement(AchievementEvent event) {
        if(bridge.getBridgeSettings().isMinecraftAchievement()) {
            EntityPlayerMP entityPlayer = (EntityPlayerMP) event.getEntityPlayer();
            if (entityPlayer.getStatFile().hasAchievementUnlocked(event.getAchievement())) {
                // This is necessary because the Achievement event fires even if an achievement is already unlocked.
                return;
            }
            if (!entityPlayer.getStatFile().canUnlockAchievement(event.getAchievement())) {
                // This is necessary because the Achievement event fires even if an achievement can not be unlocked yet.
                return;
            }
            bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Achievement, event.getAchievement().getStatName().getUnformattedText(), event.getEntityPlayer().getDisplayNameString()));
        }
    }

}

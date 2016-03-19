package net.blay09.mods.ircbridge.handler;

import joptsimple.internal.Strings;
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

public class MCEventHandler {

    private final IRCBridge bridge;
    private final MinecraftToIRC mcToIRC;

    public MCEventHandler(IRCBridge bridge, MinecraftToIRC mcToIRC) {
        this.bridge = bridge;
        this.mcToIRC = mcToIRC;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Join, "", event.player.getDisplayNameString()));
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Leave, "", event.player.getDisplayNameString()));
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if(!event.command.checkPermission(FMLCommonHandler.instance().getMinecraftServerInstance(), event.sender)) {
            return;
        }
        if(event.command instanceof CommandBroadcast) {
            bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Broadcast, Strings.join(event.parameters, " "), event.sender.getDisplayName().getUnformattedText()));
        } else if(event.command instanceof CommandEmote) {
            bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Emote, Strings.join(event.parameters, " "), event.sender.getDisplayName().getUnformattedText()));
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Chat, event.getMessage(), event.getPlayer().getDisplayNameString()));
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if(event.entityLiving instanceof EntityPlayer) {
            bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Death, event.source.getDeathMessage(event.entityLiving).getUnformattedText(), ((EntityPlayer) event.entityLiving).getDisplayNameString()));
        }
    }

    @SubscribeEvent
    public void onAchievement(AchievementEvent event) {
        EntityPlayerMP entityPlayer = (EntityPlayerMP) event.entityPlayer;
        if(entityPlayer.getStatFile().hasAchievementUnlocked(event.achievement)) {
            // This is necessary because the Achievement event fires even if an achievement is already unlocked.
            return;
        }
        if(!entityPlayer.getStatFile().canUnlockAchievement(event.achievement)) {
            // This is necessary because the Achievement event fires even if an achievement can not be unlocked yet.
            return;
        }
        bridge.sendToIRC(mcToIRC.format(MinecraftToIRC.Type.Achievement, event.achievement.getStatName().getUnformattedText(), event.entityPlayer.getDisplayNameString()));
    }

}

package net.blay09.mods.ircbridge;

import net.blay09.javairc.IRCConfiguration;
import net.blay09.javairc.IRCConnection;
import net.blay09.mods.ircbridge.config.*;
import net.blay09.mods.ircbridge.handler.IRCCommandHandler;
import net.blay09.mods.ircbridge.handler.IRCEventHandler;
import net.blay09.mods.ircbridge.handler.MCEventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = IRCBridge.MOD_ID, name = "IRC Bridge", serverSideOnly = true, acceptableRemoteVersions = "*")
public class IRCBridge {

    public static final Logger logger = LogManager.getLogger();
    public static final String MOD_ID = "ircbridge";

    private IRCConfiguration config;
    private BridgeSettings bridgeSettings;
    private MinecraftToIRC minecraftToIRC;
    private IRCToMinecraft ircToMinecraft;
    private IRCConnection connection;

    @Mod.Instance
    public static IRCBridge instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = ConfigHandler.load(event.getSuggestedConfigurationFile());
        bridgeSettings = new BridgeSettings();
        minecraftToIRC = new MinecraftToIRC();
        ircToMinecraft = new IRCToMinecraft();

        MinecraftForge.EVENT_BUS.register(new MCEventHandler(this, minecraftToIRC));
        MinecraftForge.EVENT_BUS.register(new IRCCommandHandler());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandIRCBridge());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        reload();
    }

    @Mod.EventHandler
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(isOP(event.player)) {
            event.player.addChatComponentMessage(ConfigHandler.getErrorChatComponent());
        }
    }

    public void sendToIRC(String message) {
        if(connection == null) {
            return;
        }
        message = MinecraftToIRC.convertFormatting(message, !ConfigHandler.isConvertColors());
        for(ChannelEntry channel : ConfigHandler.getChannels()) {
            if(message.startsWith("/me ") && message.length() > 4) {
                message = "\u0001ACTION " + message.substring(5) + "\u0001";
            }
            connection.message(channel.getName(), message);
        }
    }

    public void sendToMC(String message) {
        message = message.replace('\u00a7', '$'); // We need to do this to prevent client crashes and formatting issues, since § is a special character in Minecraft
        message = IRCToMinecraft.convertFormatting(message, !ConfigHandler.isConvertColors());
        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendChatMsg(ForgeHooks.newChatWithLinks(message));
    }

    public void reload() {
        if(connection != null && connection.isConnected()) {
            connection.quit("Reloading IRC Bridge...");
            connection = null;
        }
        config = ConfigHandler.reload();
        bridgeSettings.reload();
        ircToMinecraft.reload();
        minecraftToIRC.reload();
        if(config != null) {
            connection = new IRCConnection(config, new IRCEventHandler(this, ircToMinecraft));
            connection.start();
        } else {
            ITextComponent chatComponent = ConfigHandler.getErrorChatComponent();
            if(chatComponent != null) {
                logger.error(chatComponent.getUnformattedText());
                for(EntityPlayer entityPlayer : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerList()) {
                    if(isOP(entityPlayer)) {
                        entityPlayer.addChatComponentMessage(chatComponent);
                    }
                }
            }
        }
    }

    public IRCConnection getConnection() {
        return connection;
    }

    public MinecraftToIRC getMinecraftToIRC() {
        return minecraftToIRC;
    }

    public IRCToMinecraft getIrcToMinecraft() {
        return ircToMinecraft;
    }

    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    public static boolean isOP(EntityPlayer entityPlayer) {
        UserListOpsEntry entry = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getOppedPlayers().getEntry(entityPlayer.getGameProfile());
        return entry != null && entry.getPermissionLevel() > 0;
    }
}

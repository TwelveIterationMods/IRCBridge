package net.blay09.mods.ircbridge.config;

public class CommandMapping {

    public final String ircCommand;
    public final String mcCommand;
    public final boolean requireTrusted;
    public final boolean privateResponse;

    public CommandMapping(String ircCommand, String mcCommand, boolean requireTrusted, boolean privateResponse) {
        this.ircCommand = ircCommand;
        this.mcCommand = mcCommand;
        this.requireTrusted = requireTrusted;
        this.privateResponse = privateResponse;
    }

}

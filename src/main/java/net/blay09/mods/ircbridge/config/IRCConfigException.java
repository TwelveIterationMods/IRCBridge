package net.blay09.mods.ircbridge.config;

public class IRCConfigException extends RuntimeException {

    public enum Type {
        NoChannels,
        NoNick,
        NoServer,
        Other
    }

    private final Type type;

    public IRCConfigException(Type type) {
        this.type = type;
    }

    public IRCConfigException(String message) {
        super(message);
        this.type = Type.Other;
    }

    public Type getType() {
        return type;
    }
}

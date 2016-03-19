package net.blay09.mods.ircbridge.config;

public class ChannelEntry {
    private final String name;
    private final String key;

    public ChannelEntry(String name) {
        this.name = name;
        this.key = null;
    }

    public ChannelEntry(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChannelEntry that = (ChannelEntry) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

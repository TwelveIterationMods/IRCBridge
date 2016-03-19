package net.blay09.mods.ircbridge;

public enum IRCFormatting {
    BOLD('\u0002'),
    UNDERLINE('\u001f'),
    SECRET('\u0016'),
    RESET('\u000f'),
    COLOR('\u0003');

    private final char character;

    IRCFormatting(char character) {
        this.character = character;
    }

    public char getChar() {
        return character;
    }

    public String getCharString() {
        return String.valueOf(character);
    }
}

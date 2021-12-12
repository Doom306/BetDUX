package com.general_hello.commands.commands.Work;

public enum Colors {
    RED("Red", "🔴"),
    BLUE("Blue", "🔵"),
    GREEN("Green", "🟢"),
    YELLOW("Yellow", "🟡"),
    BLACK("Black", "⚫"),
    WHITE("White", "⚪");

    private final String name;
    private final String emoji;

    Colors(String name, String emoji) {
        this.name = name;
        this.emoji = emoji;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }
}

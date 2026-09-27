package com.turkerozturk.node.properties;

public enum TitleColor {
    NONE(0x000000), BLUE(0x3584E4), GREEN(0x33D17A), YELLOW(0xF6D32D),
    ORANGE(0xFF7800), RED(0xE01B24), PURPLE(0x9141AC), BROWN(0x986A44),
    LIGHT_GREY(0xDEDDDA), DARK_GREY(0x3D3846);

    private final int rgb;
    TitleColor(int rgb) { this.rgb = rgb; }
    public int rgb() { return rgb; }
    public String hex() { return String.format("#%06X", rgb); }
    public static TitleColor fromBits(long isRichText) {
        int color = (int) ((isRichText >>> 3) & 0xFFFFFF);
        for (TitleColor value : values()) {
            if (value.rgb == color) return value;
        }
        return null;
    }
}

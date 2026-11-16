/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.helpers;

import java.util.HashMap;
import java.util.Map;

public enum NodeIcon {

    OTHER(0, 0, "zero", "No Icon", "other"),

    /**  original CherryTree mappings below: **/
    CT_CIRCLE_GREEN(2, 1, "ct_circle-green", "Circle Green", "status"),
    CT_CIRCLE_YELLOW(4, 2, "ct_circle-yellow", "Circle Yellow", "status"),
    CT_CIRCLE_RED(6, 3, "ct_circle-red", "Circle Red", "status"),
    CT_CIRCLE_GREY(8, 4, "ct_circle-grey", "Circle Grey", "status"),
    CT_ADD(10, 5, "ct_add", "Add", "actions"),
    CT_REMOVE(12, 6, "ct_remove", "Remove", "actions"),
    CT_DONE(14, 7, "ct_done", "Done", "status"),
    CT_CANCEL(16, 8, "ct_cancel", "Cancel", "status"),
    CT_EDIT_DELETE(18, 9, "ct_edit_delete", "Edit Delete", "actions"),
    CT_WARNING(20, 10, "ct_warning", "Warning", "status"),
    CT_STAR(22, 11, "ct_star", "Star", "symbols"),
    CT_INFO(24, 12, "ct_info", "Info", "status"),
    CT_HELP(26, 13, "ct_help", "Help", "status"),
    CT_HOME(28, 14, "ct_home", "Home", "places"),
    CT_INDEX(30, 15, "ct_index", "Index", "navigation"),
    CT_MAIL(32, 16, "ct_mail", "Mail", "technology"),
    CT_HTML(34, 17, "ct_html", "Html", "formats"),
    CT_NOTES(36, 18, "ct_notes", "Notes", "files"),
    CT_TIMESTAMP(38, 19, "ct_timestamp", "Timestamp", "activities"),
    CT_CALENDAR(40, 20, "ct_calendar", "Calendar", "activities"),
    CT_TERM(42, 21, "ct_term", "Term", "technology"),
    CT_TERM_RED(44, 22, "ct_term-red", "Term Red", "technology"),
    CT_PYTHON(46, 23, "ct_python", "Python", "programming"),
    CT_JAVA(48, 24, "ct_java", "Java", "programming"),
    CT_NODE_BULLET(50, 25, "ct_node_bullet", "Node Bullet", "symbols"),
    CT_NODE_NO_ICON(52, 26, "ct_node_no_icon", "Node No Icon", "symbols"),
    CHERRY_BLACK(54, 27, "cherry_black", "Cherry Black", "cherries"),
    CHERRY_BLUE(56, 28, "cherry_blue", "Cherry Blue", "cherries"),
    CHERRY_CYAN(58, 29, "cherry_cyan", "Cherry Cyan", "cherries"),
    CHERRY_GREEN(60, 30, "cherry_green", "Cherry Green", "cherries"),
    CHERRY_GREY(62, 31, "cherry_grey", "Cherry Grey", "cherries"),
    CHERRY_ORANGE(64, 32, "cherry_orange", "Cherry Orange", "cherries"),
    CHERRY_ORANGE_DARK(66, 33, "cherry_orange_dark", "Cherry Orange Dark", "cherries"),
    CHERRY_PURPLE(68, 34, "cherry_purple", "Cherry Purple", "cherries"),
    CHERRY_RED(70, 35, "cherry_red", "Cherry Red", "cherries"),
    CHERRY_SHERBERT(72, 36, "cherry_sherbert", "Cherry Sherbert", "cherries"),
    CHERRY_YELLOW(74, 37, "cherry_yellow", "Cherry Yellow", "cherries"),
    CT_CODE(76, 38, "ct_code", "Code", "programming"),
    CT_FIND(78, 39, "ct_find", "Find", "actions"),
    CT_LOCKED(80, 40, "ct_locked", "Locked", "status"),
    CT_UNLOCKED(82, 41, "ct_unlocked", "Unlocked", "status"),
    CT_PEOPLE(84, 42, "ct_people", "People", "people"),
    CT_URGENT(86, 43, "ct_urgent", "Urgent", "status"),
    CT_DIRECTORY(88, 44, "ct_directory", "Directory", "files"),
    CT_LEAF(90, 45, "ct_leaf", "Leaf", "nature"),
    CT_XML(92, 46, "ct_xml", "Xml", "formats"),
    CT_C(94, 47, "ct_c", "C", "programming"),
    CT_CPP(96, 48, "ct_cpp", "Cpp", "programming"),
    CT_PERL(98, 49, "ct_perl", "Perl", "programming"),
    CT_PIN(100, 50, "ct_pin", "Pin", "navigation"),
    CT_ANCHOR(102, 51, "ct_anchor", "Anchor", "navigation"),
    CT_EDIT(104, 52, "ct_edit", "Edit", "actions"),
    CT_SAVE(106, 53, "ct_save", "Save", "actions"),
    CT_EXECUTE(108, 54, "ct_execute", "Execute", "actions"),
    CT_PREFERENCES(110, 55, "ct_preferences", "Preferences", "actions"),
    CT_CLEAR(112, 56, "ct_clear", "Clear", "actions"),
    CT_STOP(114, 57, "ct_stop", "Stop", "actions"),
    CT_CLOSE(116, 58, "ct_close", "Close", "actions"),
    CT_QUIT_APP(118, 59, "ct_quit-app", "Quit App", "actions"),
    CT_FILE(120, 60, "ct_file", "File", "files"),
    CT_PRINT(122, 61, "ct_print", "Print", "actions"),
    CT_FILE_ICON(124, 62, "ct_file_icon", "File Icon", "files"),
    CT_LINK_HANDLE(126, 63, "ct_link_handle", "Link Handle", "navigation"),
    CT_LINK_WEBSITE(128, 64, "ct_link_website", "Link Website", "navigation"),
    CT_NETWORK(130, 65, "ct_network", "Network", "technology"),
    CT_GO_BACK(132, 66, "ct_go-back", "Go Back", "navigation"),
    CT_GO_DOWN(134, 67, "ct_go-down", "Go Down", "navigation"),
    CT_GO_FORWARD(136, 68, "ct_go-forward", "Go Forward", "navigation"),
    CT_GO_UP(138, 69, "ct_go-up", "Go Up", "navigation"),
    CT_GO_JUMP(140, 70, "ct_go-jump", "Go Jump", "navigation"),
    CT_ZOOM_OUT(142, 71, "ct_zoom-out", "Zoom Out", "navigation"),
    CT_ZOOM_IN(144, 72, "ct_zoom-in", "Zoom In", "navigation"),
    CT_BG(146, 73, "ct_bg", "Bg", "flags"),
    CT_CS(148, 74, "ct_cs", "Cs", "flags"),
    CT_DE(150, 75, "ct_de", "De", "flags"),
    CT_EL(152, 76, "ct_el", "El", "flags"),
    CT_EN(154, 77, "ct_en", "En", "flags"),
    CT_EN_US(156, 78, "ct_en_US", "En Us", "flags"),
    CT_ES(158, 79, "ct_es", "Es", "flags"),
    CT_FI(160, 80, "ct_fi", "Fi", "flags"),
    CT_FR(162, 81, "ct_fr", "Fr", "flags"),
    CT_HY(164, 82, "ct_hy", "Hy", "flags"),
    CT_IT(166, 83, "ct_it", "It", "flags"),
    CT_JA(168, 84, "ct_ja", "Ja", "flags"),
    CT_LT(170, 85, "ct_lt", "Lt", "flags"),
    CT_NL(172, 86, "ct_nl", "Nl", "flags"),
    CT_PL(174, 87, "ct_pl", "Pl", "flags"),
    CT_PT_BR(176, 88, "ct_pt_BR", "Pt Br", "flags"),
    CT_RU(178, 89, "ct_ru", "Ru", "flags"),
    CT_SL(180, 90, "ct_sl", "Sl", "flags"),
    CT_SV(182, 91, "ct_sv", "Sv", "flags"),
    CT_TR(184, 92, "ct_tr", "Tr", "flags"),
    CT_UK(186, 93, "ct_uk", "Uk", "flags"),
    CT_ZH_CN(188, 94, "ct_zh_CN", "Zh Cn", "flags"),
    CT_SPORTS(190, 95, "ct_sports", "Sports", "activities"),
    CT_BRIEFCASE(192, 96, "ct_briefcase", "Briefcase", "activities"),
    CT_CAMERA(194, 97, "ct_camera", "Camera", "activities"),
    CT_CHART(196, 98, "ct_chart", "Chart", "activities"),
    CT_CLAPPERBOARD(198, 99, "ct_clapperboard", "Clapperboard", "activities"),
    CT_MATHS(200, 100, "ct_maths", "Maths", "activities"),
    CT_GAMES(202, 101, "ct_games", "Games", "activities"),
    CT_GLOBE(204, 102, "ct_globe", "Globe", "places"),
    CT_SERVER(206, 103, "ct_server", "Server", "technology"),
    CT_MONEY(208, 104, "ct_money", "Money", "activities"),
    CT_PAINTING(210, 105, "ct_painting", "Painting", "activities"),
    CT_PUZZLE(212, 106, "ct_puzzle", "Puzzle", "activities"),
    CT_SHOPPING(214, 107, "ct_shopping", "Shopping", "activities"),
    CT_HEART(216, 108, "ct_heart", "Heart", "symbols"),
    CT_SMILE(218, 109, "ct_smile", "Smile", "people"),
    CT_SMILE_COOL(220, 110, "ct_smile_cool", "Smile Cool", "people"),
    CT_SMILE_SURPR(222, 111, "ct_smile_surpr", "Smile Surpr", "people"),
    CT_SKULL(224, 112, "ct_skull", "Skull", "status"),
    CT_NO_ACCESS(226, 113, "ct_no_access", "No Access", "status"),
    CT_RUBY(228, 114, "ct_ruby", "Ruby", "programming"),
    CT_TUX(230, 115, "ct_tux", "Tux", "technology"),
    CT_GNOME(232, 116, "ct_gnome", "Gnome", "technology"),
    CT_DEBIAN(234, 117, "ct_debian", "Debian", "technology"),
    CT_UBUNTU(236, 118, "ct_ubuntu", "Ubuntu", "technology"),
    CT_FREEBSD(238, 119, "ct_freebsd", "Freebsd", "technology"),
    CT_WIN10(240, 120, "ct_win10", "Win10", "technology"),
    CT_WIN2012(242, 121, "ct_win2012", "Win2012", "technology"),
    CT_KO(244, 122, "ct_ko", "Ko", "flags"),
    CT_KK_KZ(246, 123, "ct_kk_KZ", "Kk Kz", "flags"),
    CT_RO(248, 124, "ct_ro", "Ro", "flags"),
    CT_HR(250, 125, "ct_hr", "Hr", "flags"),
    CT_GHOST(252, 126, "ct_ghost", "Ghost", "status"),
    CT_PT(254, 127, "ct_pt", "Pt", "flags"),
    CT_ZH_TW(256, 128, "ct_zh_TW", "Zh Tw", "flags"),
    CT_HI_IN(258, 129, "ct_hi_IN", "Hi In", "flags"),
    CT_AR(260, 130, "ct_ar", "Ar", "flags"),
    CT_HU(262, 131, "ct_hu", "Hu", "flags"),
    CT_HEART_UKRAINE(264, 132, "ct_heart_ukraine", "Heart Ukraine", "flags"),
    CT_MICROCHIP(266, 133, "ct_microchip", "Microchip", "technology"),
    CT_ANSIBLE(268, 134, "ct_ansible", "Ansible", "technology"),
    CT_AWS(270, 135, "ct_aws", "Aws", "technology"),
    CT_AZURE(272, 136, "ct_azure", "Azure", "technology"),
    CT_DOCKER(274, 137, "ct_docker", "Docker", "technology"),
    CT_GCP(276, 138, "ct_gcp", "Gcp", "technology"),
    CT_KUBERNETES(278, 139, "ct_kubernetes", "Kubernetes", "technology"),
    CT_CSHARP(280, 140, "ct_csharp", "Csharp", "programming"),
    CT_7ZIP(282, 141, "ct_7zip", "7zip", "technology"),
    CT_CHAT(284, 142, "ct_chat", "Chat", "technology"),
    CT_DB(286, 143, "ct_db", "Db", "technology"),
    CT_DICTIONARY(288, 144, "ct_dictionary", "Dictionary", "files"),
    CT_INVEST(290, 145, "ct_invest", "Invest", "activities"),
    CT_KEYS(292, 146, "ct_keys", "Keys", "technology"),
    CT_W_CLOUDY(294, 147, "ct_W-cloudy", "W Cloudy", "weather"),
    CT_W_FEW_CLOUDS(296, 148, "ct_W-few-clouds", "W Few Clouds", "weather"),
    CT_W_FOG(298, 149, "ct_W-fog", "W Fog", "weather"),
    CT_W_NIGHT_CLEAR(300, 150, "ct_W-night-clear", "W Night Clear", "weather"),
    CT_W_NIGHT_FEW_CLOUDS(302, 151, "ct_W-night-few-clouds", "W Night Few Clouds", "weather"),
    CT_W_SHOWERS(304, 152, "ct_W-showers", "W Showers", "weather"),
    CT_W_SNOW(306, 153, "ct_W-snow", "W Snow", "weather"),
    CT_W_STORM(308, 154, "ct_W-storm", "W Storm", "weather"),
    CT_W_SUNNY(310, 155, "ct_W-sunny", "W Sunny", "weather"),
    CT_APPLE(312, 156, "ct_apple", "Apple", "technology"),
    CT_BIKE(314, 157, "ct_bike", "Bike", "transport"),
    CT_BLUETOOTH(316, 158, "ct_bluetooth", "Bluetooth", "technology"),
    CT_BUILDING(318, 159, "ct_building", "Building", "places"),
    CT_BUS(320, 160, "ct_bus", "Bus", "transport"),
    CT_CAR(322, 161, "ct_car", "Car", "transport"),
    CT_CELLPHONE(324, 162, "ct_cellphone", "Cellphone", "technology"),
    CT_CLOUD(326, 163, "ct_cloud", "Cloud", "weather"),
    CT_COMPUTER(328, 164, "ct_computer", "Computer", "technology"),
    CT_DISPLAY(330, 165, "ct_display", "Display", "technology"),
    CT_DRIVE_HARDDISK(332, 166, "ct_drive-harddisk", "Drive Harddisk", "technology"),
    CT_DRIVE_USB(334, 167, "ct_drive-usb", "Drive Usb", "technology"),
    CT_FEMALE(336, 168, "ct_female", "Female", "people"),
    CT_FOOD(338, 169, "ct_food", "Food", "food"),
    CT_HAMBURGER(340, 170, "ct_hamburger", "Hamburger", "food"),
    CT_LIFEBUOY(342, 171, "ct_lifebuoy", "Lifebuoy", "status"),
    CT_LINUXMINT(344, 172, "ct_linuxmint", "Linuxmint", "technology"),
    CT_MALE(346, 173, "ct_male", "Male", "people"),
    CT_PIZZA(348, 174, "ct_pizza", "Pizza", "food"),
    CT_TELEPHONE(350, 175, "ct_telephone", "Telephone", "technology"),
    CT_WIFI(352, 176, "ct_wifi", "Wifi", "technology"),
    CT_ANTENNA(354, 177, "ct_antenna", "Antenna", "technology"),
    CT_RUST(356, 178, "ct_rust", "Rust", "programming"),
    CT_FA(358, 179, "ct_fa", "Fa", "flags"),
    CT_GO(360, 180, "ct_go", "Go", "programming"),
    CT_BUG(362, 181, "ct_bug", "Bug", "animals"),
    CT_GITHUB(364, 182, "ct_github", "Github", "technology"),
    CT_GITLAB(366, 183, "ct_gitlab", "Gitlab", "technology"),
    CT_CMAKE(368, 184, "ct_cmake", "Cmake", "technology"),
    CT_CSS(370, 185, "ct_css", "Css", "formats"),
    CT_CSV(372, 186, "ct_csv", "Csv", "formats"),
    CT_DIFF(374, 187, "ct_diff", "Diff", "formats"),
    CT_JS(376, 188, "ct_js", "Js", "programming"),
    CT_JSON(378, 189, "ct_json", "Json", "formats"),
    CT_YAML(380, 190, "ct_yaml", "Yaml", "formats"),
    CT_LATEX(382, 191, "ct_latex", "Latex", "formats"),
    CT_LUA(384, 192, "ct_lua", "Lua", "programming"),
    CT_MARKDOWN(386, 193, "ct_markdown", "Markdown", "formats"),
    CT_MATLAB(388, 194, "ct_matlab", "Matlab", "programming"),
    CT_MESON(390, 195, "ct_meson", "Meson", "technology"),
    CT_PHP(392, 196, "ct_php", "Php", "programming"),
    CT_SCALA(394, 197, "ct_scala", "Scala", "programming"),
    CT_SWIFT(396, 198, "ct_swift", "Swift", "programming"),
    CT_INI(398, 199, "ct_ini", "Ini", "formats"),
    CT_GTK(400, 200, "ct_gtk", "Gtk", "technology"),
    CT_QT(402, 201, "ct_qt", "Qt", "technology"),
    CT_BULB(404, 202, "ct_bulb", "Bulb", "symbols"),
    CT_AIRPLANE(406, 203, "ct_airplane", "Airplane", "transport"),
    CT_ALARM_CLOCK(408, 204, "ct_alarm_clock", "Alarm Clock", "status"),
    CT_ANDROID(410, 205, "ct_android", "Android", "technology"),
    CT_BAT(412, 206, "ct_bat", "Bat", "animals"),
    CT_BEAR(414, 207, "ct_bear", "Bear", "animals"),
    CT_BELL(416, 208, "ct_bell", "Bell", "status"),
    CT_BULLSEYE(418, 209, "ct_bullseye", "Bullseye", "activities"),
    CT_BUTTERFLY(420, 210, "ct_butterfly", "Butterfly", "animals"),
    CT_CAT(422, 211, "ct_cat", "Cat", "animals"),
    CT_CHICK(424, 212, "ct_chick", "Chick", "animals"),
    CT_COFFEE_BEANS(426, 213, "ct_coffee_beans", "Coffee Beans", "food"),
    CT_DOG(428, 214, "ct_dog", "Dog", "animals"),
    CT_DOLPHIN(430, 215, "ct_dolphin", "Dolphin", "animals"),
    CT_DOWNLOAD(432, 216, "ct_download", "Download", "actions"),
    CT_DUCK(434, 217, "ct_duck", "Duck", "animals"),
    CT_FEDORA(436, 218, "ct_fedora", "Fedora", "technology"),
    CT_FISH(438, 219, "ct_fish", "Fish", "animals"),
    CT_FOUR_LEAF_CLOVER(440, 220, "ct_four_leaf_clover", "Four Leaf Clover", "nature"),
    CT_FOX(442, 221, "ct_fox", "Fox", "animals"),
    CT_GIT(444, 222, "ct_git", "Git", "technology"),
    CT_GREEN_APPLE(446, 223, "ct_green_apple", "Green Apple", "food"),
    CT_HAMSTER(448, 224, "ct_hamster", "Hamster", "animals"),
    CT_HORSE(450, 225, "ct_horse", "Horse", "animals"),
    CT_HOT_DRINK(452, 226, "ct_hot_drink", "Hot Drink", "food"),
    CT_KOALA(454, 227, "ct_koala", "Koala", "animals"),
    CT_LADY_BEETLE(456, 228, "ct_lady_beetle", "Lady Beetle", "animals"),
    CT_LION(458, 229, "ct_lion", "Lion", "animals"),
    CT_MAP_MARKER(460, 230, "ct_map_marker", "Map Marker", "places"),
    CT_MONKEY(462, 231, "ct_monkey", "Monkey", "animals"),
    CT_MUSHROOM(464, 232, "ct_mushroom", "Mushroom", "food"),
    CT_OWL(466, 233, "ct_owl", "Owl", "animals"),
    CT_PANDA(468, 234, "ct_panda", "Panda", "animals"),
    CT_PIG(470, 235, "ct_pig", "Pig", "animals"),
    CT_POOL_8_BALL(472, 236, "ct_pool_8_ball", "Pool 8 Ball", "activities"),
    CT_RABBIT(474, 237, "ct_rabbit", "Rabbit", "animals"),
    CT_RAINBOW(476, 238, "ct_rainbow", "Rainbow", "weather"),
    CT_ROCKET(478, 239, "ct_rocket", "Rocket", "transport"),
    CT_ROOSTER(480, 240, "ct_rooster", "Rooster", "animals"),
    CT_SHIELD(482, 241, "ct_shield", "Shield", "status"),
    CT_SHIP(484, 242, "ct_ship", "Ship", "transport"),
    CT_STACKOVERFLOW(486, 243, "ct_stackoverflow", "Stackoverflow", "technology"),
    CT_ZEBRA(488, 244, "ct_zebra", "Zebra", "animals"),
    CT_POSTMAN(490, 245, "ct_postman", "Postman", "technology"),

    // CherryTree ct_const.h: ID is stable; is_ro stores (ID << 1) plus its low read-only bit.
    // Source: eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f
    CT_SK(492, 246, "ct_sk", "Slovak", "flags"),
    CT_VISIBLE(494, 247, "ct_visible", "Visible", "status"),
    CT_INVISIBLE(496, 248, "ct_invisible", "Invisible", "status"),
    CT_ARROW_LEFT(498, 249, "ct_arrow-left", "Arrow Left", "navigation"),
    CT_ARROW_RIGHT(500, 250, "ct_arrow-right", "Arrow Right", "navigation"),
    CT_ARROW_DOWN(502, 251, "ct_arrow-down", "Arrow Down", "navigation"),
    CT_ARROW_UP(504, 252, "ct_arrow-up", "Arrow Up", "navigation"),
    CT_JENKINS(506, 253, "ct_jenkins", "Jenkins", "technology"),
    CT_POSTGRESQL(508, 254, "ct_postgresql", "PostgreSQL", "technology"),
    CT_SPRING_BOOT(510, 255, "ct_spring-boot", "Spring Boot", "technology"),
    CT_SNOWFLAKE(512, 256, "ct_snowflake", "Snowflake", "technology"),
    CT_VIM(514, 257, "ct_vim", "Vim", "technology"),
    CT_LOCALE(516, 258, "ct_locale", "Locale", "technology"),
    CT_GEMINI(518, 259, "ct_gemini", "Gemini", "technology"),
    CT_CHATGPT(520, 260, "ct_chatgpt", "ChatGPT", "technology"),
    CT_COPILOT(522, 261, "ct_copilot", "Copilot", "technology"),
    CT_CLAUDE(524, 262, "ct_claude", "Claude", "technology"),
    CT_TA(526, 263, "ct_ta", "Tamil", "flags");


    private final Integer iconIdIn16bit;
    private final Integer iconId;

    private final String iconName;
    private final String iconNameForHuman;
    private final String category;

    NodeIcon(Integer iconIdIn16bit, Integer iconId, String iconName, String iconNameForHuman, String category) {
        this.iconIdIn16bit = iconIdIn16bit;
        this.iconId = iconId;
        this.iconName = iconName;
        this.iconNameForHuman = iconNameForHuman;
        this.category = category;
    }

    private static final Map<Integer, NodeIcon> _map = new HashMap<Integer, NodeIcon>();
    static {
        for (NodeIcon enumVariableName : NodeIcon.values())
            _map.put(enumVariableName.iconIdIn16bit, enumVariableName);
    }

    public static NodeIcon getNodeIconByIconIdIn16bit(Integer iconIdIn16bit) {
        NodeIcon nodeIcon = _map.get(iconIdIn16bit);
        return nodeIcon != null ? nodeIcon : OTHER;
    }

    private static final Map<Integer, NodeIcon> _mapByIconId = new HashMap<Integer, NodeIcon>();
    static {
        for (NodeIcon enumVariableName : NodeIcon.values())
            _mapByIconId.put(enumVariableName.iconId, enumVariableName);
    }

    public static NodeIcon getNodeIconByIconId(Integer iconId) {
        NodeIcon nodeIcon = _mapByIconId.get(iconId);
        return nodeIcon != null ? nodeIcon : OTHER;
    }


    /** Accepts canonical PNG basenames or human labels, independent of case and word separators. */
    public static NodeIcon fromNameOrLabel(String value) {
        if(value==null || value.length()>128) return OTHER;
        String key=value.trim().toLowerCase(java.util.Locale.ROOT);
        for(NodeIcon icon:values()) if(icon.iconName.equalsIgnoreCase(key)) return icon;
        String normalized=key.replaceAll("[\\s_-]+", "");
        for(NodeIcon icon:values()) if(icon.iconNameForHuman.toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s_-]+", "").equals(normalized)) return icon;
        return OTHER;
    }

    @Override
    public String toString() {
        return "NodeIcon{" +
                "iconIdIn16bit=" + iconIdIn16bit +
                ", iconId=" + iconId +
                ", iconName='" + iconName + '\'' +
                ", iconNameForHuman='" + iconNameForHuman + '\'' +
                '}';
    }

    public String getIconName() {
        return iconName;
    }


    public Integer getIconIdIn16bit() {
        return iconIdIn16bit;
    }

    public Integer getIconId() {
        return iconId;
    }

    /** Presentation-only metadata; never encoded in a CTB icon ID. */
    public String getCategory() { return category; }

    public String getIconNameForHuman() {
        return iconNameForHuman;
    }
}

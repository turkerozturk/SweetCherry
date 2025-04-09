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

    OTHER(0, 0, "zero", "No Icon"),

    /**  original CherryTree mappings below: **/
    CT_CIRCLE_GREEN(2, 1, "ct_circle-green", "Circle Green"),
    CT_CIRCLE_YELLOW(4, 2, "ct_circle-yellow", "Circle Yellow"),
    CT_CIRCLE_RED(6, 3, "ct_circle-red", "Circle Red"),
    CT_CIRCLE_GREY(8, 4, "ct_circle-grey", "Circle Grey"),
    CT_ADD(10, 5, "ct_add", "Add"),
    CT_REMOVE(12, 6, "ct_remove", "Remove"),
    CT_DONE(14, 7, "ct_done", "Done"),
    CT_CANCEL(16, 8, "ct_cancel", "Cancel"),
    CT_EDIT_DELETE(18, 9, "ct_edit_delete", "Edit Delete"),
    CT_WARNING(20, 10, "ct_warning", "Warning"),
    CT_STAR(22, 11, "ct_star", "Star"),
    CT_INFO(24, 12, "ct_info", "Info"),
    CT_HELP(26, 13, "ct_help", "Help"),
    CT_HOME(28, 14, "ct_home", "Home"),
    CT_INDEX(30, 15, "ct_index", "Index"),
    CT_MAIL(32, 16, "ct_mail", "Mail"),
    CT_HTML(34, 17, "ct_html", "Html"),
    CT_NOTES(36, 18, "ct_notes", "Notes"),
    CT_TIMESTAMP(38, 19, "ct_timestamp", "Timestamp"),
    CT_CALENDAR(40, 20, "ct_calendar", "Calendar"),
    CT_TERM(42, 21, "ct_term", "Term"),
    CT_TERM_RED(44, 22, "ct_term-red", "Term Red"),
    CT_PYTHON(46, 23, "ct_python", "Python"),
    CT_JAVA(48, 24, "ct_java", "Java"),
    CT_NODE_BULLET(50, 25, "ct_node_bullet", "Node Bullet"),
    CT_NODE_NO_ICON(52, 26, "ct_node_no_icon", "Node No Icon"),
    CHERRY_BLACK(54, 27, "cherry_black", "Cherry Black"),
    CHERRY_BLUE(56, 28, "cherry_blue", "Cherry Blue"),
    CHERRY_CYAN(58, 29, "cherry_cyan", "Cherry Cyan"),
    CHERRY_GREEN(60, 30, "cherry_green", "Cherry Green"),
    CHERRY_GREY(62, 31, "cherry_grey", "Cherry Grey"),
    CHERRY_ORANGE(64, 32, "cherry_orange", "Cherry Orange"),
    CHERRY_ORANGE_DARK(66, 33, "cherry_orange_dark", "Cherry Orange Dark"),
    CHERRY_PURPLE(68, 34, "cherry_purple", "Cherry Purple"),
    CHERRY_RED(70, 35, "cherry_red", "Cherry Red"),
    CHERRY_SHERBERT(72, 36, "cherry_sherbert", "Cherry Sherbert"),
    CHERRY_YELLOW(74, 37, "cherry_yellow", "Cherry Yellow"),
    CT_CODE(76, 38, "ct_code", "Code"),
    CT_FIND(78, 39, "ct_find", "Find"),
    CT_LOCKED(80, 40, "ct_locked", "Locked"),
    CT_UNLOCKED(82, 41, "ct_unlocked", "Unlocked"),
    CT_PEOPLE(84, 42, "ct_people", "People"),
    CT_URGENT(86, 43, "ct_urgent", "Urgent"),
    CT_DIRECTORY(88, 44, "ct_directory", "Directory"),
    CT_LEAF(90, 45, "ct_leaf", "Leaf"),
    CT_XML(92, 46, "ct_xml", "Xml"),
    CT_C(94, 47, "ct_c", "C"),
    CT_CPP(96, 48, "ct_cpp", "Cpp"),
    CT_PERL(98, 49, "ct_perl", "Perl"),
    CT_PIN(100, 50, "ct_pin", "Pin"),
    CT_ANCHOR(102, 51, "ct_anchor", "Anchor"),
    CT_EDIT(104, 52, "ct_edit", "Edit"),
    CT_SAVE(106, 53, "ct_save", "Save"),
    CT_EXECUTE(108, 54, "ct_execute", "Execute"),
    CT_PREFERENCES(110, 55, "ct_preferences", "Preferences"),
    CT_CLEAR(112, 56, "ct_clear", "Clear"),
    CT_STOP(114, 57, "ct_stop", "Stop"),
    CT_CLOSE(116, 58, "ct_close", "Close"),
    CT_QUIT_APP(118, 59, "ct_quit-app", "Quit App"),
    CT_FILE(120, 60, "ct_file", "File"),
    CT_PRINT(122, 61, "ct_print", "Print"),
    CT_FILE_ICON(124, 62, "ct_file_icon", "File Icon"),
    CT_LINK_HANDLE(126, 63, "ct_link_handle", "Link Handle"),
    CT_LINK_WEBSITE(128, 64, "ct_link_website", "Link Website"),
    CT_NETWORK(130, 65, "ct_network", "Network"),
    CT_GO_BACK(132, 66, "ct_go-back", "Go Back"),
    CT_GO_DOWN(134, 67, "ct_go-down", "Go Down"),
    CT_GO_FORWARD(136, 68, "ct_go-forward", "Go Forward"),
    CT_GO_UP(138, 69, "ct_go-up", "Go Up"),
    CT_GO_JUMP(140, 70, "ct_go-jump", "Go Jump"),
    CT_ZOOM_OUT(142, 71, "ct_zoom-out", "Zoom Out"),
    CT_ZOOM_IN(144, 72, "ct_zoom-in", "Zoom In"),
    CT_BG(146, 73, "ct_bg", "Bg"),
    CT_CS(148, 74, "ct_cs", "Cs"),
    CT_DE(150, 75, "ct_de", "De"),
    CT_EL(152, 76, "ct_el", "El"),
    CT_EN(154, 77, "ct_en", "En"),
    CT_EN_US(156, 78, "ct_en_US", "En Us"),
    CT_ES(158, 79, "ct_es", "Es"),
    CT_FI(160, 80, "ct_fi", "Fi"),
    CT_FR(162, 81, "ct_fr", "Fr"),
    CT_HY(164, 82, "ct_hy", "Hy"),
    CT_IT(166, 83, "ct_it", "It"),
    CT_JA(168, 84, "ct_ja", "Ja"),
    CT_LT(170, 85, "ct_lt", "Lt"),
    CT_NL(172, 86, "ct_nl", "Nl"),
    CT_PL(174, 87, "ct_pl", "Pl"),
    CT_PT_BR(176, 88, "ct_pt_BR", "Pt Br"),
    CT_RU(178, 89, "ct_ru", "Ru"),
    CT_SL(180, 90, "ct_sl", "Sl"),
    CT_SV(182, 91, "ct_sv", "Sv"),
    CT_TR(184, 92, "ct_tr", "Tr"),
    CT_UK(186, 93, "ct_uk", "Uk"),
    CT_ZH_CN(188, 94, "ct_zh_CN", "Zh Cn"),
    CT_SPORTS(190, 95, "ct_sports", "Sports"),
    CT_BRIEFCASE(192, 96, "ct_briefcase", "Briefcase"),
    CT_CAMERA(194, 97, "ct_camera", "Camera"),
    CT_CHART(196, 98, "ct_chart", "Chart"),
    CT_CLAPPERBOARD(198, 99, "ct_clapperboard", "Clapperboard"),
    CT_MATHS(200, 100, "ct_maths", "Maths"),
    CT_GAMES(202, 101, "ct_games", "Games"),
    CT_GLOBE(204, 102, "ct_globe", "Globe"),
    CT_SERVER(206, 103, "ct_server", "Server"),
    CT_MONEY(208, 104, "ct_money", "Money"),
    CT_PAINTING(210, 105, "ct_painting", "Painting"),
    CT_PUZZLE(212, 106, "ct_puzzle", "Puzzle"),
    CT_SHOPPING(214, 107, "ct_shopping", "Shopping"),
    CT_HEART(216, 108, "ct_heart", "Heart"),
    CT_SMILE(218, 109, "ct_smile", "Smile"),
    CT_SMILE_COOL(220, 110, "ct_smile_cool", "Smile Cool"),
    CT_SMILE_SURPR(222, 111, "ct_smile_surpr", "Smile Surpr"),
    CT_SKULL(224, 112, "ct_skull", "Skull"),
    CT_NO_ACCESS(226, 113, "ct_no_access", "No Access"),
    CT_RUBY(228, 114, "ct_ruby", "Ruby"),
    CT_TUX(230, 115, "ct_tux", "Tux"),
    CT_GNOME(232, 116, "ct_gnome", "Gnome"),
    CT_DEBIAN(234, 117, "ct_debian", "Debian"),
    CT_UBUNTU(236, 118, "ct_ubuntu", "Ubuntu"),
    CT_FREEBSD(238, 119, "ct_freebsd", "Freebsd"),
    CT_WIN10(240, 120, "ct_win10", "Win10"),
    CT_WIN2012(242, 121, "ct_win2012", "Win2012"),
    CT_KO(244, 122, "ct_ko", "Ko"),
    CT_KK_KZ(246, 123, "ct_kk_KZ", "Kk Kz"),
    CT_RO(248, 124, "ct_ro", "Ro"),
    CT_HR(250, 125, "ct_hr", "Hr"),
    CT_GHOST(252, 126, "ct_ghost", "Ghost"),
    CT_PT(254, 127, "ct_pt", "Pt"),
    CT_ZH_TW(256, 128, "ct_zh_TW", "Zh Tw"),
    CT_HI_IN(258, 129, "ct_hi_IN", "Hi In"),
    CT_AR(260, 130, "ct_ar", "Ar"),
    CT_HU(262, 131, "ct_hu", "Hu"),
    CT_HEART_UKRAINE(264, 132, "ct_heart_ukraine", "Heart Ukraine"),
    CT_MICROCHIP(266, 133, "ct_microchip", "Microchip"),
    CT_ANSIBLE(268, 134, "ct_ansible", "Ansible"),
    CT_AWS(270, 135, "ct_aws", "Aws"),
    CT_AZURE(272, 136, "ct_azure", "Azure"),
    CT_DOCKER(274, 137, "ct_docker", "Docker"),
    CT_GCP(276, 138, "ct_gcp", "Gcp"),
    CT_KUBERNETES(278, 139, "ct_kubernetes", "Kubernetes"),
    CT_CSHARP(280, 140, "ct_csharp", "Csharp"),
    CT_7ZIP(282, 141, "ct_7zip", "7zip"),
    CT_CHAT(284, 142, "ct_chat", "Chat"),
    CT_DB(286, 143, "ct_db", "Db"),
    CT_DICTIONARY(288, 144, "ct_dictionary", "Dictionary"),
    CT_INVEST(290, 145, "ct_invest", "Invest"),
    CT_KEYS(292, 146, "ct_keys", "Keys"),
    CT_W_CLOUDY(294, 147, "ct_W-cloudy", "W Cloudy"),
    CT_W_FEW_CLOUDS(296, 148, "ct_W-few-clouds", "W Few Clouds"),
    CT_W_FOG(298, 149, "ct_W-fog", "W Fog"),
    CT_W_NIGHT_CLEAR(300, 150, "ct_W-night-clear", "W Night Clear"),
    CT_W_NIGHT_FEW_CLOUDS(302, 151, "ct_W-night-few-clouds", "W Night Few Clouds"),
    CT_W_SHOWERS(304, 152, "ct_W-showers", "W Showers"),
    CT_W_SNOW(306, 153, "ct_W-snow", "W Snow"),
    CT_W_STORM(308, 154, "ct_W-storm", "W Storm"),
    CT_W_SUNNY(310, 155, "ct_W-sunny", "W Sunny"),
    CT_APPLE(312, 156, "ct_apple", "Apple"),
    CT_BIKE(314, 157, "ct_bike", "Bike"),
    CT_BLUETOOTH(316, 158, "ct_bluetooth", "Bluetooth"),
    CT_BUILDING(318, 159, "ct_building", "Building"),
    CT_BUS(320, 160, "ct_bus", "Bus"),
    CT_CAR(322, 161, "ct_car", "Car"),
    CT_CELLPHONE(324, 162, "ct_cellphone", "Cellphone"),
    CT_CLOUD(326, 163, "ct_cloud", "Cloud"),
    CT_COMPUTER(328, 164, "ct_computer", "Computer"),
    CT_DISPLAY(330, 165, "ct_display", "Display"),
    CT_DRIVE_HARDDISK(332, 166, "ct_drive-harddisk", "Drive Harddisk"),
    CT_DRIVE_USB(334, 167, "ct_drive-usb", "Drive Usb"),
    CT_FEMALE(336, 168, "ct_female", "Female"),
    CT_FOOD(338, 169, "ct_food", "Food"),
    CT_HAMBURGER(340, 170, "ct_hamburger", "Hamburger"),
    CT_LIFEBUOY(342, 171, "ct_lifebuoy", "Lifebuoy"),
    CT_LINUXMINT(344, 172, "ct_linuxmint", "Linuxmint"),
    CT_MALE(346, 173, "ct_male", "Male"),
    CT_PIZZA(348, 174, "ct_pizza", "Pizza"),
    CT_TELEPHONE(350, 175, "ct_telephone", "Telephone"),
    CT_WIFI(352, 176, "ct_wifi", "Wifi"),
    CT_ANTENNA(354, 177, "ct_antenna", "Antenna"),
    CT_RUST(356, 178, "ct_rust", "Rust"),
    CT_FA(358, 179, "ct_fa", "Fa"),
    CT_GO(360, 180, "ct_go", "Go"),
    CT_BUG(362, 181, "ct_bug", "Bug"),
    CT_GITHUB(364, 182, "ct_github", "Github"),
    CT_GITLAB(366, 183, "ct_gitlab", "Gitlab"),
    CT_CMAKE(368, 184, "ct_cmake", "Cmake"),
    CT_CSS(370, 185, "ct_css", "Css"),
    CT_CSV(372, 186, "ct_csv", "Csv"),
    CT_DIFF(374, 187, "ct_diff", "Diff"),
    CT_JS(376, 188, "ct_js", "Js"),
    CT_JSON(378, 189, "ct_json", "Json"),
    CT_YAML(380, 190, "ct_yaml", "Yaml"),
    CT_LATEX(382, 191, "ct_latex", "Latex"),
    CT_LUA(384, 192, "ct_lua", "Lua"),
    CT_MARKDOWN(386, 193, "ct_markdown", "Markdown"),
    CT_MATLAB(388, 194, "ct_matlab", "Matlab"),
    CT_MESON(390, 195, "ct_meson", "Meson"),
    CT_PHP(392, 196, "ct_php", "Php"),
    CT_SCALA(394, 197, "ct_scala", "Scala"),
    CT_SWIFT(396, 198, "ct_swift", "Swift"),
    CT_INI(398, 199, "ct_ini", "Ini"),
    CT_GTK(400, 200, "ct_gtk", "Gtk"),
    CT_QT(402, 201, "ct_qt", "Qt"),
    CT_BULB(404, 202, "ct_bulb", "Bulb"),
    CT_AIRPLANE(406, 203, "ct_airplane", "Airplane"),
    CT_ALARM_CLOCK(408, 204, "ct_alarm_clock", "Alarm Clock"),
    CT_ANDROID(410, 205, "ct_android", "Android"),
    CT_BAT(412, 206, "ct_bat", "Bat"),
    CT_BEAR(414, 207, "ct_bear", "Bear"),
    CT_BELL(416, 208, "ct_bell", "Bell"),
    CT_BULLSEYE(418, 209, "ct_bullseye", "Bullseye"),
    CT_BUTTERFLY(420, 210, "ct_butterfly", "Butterfly"),
    CT_CAT(422, 211, "ct_cat", "Cat"),
    CT_CHICK(424, 212, "ct_chick", "Chick"),
    CT_COFFEE_BEANS(426, 213, "ct_coffee_beans", "Coffee Beans"),
    CT_DOG(428, 214, "ct_dog", "Dog"),
    CT_DOLPHIN(430, 215, "ct_dolphin", "Dolphin"),
    CT_DOWNLOAD(432, 216, "ct_download", "Download"),
    CT_DUCK(434, 217, "ct_duck", "Duck"),
    CT_FEDORA(436, 218, "ct_fedora", "Fedora"),
    CT_FISH(438, 219, "ct_fish", "Fish"),
    CT_FOUR_LEAF_CLOVER(440, 220, "ct_four_leaf_clover", "Four Leaf Clover"),
    CT_FOX(442, 221, "ct_fox", "Fox"),
    CT_GIT(444, 222, "ct_git", "Git"),
    CT_GREEN_APPLE(446, 223, "ct_green_apple", "Green Apple"),
    CT_HAMSTER(448, 224, "ct_hamster", "Hamster"),
    CT_HORSE(450, 225, "ct_horse", "Horse"),
    CT_HOT_DRINK(452, 226, "ct_hot_drink", "Hot Drink"),
    CT_KOALA(454, 227, "ct_koala", "Koala"),
    CT_LADY_BEETLE(456, 228, "ct_lady_beetle", "Lady Beetle"),
    CT_LION(458, 229, "ct_lion", "Lion"),
    CT_MAP_MARKER(460, 230, "ct_map_marker", "Map Marker"),
    CT_MONKEY(462, 231, "ct_monkey", "Monkey"),
    CT_MUSHROOM(464, 232, "ct_mushroom", "Mushroom"),
    CT_OWL(466, 233, "ct_owl", "Owl"),
    CT_PANDA(468, 234, "ct_panda", "Panda"),
    CT_PIG(470, 235, "ct_pig", "Pig"),
    CT_POOL_8_BALL(472, 236, "ct_pool_8_ball", "Pool 8 Ball"),
    CT_RABBIT(474, 237, "ct_rabbit", "Rabbit"),
    CT_RAINBOW(476, 238, "ct_rainbow", "Rainbow"),
    CT_ROCKET(478, 239, "ct_rocket", "Rocket"),
    CT_ROOSTER(480, 240, "ct_rooster", "Rooster"),
    CT_SHIELD(482, 241, "ct_shield", "Shield"),
    CT_SHIP(484, 242, "ct_ship", "Ship"),
    CT_STACKOVERFLOW(486, 243, "ct_stackoverflow", "Stackoverflow"),
    CT_ZEBRA(488, 244, "ct_zebra", "Zebra"),
    CT_POSTMAN(490, 245, "ct_postman", "Postman");


    private final Integer iconIdIn16bit;
    private final Integer iconId;

    private final String iconName;
    private final String iconNameForHuman;

    NodeIcon(Integer iconIdIn16bit, Integer iconId, String iconName, String iconNameForHuman) {
        this.iconIdIn16bit = iconIdIn16bit;
        this.iconId = iconId;
        this.iconName = iconName;
        this.iconNameForHuman = iconNameForHuman;
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

    public String getIconNameForHuman() {
        return iconNameForHuman;
    }
}

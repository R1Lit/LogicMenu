package pb.r1lit.LogicMenu.util;

import org.bukkit.ChatColor;
import java.util.List;

public class CC {

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> color(List<String> list) {
        if (list == null) return List.of();
        return list.stream().map(CC::color).toList();
    }
}



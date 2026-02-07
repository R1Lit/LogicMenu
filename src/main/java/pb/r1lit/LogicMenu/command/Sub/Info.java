package pb.r1lit.LogicMenu.command.Sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;

import java.io.File;

public class Info implements Lm {
    private final LogicMenu plugin;

    public Info(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String permission() {
        return "logicmenu.meta";
    }

    @Override
    public String description() {
        return "Show plugin info";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int menus = plugin.getMenus() != null ? plugin.getMenus().getMenuCount() : 0;
        File expansions = new File(plugin.getDataFolder(), "expansions");
        File[] jars = expansions.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        int expCount = jars == null ? 0 : jars.length;

        sender.sendMessage(plugin.getLang().get("meta.header", "&eLogicMenu &f{version}")
                .replace("{version}", plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getLang().get("meta.menus", "&7Menus: &f{count}")
                .replace("{count}", String.valueOf(menus)));
        sender.sendMessage(plugin.getLang().get("meta.expansions", "&7Expansions: &f{count}")
                .replace("{count}", String.valueOf(expCount)));
        sender.sendMessage(plugin.getLang().get("meta.gui_folder", "&7GUI folder: &f{path}")
                .replace("{path}", new File(plugin.getDataFolder(), "gui").getPath()));
        sender.sendMessage(plugin.getLang().get("meta.expansions_folder", "&7Expansions folder: &f{path}")
                .replace("{path}", expansions.getPath()));
        sender.sendMessage(plugin.getLang().get("meta.placeholderapi", "&7PlaceholderAPI: &f{state}")
                .replace("{state}", Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "enabled" : "disabled"));
        return true;
    }
}

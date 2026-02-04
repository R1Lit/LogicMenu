package pb.r1lit.LogicMenu.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpansionsSubCommand implements LmSubCommand {
    private final LogicMenu plugin;

    public ExpansionsSubCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "expansions";
    }

    @Override
    public String permission() {
        return "logicmenu.expansions";
    }

    @Override
    public String description() {
        return "Manage expansions";
    }

    @Override
    public String usage() {
        return "[list|reload|path]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "list";
        File folder = new File(plugin.getDataFolder(), "expansions");

        switch (sub) {
            case "path":
                sender.sendMessage("§eExpansions folder: §f" + folder.getPath());
                return true;
            case "reload":
                plugin.loadExpansions();
                sender.sendMessage("§aExpansion scan complete.");
                return true;
            case "list":
            default:
                File[] jars = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (jars == null || jars.length == 0) {
                    sender.sendMessage("§eNo expansions found.");
                    return true;
                }
                sender.sendMessage("§eExpansions: §f" + jars.length);
                for (File jar : jars) {
                    Optional<String> name = PluginJarUtil.readPluginName(jar);
                    String pluginName = name.orElse(jar.getName());
                    boolean loaded = Bukkit.getPluginManager().getPlugin(pluginName) != null;
                    sender.sendMessage("§7- " + pluginName + " §8(" + jar.getName() + ") §f" + (loaded ? "§aLOADED" : "§cNOT LOADED"));
                }
                return true;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String opt : List.of("list", "reload", "path")) {
                if (opt.startsWith(prefix)) out.add(opt);
            }
            return out;
        }
        return List.of();
    }
}

package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.config.MenuConfigLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ValidateSubCommand implements LmSubCommand {
    private final LogicMenu plugin;
    private final MenuConfigLoader loader = new MenuConfigLoader();

    public ValidateSubCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "validate";
    }

    @Override
    public String permission() {
        return "logicmenu.validate";
    }

    @Override
    public String description() {
        return "Validate GUI YAML files";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        File folder = new File(plugin.getDataFolder(), "gui");
        if (!folder.exists() || !folder.isDirectory()) {
            sender.sendMessage("§cGUI folder not found: " + folder.getPath());
            return true;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            sender.sendMessage("§eNo GUI files found.");
            return true;
        }

        List<String> errors = new ArrayList<>();
        for (File file : files) {
            try {
                YamlConfiguration cfg = new YamlConfiguration();
                cfg.load(file);
                loader.loadSingle(cfg, file.getName().replace(".yml", ""));
            } catch (InvalidConfigurationException e) {
                errors.add(file.getName() + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add(file.getName() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        if (errors.isEmpty()) {
            sender.sendMessage("§aAll GUI files are valid.");
        } else {
            sender.sendMessage("§cFound errors: " + errors.size());
            for (String err : errors) {
                sender.sendMessage("§7- " + err);
            }
        }
        return true;
    }
}

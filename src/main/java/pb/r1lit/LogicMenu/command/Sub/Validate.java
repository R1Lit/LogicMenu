package pb.r1lit.LogicMenu.command.Sub;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.config.MenuConfigLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Validate implements Lm {
    private final LogicMenu plugin;
    private final MenuConfigLoader loader = new MenuConfigLoader();

    public Validate(LogicMenu plugin) {
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
            sender.sendMessage(plugin.getLang().get("validate.gui_folder_missing", "&cGUI folder not found: {path}")
                    .replace("{path}", folder.getPath()));
            return true;
        }

        List<File> files = new ArrayList<>();
        collectYamlFiles(folder, files);
        if (files.isEmpty()) {
            sender.sendMessage(plugin.getLang().get("validate.no_files", "&eNo GUI files found."));
            return true;
        }

        List<String> errors = new ArrayList<>();
        for (File file : files) {
            try {
                YamlConfiguration cfg = new YamlConfiguration();
                cfg.load(file);
                String id = toRelativePath(folder, file);
                if (id == null) id = file.getName();
                if (id.toLowerCase().endsWith(".yml")) {
                    id = id.substring(0, id.length() - 4);
                }
                id = id.replace('\\', '/');
                loader.loadSingle(cfg, id);
            } catch (InvalidConfigurationException e) {
                errors.add(file.getName() + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add(file.getName() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        if (errors.isEmpty()) {
            sender.sendMessage(plugin.getLang().get("validate.ok", "&aAll GUI files are valid."));
        } else {
            sender.sendMessage(plugin.getLang().get("validate.errors", "&cFound errors: {count}")
                    .replace("{count}", String.valueOf(errors.size())));
            for (String err : errors) {
                sender.sendMessage(plugin.getLang().get("validate.error_item", "&7- {error}")
                        .replace("{error}", err));
            }
        }
        return true;
    }

    private void collectYamlFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collectYamlFiles(child, out);
            } else if (child.getName().toLowerCase().endsWith(".yml")) {
                out.add(child);
            }
        }
    }

    private String toRelativePath(File root, File file) {
        try {
            return root.toPath().relativize(file.toPath()).toString();
        } catch (Exception e) {
            return null;
        }
    }
}

package pb.r1lit.LogicMenu;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.PluginDescriptionFile;
import pb.r1lit.LogicMenu.api.LogicMenuApi;
import pb.r1lit.LogicMenu.api.LogicMenuApiImpl;
import pb.r1lit.LogicMenu.gui.core.MenuEngine;

import java.io.File;
import java.util.Optional;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.JarFile;

public final class LogicMenu extends JavaPlugin {

    private static LogicMenu instance;
    private MenuEngine menus;
    private LogicMenuApi api;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        menus = new MenuEngine(this);
        api = new LogicMenuApiImpl(menus);
        menus.setApi(api);
        getServer().getServicesManager().register(LogicMenuApi.class, api, this, ServicePriority.Normal);

        loadExpansions();

        menus.reload();
        getServer().getPluginManager().registerEvents(menus, this);

        var router = new pb.r1lit.LogicMenu.command.CommandRouter(this);
        if (getCommand("lm") != null) {
            getCommand("lm").setExecutor(router);
            getCommand("lm").setTabCompleter(router);
        }
        if (getCommand("logicmenu") != null) {
            getCommand("logicmenu").setExecutor(router);
            getCommand("logicmenu").setTabCompleter(router);
        }

        getLogger().info("LogicMenu enabled.");
    }

    public static LogicMenu getInstance() {
        return instance;
    }

    public MenuEngine getMenus() {
        return menus;
    }

    public LogicMenuApi getApi() {
        return api;
    }

    public void loadExpansions() {
        File expansions = new File(getDataFolder(), "expansions");
        if (!expansions.exists()) {
            expansions.mkdirs();
        }

        File[] jars = expansions.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null || jars.length == 0) return;

        Arrays.sort(jars, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        PluginManager pm = getServer().getPluginManager();
        for (File jar : jars) {
            try {
                Optional<String> name = readPluginName(jar);
                if (name.isPresent() && pm.getPlugin(name.get()) != null) {
                    getLogger().warning("Expansion already loaded (duplicate): " + name.get());
                    continue;
                }
                Plugin loaded = pm.loadPlugin(jar);
                if (loaded == null) continue;
                if (!loaded.isEnabled()) {
                    pm.enablePlugin(loaded);
                }
                getLogger().info("Expansion loaded: " + loaded.getName());
            } catch (InvalidPluginException | UnknownDependencyException e) {
                getLogger().warning("Failed to load expansion " + jar.getName() + ": " + e.getMessage());
            } catch (Exception e) {
                getLogger().warning("Failed to load expansion " + jar.getName() + ": " + e.getMessage());
            }
        }
    }

    private Optional<String> readPluginName(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            var entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) return Optional.empty();
            try (var in = jarFile.getInputStream(entry)) {
                PluginDescriptionFile desc = new PluginDescriptionFile(in);
                return Optional.ofNullable(desc.getName());
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}

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
    private pb.r1lit.LogicMenu.lang.Lang lang;
    private pb.r1lit.LogicMenu.meta.MetaStore metaStore;
    private pb.r1lit.LogicMenu.anchor.AnchorStore anchorStore;
    private pb.r1lit.LogicMenu.gui.input.AnvilInputManager anvilInput;
    private pb.r1lit.LogicMenu.gui.service.MenuItemMarker menuItemMarker;
    private final java.util.Map<String, org.bukkit.command.Command> registeredCommands = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        boolean antiDupeEnabled = getConfig().getBoolean("anti-dupe.enabled", true);
        boolean antiDupeDebug = getConfig().getBoolean("anti-dupe.debug", false);
        menuItemMarker = new pb.r1lit.LogicMenu.gui.service.MenuItemMarker(this, antiDupeEnabled);
        menus = new MenuEngine(this, menuItemMarker);
        api = new LogicMenuApiImpl(menus);
        menus.setApi(api);
        getServer().getServicesManager().register(LogicMenuApi.class, api, this, ServicePriority.Normal);
        lang = new pb.r1lit.LogicMenu.lang.Lang(this);
        lang.reload();
        metaStore = new pb.r1lit.LogicMenu.meta.MetaStore(this);
        anchorStore = new pb.r1lit.LogicMenu.anchor.AnchorStore();
        anvilInput = new pb.r1lit.LogicMenu.gui.input.AnvilInputManager(this);

        registerInternalActions();

        loadExpansions(false);

        menus.reload();
        getLogger().info(lang.get("log.menus_loaded", "Menus loaded: {count}")
                .replace("{count}", String.valueOf(menus.getMenuCount())));
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(new pb.r1lit.LogicMenu.listener.DefCommandOpenListener(this, api), this);
        getServer().getPluginManager().registerEvents(anvilInput, this);
        if (antiDupeEnabled) {
            getServer().getPluginManager().registerEvents(
                    new pb.r1lit.LogicMenu.listener.MenuItemDupeListener(this, menuItemMarker, antiDupeDebug), this);
        }

        registerGuiCommands();

        var router = new pb.r1lit.LogicMenu.command.CommandRouter(this);
        if (getCommand("lm") != null) {
            getCommand("lm").setExecutor(router);
            getCommand("lm").setTabCompleter(router);
        }
        if (getCommand("logicmenu") != null) {
            getCommand("logicmenu").setExecutor(router);
            getCommand("logicmenu").setTabCompleter(router);
        }

        getLogger().info(lang.get("log.enabled", "LogicMenu enabled."));
    }

    @Override
    public void onDisable() {
        // Cancel all plugin tasks (refresh timer, meta auto-save, etc.)
        getServer().getScheduler().cancelTasks(this);

        // Close all open LogicMenu inventories
        getServer().getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof pb.r1lit.LogicMenu.gui.model.MenuHolder) {
                player.closeInventory();
            }
        });

        // Flush MetaStore to disk synchronously
        if (metaStore != null) {
            metaStore.shutdown();
        }

        // Unregister services
        getServer().getServicesManager().unregisterAll(this);

        instance = null;
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

    public pb.r1lit.LogicMenu.lang.Lang getLang() {
        return lang;
    }

    public pb.r1lit.LogicMenu.meta.MetaStore getMetaStore() {
        return metaStore;
    }

    public pb.r1lit.LogicMenu.anchor.AnchorStore getAnchorStore() {
        return anchorStore;
    }

    public int loadExpansions(boolean enableExisting) {
        File expansions = new File(getDataFolder(), "expansions");
        if (!expansions.exists()) {
            expansions.mkdirs();
        }

        File[] jars = expansions.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null || jars.length == 0) return 0;

        Arrays.sort(jars, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        PluginManager pm = getServer().getPluginManager();
        int loadedCount = 0;
        for (File jar : jars) {
            try {
                Optional<String> name = readPluginName(jar);
                if (name.isPresent()) {
                    Plugin existing = pm.getPlugin(name.get());
                    if (existing != null) {
                        if (enableExisting && !existing.isEnabled()) {
                            pm.enablePlugin(existing);
                            getLogger().info(lang.get("log.expansion_enabled", "Expansion enabled: {name}")
                                    .replace("{name}", existing.getName()));
                            loadedCount++;
                        } else {
                            getLogger().warning(lang.get("log.expansion_duplicate", "Expansion already loaded (duplicate): {name}")
                                    .replace("{name}", name.get()));
                        }
                        continue;
                    }
                }
                Plugin loaded = pm.loadPlugin(jar);
                if (loaded == null) continue;
                if (!loaded.isEnabled()) {
                    pm.enablePlugin(loaded);
                }
                getLogger().info(lang.get("log.expansion_loaded", "Expansion loaded: {name}")
                        .replace("{name}", loaded.getName()));
                loadedCount++;
            } catch (InvalidPluginException | UnknownDependencyException e) {
                getLogger().warning(lang.get("log.expansion_failed", "Failed to load expansion {file}: {error}")
                        .replace("{file}", jar.getName())
                        .replace("{error}", e.getMessage()));
            } catch (Exception e) {
                getLogger().warning(lang.get("log.expansion_failed", "Failed to load expansion {file}: {error}")
                        .replace("{file}", jar.getName())
                        .replace("{error}", e.getMessage()));
            }
        }
        return loadedCount;
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

    public int reloadExpansions() {
        clearApiRegistries();
        registerInternalActions();
        disableLoadedExpansions();
        return loadExpansions(true);
    }

    private void clearApiRegistries() {
        if (api instanceof pb.r1lit.LogicMenu.api.LogicMenuApiImpl impl) {
            impl.clearRegistries();
        }
    }

    private void disableLoadedExpansions() {
        File expansions = new File(getDataFolder(), "expansions");
        if (!expansions.exists()) return;
        File[] jars = expansions.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null || jars.length == 0) return;
        PluginManager pm = getServer().getPluginManager();
        for (File jar : jars) {
            Optional<String> name = readPluginName(jar);
            if (name.isEmpty()) continue;
            Plugin existing = pm.getPlugin(name.get());
            if (existing != null && existing.isEnabled()) {
                pm.disablePlugin(existing);
                getLogger().info(lang.get("log.expansion_disabled", "Expansion disabled: {name}")
                        .replace("{name}", existing.getName()));
            }
        }
    }

    private void registerMetaActions() {
        if (api == null || metaStore == null) return;
        api.registerAction("META_SET", (player, current, action, vars) -> {
            if (player == null) return;
            String raw = action.getValue() == null ? "" : action.getValue().trim();
            if (raw.isEmpty()) return;
            String key;
            String value;
            int eq = raw.indexOf('=');
            if (eq > 0) {
                key = raw.substring(0, eq).trim();
                value = raw.substring(eq + 1).trim();
            } else {
                String[] parts = raw.split("\\s+", 2);
                key = parts[0].trim();
                value = parts.length > 1 ? parts[1].trim() : "";
            }
            if (!key.isEmpty()) {
                metaStore.set(player, key, value);
            }
        });

        api.registerAction("META_REMOVE", (player, current, action, vars) -> {
            if (player == null) return;
            String key = action.getValue() == null ? "" : action.getValue().trim();
            if (!key.isEmpty()) metaStore.remove(player, key);
        });

        api.registerAction("META_TOGGLE", (player, current, action, vars) -> {
            if (player == null) return;
            String key = action.getValue() == null ? "" : action.getValue().trim();
            if (!key.isEmpty()) metaStore.toggle(player, key);
        });

        api.registerAction("META_INCREMENT", (player, current, action, vars) -> {
            if (player == null) return;
            String raw = action.getValue() == null ? "" : action.getValue().trim();
            if (raw.isEmpty()) return;
            String[] parts = raw.split("\\s+");
            String key = parts[0].trim();
            int delta = 1;
            if (parts.length > 1) {
                try {
                    delta = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (!key.isEmpty()) metaStore.increment(player, key, delta);
        });
    }

    private void registerAnchorActions() {
        if (api == null || anchorStore == null) return;
        api.registerAction("ANCHOR", (player, current, action, vars) -> {
            if (player == null) return;
            String raw = action.getValue() == null ? "" : action.getValue().trim();
            if (raw.isEmpty()) return;
            String resolved = getMenus().getResolver().resolve(raw, player, vars);
            String key;
            String value;
            int eq = resolved.indexOf('=');
            if (eq > 0) {
                key = resolved.substring(0, eq).trim();
                value = resolved.substring(eq + 1).trim();
            } else {
                String[] parts = resolved.split("\\s+", 2);
                key = parts[0].trim();
                value = parts.length > 1 ? parts[1].trim() : "";
            }
            if (!key.isEmpty()) {
                anchorStore.set(player, key, value);
            }
        });

        api.registerAction("ANCHOR_REMOVE", (player, current, action, vars) -> {
            if (player == null) return;
            String key = action.getValue() == null ? "" : action.getValue().trim();
            if (!key.isEmpty()) anchorStore.remove(player, key);
        });

        api.registerAction("ANCHOR_CLEAR", (player, current, action, vars) -> {
            if (player == null) return;
            anchorStore.clear(player);
        });
    }

    private void registerAnvilActions() {
        if (api == null || anvilInput == null || menus == null) return;
        api.registerAction("ANVIL_INPUT", (player, current, action, vars) -> {
            if (player == null) return;
            String title = action.getParams().getOrDefault("_title", "&8Input");
            String prompt = action.getParams().getOrDefault("_prompt", "");
            String followup = action.getValue() == null ? "" : action.getValue().trim();

            anvilInput.open(player, title, prompt, input -> {
                java.util.Map<String, String> newVars = new java.util.HashMap<>(vars == null ? java.util.Map.of() : vars);
                newVars.put("input", input);

                String anchorKey = action.getParams().getOrDefault("_anchor", "");
                if (anchorStore != null && !anchorKey.isBlank()) {
                    anchorStore.set(player, anchorKey, input);
                }
                String metaKey = action.getParams().getOrDefault("_meta", "");
                if (metaStore != null && !metaKey.isBlank()) {
                    metaStore.set(player, metaKey, input);
                }

                  if (!followup.isBlank()) {
                      String resolved = menus.getResolver().resolve(followup, player, newVars);
                      pb.r1lit.LogicMenu.gui.model.MenuAction next = pb.r1lit.LogicMenu.gui.model.MenuAction.parse(resolved);
                      menus.executeAction(player, current, next, newVars);
                  }
              });
          });
      }

    private void registerInternalActions() {
        registerMetaActions();
        registerAnchorActions();
        registerAnvilActions();
    }

    public void registerGuiCommands() {
        var cfg = getConfig();
        var section = cfg.getConfigurationSection("gui-commands");
        if (section == null) return;

        var map = getCommandMap();
        if (map == null) {
            getLogger().warning(lang.get("log.commandmap_missing", "Cannot register gui-commands: CommandMap not available."));
            return;
        }

        for (String key : section.getKeys(false)) {
            var cmdSec = section.getConfigurationSection(key);
            if (cmdSec == null) continue;

            String typeRaw = cmdSec.getString("type", "DEF");
            String type = typeRaw == null ? "DEF" : typeRaw.toUpperCase(java.util.Locale.ROOT);
            if (!"DEF".equals(type)) {
                getLogger().info(lang.get("log.gui_commands_skip", "gui-commands: skip {key} type={type} (handled by expansion)")
                        .replace("{key}", key)
                        .replace("{type}", type));
                continue;
            }

            java.util.List<String> commands = new java.util.ArrayList<>();
            Object rawCommands = cmdSec.get("commands");
            if (rawCommands instanceof java.util.List<?> list) {
                for (Object c : list) {
                    if (c == null) continue;
                    String value = String.valueOf(c).trim();
                    if (!value.isBlank()) commands.add(value.toLowerCase(java.util.Locale.ROOT));
                }
            } else {
                String commandsRaw = cmdSec.getString("commands", "");
                if (commandsRaw != null && !commandsRaw.isBlank()) {
                    for (String part : commandsRaw.split(",")) {
                        String value = part.trim();
                        if (!value.isBlank()) commands.add(value.toLowerCase(java.util.Locale.ROOT));
                    }
                }
                String command = cmdSec.getString("command", key);
                if (command != null && !command.isBlank()) commands.add(command.toLowerCase(java.util.Locale.ROOT));
            }
            if (commands.isEmpty()) continue;

            String menuId = cmdSec.getString("menu", "");
            if (menuId == null || menuId.isBlank()) continue;

            boolean ignoreArgs = cmdSec.getBoolean("ignoreargs", false);
            String permission = cmdSec.getString("permission", "");
            if (ignoreArgs) {
                for (String command : commands) {
                    registerDefCommand(map, command, permission, menuId);
                }
            }
        }
    }

    private void registerDefCommand(org.bukkit.command.CommandMap map, String commandName, String permission, String menuId) {
        if (commandName == null || commandName.isBlank()) return;

        org.bukkit.command.Command existing = map.getCommand(commandName);
        if (existing instanceof org.bukkit.command.PluginCommand pluginCommand) {
            if (pluginCommand.getPlugin() == this) {
                pluginCommand.setExecutor((sender, command, label, args) -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) return false;
                    if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
                        player.sendMessage(lang.get("command.no_permission", "&cNo permission."));
                        return true;
                    }
                    api.openMenu(player, menuId, 0);
                    return true;
                });
                return;
            }
        }

        org.bukkit.command.Command dynamic = registeredCommands.get(commandName);
        if (dynamic == null) {
            dynamic = new org.bukkit.command.Command(commandName) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    if (!(sender instanceof org.bukkit.entity.Player player)) return false;
                    if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
                        player.sendMessage(lang.get("command.no_permission", "&cNo permission."));
                        return true;
                    }
                    api.openMenu(player, menuId, 0);
                    return true;
                }
            };
            if (permission != null && !permission.isBlank()) {
                dynamic.setPermission(permission);
            }
            map.register(getDescription().getName(), dynamic);
            registeredCommands.put(commandName, dynamic);
        }
    }

    private org.bukkit.command.CommandMap getCommandMap() {
        try {
            var method = getServer().getClass().getMethod("getCommandMap");
            Object result = method.invoke(getServer());
            if (result instanceof org.bukkit.command.CommandMap map) return map;
        } catch (Exception ignored) {
        }
        return null;
    }

}

package pb.r1lit.LogicMenu.gui.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class PermissionBridge {
    private static final PermissionBridge INSTANCE = new PermissionBridge();
    private net.milkbowl.vault.permission.Permission permission;

    private PermissionBridge() {
        setup();
    }

    public static PermissionBridge get() {
        return INSTANCE;
    }

    private void setup() {
        try {
            RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> rsp =
                    Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (rsp != null) {
                permission = rsp.getProvider();
            }
        } catch (Throwable ignored) {
        }
    }

    public void add(Player player, String perm) {
        if (permission == null || perm == null || perm.isBlank()) return;
        permission.playerAdd(player, perm);
    }

    public void remove(Player player, String perm) {
        if (permission == null || perm == null || perm.isBlank()) return;
        permission.playerRemove(player, perm);
    }
}


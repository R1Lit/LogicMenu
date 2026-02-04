package pb.r1lit.LogicMenu.gui.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyBridge {
    private static final EconomyBridge INSTANCE = new EconomyBridge();
    private net.milkbowl.vault.economy.Economy economy;

    private EconomyBridge() {
        setup();
    }

    public static EconomyBridge get() {
        return INSTANCE;
    }

    private void setup() {
        try {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean has(Player player, double amount) {
        if (economy == null) setup();
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        if (economy == null) setup();
        if (economy == null) return;
        economy.withdrawPlayer(player, amount);
    }

    public void deposit(Player player, double amount) {
        if (economy == null) setup();
        if (economy == null) return;
        economy.depositPlayer(player, amount);
    }

    public Double getBalance(Player player) {
        if (economy == null) setup();
        if (economy == null) return null;
        return economy.getBalance(player);
    }
}


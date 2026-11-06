package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class Check implements Listener {
    protected final Sentinel plugin;
    private final String name;
    private final Map<UUID, Object> states = new HashMap<>();

    protected Check(Sentinel plugin, String name) { this.plugin = plugin; this.name = name; }

    public String name() { return name; }
    public boolean enabled() { return plugin.getConfig().getBoolean("checks." + name + ".enabled", true); }

    /** Enabled, player not exempt, server not lagging. */
    public boolean usable(Player p) {
        if (!enabled() || plugin.lagging() || p.hasPermission("sentinel.bypass")) return false;
        GameMode gm = p.getGameMode();
        return gm != GameMode.CREATIVE && gm != GameMode.SPECTATOR;
    }

    protected double cfg(String k, double d) { return plugin.getConfig().getDouble("checks." + name + "." + k, d); }
    protected int cfgInt(String k, int d) { return plugin.getConfig().getInt("checks." + name + "." + k, d); }
    protected boolean cfgBool(String k, boolean d) { return plugin.getConfig().getBoolean("checks." + name + "." + k, d); }

    @SuppressWarnings("unchecked")
    protected <T> T data(Player p, Supplier<T> init) { return (T) states.computeIfAbsent(p.getUniqueId(), k -> init.get()); }
    public void remove(UUID id) { states.remove(id); }

    protected void flag(Player p, String info, double vl) { plugin.violations().flag(p, name, info, vl); }
    protected static int now() { return Bukkit.getCurrentTick(); }

    /** One call per client movement packet (position or rotation). */
    public void onMove(Player p, Movement m, PlayerMoveEvent e) {}
    /** One call per server tick. */
    public void onTick(Player p, Movement m) {}
}

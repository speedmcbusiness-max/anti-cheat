package dev.sentinel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public final class Violations {
    private final Sentinel plugin;
    private final Map<UUID, Map<String, Double>> vl = new HashMap<>();
    private final Map<String, Long> lastAlert = new HashMap<>();
    private final Set<UUID> muted = new HashSet<>();

    public Violations(Sentinel plugin) { this.plugin = plugin; }

    public void flag(Player p, String check, String info, double add) {
        Map<String, Double> m = vl.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        double v = m.merge(check, add, Double::sum);
        FileConfiguration c = plugin.getConfig();
        long now = System.currentTimeMillis();
        String key = p.getUniqueId() + check;
        if (now - lastAlert.getOrDefault(key, 0L) >= 1000) {
            lastAlert.put(key, now);
            String vlText = String.format(Locale.ROOT, "%.1f", v);
            String msg = c.getString("alerts.format", "&c%player% failed %check% (%info%) VL %vl%")
                    .replace("%player%", p.getName()).replace("%check%", check)
                    .replace("%info%", info).replace("%vl%", vlText);
            Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
            for (Player s : Bukkit.getOnlinePlayers())
                if (s.hasPermission("sentinel.alerts") && !muted.contains(s.getUniqueId())) s.sendMessage(comp);
            plugin.getLogger().info(p.getName() + " failed " + check + " (" + info + ") VL " + vlText);
        }
        double limit = c.getDouble("checks." + check + ".punish-vl", c.getDouble("punishment.default-vl", 20));
        if (c.getBoolean("punishment.enabled", false) && v >= limit) {
            m.put(check, 0.0);
            for (String cmd : c.getStringList("punishment.commands"))
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd.replace("%player%", p.getName()).replace("%check%", check));
        }
    }

    public Map<String, Double> of(UUID id) { return vl.getOrDefault(id, Map.of()); }
    public void reset(UUID id) { vl.remove(id); }
    public void remove(UUID id) { vl.remove(id); muted.remove(id); }
    public boolean toggleAlerts(UUID id) { return muted.remove(id) ? true : !muted.add(id); }

    /** Called once a minute: violation levels fade so honest players are never punished over time. */
    public void decay() {
        vl.values().forEach(m -> m.replaceAll((k, v) -> v - 1.0));
        vl.values().forEach(m -> m.values().removeIf(v -> v <= 0));
        vl.values().removeIf(Map::isEmpty);
    }
}

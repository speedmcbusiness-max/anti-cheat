package dev.sentinel;

import dev.sentinel.checks.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Sentinel extends JavaPlugin {
    private Violations violations;
    private MovementTracker movement;
    private final List<Check> checks = new ArrayList<>();
    private boolean lagging;

    @Override public void onEnable() {
        saveDefaultConfig();
        violations = new Violations(this);
        movement = new MovementTracker(this);
        checks.addAll(List.of(
                new ReachCheck(this), new HitBoxCheck(this), new AimAssistCheck(this), new ClickAimAssistCheck(this),
                new AutoCrystalCheck(this), new AutoTotemCheck(this), new AutoAnchorCheck(this), new InvMoveCheck(this),
                new FlightCheck(this), new NoFallCheck(this), new VelocityCheck(this), new JumpResetCheck(this),
                new NoJumpDelayCheck(this), new NoSlowDownCheck(this), new ShieldBreakerCheck(this),
                new AutoStunSlamCheck(this), new XrayCheck(this), new EspGuard(this)));
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(movement, this);
        for (Check c : checks) pm.registerEvents(c, this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, violations::decay, 1200L, 1200L);
        getLogger().info("Sentinel enabled with " + checks.size() + " check modules.");
    }

    private void tick() {
        lagging = Bukkit.getTPS()[0] < getConfig().getDouble("general.min-tps", 18.0);
        for (Player p : getServer().getOnlinePlayers()) {
            Movement m = movement.tick(p);
            for (Check c : checks) if (c.usable(p)) c.onTick(p, m);
        }
    }

    public void forget(UUID id) {
        movement.remove(id);
        violations.remove(id);
        for (Check c : checks) c.remove(id);
    }

    public boolean lagging() { return lagging; }
    public Violations violations() { return violations; }
    public MovementTracker movement() { return movement; }
    public List<Check> checks() { return checks; }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("sentinel.admin")) { s.sendMessage("No permission."); return true; }
        if (a.length == 0) { s.sendMessage("/sentinel <reload|alerts|vl <player>|reset <player>>"); return true; }
        switch (a[0].toLowerCase()) {
            case "reload" -> { reloadConfig(); s.sendMessage("Sentinel config reloaded."); }
            case "alerts" -> {
                if (s instanceof Player p) s.sendMessage("Alerts " + (violations.toggleAlerts(p.getUniqueId()) ? "enabled" : "disabled"));
            }
            case "vl" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null) { s.sendMessage("Player not found."); break; }
                s.sendMessage(t.getName() + ": " + violations.of(t.getUniqueId()));
            }
            case "reset" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null) { s.sendMessage("Player not found."); break; }
                violations.reset(t.getUniqueId()); s.sendMessage("Reset " + t.getName());
            }
            default -> s.sendMessage("Unknown subcommand.");
        }
        return true;
    }
}

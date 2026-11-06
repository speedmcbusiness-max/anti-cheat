package dev.sentinel;

import dev.sentinel.checks.Check;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MovementTracker implements Listener {
    private final Sentinel plugin;
    private final Map<UUID, Movement> data = new HashMap<>();

    public MovementTracker(Sentinel plugin) { this.plugin = plugin; }

    public Movement get(Player p) { return data.computeIfAbsent(p.getUniqueId(), k -> new Movement()); }
    public void remove(UUID id) { data.remove(id); }

    /** Called once per server tick for every player. */
    public Movement tick(Player p) {
        Movement m = get(p);
        m.idleTicks++;
        m.usingTicks = p.isHandRaised() ? m.usingTicks + 1 : 0;
        m.sneakTicks = p.isSneaking() ? m.sneakTicks + 1 : 0;
        return m;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        Movement m = get(p);
        double ndx = to.getX() - from.getX(), ndy = to.getY() - from.getY(), ndz = to.getZ() - from.getZ();
        m.moved = ndx != 0 || ndy != 0 || ndz != 0;
        if (m.moved) {
            m.idleTicks = 0;
            m.moveCount++;
            m.lastDy = m.dy;
            m.dx = ndx; m.dy = ndy; m.dz = ndz;
            m.horiz = Math.hypot(ndx, ndz);
            // p.isOnGround() is the flag from the PREVIOUS packet; m.ground is the server's verdict for that same position.
            m.clientGround = p.isOnGround();
            m.lastGround = m.ground;
            m.ground = Util.ground(to.getWorld(), to.getX(), to.getY(), to.getZ(), m.clientGround);
            m.special = Util.special(p, to);
            if (m.special) m.lastSpecialTick = Bukkit.getCurrentTick();
            if (m.ground) { m.airTicks = 0; m.groundTicks++; m.lastGroundLoc = to.clone(); }
            else { m.airTicks++; m.groundTicks = 0; }
            m.jumped = m.lastGround && !m.ground && m.dy >= 0.4;
        }
        for (Check c : plugin.checks()) if (c.usable(p)) c.onMove(p, m, e);

        if (m.setback) {
            m.setback = false;
            Location safe = (m.lastGroundLoc != null ? m.lastGroundLoc : from).clone();
            safe.setYaw(to.getYaw()); safe.setPitch(to.getPitch());
            e.setTo(safe);
            m.dy = 0; m.lastDy = 0; m.ground = true; m.airTicks = 0;
            m.lastTeleportTick = Bukkit.getCurrentTick() - 12; // short grace after a setback
        }
    }

    private void reset(Player p, Location at) {
        Movement m = get(p);
        m.lastTeleportTick = Bukkit.getCurrentTick();
        m.dy = 0; m.lastDy = 0; m.airTicks = 0; m.ground = true;
        if (at != null) m.lastGroundLoc = at.clone();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent e) { get(e.getPlayer()).lastVelocityTick = Bukkit.getCurrentTick(); }
    @EventHandler(priority = EventPriority.MONITOR) public void onTp(PlayerTeleportEvent e) { reset(e.getPlayer(), e.getTo()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) { reset(e.getPlayer(), e.getRespawnLocation()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) { reset(e.getPlayer(), e.getPlayer().getLocation()); }
    @EventHandler public void onGm(PlayerGameModeChangeEvent e) { reset(e.getPlayer(), null); }
    @EventHandler public void onJoin(PlayerJoinEvent e) { reset(e.getPlayer(), e.getPlayer().getLocation()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { plugin.forget(e.getPlayer().getUniqueId()); }
}

package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** HitBoxes: the attacker's crosshair (current or previous rotation) must pass through the expanded target box. */
public final class HitBoxCheck extends Check {
    private static final class S { Vector prev; double buf; }
    public HitBoxCheck(Sentinel p) { super(p, "hitbox"); }

    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        data(p, S::new).prev = e.getFrom().getDirection();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !usable(p)) return;
        if (e.getEntity().getWorld() != p.getWorld()) return;
        S s = data(p, S::new);
        double grow = cfg("expand", 0.15) + Math.min(p.getPing(), 300) / 1000.0 * 0.3;
        BoundingBox box = e.getEntity().getBoundingBox().expand(grow);
        Vector eye = p.getEyeLocation().toVector();
        boolean hit = box.rayTrace(eye, p.getEyeLocation().getDirection(), 6.0) != null
                || (s.prev != null && box.rayTrace(eye, s.prev, 6.0) != null);
        if (hit) { s.buf = Math.max(0, s.buf - 0.5); return; }
        if (++s.buf >= cfgInt("buffer", 3)) flag(p, "attack outside hitbox (+" + String.format("%.2f", grow) + ")", 1.5);
        if (cfgBool("cancel-hits", true)) e.setCancelled(true);
    }
}

package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** Reach: distance from the eye to the closest point of the target's hitbox. */
public final class ReachCheck extends Check {
    private static final class S { double buf; }
    public ReachCheck(Sentinel p) { super(p, "reach"); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!usable(p) || p.getInventory().getItemInMainHand().getType().name().endsWith("_SPEAR")) return; // spears have longer reach
        Entity t = e.getEntity();
        if (t.getWorld() != p.getWorld()) return;
        AttributeInstance ai = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double range = ai == null ? 3.0 : ai.getValue();
        BoundingBox b = t.getBoundingBox();
        Vector eye = p.getEyeLocation().toVector();
        Vector closest = new Vector(Util.clamp(eye.getX(), b.getMinX(), b.getMaxX()),
                Util.clamp(eye.getY(), b.getMinY(), b.getMaxY()), Util.clamp(eye.getZ(), b.getMinZ(), b.getMaxZ()));
        double dist = eye.distance(closest);
        double allowed = range + cfg("leniency", 0.3) + Math.min(p.getPing(), 300) / 50.0 * cfg("ping-factor", 0.1);
        S s = data(p, S::new);
        if (dist > allowed) {
            if (++s.buf >= cfgInt("buffer", 2)) flag(p, "dist=" + Util.fmt(dist) + " max=" + Util.fmt(allowed), 1 + (dist - allowed) * 4);
            if (cfgBool("cancel-hits", true)) e.setCancelled(true);
        } else s.buf = Math.max(0, s.buf - 0.25);
    }
}

package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

/** ClickAimAssist: at the moment of each hit on a MOVING target, humans wobble around the centre; aim-lock does not. */
public final class ClickAimAssistCheck extends Check {
    private static final class S { double[] a = new double[64]; int n; UUID last; Vector lastPos; }
    public ClickAimAssistCheck(Sentinel p) { super(p, "clickaimassist"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !usable(p)) return;
        Entity t = e.getEntity();
        S s = data(p, S::new);
        Vector pos = t.getLocation().toVector();
        boolean sameTarget = t.getUniqueId().equals(s.last);
        boolean moving = sameTarget && s.lastPos != null && s.lastPos.distance(pos) > 0.2;
        s.last = t.getUniqueId(); s.lastPos = pos;
        if (!moving) return;
        Vector to = t.getBoundingBox().getCenter().subtract(p.getEyeLocation().toVector());
        double angle = Math.toDegrees(to.angle(p.getEyeLocation().getDirection()));
        int need = Math.min(cfgInt("samples", 20), s.a.length);
        s.a[s.n++] = angle;
        if (s.n < need) return;
        double mean = 0; for (int i = 0; i < s.n; i++) mean += s.a[i]; mean /= s.n;
        double var = 0; for (int i = 0; i < s.n; i++) var += (s.a[i] - mean) * (s.a[i] - mean);
        double sd = Math.sqrt(var / s.n);
        s.n = 0;
        if (mean < cfg("max-mean", 2.0) && sd < cfg("max-stddev", 0.6))
            flag(p, String.format("aim lock mean=%.2f sd=%.2f", mean, sd), 3);
    }
}

package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

/** Velocity (anti-knockback): after a server-sent velocity the client must actually move that way. */
public final class VelocityCheck extends Check {
    private static final class P { int expire; double vy, h, maxProj = -9, maxDy = -9; Vector dir; boolean ceiling, wall; }
    private static final class S { P pend; double buf; }
    public VelocityCheck(Sentinel p) { super(p, "velocity"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent e) {
        Player p = e.getPlayer();
        if (!usable(p) || p.isInsideVehicle() || plugin.movement().get(p).special) return;
        Vector v = e.getVelocity();
        double h = Math.hypot(v.getX(), v.getZ()), vy = v.getY();
        if (h < 0.45 && vy < 0.3) return;
        P pe = new P();
        pe.h = h; pe.vy = vy;
        pe.dir = h > 0.01 ? new Vector(v.getX() / h, 0, v.getZ() / h) : new Vector();
        Location l = p.getLocation();
        pe.ceiling = Util.ceilingAbove(l);
        pe.wall = h > 0.01 && Util.wallAhead(l, pe.dir);
        pe.expire = now() + 4 + Math.min(p.getPing(), 400) / 50;
        data(p, S::new).pend = pe;
    }

    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        S s = data(p, S::new);
        if (s.pend == null || !m.moved) return;
        if (m.special) { s.pend = null; return; }
        s.pend.maxProj = Math.max(s.pend.maxProj, m.dx * s.pend.dir.getX() + m.dz * s.pend.dir.getZ());
        s.pend.maxDy = Math.max(s.pend.maxDy, m.dy);
    }

    @Override public void onTick(Player p, Movement m) {
        S s = data(p, S::new);
        P pe = s.pend;
        if (pe == null || now() < pe.expire) return;
        s.pend = null;
        if (pe.maxDy == -9) return; // no movement packets in the window (lag) - inconclusive
        boolean vert = pe.vy >= 0.3 && !pe.ceiling && pe.maxDy < pe.vy * 0.5;
        boolean hor = pe.h >= 0.45 && !pe.wall && pe.maxProj < pe.h * 0.5;
        if (vert || hor) {
            if (++s.buf >= cfgInt("buffer", 3)) {
                flag(p, (vert ? "vertical" : "horizontal") + " kb ignored (vy=" + Util.fmt(pe.vy) + " h=" + Util.fmt(pe.h) + ")", 2);
                s.buf = 1;
            }
        } else s.buf = Math.max(0, s.buf - 0.5);
    }
}

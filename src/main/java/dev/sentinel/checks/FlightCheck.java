package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/** Flight / Fly: vanilla vertical physics are exact (dy' = (dy - 0.08) * 0.98). Also catches hovering. */
public final class FlightCheck extends Check {
    private static final class S { double buf; }
    public FlightCheck(Sentinel p) { super(p, "flight"); }

    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        S s = data(p, S::new);
        if (!m.moved || m.special || m.grace(20) || m.ground || m.airTicks < 3) { s.buf = Math.max(0, s.buf - 0.25); return; }
        double expected = (m.lastDy - 0.08) * 0.98;
        double diff = m.dy - expected;
        if (diff > cfg("max-excess", 0.07)) {
            if (++s.buf >= cfgInt("buffer", 3)) {
                flag(p, "gravity dy=" + Util.fmt(m.dy) + " expected=" + Util.fmt(expected), 1 + diff * 4);
                m.setback = true; s.buf = 1;
            }
        } else s.buf = Math.max(0, s.buf - 0.5);
    }

    /** Hovering produces no movement packets, so it is caught on the server tick instead. */
    @Override public void onTick(Player p, Movement m) {
        if (m.idleTicks != cfgInt("hover-ticks", 20) || m.grace(30) || p.getPing() > 400) return;
        Location l = p.getLocation();
        if (Util.special(p, l) || Util.ground(p.getWorld(), l.getX(), l.getY(), l.getZ(), true)) return;
        flag(p, "stationary in mid-air", 3);
        if (cfgBool("hover-setback", true) && m.lastGroundLoc != null) p.teleport(m.lastGroundLoc);
    }
}

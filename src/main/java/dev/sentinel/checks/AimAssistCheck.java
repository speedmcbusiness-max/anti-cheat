package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * AimAssist: real mouse input produces pitch deltas that are all multiples of one sensitivity step.
 * Software-generated rotations break that pattern. EXPERIMENTAL: tune min-step/buffer for your server.
 */
public final class AimAssistCheck extends Check {
    private static final double EXP = 16777216.0, TOL = 0.001 * EXP;
    private static final class S { final long[] d = new long[20]; int n, buf, lastAttack = -1000; }
    public AimAssistCheck(Sentinel p) { super(p, "aimassist"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && usable(p)) data(p, S::new).lastAttack = now();
    }

    private static long gcd(long a, long b) {
        while (b > TOL) { long r = a % b; if (b - r < TOL) r = 0; a = b; b = r; }
        return a;
    }

    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        S s = data(p, S::new);
        Location f = e.getFrom(), t = e.getTo();
        if (t == null || now() - s.lastAttack > 60 || m.grace(10)) return;
        double dp = Math.abs(t.getPitch() - f.getPitch());
        if (dp < 1e-4 || dp > 25 || Math.abs(t.getPitch()) > 89) return;
        s.d[s.n++] = (long) (dp * EXP);
        if (s.n < s.d.length) return;
        long g = s.d[0];
        for (int i = 1; i < s.d.length; i++) g = gcd(Math.max(g, s.d[i]), Math.min(g, s.d[i]));
        s.n = 0;
        double step = g / EXP;
        if (step < cfg("min-step", 0.0045)) {
            if (++s.buf >= cfgInt("buffer", 4)) { flag(p, "rotation gcd=" + Util.fmt(step), 2); s.buf = 0; }
        } else s.buf = Math.max(0, s.buf - 1);
    }
}

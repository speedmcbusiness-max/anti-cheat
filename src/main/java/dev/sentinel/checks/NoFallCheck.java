package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** AntiFallDamage / NoFall: (A) client claims onGround while airborne, (B) long fall onto solid ground with no damage. */
public final class NoFallCheck extends Check {
    private static final class S { double fall, buf, landFall; int landTick = -1000, lastFallDmg = -1000; boolean pending; }
    public NoFallCheck(Sentinel p) { super(p, "antifalldamage"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && e.getCause() == EntityDamageEvent.DamageCause.FALL) data(p, S::new).lastFallDmg = now();
    }

    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        S s = data(p, S::new);
        if (!m.moved) return;
        if (m.special || m.grace(10)) { s.fall = 0; s.buf = 0; s.pending = false; return; }

        if (m.clientGround && !m.lastGround) {
            if (++s.buf >= cfgInt("spoof-buffer", 3)) { flag(p, "spoofed onGround", 2); m.setback = true; s.buf = 0; }
        } else s.buf = Math.max(0, s.buf - 0.5);

        if (!m.ground) { s.fall = Math.max(0, s.fall - m.dy); return; }
        Location to = e.getTo();
        if (!m.lastGround && s.fall >= cfg("min-fall", 5.0) && to != null
                && Util.ground(to.getWorld(), to.getX(), to.getY(), to.getZ(), false)
                && !Util.BOUNCY.contains(to.getBlock().getRelative(0, -1, 0).getType())) {
            s.pending = true; s.landTick = now(); s.landFall = s.fall;
        }
        s.fall = 0;
    }

    @Override public void onTick(Player p, Movement m) {
        S s = data(p, S::new);
        if (!s.pending || now() - s.landTick < 6) return;
        s.pending = false;
        PotionEffect res = p.getPotionEffect(PotionEffectType.RESISTANCE), jump = p.getPotionEffect(PotionEffectType.JUMP_BOOST);
        if ((res != null && res.getAmplifier() >= 4) || p.hasPotionEffect(PotionEffectType.SLOW_FALLING)
                || p.isInvulnerable() || p.isDead() || p.getNoDamageTicks() > 10) return;
        double safe = 3.0 + (jump == null ? 0 : jump.getAmplifier() + 1);
        if (s.landFall - safe < 1.5) return;
        if (s.lastFallDmg < s.landTick - 2) flag(p, "fell " + Util.fmt(s.landFall) + " blocks, no damage", 3);
    }
}

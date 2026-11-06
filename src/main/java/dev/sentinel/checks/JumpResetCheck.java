package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** JumpReset: a victim whose jump key goes from released to pressed within 2 ticks of being hit, nearly every time. */
public final class JumpResetCheck extends Check {
    private static final class S { final Util.Window w = new Util.Window(10); }
    public JumpResetCheck(Sentinel p) { super(p, "jumpreset"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player v) || !(e.getDamager() instanceof Player) || !usable(v)) return;
        if (!v.isOnGround() || v.getCurrentInput().isJump()) return;
        S s = data(v, S::new);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!v.isOnline()) return;
            s.w.add(v.getCurrentInput().isJump());
            if (s.w.size() >= 8 && s.w.count() >= 7) { flag(v, "jumped within 2t of " + s.w.count() + "/" + s.w.size() + " hits", 2); s.w.clear(); }
        }, 2L);
    }
}

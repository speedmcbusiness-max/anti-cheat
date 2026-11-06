package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/** AutoStunSlam: axe hit on a blocking target immediately followed (<= 2 ticks) by a mace hit on the same target. */
public final class AutoStunSlamCheck extends Check {
    private static final class S { int axeTick = -1000; UUID target; final Util.Window fast = new Util.Window(5); }
    public AutoStunSlamCheck(Sentinel p) { super(p, "autostunslam"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof Player t) || !usable(p)) return;
        Material held = p.getInventory().getItemInMainHand().getType();
        S s = data(p, S::new);
        if (Tag.ITEMS_AXES.isTagged(held) && t.isBlocking()) { s.axeTick = now(); s.target = t.getUniqueId(); return; }
        if (held != Material.MACE || !t.getUniqueId().equals(s.target)) return;
        int delta = now() - s.axeTick;
        if (delta > 20) return;
        s.target = null;
        s.fast.add(delta <= cfgInt("max-ticks", 2));
        if (s.fast.size() >= 4 && s.fast.count() >= 3) { flag(p, "axe->mace combo in " + delta + "t", 2.5); s.fast.clear(); }
    }
}

package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/** ShieldBreaker: switching to an axe and hitting a blocking player within <= 1 tick, repeatedly. */
public final class ShieldBreakerCheck extends Check {
    private static final class S { int swapTick = -1000; final Util.Window fast = new Util.Window(5); }
    public ShieldBreakerCheck(Sentinel p) { super(p, "shieldbreaker"); }

    @EventHandler(ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (!usable(p)) return;
        ItemStack it = p.getInventory().getItem(e.getNewSlot());
        if (it != null && Tag.ITEMS_AXES.isTagged(it.getType())) data(p, S::new).swapTick = now();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof Player t) || !usable(p) || !t.isBlocking()) return;
        if (!Tag.ITEMS_AXES.isTagged(p.getInventory().getItemInMainHand().getType())) return;
        S s = data(p, S::new);
        int delta = now() - s.swapTick;
        if (delta > 20) return;
        s.swapTick = -1000;
        s.fast.add(delta <= cfgInt("max-ticks", 1));
        if (s.fast.size() >= 4 && s.fast.count() >= 3) { flag(p, "axe swap + shield hit in " + delta + "t", 2); s.fast.clear(); }
    }
}

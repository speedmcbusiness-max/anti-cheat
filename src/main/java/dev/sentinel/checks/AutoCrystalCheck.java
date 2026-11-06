package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** AutoCrystal: place->break loops completed in <= 1 tick, repeatedly (humans need several ticks). */
public final class AutoCrystalCheck extends Check {
    private static final class S { int placeTick = -1000; Location at; final Util.Window fast = new Util.Window(10); }
    public AutoCrystalCheck(Sentinel p) { super(p, "autocrystal"); }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null
                || e.getItem() == null || e.getItem().getType() != Material.END_CRYSTAL || !usable(p)) return;
        S s = data(p, S::new);
        s.placeTick = now();
        s.at = e.getClickedBlock().getLocation().add(0.5, 1, 0.5);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof EnderCrystal c) || !usable(p)) return;
        S s = data(p, S::new);
        int delta = now() - s.placeTick;
        if (s.at == null || delta > 40 || c.getWorld() != s.at.getWorld() || c.getLocation().distanceSquared(s.at) > 2.25) return;
        s.placeTick = -1000;
        s.fast.add(delta <= cfgInt("max-ticks", 1));
        if (s.fast.size() >= 8 && s.fast.count() >= 6) { flag(p, "place->break in " + delta + "t (" + s.fast.count() + "/" + s.fast.size() + ")", 2); s.fast.clear(); }
    }
}

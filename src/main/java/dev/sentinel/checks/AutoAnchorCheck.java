package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** AutoAnchor: charge (glowstone) -> detonate (other item) on the same anchor in <= 1 tick, repeatedly. */
public final class AutoAnchorCheck extends Check {
    private static final class S { int chargeTick = -1000; Block anchor; final Util.Window fast = new Util.Window(6); }
    public AutoAnchorCheck(Sentinel p) { super(p, "autoanchor"); }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || b == null
                || b.getType() != Material.RESPAWN_ANCHOR || !usable(p)) return;
        S s = data(p, S::new);
        if (e.getItem() != null && e.getItem().getType() == Material.GLOWSTONE) { s.chargeTick = now(); s.anchor = b; return; }
        if (s.anchor == null || !s.anchor.equals(b)) return;
        int delta = now() - s.chargeTick;
        s.anchor = null;
        if (delta > 20) return;
        s.fast.add(delta <= cfgInt("max-ticks", 1));
        if (s.fast.size() >= 4 && s.fast.count() >= 3) { flag(p, "charge->explode in " + delta + "t", 2); s.fast.clear(); }
    }
}

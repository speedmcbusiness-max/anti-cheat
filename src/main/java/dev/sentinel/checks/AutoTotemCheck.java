package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/** AutoTotem: re-equipping a totem within <= 1 tick of popping one, repeatedly. */
public final class AutoTotemCheck extends Check {
    private static final class S { int popTick = -1000; final Util.Window fast = new Util.Window(6); }
    public AutoTotemCheck(Sentinel p) { super(p, "autototem"); }

    private static boolean totem(ItemStack i) { return i != null && i.getType() == Material.TOTEM_OF_UNDYING; }

    @EventHandler
    public void onPop(EntityResurrectEvent e) {
        if (e.getEntity() instanceof Player p && !e.isCancelled() && usable(p)) data(p, S::new).popTick = now();
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !usable(p)) return;
        boolean toOffhand = e.getClick() == ClickType.SWAP_OFFHAND || e.getSlot() == 40;
        if (toOffhand && (totem(e.getCurrentItem()) || totem(e.getCursor()))) record(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (usable(p) && (totem(e.getMainHandItem()) || totem(e.getOffHandItem()))) record(p);
    }

    private void record(Player p) {
        S s = data(p, S::new);
        int delta = now() - s.popTick;
        if (delta < 0 || delta > 20) return;
        s.popTick = -1000;
        s.fast.add(delta <= cfgInt("max-ticks", 1));
        if (s.fast.size() >= 4 && s.fast.count() >= 3) { flag(p, "re-equipped totem in " + delta + "t", 2); s.fast.clear(); }
    }
}

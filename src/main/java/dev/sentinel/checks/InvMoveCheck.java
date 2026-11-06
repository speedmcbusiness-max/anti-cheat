package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;

/** InvMove: the vanilla client releases all movement keys while a screen is open. Any held key = cheat. */
public final class InvMoveCheck extends Check {
    private static final class S { double buf; int openTick = -1000; }
    public InvMoveCheck(Sentinel p) { super(p, "invmove"); }

    private static boolean keys(Player p) {
        Input in = p.getCurrentInput();
        return in.isForward() || in.isBackward() || in.isLeft() || in.isRight() || in.isJump();
    }

    @EventHandler public void onOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) data(p, S::new).openTick = now();
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !usable(p)) return;
        S s = data(p, S::new);
        if (keys(p)) { if (++s.buf >= cfgInt("buffer", 3)) { flag(p, "clicked inventory while holding movement keys", 1.5); s.buf = 0; } }
        else s.buf = Math.max(0, s.buf - 0.5);
    }

    /** Containers the server knows about (chests, furnaces...). */
    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        if (!m.moved || p.getOpenInventory().getType() == InventoryType.CRAFTING) return;
        S s = data(p, S::new);
        if (now() - s.openTick < 10 || m.grace(10)) return;
        if (keys(p)) { if (++s.buf >= 6) { flag(p, "moving with container open", 1.5); s.buf = 0; } }
    }
}

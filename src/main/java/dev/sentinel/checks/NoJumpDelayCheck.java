package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/** NoJumpDelay: vanilla enforces a 10 tick cooldown between jump take-offs, on any terrain. */
public final class NoJumpDelayCheck extends Check {
    private static final class S { int lastJump = -1000; }
    public NoJumpDelayCheck(Sentinel p) { super(p, "nojumpdelay"); }

    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        if (!m.jumped || m.special || m.grace(20)) return;
        S s = data(p, S::new);
        int interval = m.moveCount - s.lastJump; // counted in client ticks (movement packets), immune to server lag
        s.lastJump = m.moveCount;
        if (interval < cfgInt("min-interval", 9)) flag(p, "jumped again after " + interval + " ticks", 1.5);
    }
}

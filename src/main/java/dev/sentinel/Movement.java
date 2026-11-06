package dev.sentinel;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/** Per-player movement snapshot, updated once per client movement packet. */
public final class Movement {
    public double dx, dy, dz, lastDy, horiz;
    public boolean moved, ground, lastGround, clientGround, special, jumped, setback;
    public int airTicks, groundTicks, idleTicks, moveCount, usingTicks, sneakTicks;
    public int lastVelocityTick = -1000, lastTeleportTick = -1000, lastSpecialTick = -1000;
    public Location lastGroundLoc;

    /** True if a velocity, teleport or special-physics event happened within the last n ticks. */
    public boolean grace(int ticks) {
        int last = Math.max(lastVelocityTick, Math.max(lastTeleportTick, lastSpecialTick));
        return Bukkit.getCurrentTick() - last < ticks;
    }
}

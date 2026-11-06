package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ESP / ItemESP cannot be detected (they only change what the client draws). They are defeated by never
 * sending hidden entities: players and dropped items without line of sight are hidden from the viewer.
 */
public final class EspGuard extends Check {
    private final Map<UUID, Set<UUID>> hiddenByUs = new HashMap<>();
    public EspGuard(Sentinel p) { super(p, "esp"); }

    @Override public void remove(UUID id) { super.remove(id); hiddenByUs.remove(id); }

    @Override public void onTick(Player viewer, Movement m) {
        int interval = Math.max(1, cfgInt("interval", 4));
        if ((now() + viewer.getEntityId()) % interval != 0) return;
        boolean players = cfgBool("players", true), items = cfgBool("items", true);
        double range = cfg("range", 80), near = cfg("always-visible-within", 7);
        Location eye = viewer.getEyeLocation();
        Set<UUID> mine = hiddenByUs.computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>());
        int budget = cfgInt("max-entities-per-run", 48);
        for (Entity e : viewer.getNearbyEntities(range, range, range)) {
            boolean isPlayer = e instanceof Player, isItem = e instanceof Item;
            if (!(isPlayer && players) && !(isItem && items)) continue;
            if (isPlayer && ((Player) e).getGameMode() == GameMode.SPECTATOR) continue;
            if (budget-- <= 0) break;
            boolean visible = eye.distanceSquared(e.getLocation()) <= near * near || visibleFrom(eye, e);
            boolean ours = mine.contains(e.getUniqueId());
            if (visible && ours) { viewer.showEntity(plugin, e); mine.remove(e.getUniqueId()); }
            else if (!visible && !ours && viewer.canSee(e)) { viewer.hideEntity(plugin, e); mine.add(e.getUniqueId()); }
        }
        mine.removeIf(id -> Bukkit.getEntity(id) == null);
    }

    private boolean visibleFrom(Location eye, Entity e) {
        BoundingBox b = e.getBoundingBox();
        Vector c = b.getCenter();
        Vector[] pts = {
                new Vector(c.getX(), b.getMaxY() - 0.1, c.getZ()), c, new Vector(c.getX(), b.getMinY() + 0.1, c.getZ()),
                new Vector(b.getMinX(), c.getY(), b.getMinZ()), new Vector(b.getMaxX(), c.getY(), b.getMaxZ())};
        for (Vector pt : pts) if (clear(eye, pt)) return true;
        return false;
    }

    /** Ray test that looks through non-occluding blocks (glass, leaves, fences...). */
    private boolean clear(Location eye, Vector to) {
        World w = eye.getWorld();
        Vector from = eye.toVector();
        for (int i = 0; i < 4; i++) {
            Vector dir = to.clone().subtract(from);
            double len = dir.length();
            if (len < 0.1) return true;
            dir.normalize();
            RayTraceResult r = w.rayTraceBlocks(from.toLocation(w), dir, len, FluidCollisionMode.NEVER, true);
            if (r == null || r.getHitBlock() == null) return true;
            if (r.getHitBlock().getType().isOccluding()) return false;
            from = r.getHitPosition().add(dir.multiply(0.05));
        }
        return false;
    }
}

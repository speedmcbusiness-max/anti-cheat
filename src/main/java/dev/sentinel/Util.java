package dev.sentinel;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Set;

public final class Util {
    private Util() {}

    public static final Set<Material> BOUNCY = Set.of(Material.SLIME_BLOCK, Material.HONEY_BLOCK, Material.HAY_BLOCK);
    private static final Set<Material> SPECIAL = Set.of(Material.COBWEB, Material.POWDER_SNOW, Material.SWEET_BERRY_BUSH,
            Material.SCAFFOLDING, Material.BUBBLE_COLUMN, Material.HONEY_BLOCK);

    public static String fmt(double d) { return String.format(Locale.ROOT, "%.3f", d); }
    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Fixed-size rolling window of booleans. */
    public static final class Window {
        private final boolean[] a; private int i, n;
        public Window(int size) { a = new boolean[size]; }
        public void add(boolean b) { a[i] = b; i = (i + 1) % a.length; if (n < a.length) n++; }
        public int size() { return n; }
        public int count() { int c = 0; for (int k = 0; k < n; k++) if (a[k]) c++; return c; }
        public void clear() { n = 0; i = 0; }
    }

    /** Server-side ground test using real block collision shapes under the player's feet. */
    public static boolean ground(World w, double x, double y, double z, boolean checkEntities) {
        double minX = x - 0.3, maxX = x + 0.3, minZ = z - 0.3, maxZ = z + 0.3;
        int base = (int) Math.floor(y - 0.2);
        for (int by = base; by >= base - 1; by--)
            for (int bx = (int) Math.floor(minX); bx <= (int) Math.floor(maxX); bx++)
                for (int bz = (int) Math.floor(minZ); bz <= (int) Math.floor(maxZ); bz++) {
                    if (!w.isChunkLoaded(bx >> 4, bz >> 4)) continue;
                    Block bl = w.getBlockAt(bx, by, bz);
                    if (bl.isEmpty()) continue;
                    for (BoundingBox sb : bl.getCollisionShape().getBoundingBoxes()) {
                        double top = by + sb.getMaxY();
                        if (top <= y + 0.05 && top >= y - 0.2
                                && sb.getMinX() + bx < maxX && sb.getMaxX() + bx > minX
                                && sb.getMinZ() + bz < maxZ && sb.getMaxZ() + bz > minZ) return true;
                    }
                }
        if (checkEntities) {
            BoundingBox feet = new BoundingBox(minX, y - 0.3, minZ, maxX, y + 0.1, maxZ);
            return !w.getNearbyEntities(feet, e -> e instanceof Boat || e instanceof Shulker || e instanceof Minecart).isEmpty();
        }
        return false;
    }

    /** Situations where vanilla physics are non-standard; movement checks skip these. */
    public static boolean special(Player p, Location at) {
        if (p.getAllowFlight() || p.isFlying() || p.isGliding() || p.isRiptiding() || p.isInsideVehicle()
                || p.isSwimming() || p.isDead()) return true;
        if (p.hasPotionEffect(PotionEffectType.LEVITATION) || p.hasPotionEffect(PotionEffectType.SLOW_FALLING)) return true;
        Block feet = at.getBlock(), head = feet.getRelative(0, 1, 0);
        if (p.isInWater() || p.isInLava() || feet.isLiquid() || head.isLiquid()) return true;
        Material f = feet.getType(), h = head.getType();
        if (Tag.CLIMBABLE.isTagged(f) || Tag.CLIMBABLE.isTagged(h) || SPECIAL.contains(f) || SPECIAL.contains(h)) return true;
        for (int i = 1; i <= 2; i++) {
            Material b = feet.getRelative(0, -i, 0).getType();
            if (BOUNCY.contains(b) || Tag.BEDS.isTagged(b)) return true;
        }
        return false;
    }

    public static boolean ceilingAbove(Location at) {
        for (double dy = 1.9; dy <= 3.1; dy += 0.6)
            if (!at.clone().add(0, dy, 0).getBlock().isPassable()) return true;
        return false;
    }

    public static boolean wallAhead(Location at, Vector dir) {
        Location f = at.clone().add(dir.clone().multiply(0.8));
        return !f.clone().add(0, 0.2, 0).getBlock().isPassable() || !f.clone().add(0, 1.2, 0).getBlock().isPassable();
    }
}

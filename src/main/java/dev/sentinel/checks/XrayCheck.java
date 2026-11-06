package dev.sentinel.checks;

import dev.sentinel.Sentinel;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Xray: counts how often tunnelling into plain rock lands directly next to a rare ore that was completely
 * enclosed (impossible to see without cheats). Statistical - pair it with Paper's built-in anti-xray.
 */
public final class XrayCheck extends Check {
    private static final Set<Material> RARE = EnumSet.of(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.ANCIENT_DEBRIS,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE);
    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
    private static final class S { int rock, found; }
    public XrayCheck(Sentinel p) { super(p, "xray"); }

    private static boolean rock(Material m) { return Tag.BASE_STONE_OVERWORLD.isTagged(m) || Tag.BASE_STONE_NETHER.isTagged(m); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (!usable(p) || !rock(b.getType())) return;
        S s = data(p, S::new);
        s.rock++;
        for (BlockFace f : FACES) {
            Block n = b.getRelative(f);
            if (RARE.contains(n.getType()) && onlyExposedTo(n, b)) s.found++;
        }
        if (s.rock >= 300) { s.rock /= 2; s.found /= 2; }
        double ratio = s.found / (double) Math.max(1, s.rock);
        if (s.found >= cfgInt("min-found", 4) && ratio >= cfg("ratio", 0.04)) {
            flag(p, "hidden ores found=" + s.found + " rock=" + s.rock, 3);
            s.found /= 2;
        }
    }

    /** True if every neighbour of the ore is solid except the block the player just broke. */
    private static boolean onlyExposedTo(Block ore, Block broken) {
        for (BlockFace f : FACES) {
            Block r = ore.getRelative(f);
            if (r.equals(broken)) continue;
            if (!r.getType().isOccluding() || r.isLiquid()) return false;
        }
        return true;
    }
}

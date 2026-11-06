package dev.sentinel.checks;

import dev.sentinel.Movement;
import dev.sentinel.Sentinel;
import dev.sentinel.Util;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** NoSlowDown: speed while using an item (eat/bow/shield) or sneaking must stay at vanilla slowed values. */
public final class NoSlowDownCheck extends Check {
    private static final class S { double buf; }
    public NoSlowDownCheck(Sentinel p) { super(p, "noslowdown"); }

    @Override public void onMove(Player p, Movement m, PlayerMoveEvent e) {
        S s = data(p, S::new);
        if (!m.moved || m.special || m.grace(15)) { s.buf = 0; return; }
        boolean using = m.usingTicks >= (m.ground ? 6 : 15);
        ItemStack legs = p.getInventory().getLeggings();
        boolean swift = legs != null && legs.containsEnchantment(Enchantment.SWIFT_SNEAK);
        boolean sneaking = !swift && m.sneakTicks >= (m.ground ? 6 : 15);
        if (!using && !sneaking) { s.buf = Math.max(0, s.buf - 1); return; }
        Material below = e.getTo().getBlock().getRelative(0, -1, 0).getType();
        if (Tag.ICE.isTagged(below)) return;
        PotionEffect speed = p.getPotionEffect(PotionEffectType.SPEED);
        double mult = 1 + (speed == null ? 0 : 0.2 * (speed.getAmplifier() + 1));
        double max = (using ? cfg("max-using", 0.10) : cfg("max-sneaking", 0.085)) * mult;
        if (m.horiz > max) {
            if (++s.buf >= cfgInt("buffer", 4)) { flag(p, (using ? "using item" : "sneaking") + " speed=" + Util.fmt(m.horiz) + " max=" + Util.fmt(max), 1.5); s.buf = 0; }
        } else s.buf = Math.max(0, s.buf - 1);
    }
}

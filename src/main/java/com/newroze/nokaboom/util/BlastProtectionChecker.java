package com.newroze.nokaboom.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Answers one question: "does this armor piece carry Blast Protection?".
 *
 * <p>Since 1.20.5 enchantments are data-driven registry entries, so we match
 * the vanilla {@code minecraft:blast_protection} key instead of any numeric id.
 * Iterating the stack's own enchantment set means this check needs no world,
 * no registry manager and no server round-trip — it is a pure function of the
 * {@link ItemStack}, safe to call from the render thread.
 */
public final class BlastProtectionChecker {
	private BlastProtectionChecker() {
	}

	/**
	 * @param stack the worn armor piece (any slot)
	 * @return {@code true} if the stack has Blast Protection at any level
	 */
	public static boolean hasBlastProtection(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		ItemEnchantmentsComponent enchantments;
		try {
			enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
		} catch (Exception e) {
			return false;
		}
		if (enchantments == null || enchantments.isEmpty()) {
			return false;
		}
		for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
			if (entry == null) {
				continue;
			}
			try {
				if (entry.matchesKey(Enchantments.BLAST_PROTECTION)) {
					return true;
				}
			} catch (Exception ignored) {
				// A broken registry entry must never crash rendering.
			}
		}
		return false;
	}
}

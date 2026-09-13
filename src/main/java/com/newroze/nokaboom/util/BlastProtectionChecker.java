package com.newroze.nokaboom.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Answers one question: "does this armor piece carry Blast Protection?".
 *
 * <p>Since 1.20.5 enchantments are data-driven registry entries, so we compare
 * the vanilla {@code minecraft:blast_protection} <em>key</em> instead of any
 * numeric id. Iterating the stack's own enchantment set means this check needs
 * no world, no registry manager and no server round-trip — it is a pure
 * function of the {@link ItemStack}, safe to call from the render thread.
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
		ItemEnchantments enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
		if (enchantments == null || enchantments.isEmpty()) {
			return false;
		}
		for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
			if (entry.getKey().map(Enchantments.BLAST_PROTECTION::equals).orElse(false)) {
				return true;
			}
		}
		return false;
	}
}

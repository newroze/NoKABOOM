package com.newroze.nokaboom.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.newroze.nokaboom.NoKABOOM;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON config with per-enchantment toggles + custom colors.
 *
 * <p>File: {@code config/nokaboom.json}. Created with defaults on first launch.
 * Every value is clamped on load, so a hand-edited broken file can never crash rendering.
 *
 * <p>Migration: old configs (1.0.0) had a single {@code highlightRgb} for Blast Protection.
 * On load, if that legacy key is present, its value becomes the color of
 * {@code minecraft:blast_protection} and is then dropped (Gson ignores unknown keys afterwards).
 */
public final class NoKABOOMConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "nokaboom.json";

	private static NoKABOOMConfig instance;

	/** Master switch. {@code false} disables the mod without uninstalling it. */
	public boolean enabled = true;

	/** Highlight armor worn by players (the main PvP use-case). */
	public boolean highlightPlayers = true;

	/** Highlight armor displayed on armor stands (kits, previews, bases). */
	public boolean highlightArmorStands = true;

	/** Highlight held items (swords etc.) in players' / stands' hands. */
	public boolean highlightHeldItems = true;

	/** Legacy field, kept so old configs still load. The menu now uses a single opacity ({@link #maxAlpha}). */
	public int minAlpha = 0x55;

	/**
	 * Glow opacity (0-255): how visible the tint is.
	 * Shown in the menu as «Прозрачность»: {@code 0} — almost invisible,
	 * {@code 255} — solid color.
	 */
	public int maxAlpha = 0xA0;

	/**
	 * Legacy pulse speed. Pulsing was removed from the menu (the glow is now
	 * always static); old configs are migrated to {@code 0} on load.
	 */
	public double pulseSpeed = 0.0;

	/**
	 * The armor overlay is rendered slightly larger than the armor piece itself
	 * so the two shells never z-fight. {@code 1.03} ≈ +3%.
	 */
	public float expandScale = 1.03F;

	/**
	 * Render overlays at full brightness, so marks read even in the dark.
	 * When {@code false}, vanilla lighting applies.
	 */
	public boolean fullbright = true;

	/**
	 * Scale of the held-item glow overlay (the enchant-colored copy of the item
	 * model drawn slightly larger so it never z-fights the real item).
	 * {@code 1.04} ≈ +4%. Pure geometry scale — no shader, no FPS cost.
	 */
	public float heldGlowScale = 1.04F;

	/** Per-enchantment settings, keyed by enchantment id ({@code minecraft:sharpness}). */
	public Map<String, EnchantEntry> enchantments = defaultEnchantments();

	/** One row of {@link #enchantments}. */
	public static final class EnchantEntry {
		public boolean enabled;
		public int color;

		public EnchantEntry() {
		}

		public EnchantEntry(boolean enabled, int color) {
			this.enabled = enabled;
			this.color = color & 0xFFFFFF;
		}
	}

	/** Default table: Blast Protection + Sharpness on, everything else off but listed. */
	public static LinkedHashMap<String, EnchantEntry> defaultEnchantments() {
		LinkedHashMap<String, EnchantEntry> map = new LinkedHashMap<>();
		// Armor — the original feature.
		map.put("minecraft:blast_protection", new EnchantEntry(true, 0xFF2E2E));
		map.put("minecraft:protection", new EnchantEntry(false, 0x3B82F6));
		map.put("minecraft:fire_protection", new EnchantEntry(false, 0xFF9500));
		map.put("minecraft:projectile_protection", new EnchantEntry(false, 0x9B59B6));
		map.put("minecraft:feather_falling", new EnchantEntry(false, 0xB2BEC3));
		map.put("minecraft:respiration", new EnchantEntry(false, 0x00BFFF));
		map.put("minecraft:aqua_affinity", new EnchantEntry(false, 0x1E90FF));
		map.put("minecraft:thorns", new EnchantEntry(false, 0xE84393));
		map.put("minecraft:depth_strider", new EnchantEntry(false, 0x00CED1));
		map.put("minecraft:frost_walker", new EnchantEntry(false, 0x74B9FF));
		map.put("minecraft:soul_speed", new EnchantEntry(false, 0xD63031));
		map.put("minecraft:swift_sneak", new EnchantEntry(false, 0x6C5CE7));
		// Melee — swords, maces, axes.
		map.put("minecraft:sharpness", new EnchantEntry(true, 0xFF7A1A));
		map.put("minecraft:smite", new EnchantEntry(false, 0xFF5FA2));
		map.put("minecraft:bane_of_arthropods", new EnchantEntry(false, 0x7BED9F));
		map.put("minecraft:fire_aspect", new EnchantEntry(false, 0xFF4500));
		map.put("minecraft:knockback", new EnchantEntry(false, 0x70A1FF));
		map.put("minecraft:looting", new EnchantEntry(false, 0xFFD32A));
		map.put("minecraft:sweeping_edge", new EnchantEntry(false, 0x00CEC9));
		map.put("minecraft:breach", new EnchantEntry(false, 0xE17055));
		map.put("minecraft:density", new EnchantEntry(false, 0xA29BFE));
		map.put("minecraft:wind_burst", new EnchantEntry(false, 0x81ECEC));
		// Bows / crossbows / tridents.
		map.put("minecraft:power", new EnchantEntry(false, 0xFFE14D));
		map.put("minecraft:punch", new EnchantEntry(false, 0xE67E22));
		map.put("minecraft:flame", new EnchantEntry(false, 0xE74C3C));
		map.put("minecraft:infinity", new EnchantEntry(false, 0x9B59B6));
		map.put("minecraft:quick_charge", new EnchantEntry(false, 0x00D2D3));
		map.put("minecraft:multishot", new EnchantEntry(false, 0x5F27CD));
		map.put("minecraft:piercing", new EnchantEntry(false, 0x48DBFB));
		map.put("minecraft:loyalty", new EnchantEntry(false, 0x1DD1A1));
		map.put("minecraft:impaling", new EnchantEntry(false, 0x54A0FF));
		map.put("minecraft:riptide", new EnchantEntry(false, 0x5F27CD));
		map.put("minecraft:channeling", new EnchantEntry(false, 0xF368E0));
		// Tools / fishing / generic.
		map.put("minecraft:efficiency", new EnchantEntry(false, 0xFDCB6E));
		map.put("minecraft:fortune", new EnchantEntry(false, 0xF9CA24));
		map.put("minecraft:silk_touch", new EnchantEntry(false, 0xECF0F1));
		map.put("minecraft:luck_of_the_sea", new EnchantEntry(false, 0x00D2D3));
		map.put("minecraft:lure", new EnchantEntry(false, 0xFF9FF3));
		map.put("minecraft:mending", new EnchantEntry(false, 0x2ECC71));
		map.put("minecraft:unbreaking", new EnchantEntry(false, 0x95A5A6));
		map.put("minecraft:binding_curse", new EnchantEntry(false, 0x636E72));
		map.put("minecraft:vanishing_curse", new EnchantEntry(false, 0x636E72));
		return map;
	}

	/** @return the active (singleton) config, defaults if never loaded. */
	public static NoKABOOMConfig get() {
		if (instance == null) {
			instance = new NoKABOOMConfig();
		}
		return instance;
	}

	/** Loads {@code config/nokaboom.json}, creating it with defaults if missing. */
	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		if (Files.isRegularFile(file)) {
			try {
				String json = Files.readString(file);
				NoKABOOMConfig loaded = GSON.fromJson(json, NoKABOOMConfig.class);
				instance = loaded != null ? loaded : new NoKABOOMConfig();
				migrateLegacyColor(json, instance);
			} catch (Exception e) {
				NoKABOOM.LOGGER.warn("[NoKABOOM] Broken config, falling back to defaults: {}", e.toString());
				instance = new NoKABOOMConfig();
			}
		} else {
			instance = new NoKABOOMConfig();
			save();
			return;
		}
		try {
			instance.validate();
			save();
		} catch (Exception e) {
			NoKABOOM.LOGGER.warn("[NoKABOOM] Config validate/save failed: {}", e.toString());
		}
	}

	/**
	 * Old (1.0.0) configs stored a single {@code highlightRgb}. If present,
	 * adopt it as the Blast Protection color so users keep their custom red.
	 */
	private static void migrateLegacyColor(String json, NoKABOOMConfig into) {
		try {
			JsonObject obj = GSON.fromJson(json, JsonObject.class);
			if (obj != null && obj.has("highlightRgb") && obj.get("highlightRgb").isJsonPrimitive()) {
				int rgb = obj.get("highlightRgb").getAsInt() & 0xFFFFFF;
				if (into.enchantments == null) {
					into.enchantments = defaultEnchantments();
				}
				EnchantEntry blast = into.enchantments.get("minecraft:blast_protection");
				if (blast == null) {
					into.enchantments.put("minecraft:blast_protection", new EnchantEntry(true, rgb));
				} else {
					blast.color = rgb;
					blast.enabled = true;
				}
				NoKABOOM.LOGGER.info("[NoKABOOM] Migrated legacy highlightRgb to blast_protection color.");
			}
		} catch (Exception e) {
			NoKABOOM.LOGGER.warn("[NoKABOOM] Legacy migration skipped: {}", e.toString());
		}
	}

	/** Writes the current config back to disk. */
	public static void save() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(get(), writer);
			}
		} catch (Exception e) {
			NoKABOOM.LOGGER.warn("[NoKABOOM] Could not save config: {}", e.toString());
		}
	}

	/**
	 * First enabled tracked enchantment on this stack, as an id string
	 * ({@code minecraft:sharpness}), or {@code null} if none matches.
	 * Pure function of the {@link ItemStack} + config map — render-thread safe.
	 */
	public static String matchEnchantId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		NoKABOOMConfig config = get();
		if (!config.enabled || config.enchantments == null || config.enchantments.isEmpty()) {
			return null;
		}
		ItemEnchantmentsComponent enchantments;
		try {
			enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
		} catch (Exception e) {
			return null;
		}
		if (enchantments == null || enchantments.isEmpty()) {
			return null;
		}
		for (var entry : enchantments.getEnchantments()) {
			String id;
			try {
				id = entry.getKey().map(key -> key.getValue().toString()).orElse(null);
			} catch (Exception e) {
				continue;
			}
			if (id == null) {
				continue;
			}
			EnchantEntry setting = config.enchantments.get(id);
			if (setting != null && setting.enabled) {
				return id;
			}
		}
		return null;
	}

	/** RGB color for the first enabled tracked enchantment on this stack, or {@code null}. */
	public static Integer colorFor(ItemStack stack) {
		String id = matchEnchantId(stack);
		if (id == null) {
			return null;
		}
		Map<String, EnchantEntry> m = get().enchantments;
		if (m == null) {
			return null;
		}
		EnchantEntry setting = m.get(id);
		return setting != null ? setting.color & 0xFFFFFF : null;
	}

	/** Ordered ids for the settings screen: defaults order first, then any custom ids. */
	public static List<String> orderedIds() {
		NoKABOOMConfig config = get();
		List<String> ids = new ArrayList<>();
		for (String id : defaultEnchantments().keySet()) {
			ids.add(id);
		}
		if (config.enchantments != null) {
			for (String id : config.enchantments.keySet()) {
				if (!ids.contains(id)) {
					ids.add(id);
				}
			}
		}
		return ids;
	}

	/** Item groups for the menu filter chips («выбор предмета»). */
	public enum ItemCat {
		ALL("Все"),
		ARMOR("Броня"),
		WEAPON("Оружие"),
		RANGED("Луки"),
		TOOLS("Инструменты");

		public final String label;

		ItemCat(String label) {
			this.label = label;
		}
	}

	/**
	 * Which item groups this enchantment can be applied to.
	 * Used by the menu chips: pick «Меч» and the list shows only sword enchants.
	 * Unknown (modded) ids show up in every group so they are never hidden.
	 */
	public static java.util.EnumSet<ItemCat> catsFor(String id) {
		if (id == null) {
			return java.util.EnumSet.of(ItemCat.ALL);
		}
		switch (id) {
			case "minecraft:protection":
			case "minecraft:fire_protection":
			case "minecraft:blast_protection":
			case "minecraft:projectile_protection":
			case "minecraft:feather_falling":
			case "minecraft:respiration":
			case "minecraft:aqua_affinity":
			case "minecraft:thorns":
			case "minecraft:depth_strider":
			case "minecraft:frost_walker":
			case "minecraft:soul_speed":
			case "minecraft:swift_sneak":
			case "minecraft:binding_curse":
				return java.util.EnumSet.of(ItemCat.ARMOR);
			case "minecraft:sharpness":
			case "minecraft:smite":
			case "minecraft:bane_of_arthropods":
			case "minecraft:fire_aspect":
			case "minecraft:knockback":
			case "minecraft:looting":
			case "minecraft:sweeping_edge":
			case "minecraft:breach":
			case "minecraft:density":
			case "minecraft:wind_burst":
				return java.util.EnumSet.of(ItemCat.WEAPON);
			case "minecraft:power":
			case "minecraft:punch":
			case "minecraft:flame":
			case "minecraft:infinity":
			case "minecraft:quick_charge":
			case "minecraft:multishot":
			case "minecraft:piercing":
			case "minecraft:loyalty":
			case "minecraft:impaling":
			case "minecraft:riptide":
			case "minecraft:channeling":
				return java.util.EnumSet.of(ItemCat.RANGED);
			case "minecraft:efficiency":
			case "minecraft:fortune":
			case "minecraft:silk_touch":
			case "minecraft:luck_of_the_sea":
			case "minecraft:lure":
				return java.util.EnumSet.of(ItemCat.TOOLS);
			case "minecraft:mending":
			case "minecraft:unbreaking":
			case "minecraft:vanishing_curse":
				return java.util.EnumSet.of(ItemCat.ARMOR, ItemCat.WEAPON, ItemCat.RANGED, ItemCat.TOOLS);
			default:
				return java.util.EnumSet.of(ItemCat.ARMOR, ItemCat.WEAPON, ItemCat.RANGED, ItemCat.TOOLS);
		}
	}

	/**
	 * Short Russian hint shown under the enchantment, e.g. «Меч».
	 * Answers «на каком предмете бывает это зачарование».
	 */
	public static String itemHint(String id) {
		if (id == null) {
			return "";
		}
		switch (id) {
			case "minecraft:protection":
			case "minecraft:fire_protection":
			case "minecraft:blast_protection":
			case "minecraft:projectile_protection":
			case "minecraft:thorns":
			case "minecraft:binding_curse":
				return "Броня";
			case "minecraft:feather_falling":
			case "minecraft:depth_strider":
			case "minecraft:frost_walker":
			case "minecraft:soul_speed":
				return "Ботинки";
			case "minecraft:respiration":
			case "minecraft:aqua_affinity":
				return "Шлем";
			case "minecraft:swift_sneak":
				return "Штаны";
			case "minecraft:sharpness":
			case "minecraft:smite":
			case "minecraft:bane_of_arthropods":
			case "minecraft:fire_aspect":
			case "minecraft:knockback":
			case "minecraft:looting":
			case "minecraft:sweeping_edge":
				return "Меч";
			case "minecraft:breach":
			case "minecraft:density":
			case "minecraft:wind_burst":
				return "Булава";
			case "minecraft:power":
			case "minecraft:punch":
			case "minecraft:flame":
			case "minecraft:infinity":
				return "Лук";
			case "minecraft:quick_charge":
			case "minecraft:multishot":
			case "minecraft:piercing":
				return "Арбалет";
			case "minecraft:loyalty":
			case "minecraft:impaling":
			case "minecraft:riptide":
			case "minecraft:channeling":
				return "Трезубец";
			case "minecraft:efficiency":
			case "minecraft:fortune":
			case "minecraft:silk_touch":
				return "Кирка, топор";
			case "minecraft:luck_of_the_sea":
			case "minecraft:lure":
				return "Удочка";
			case "minecraft:mending":
			case "minecraft:unbreaking":
			case "minecraft:vanishing_curse":
				return "Все предметы";
			default:
				return "";
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private void validate() {
		// Pulse was removed from the menu: the glow is always static,
		// opacity comes from maxAlpha («Прозрачность»).
		pulseSpeed = 0.0;
		if (enchantments == null) {
			enchantments = defaultEnchantments();
		} else {
			// Add newly introduced defaults without touching user choices.
			for (Map.Entry<String, EnchantEntry> e : defaultEnchantments().entrySet()) {
				enchantments.putIfAbsent(e.getKey(), e.getValue());
			}
			// Clamp colors, drop broken keys.
			enchantments.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
			for (EnchantEntry entry : enchantments.values()) {
				entry.color &= 0xFFFFFF;
			}
		}
		minAlpha = clamp(minAlpha, 0, 255);
		maxAlpha = clamp(maxAlpha, 0, 255);
		if (minAlpha > maxAlpha) {
			int swap = minAlpha;
			minAlpha = maxAlpha;
			maxAlpha = swap;
		}
		pulseSpeed = 0.0;
		if (!Float.isFinite(expandScale) || expandScale < 1.0F || expandScale > 1.2F) {
			expandScale = 1.03F;
		}
		if (!Float.isFinite(heldGlowScale) || heldGlowScale < 1.0F || heldGlowScale > 1.15F) {
			heldGlowScale = 1.04F;
		}
	}
}

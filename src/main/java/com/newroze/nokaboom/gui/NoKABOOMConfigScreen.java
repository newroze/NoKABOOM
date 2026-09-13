package com.newroze.nokaboom.gui;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Vanilla settings screen (no Cloth Config / no Fabric API needed).
 *
 * <p>How it works, top to bottom:
 * <ul>
 *   <li>search field — filters by name, id or item;</li>
 *   <li>item chips («Все / Броня / Оружие / Луки / Инструменты») — pick an
 *       item and the list shows only enchants that go on it;</li>
 *   <li>enchantment list — real vanilla names (Russian if the game is in
 *       Russian), id plus item hint underneath, ON/OFF state and color dot;</li>
 *   <li>color picker for the selected enchant — saturation/brightness square,
 *       hue strip, hex field, RGB sliders, presets, live preview;</li>
 *   <li>switches with hover hints explaining each one, plus a single
 *       «Прозрачность» slider (glow strength).</li>
 * </ul>
 * Everything applies live in memory, {@code Готово} (or Esc) writes
 * {@code config/nokaboom.json} to disk.
 */
public final class NoKABOOMConfigScreen extends Screen {
	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 190;

	/** One-click colors for the preset grid. */
	private static final int[] PRESETS = {
			0xFF2E2E, 0xFF7A1A, 0xFFC933, 0x2ECC71, 0x00CEC9, 0x3B82F6, 0x9B59B6, 0xFF5FA2,
			0xFFFFFF, 0xB2BEC3, 0x636E72, 0x2D3436, 0x5F27CD, 0x00D2D3, 0xE17055, 0x74B9FF,
	};

	private final Screen parent;
	private TextFieldWidget searchField;
	private TextFieldWidget hexField;
	private EnchantList list;
	private NoKABOOMConfig.ItemCat selectedCat = NoKABOOMConfig.ItemCat.ALL;
	private final List<Chip> chips = new ArrayList<>();
	private String selectedId = "minecraft:blast_protection";
	private String searchText = "";
	private boolean updatingHex;
	private float lastHue = 0.0F;
	private ButtonWidget selectedToggle;
	private final List<RgbSlider> rgbSliders = new ArrayList<>();
	private final List<PresetSwatch> presetSwatches = new ArrayList<>();
	private final Map<String, String> displayNames = new HashMap<>();
	private final Map<String, String> searchHaystacks = new HashMap<>();

	public NoKABOOMConfigScreen(Screen parent) {
		super(Text.literal("NoKABOOM"));
		this.parent = parent;
	}

	public Screen getParent() {
		return parent;
	}

	/** True while the user is typing in one of the text fields (used to not steal the H hotkey). */
	public boolean isTyping() {
		return (searchField != null && searchField.isFocused())
				|| (hexField != null && hexField.isFocused());
	}

	private int panelLeft() {
		return this.width / 2 - PANEL_WIDTH / 2;
	}

	private int detailTop() {
		return this.height - PANEL_HEIGHT;
	}

	@Override
	protected void init() {
		rgbSliders.clear();
		presetSwatches.clear();
		buildNameCaches();
		int cx = this.width / 2;
		int left = panelLeft();
		int dTop = detailTop();

		searchField = new TextFieldWidget(this.textRenderer, cx - 160, 30, 320, 20, Text.literal("Поиск"));
		searchField.setPlaceholder(Text.literal("Поиск зачарования..."));
		searchField.setMaxLength(64);
		searchField.setChangedListener(s -> {
			searchText = s;
			if (list != null) {
				list.refreshList(s);
				syncDetailFromSelection();
			}
		});
		searchField.setText(searchText);
		searchField.setTooltip(Tooltip.of(Text.literal("Поиск по названию, id и предмету (например «меч»)")));
		addDrawableChild(searchField);

		addChips();

		int listTop = 76;
		int listBottom = dTop - 6;
		if (listBottom < listTop + 40) {
			listBottom = listTop + 40;
		}
		list = new EnchantList(this.client, this.width, listBottom - listTop, listTop, 24);
		addDrawableChild(list);
		list.refreshList(searchField.getText());
		if (list.getSelectedOrNull() == null && !list.children().isEmpty()) {
			list.setSelected(list.children().get(0));
		}
		if (list.getSelectedOrNull() != null) {
			selectedId = list.getSelectedOrNull().id;
		}

		// ---- Color picker: SV square | preview + hex + toggle | RGB sliders, then full-width hue.
		int pickY = dTop + 14;
		SvPicker sv = new SvPicker(left, pickY, 90, 64);
		sv.setTooltip(Tooltip.of(Text.literal("Цвет: насыщенность (влево-вправо) и яркость (вверх-вниз) — тяни")));
		addDrawableChild(sv);
		HueSlider hue = new HueSlider(left, pickY + 66, PANEL_WIDTH, 12);
		hue.setTooltip(Tooltip.of(Text.literal("Оттенок радуги — тяни")));
		addDrawableChild(hue);

		int midX = left + 98;
		hexField = new TextFieldWidget(this.textRenderer, midX, pickY + 24, 90, 18, Text.literal("HEX"));
		hexField.setPlaceholder(Text.literal("#RRGGBB"));
		hexField.setMaxLength(7);
		hexField.setTooltip(Tooltip.of(Text.literal("Код цвета: можно вписать свой, например #FF2E2E")));
		hexField.setChangedListener(this::onHexChanged);
		addDrawableChild(hexField);

		selectedToggle = toggleButton(midX, pickY + 44, 90, "Зачар", this::isSelectedEnabled, this::toggleSelected,
				Tooltip.of(Text.literal("Красить ли это зачарование в игре выбранным цветом")));
		addDrawableChild(selectedToggle);

		int rgbX = left + 196;
		addRgbSlider(rgbX, pickY, 144, "R", () -> redOf(selectedColor()), v -> setSelectedRgb(withRed(selectedColor(), v)),
				Tooltip.of(Text.literal("Красный канал: 0 — нет красного, 255 — максимум")));
		addRgbSlider(rgbX, pickY + 22, 144, "G", () -> greenOf(selectedColor()), v -> setSelectedRgb(withGreen(selectedColor(), v)),
				Tooltip.of(Text.literal("Зелёный канал: 0 — нет зелёного, 255 — максимум")));
		addRgbSlider(rgbX, pickY + 44, 144, "B", () -> blueOf(selectedColor()), v -> setSelectedRgb(withBlue(selectedColor(), v)),
				Tooltip.of(Text.literal("Синий канал: 0 — нет синего, 255 — максимум")));

		// ---- Preset swatches: one row of large clickable colors (no clipped text).
		int presetY = pickY + 82;
		for (int i = 0; i < PRESETS.length; i++) {
			int color = PRESETS[i];
			int px = left + 3 + i * 21;
			ButtonWidget b = ButtonWidget.builder(Text.empty(), btn -> setSelectedRgb(color))
					.dimensions(px, presetY, 18, 18)
					.tooltip(Tooltip.of(Text.literal(String.format("#%06X", color))))
					.build();
			presetSwatches.add(new PresetSwatch(b, color));
			addDrawableChild(b);
		}

		// ---- Global switches (every button has a hover hint explaining it).
		int tgY = dTop + 118;
		addDrawableChild(toggleButton(left + 1, tgY, 66, "Мод", () -> NoKABOOMConfig.get().enabled,
				() -> NoKABOOMConfig.get().enabled = !NoKABOOMConfig.get().enabled,
				Tooltip.of(Text.literal("Главный выключатель: выкл — мод ничего не подсвечивает"))));
		addDrawableChild(toggleButton(left + 69, tgY, 66, "Игрок", () -> NoKABOOMConfig.get().highlightPlayers,
				() -> NoKABOOMConfig.get().highlightPlayers = !NoKABOOMConfig.get().highlightPlayers,
				Tooltip.of(Text.literal("Подсвечивать броню и оружие в руках других игроков"))));
		addDrawableChild(toggleButton(left + 137, tgY, 66, "Стенд", () -> NoKABOOMConfig.get().highlightArmorStands,
				() -> NoKABOOMConfig.get().highlightArmorStands = !NoKABOOMConfig.get().highlightArmorStands,
				Tooltip.of(Text.literal("Подсвечивать броню и предметы на стойках для брони"))));
		addDrawableChild(toggleButton(left + 205, tgY, 66, "Руки", () -> NoKABOOMConfig.get().highlightHeldItems,
				() -> NoKABOOMConfig.get().highlightHeldItems = !NoKABOOMConfig.get().highlightHeldItems,
				Tooltip.of(Text.literal("Подсвечивать оружие в руках: меч светится своим цветом"))));
		addDrawableChild(toggleButton(left + 273, tgY, 66, "Свет", () -> NoKABOOMConfig.get().fullbright,
				() -> NoKABOOMConfig.get().fullbright = !NoKABOOMConfig.get().fullbright,
				Tooltip.of(Text.literal("Свечение всегда яркое — видно даже в темноте"))));

		int opY = dTop + 142;
		addDoubleSlider(left + 1, opY, PANEL_WIDTH - 2, "Прозрачность", NoKABOOMConfig.get().maxAlpha / 255.0,
				v -> String.format(Locale.ROOT, "Прозрачность: %d", (int) Math.round(v * 255.0)),
				v -> {
					int a = (int) Math.round(v * 255.0);
					NoKABOOMConfig.get().maxAlpha = a;
					NoKABOOMConfig.get().minAlpha = a;
					NoKABOOMConfig.get().pulseSpeed = 0.0;
				},
				Tooltip.of(Text.literal("Сила свечения: 0 — почти не видно, 255 — сплошной цвет")));

		int ftY = this.height - 24;
		addDrawableChild(ButtonWidget.builder(Text.literal("Сброс"),
				btn -> {
					NoKABOOMConfig fresh = new NoKABOOMConfig();
					NoKABOOMConfig.get().enchantments = fresh.enchantments;
					NoKABOOMConfig.get().enabled = fresh.enabled;
					NoKABOOMConfig.get().highlightPlayers = fresh.highlightPlayers;
					NoKABOOMConfig.get().highlightArmorStands = fresh.highlightArmorStands;
					NoKABOOMConfig.get().highlightHeldItems = fresh.highlightHeldItems;
					NoKABOOMConfig.get().minAlpha = fresh.minAlpha;
					NoKABOOMConfig.get().maxAlpha = fresh.maxAlpha;
					NoKABOOMConfig.get().pulseSpeed = fresh.pulseSpeed;
					NoKABOOMConfig.get().expandScale = fresh.expandScale;
					NoKABOOMConfig.get().fullbright = fresh.fullbright;
					NoKABOOMConfig.get().heldGlowScale = fresh.heldGlowScale;
					NoKABOOMConfig.save();
					MinecraftClient c = MinecraftClient.getInstance();
					c.setScreen(new NoKABOOMConfigScreen(parent));
				}).dimensions(left, ftY, 80, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Готово"),
				btn -> close()).dimensions(left + 260, ftY, 80, 20).build());

		syncDetailFromSelection();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
		int left = panelLeft();
		int dTop = detailTop();

		// Selected enchant header: localized name (trimmed) + colored ON/OFF status.
		String name = displayName(selectedId);
		var map = NoKABOOMConfig.get().enchantments;
		NoKABOOMConfig.EnchantEntry entry = (map == null || selectedId == null) ? null : map.get(selectedId);
		boolean on = entry != null && entry.enabled;
		String status = on ? "ВКЛ" : "ВЫКЛ";
		int statusW = this.textRenderer.getWidth(status);
		int nameW = PANEL_WIDTH - statusW - 12;
		String shown = trimToWidth(name, nameW);
		context.drawText(this.textRenderer, Text.literal(shown), left, dTop, 0xFFFFFF, false);
		context.drawText(this.textRenderer, Text.literal(status),
				left + PANEL_WIDTH - statusW, dTop, on ? 0x55FF55 : 0xFF5555, false);

		// Live color preview next to the hex field (strictly left of the RGB sliders).
		int pvX = left + 98;
		int pvY = dTop + 14;
		int argb = 0xFF000000 | (selectedColor() & 0xFFFFFF);
		context.fill(pvX, pvY, pvX + 90, pvY + 22, argb);
		drawBorder(context, pvX, pvY, 90, 22, 0xFF000000);
		String hex = String.format(Locale.ROOT, "#%06X", selectedColor() & 0xFFFFFF);
		int hexW = this.textRenderer.getWidth(hex);
		int lum = (redOf(selectedColor()) * 30 + greenOf(selectedColor()) * 59 + blueOf(selectedColor()) * 11) / 100;
		context.drawText(this.textRenderer, Text.literal(hex),
				pvX + (90 - hexW) / 2, pvY + 7, lum < 90 ? 0xFFFFFF : 0x202020, lum < 90);

		// Preset swatch colors on top of their buttons.
		int current = selectedColor() & 0xFFFFFF;
		for (PresetSwatch sw : presetSwatches) {
			ButtonWidget b = sw.button;
			context.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(),
					0xFF000000 | (sw.color & 0xFFFFFF));
			boolean active = (sw.color & 0xFFFFFF) == current;
			drawBorder(context, b.getX(), b.getY(), b.getWidth(), b.getHeight(),
					active ? 0xFFFFFFFF : (b.isHovered() ? 0xFFFFFFFF : 0xFF000000));
		}

		if (list != null && list.children().isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("Ничего не найдено — измени запрос"),
					this.width / 2, 56 + 20, 0xAAAAAA);
		}
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("H — открыть / закрыть меню"),
				this.width / 2, this.height - 22, 0xAAAAAA);
	}

	@Override
	public void close() {
		NoKABOOMConfig cfg = NoKABOOMConfig.get();
		if (cfg.minAlpha > cfg.maxAlpha) {
			int swap = cfg.minAlpha;
			cfg.minAlpha = cfg.maxAlpha;
			cfg.maxAlpha = swap;
		}
		NoKABOOMConfig.save();
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}

	// ------------------------------------------------------------------ widgets

	private ButtonWidget toggleButton(int x, int y, int w, String label, BooleanSupplier get, Runnable flip, Tooltip tip) {
		ButtonWidget b = ButtonWidget.builder(Text.literal(label + ": " + (get.getAsBoolean() ? "ВКЛ" : "ВЫКЛ")),
				btn -> {
					flip.run();
					btn.setMessage(Text.literal(label + ": " + (get.getAsBoolean() ? "ВКЛ" : "ВЫКЛ")));
				}).dimensions(x, y, w, 20).tooltip(tip).build();
		return b;
	}

	/** Item filter chips: pick an item and the list shows only enchants that go on it. */
	private void addChips() {
		chips.clear();
		NoKABOOMConfig.ItemCat[] cats = NoKABOOMConfig.ItemCat.values();
		int[] widths = {44, 60, 64, 56, 92};
		String[] tips = {
			"Показать все зачарования",
			"Чары для брони: защита, шипы, невесомость...",
			"Чары для меча и булавы: острота, добыча...",
			"Чары для лука, арбалета и трезубца",
			"Чары для кирки, топора и удочки"
		};
		int gap = 4;
		int total = -gap;
		for (int w : widths) {
			total += w + gap;
		}
		int x = this.width / 2 - total / 2;
		for (int i = 0; i < cats.length; i++) {
			final NoKABOOMConfig.ItemCat cat = cats[i];
			ButtonWidget b = ButtonWidget.builder(Text.literal(cat.label), btn -> {
				selectedCat = cat;
				if (list != null) {
					list.refreshList(searchField != null ? searchField.getText() : "");
					syncDetailFromSelection();
				}
				syncChips();
			}).dimensions(x, 54, widths[i], 18)
					.tooltip(Tooltip.of(Text.literal(tips[i])))
					.build();
			chips.add(new Chip(b, cat));
			addDrawableChild(b);
			x += widths[i] + gap;
		}
		syncChips();
	}

	private void syncChips() {
		for (Chip chip : chips) {
			boolean active = chip.cat == selectedCat;
			chip.button.setMessage(Text.literal(active ? "[" + chip.cat.label + "]" : chip.cat.label));
		}
	}

	private NoKABOOMConfig.EnchantEntry selectedEntry() {
		var map = NoKABOOMConfig.get().enchantments;
		if (map == null || selectedId == null) {
			return null;
		}
		try {
			return map.get(selectedId);
		} catch (Exception e) {
			return null;
		}
	}

	private boolean isSelectedEnabled() {
		NoKABOOMConfig.EnchantEntry e = selectedEntry();
		return e != null && e.enabled;
	}

	private void addDoubleSlider(int x, int y, int w, String label, double norm, DoubleFunction<String> msg, DoubleConsumer apply, Tooltip tip) {
		double clamped = Math.max(0.0, Math.min(1.0, norm));
		DoubleSlider s = new DoubleSlider(x, y, w, 20, Text.literal(label), clamped, msg, apply);
		s.setTooltip(tip);
		s.refresh();
		addDrawableChild(s);
	}

	private void addRgbSlider(int x, int y, int w, String channel, IntSupplier get, IntConsumer set, Tooltip tip) {
		RgbSlider s = new RgbSlider(x, y, w, 20, Text.literal(channel), get.getAsInt() / 255.0, channel, get, set);
		s.setTooltip(tip);
		s.refresh();
		rgbSliders.add(s);
		addDrawableChild(s);
	}

	private int selectedColor() {
		NoKABOOMConfig.EnchantEntry e = selectedEntry();
		return e != null ? e.color & 0xFFFFFF : 0xFFFFFF;
	}

	private void setSelectedRgb(int rgb) {
		rgb &= 0xFFFFFF;
		float[] hsv = rgbToHsv(rgb);
		if (hsv[1] >= 0.05F) {
			lastHue = hsv[0];
		}
		NoKABOOMConfig.EnchantEntry e = selectedEntry();
		if (e == null) {
			e = new NoKABOOMConfig.EnchantEntry(false, rgb);
			var map = NoKABOOMConfig.get().enchantments;
			if (map == null) {
				map = new java.util.LinkedHashMap<String, NoKABOOMConfig.EnchantEntry>();
				NoKABOOMConfig.get().enchantments = map;
			}
			map.put(selectedId, e);
		} else {
			e.color = rgb;
		}
		afterSelectedColorChanged();
	}

	private float displayHue() {
		float[] hsv = rgbToHsv(selectedColor());
		return hsv[1] >= 0.05F ? hsv[0] : lastHue;
	}

	private void afterSelectedColorChanged() {
		updatingHex = true;
		try {
			if (hexField != null) {
				hexField.setText(String.format(Locale.ROOT, "#%06X", selectedColor() & 0xFFFFFF));
			}
		} finally {
			updatingHex = false;
		}
		for (RgbSlider s : rgbSliders) {
			s.syncFromColor();
		}
	}

	private void onHexChanged(String text) {
		if (updatingHex) {
			return;
		}
		Integer rgb = parseHex(text);
		if (rgb == null) {
			return;
		}
		NoKABOOMConfig.EnchantEntry e = selectedEntry();
		if (e != null) {
			e.color = rgb;
		} else {
			var map = NoKABOOMConfig.get().enchantments;
			if (map == null) {
				map = new java.util.LinkedHashMap<String, NoKABOOMConfig.EnchantEntry>();
				NoKABOOMConfig.get().enchantments = map;
			}
			map.put(selectedId, new NoKABOOMConfig.EnchantEntry(false, rgb));
		}
		float[] hsv = rgbToHsv(rgb);
		if (hsv[1] >= 0.05F) {
			lastHue = hsv[0];
		}
		for (RgbSlider s : rgbSliders) {
			s.syncFromColor();
		}
	}

	private void toggleSelected() {
		NoKABOOMConfig.EnchantEntry e = selectedEntry();
		if (e != null) {
			e.enabled = !e.enabled;
		}
	}

	private void syncDetailFromSelection() {
		EnchantList.Row row = list != null ? list.getSelectedOrNull() : null;
		if (row != null) {
			selectedId = row.id;
		}
		float[] hsv = rgbToHsv(selectedColor());
		if (hsv[1] >= 0.05F) {
			lastHue = hsv[0];
		}
		if (selectedToggle != null) {
			selectedToggle.setMessage(Text.literal("Зачар: " + (isSelectedEnabled() ? "ВКЛ" : "ВЫКЛ")));
		}
		updatingHex = true;
		try {
			if (hexField != null) {
				hexField.setText(String.format(Locale.ROOT, "#%06X", selectedColor() & 0xFFFFFF));
			}
		} finally {
			updatingHex = false;
		}
		for (RgbSlider s : rgbSliders) {
			s.syncFromColor();
		}
	}

	private String trimToWidth(String s, int maxWidth) {
		if (s == null) {
			return "?";
		}
		if (this.textRenderer.getWidth(s) <= maxWidth || maxWidth <= 10) {
			return s;
		}
		String dots = "...";
		while (s.length() > 1 && this.textRenderer.getWidth(s + dots) > maxWidth) {
			s = s.substring(0, s.length() - 1);
		}
		return s + dots;
	}

	private static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y, x + 1, y + h, color);
		context.fill(x + w - 1, y, x + w, y + h, color);
	}

	// ---------------------------------------------------------- localized names

	private void buildNameCaches() {
		displayNames.clear();
		searchHaystacks.clear();
		for (String id : NoKABOOMConfig.orderedIds()) {
			String localized = resolveLocalized(id);
			displayNames.put(id, localized);
			String hay = (id + " " + localized + " " + prettyName(id) + " " + NoKABOOMConfig.itemHint(id)).toLowerCase(Locale.ROOT);
			searchHaystacks.put(id, hay);
		}
	}

	private String displayName(String id) {
		if (id == null) {
			return "?";
		}
		String s = displayNames.get(id);
		return s != null ? s : prettyName(id);
	}

	/**
	 * The real vanilla enchantment name in the player's language
	 * (Russian when the game is Russian). Falls back to the plain id part.
	 */
	private static String resolveLocalized(String id) {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null && client.world != null) {
				Identifier ident = Identifier.tryParse(id);
				if (ident != null) {
					var registry = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
					var entry = registry.getEntry(ident);
					if (entry.isPresent()) {
						String s = entry.get().value().description().getString();
						if (s != null && !s.isBlank()) {
							return s;
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
		return prettyName(id);
	}

	static Integer parseHex(String text) {
		if (text == null) {
			return null;
		}
		String t = text.trim();
		if (t.startsWith("#")) {
			t = t.substring(1);
		}
		if (t.length() == 3) {
			char[] c = t.toCharArray();
			t = "" + c[0] + c[0] + c[1] + c[1] + c[2] + c[2];
		}
		if (t.length() != 6) {
			return null;
		}
		try {
			return Integer.parseInt(t, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static String prettyName(String id) {
		if (id == null) {
			return "?";
		}
		String path;
		int i = id.indexOf(':');
		if (i >= 0) {
			String ns = id.substring(0, i);
			path = id.substring(i + 1);
			if (!ns.equals("minecraft")) {
				return id;
			}
		} else {
			path = id;
		}
		String[] parts = path.split("_");
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			if (p.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(p.charAt(0)));
			if (p.length() > 1) {
				sb.append(p.substring(1));
			}
		}
		return sb.length() == 0 ? id : sb.toString();
	}

	// ------------------------------------------------------------------ HSV math

	static int redOf(int rgb) {
		return (rgb >> 16) & 0xFF;
	}

	static int greenOf(int rgb) {
		return (rgb >> 8) & 0xFF;
	}

	static int blueOf(int rgb) {
		return rgb & 0xFF;
	}

	static int withRed(int rgb, int r) {
		return ((r & 0xFF) << 16) | (rgb & 0x00FFFF);
	}

	static int withGreen(int rgb, int g) {
		return (rgb & 0xFF00FF) | ((g & 0xFF) << 8);
	}

	static int withBlue(int rgb, int b) {
		return (rgb & 0xFFFF00) | (b & 0xFF);
	}

	static float[] rgbToHsv(int rgb) {
		float r = redOf(rgb) / 255.0F;
		float g = greenOf(rgb) / 255.0F;
		float b = blueOf(rgb) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float h = 0.0F;
		float s = max == 0.0F ? 0.0F : (max - min) / max;
		if (max != min) {
			float d = max - min;
			if (max == r) {
				h = (g - b) / d + (g < b ? 6.0F : 0.0F);
			} else if (max == g) {
				h = (b - r) / d + 2.0F;
			} else {
				h = (r - g) / d + 4.0F;
			}
			h /= 6.0F;
		}
		return new float[]{h, s, max};
	}

	static int hsvToRgb(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		s = Math.max(0.0F, Math.min(1.0F, s));
		v = Math.max(0.0F, Math.min(1.0F, v));
		float r;
		float g;
		float b;
		int i = (int) (h * 6.0F);
		float f = h * 6.0F - i;
		float p = v * (1.0F - s);
		float q = v * (1.0F - f * s);
		float t = v * (1.0F - (1.0F - f) * s);
		switch (i % 6) {
			case 0 -> {
				r = v;
				g = t;
				b = p;
			}
			case 1 -> {
				r = q;
				g = v;
				b = p;
			}
			case 2 -> {
				r = p;
				g = v;
				b = t;
			}
			case 3 -> {
				r = p;
				g = q;
				b = v;
			}
			case 4 -> {
				r = t;
				g = p;
				b = v;
			}
			default -> {
				r = v;
				g = p;
				b = q;
			}
		}
		return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
	}

	// ------------------------------------------------------------------ widgets

	/** Base slider with public helpers: 1.21.11 keeps {@code value}/{@code updateMessage} protected. */
	private abstract static class NkSlider extends SliderWidget {
		NkSlider(int x, int y, int w, int h, Text msg, double norm) {
			super(x, y, w, h, msg, norm);
		}

		public void refresh() {
			updateMessage();
		}
	}

	/** Generic 0..1 slider for pulse / grow / glow / alpha settings. */
	private static final class DoubleSlider extends NkSlider {
		private final DoubleFunction<String> msg;
		private final DoubleConsumer apply;

		DoubleSlider(int x, int y, int w, int h, Text label, double norm, DoubleFunction<String> msg, DoubleConsumer apply) {
			super(x, y, w, h, label, norm);
			this.msg = msg;
			this.apply = apply;
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal(msg.apply(this.value)));
		}

		@Override
		protected void applyValue() {
			apply.accept(this.value);
		}
	}

	/** RGB channel slider bound to the currently selected enchantment color. */
	private final class RgbSlider extends NkSlider {
		private final String channel;
		private final IntSupplier get;
		private final IntConsumer set;

		RgbSlider(int x, int y, int w, int h, Text label, double norm, String channel, IntSupplier get, IntConsumer set) {
			super(x, y, w, h, label, norm);
			this.channel = channel;
			this.get = get;
			this.set = set;
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal(channel + " " + get.getAsInt()));
		}

		@Override
		protected void applyValue() {
			set.accept((int) Math.round(this.value * 255.0));
			afterSelectedColorChanged();
		}

		void syncFromColor() {
			this.value = get.getAsInt() / 255.0;
			updateMessage();
		}
	}

	/** One preset swatch: an empty button (color is painted over it in {@link #render}). */
	private static final class PresetSwatch {
		final ButtonWidget button;
		final int color;

		PresetSwatch(ButtonWidget button, int color) {
			this.button = button;
			this.color = color;
		}
	}

	/** One item filter chip: a button bound to an item group. */
	private static final class Chip {
		final ButtonWidget button;
		final NoKABOOMConfig.ItemCat cat;

		Chip(ButtonWidget button, NoKABOOMConfig.ItemCat cat) {
			this.button = button;
			this.cat = cat;
		}
	}

	/** Big saturation/brightness square for the current hue. Drag to pick. */
	private final class SvPicker extends ClickableWidget {
		SvPicker(int x, int y, int w, int h) {
			super(x, y, w, h, Text.literal("Цвет: насыщенность и яркость"));
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			float hue = displayHue();
			int strips = 24;
			for (int i = 0; i < strips; i++) {
				float s = (i + 0.5F) / strips;
				int x0 = getX() + (int) ((float) i / strips * getWidth());
				int x1 = getX() + (int) ((float) (i + 1) / strips * getWidth());
				int top = 0xFF000000 | hsvToRgb(hue, s, 1.0F);
				context.fillGradient(x0, getY(), Math.max(x1, x0 + 1), getY() + getHeight(), top, 0xFF000000);
			}
			float[] hsv = rgbToHsv(selectedColor());
			int cxp = getX() + (int) (hsv[1] * (getWidth() - 1));
			int cyp = getY() + (int) ((1.0F - hsv[2]) * (getHeight() - 1));
			context.fill(cxp - 3, cyp - 1, cxp + 4, cyp + 2, 0xFF000000);
			context.fill(cxp - 2, cyp, cxp + 3, cyp + 1, 0xFFFFFFFF);
			context.fill(cxp - 1, cyp - 2, cxp, cyp + 3, 0xFFFFFFFF);
			drawBorder(context, getX(), getY(), getWidth(), getHeight(), 0xFFAAAAAA);
		}

		private void applyAt(double mx, double my) {
			float s = (float) ((mx - getX()) / (double) getWidth());
			float v = 1.0F - (float) ((my - getY()) / (double) getHeight());
			s = Math.max(0.0F, Math.min(1.0F, s));
			v = Math.max(0.0F, Math.min(1.0F, v));
			setSelectedRgb(hsvToRgb(displayHue(), s, v));
		}

		@Override
		public void onClick(Click click, boolean doubled) {
			super.onClick(click, doubled);
			applyAt(click.x(), click.y());
		}

		@Override
		protected void onDrag(Click click, double dx, double dy) {
			super.onDrag(click, dx, dy);
			applyAt(click.x(), click.y());
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			appendDefaultNarrations(builder);
		}
	}

	/** Rainbow hue strip for the current color. Drag to pick. */
	private final class HueSlider extends ClickableWidget {
		HueSlider(int x, int y, int w, int h) {
			super(x, y, w, h, Text.literal("Цвет: оттенок"));
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			int strips = 30;
			for (int i = 0; i < strips; i++) {
				float h0 = (float) i / strips;
				int x0 = getX() + (int) (h0 * getWidth());
				int x1 = getX() + (int) ((float) (i + 1) / strips * getWidth());
				context.fill(x0, getY(), Math.max(x1, x0 + 1), getY() + getHeight(),
						0xFF000000 | hsvToRgb(h0, 1.0F, 1.0F));
			}
			int kx = getX() + (int) (displayHue() * (getWidth() - 1));
			context.fill(kx - 1, getY() - 1, kx + 2, getY() + getHeight() + 1, 0xFFFFFFFF);
			context.fill(kx, getY() - 1, kx + 1, getY() + getHeight() + 1, 0xFF000000);
			drawBorder(context, getX(), getY(), getWidth(), getHeight(), 0xFFAAAAAA);
		}

		private void applyAt(double mx) {
			float h = (float) ((mx - getX()) / (double) getWidth());
			h = h - (float) Math.floor(h);
			float[] hsv = rgbToHsv(selectedColor());
			float s = Math.max(hsv[1], 0.55F);
			float v = Math.max(hsv[2], 0.55F);
			if (hsv[1] < 0.05F && hsv[2] > 0.95F) {
				s = 1.0F;
				v = 1.0F;
			}
			setSelectedRgb(hsvToRgb(h, s, v));
		}

		@Override
		public void onClick(Click click, boolean doubled) {
			super.onClick(click, doubled);
			applyAt(click.x());
		}

		@Override
		protected void onDrag(Click click, double dx, double dy) {
			super.onDrag(click, dx, dy);
			applyAt(click.x());
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
			appendDefaultNarrations(builder);
		}
	}

	/** Scrollable filterable list of enchantment ids. Full-width widget, rows centered via getRowWidth. */
	private final class EnchantList extends AlwaysSelectedEntryListWidget<EnchantList.Row> {
		EnchantList(MinecraftClient client, int width, int height, int y, int itemHeight) {
			super(client, width, height, y, itemHeight);
		}

		@Override
		public int getRowWidth() {
			return Math.min(PANEL_WIDTH, NoKABOOMConfigScreen.this.width - 20);
		}

		void refreshList(String query) {
			String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			String keepSelected = selectedId;
			NoKABOOMConfig.ItemCat cat = selectedCat;
			clearEntries();
			for (String id : NoKABOOMConfig.orderedIds()) {
				if (cat != NoKABOOMConfig.ItemCat.ALL && !NoKABOOMConfig.catsFor(id).contains(cat)) {
					continue;
				}
				if (!q.isEmpty()) {
					String hay = searchHaystacks.get(id);
					if (hay == null) {
						hay = (id + " " + prettyName(id)).toLowerCase(Locale.ROOT);
					}
					if (!hay.contains(q)) {
						continue;
					}
				}
				addEntry(new Row(id));
			}
			if (!children().isEmpty()) {
				Row reselect = null;
				for (Row row : children()) {
					if (row.id.equals(keepSelected)) {
						reselect = row;
						break;
					}
				}
				setSelected(reselect != null ? reselect : children().get(0));
				if (getSelectedOrNull() != null) {
					selectedId = getSelectedOrNull().id;
				}
			} else {
				// Empty filter result: keep selectedId as-is, don't touch selection.
				return;
			}
		}

		final class Row extends AlwaysSelectedEntryListWidget.Entry<Row> {
			final String id;

			Row(String id) {
				this.id = id;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				var map = NoKABOOMConfig.get().enchantments;
				NoKABOOMConfig.EnchantEntry e = map == null ? null : map.get(id);
				boolean on = e != null && e.enabled;
				int color = e != null ? e.color & 0xFFFFFF : 0xFFFFFF;
				int x = getX();
				int y = getY();
				int w = getWidth();
				var renderer = NoKABOOMConfigScreen.this.textRenderer;
				// Line 1: real vanilla name (Russian when the game is Russian).
				context.drawText(renderer, Text.literal(displayName(id)),
						x + 6, y + 3, 0xFFFFFF, false);
				// Line 2: raw id + item hint («на каком предмете бывает»), dimmed.
				String sub = id + "  •  " + NoKABOOMConfig.itemHint(id);
				context.drawText(renderer, Text.literal(trimToWidth(sub, w - 12)),
						x + 6, y + 13, 0xFF999999, false);
				// Right side: ON/OFF state + color swatch.
				String state = on ? "ВКЛ" : "ВЫКЛ";
				int stateW = renderer.getWidth(state);
				context.drawText(renderer, Text.literal(state),
						x + w - 8 - stateW, y + 7, on ? 0x55FF55 : 0xFF5555, false);
				int sx1 = x + w - 8 - stateW - 6 - 18;
				context.fill(sx1, y + 5, sx1 + 18, y + 19, 0xFF000000 | color);
				drawBorder(context, sx1, y + 5, 18, 14, 0xFF222222);
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				setSelected(this);
				selectedId = this.id;
				syncDetailFromSelection();
				return true;
			}

			@Override
			public Text getNarration() {
				return Text.literal(displayName(id));
			}
		}
	}
}

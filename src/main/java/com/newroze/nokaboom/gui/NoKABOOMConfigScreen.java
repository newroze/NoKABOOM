package com.newroze.nokaboom.gui;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Vanilla settings screen (no Cloth Config / no Fabric API needed).
 *
 * <p>Top: search field + scrollable enchantment list (click to select).
 * Covers both armor enchants (blast protection...) and weapon enchants
 * (sharpness, smite, fire aspect...): every entry from the config map is listed.
 * Middle: selected enchantment — ON/OFF, hex color field, RGB sliders, presets.
 * Bottom: global switches and pulse/size sliders. Everything applies live in memory,
 * {@code Done} (or Esc) writes {@code config/nokaboom.json} to disk.
 */
public final class NoKABOOMConfigScreen extends Screen {
	private final Screen parent;
	private TextFieldWidget searchField;
	private TextFieldWidget hexField;
	private EnchantList list;
	private String selectedId = "minecraft:blast_protection";
	private boolean updatingHex;
	private ButtonWidget selectedToggle;
	private final List<RgbSlider> rgbSliders = new ArrayList<>();

	public NoKABOOMConfigScreen(Screen parent) {
		super(Text.literal("NoKABOOM Settings"));
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

	@Override
	protected void init() {
		rgbSliders.clear();
		int cx = this.width / 2;

		searchField = new TextFieldWidget(this.textRenderer, cx - 160, 30, 320, 20, Text.literal("Search enchantments"));
		searchField.setMaxLength(64);
		searchField.setChangedListener(s -> {
			if (list != null) {
				list.refreshList(s);
				syncDetailFromSelection();
			}
		});
		addDrawableChild(searchField);

		int listTop = 56;
		int listBottom = this.height - 182;
		if (listBottom < listTop + 60) {
			listBottom = listTop + 60;
		}
		list = new EnchantList(this.client, this.width, listBottom - listTop, listTop, 22);
		addDrawableChild(list);
		list.refreshList(searchField.getText());
		if (list.getSelectedOrNull() == null && !list.children().isEmpty()) {
			list.setSelected(list.children().get(0));
		}
		if (list.getSelectedOrNull() != null) {
			selectedId = list.getSelectedOrNull().id;
		}

		int detailY = this.height - 176;
		hexField = new TextFieldWidget(this.textRenderer, cx + 40, detailY, 90, 20, Text.literal("#RRGGBB"));
		hexField.setMaxLength(7);
		hexField.setChangedListener(this::onHexChanged);
		addDrawableChild(hexField);

		selectedToggle = toggleButton(cx - 170, detailY, 110, "Enabled", this::isSelectedEnabled, this::toggleSelected);
		addDrawableChild(selectedToggle);

		int sliderY = this.height - 152;
		addRgbSlider(cx - 170, sliderY, 105, "R", () -> redOf(selectedColor()), v -> setSelectedRgb(withRed(selectedColor(), v)));
		addRgbSlider(cx - 58, sliderY, 105, "G", () -> greenOf(selectedColor()), v -> setSelectedRgb(withGreen(selectedColor(), v)));
		addRgbSlider(cx + 54, sliderY, 105, "B", () -> blueOf(selectedColor()), v -> setSelectedRgb(withBlue(selectedColor(), v)));

		int presetY = this.height - 128;
		int[] presets = {0xFF2E2E, 0xFF7A1A, 0xFFE14D, 0x2ECC71, 0x00CEC9, 0x3B82F6, 0x9B59B6, 0xFF5FA2};
		int pw = 38;
		int px = cx - (presets.length * (pw + 4)) / 2;
		for (int preset : presets) {
			int color = preset;
			ButtonWidget b = ButtonWidget.builder(Text.literal(String.format("#%06X", color)),
					btn -> setSelectedRgb(color)).dimensions(px, presetY, pw, 20).build();
			px += pw + 4;
			addDrawableChild(b);
		}

		int tgY = this.height - 104;
		int tw = 64;
		int tx = cx - (5 * (tw + 4)) / 2;
		addDrawableChild(toggleButton(tx, tgY, tw, "Mod", () -> NoKABOOMConfig.get().enabled,
				() -> NoKABOOMConfig.get().enabled = !NoKABOOMConfig.get().enabled));
		tx += tw + 4;
		addDrawableChild(toggleButton(tx, tgY, tw, "Players", () -> NoKABOOMConfig.get().highlightPlayers,
				() -> NoKABOOMConfig.get().highlightPlayers = !NoKABOOMConfig.get().highlightPlayers));
		tx += tw + 4;
		addDrawableChild(toggleButton(tx, tgY, tw, "Stands", () -> NoKABOOMConfig.get().highlightArmorStands,
				() -> NoKABOOMConfig.get().highlightArmorStands = !NoKABOOMConfig.get().highlightArmorStands));
		tx += tw + 4;
		addDrawableChild(toggleButton(tx, tgY, tw, "Held", () -> NoKABOOMConfig.get().highlightHeldItems,
				() -> NoKABOOMConfig.get().highlightHeldItems = !NoKABOOMConfig.get().highlightHeldItems));
		tx += tw + 4;
		addDrawableChild(toggleButton(tx, tgY, tw, "Bright", () -> NoKABOOMConfig.get().fullbright,
				() -> NoKABOOMConfig.get().fullbright = !NoKABOOMConfig.get().fullbright));

		int slY1 = this.height - 80;
		addDoubleSlider(cx - 170, slY1, 110, "Pulse", NoKABOOMConfig.get().pulseSpeed / 10.0,
				v -> String.format("Pulse %.1f", v * 10.0), v -> NoKABOOMConfig.get().pulseSpeed = v * 10.0);
		addDoubleSlider(cx - 55, slY1, 110, "Grow", (NoKABOOMConfig.get().expandScale - 1.0F) / 0.2F,
				v -> String.format("Grow %.2f", 1.0F + v * 0.2F), v -> NoKABOOMConfig.get().expandScale = (float) (1.0 + v * 0.2));
		addDoubleSlider(cx + 60, slY1, 110, "SwordBox", (NoKABOOMConfig.get().heldBoxSize - 0.3F) / 1.2F,
				v -> String.format("Box %.2f", 0.3F + v * 1.2F), v -> NoKABOOMConfig.get().heldBoxSize = (float) (0.3 + v * 1.2));

		int slY2 = this.height - 56;
		addDoubleSlider(cx - 170, slY2, 165, "MinA", NoKABOOMConfig.get().minAlpha / 255.0,
				v -> String.format("MinA %d", (int) Math.round(v * 255.0)), v -> NoKABOOMConfig.get().minAlpha = (int) Math.round(v * 255.0));
		addDoubleSlider(cx + 5, slY2, 165, "MaxA", NoKABOOMConfig.get().maxAlpha / 255.0,
				v -> String.format("MaxA %d", (int) Math.round(v * 255.0)), v -> NoKABOOMConfig.get().maxAlpha = (int) Math.round(v * 255.0));

		int ftY = this.height - 28;
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset"),
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
					NoKABOOMConfig.get().heldBoxSize = fresh.heldBoxSize;
					MinecraftClient c = MinecraftClient.getInstance();
					c.setScreen(new NoKABOOMConfigScreen(parent));
				}).dimensions(cx - 170, ftY, 80, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"),
				btn -> close()).dimensions(cx + 90, ftY, 80, 20).build());

		syncDetailFromSelection();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
		int detailY = this.height - 176;
		String pretty = prettyName(selectedId);
		NoKABOOMConfig.EnchantEntry entry = NoKABOOMConfig.get().enchantments.get(selectedId);
		String status = entry != null && entry.enabled ? "ON" : "OFF";
		context.drawText(this.textRenderer, Text.literal(pretty + " [" + status + "]"),
				this.width / 2 - 170, detailY - 12, 0xFFFFFF, false);
		// Color preview swatch next to hex field.
		int swX = this.width / 2 + 136;
		int argb = 0xFF000000 | (selectedColor() & 0xFFFFFF);
		context.fill(swX, detailY, swX + 34, detailY + 20, argb);
		context.drawText(this.textRenderer, Text.literal("H — open menu"),
				this.width / 2 - 80, this.height - 24, 0xAAAAAA, false);
	}

	@Override
	public void close() {
		NoKABOOMConfig.save();
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}

	private ButtonWidget toggleButton(int x, int y, int w, String label, BooleanSupplier get, Runnable flip) {
		ButtonWidget b = ButtonWidget.builder(Text.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")),
				btn -> {
					flip.run();
					btn.setMessage(Text.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")));
				}).dimensions(x, y, w, 20).build();
		return b;
	}

	private boolean isSelectedEnabled() {
		NoKABOOMConfig.EnchantEntry entry = NoKABOOMConfig.get().enchantments.get(selectedId);
		return entry != null && entry.enabled;
	}

	private void addDoubleSlider(int x, int y, int w, String label, double norm, DoubleFunction<String> msg, DoubleConsumer apply) {
		double clamped = Math.max(0.0, Math.min(1.0, norm));
		DoubleSlider s = new DoubleSlider(x, y, w, 20, Text.literal(label), clamped, msg, apply);
		s.refresh();
		addDrawableChild(s);
	}

	private void addRgbSlider(int x, int y, int w, String channel, IntSupplier get, IntConsumer set) {
		RgbSlider s = new RgbSlider(x, y, w, 20, Text.literal(channel), get.getAsInt() / 255.0, channel, get, set);
		s.refresh();
		rgbSliders.add(s);
		addDrawableChild(s);
	}

	private int selectedColor() {
		NoKABOOMConfig.EnchantEntry entry = NoKABOOMConfig.get().enchantments.get(selectedId);
		return entry != null ? entry.color & 0xFFFFFF : 0xFFFFFF;
	}

	private void setSelectedRgb(int rgb) {
		NoKABOOMConfig.EnchantEntry entry = NoKABOOMConfig.get().enchantments.get(selectedId);
		if (entry == null) {
			entry = new NoKABOOMConfig.EnchantEntry(false, rgb);
			NoKABOOMConfig.get().enchantments.put(selectedId, entry);
		} else {
			entry.color = rgb & 0xFFFFFF;
		}
		afterSelectedColorChanged();
	}

	private void afterSelectedColorChanged() {
		updatingHex = true;
		try {
			if (hexField != null) {
				hexField.setText(String.format("#%06X", selectedColor() & 0xFFFFFF));
			}
		} finally {
			updatingHex = false;
		}
		for (RgbSlider s : rgbSliders) {
			s.refresh();
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
		NoKABOOMConfig.EnchantEntry entry = NoKABOOMConfig.get().enchantments.get(selectedId);
		if (entry != null) {
			entry.color = rgb;
		} else {
			NoKABOOMConfig.get().enchantments.put(selectedId, new NoKABOOMConfig.EnchantEntry(false, rgb));
		}
		for (RgbSlider s : rgbSliders) {
			s.syncFromColor();
		}
	}

	private void toggleSelected() {
		NoKABOOMConfig.EnchantEntry entry = NoKABOOMConfig.get().enchantments.get(selectedId);
		if (entry != null) {
			entry.enabled = !entry.enabled;
		}
	}

	private void syncDetailFromSelection() {
		EnchantList.Row row = list != null ? list.getSelectedOrNull() : null;
		if (row != null) {
			selectedId = row.id;
		}
		if (selectedToggle != null) {
			selectedToggle.setMessage(Text.literal("Enabled: " + (isSelectedEnabled() ? "ON" : "OFF")));
		}
		updatingHex = true;
		try {
			if (hexField != null) {
				hexField.setText(String.format("#%06X", selectedColor() & 0xFFFFFF));
			}
		} finally {
			updatingHex = false;
		}
		for (RgbSlider s : rgbSliders) {
			s.syncFromColor();
		}
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
		int i = id.indexOf(':');
		String path = i >= 0 ? id.substring(i + 1) : id;
		return path.replace('_', ' ');
	}

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

	/** Base slider with public helpers: 1.21.11 keeps {@code value}/{@code updateMessage} protected. */
	private abstract static class NkSlider extends SliderWidget {
		NkSlider(int x, int y, int w, int h, Text msg, double norm) {
			super(x, y, w, h, msg, norm);
		}

		public void refresh() {
			updateMessage();
		}

		public void setNorm(double norm) {
			setValue(norm);
		}
	}

	/** Generic 0..1 slider for pulse / grow / alpha settings. */
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

	/** Scrollable filterable list of enchantment ids. Full-width widget, rows centered via getRowWidth. */
	private final class EnchantList extends AlwaysSelectedEntryListWidget<EnchantList.Row> {
		EnchantList(MinecraftClient client, int width, int height, int y, int itemHeight) {
			super(client, width, height, y, itemHeight);
		}

		@Override
		public int getRowWidth() {
			return 340;
		}

		void refreshList(String query) {
			String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			String keepSelected = selectedId;
			clearEntries();
			for (String id : NoKABOOMConfig.orderedIds()) {
				if (!q.isEmpty()) {
					String hay = (id + " " + prettyName(id)).toLowerCase(Locale.ROOT);
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
			}
		}

		final class Row extends AlwaysSelectedEntryListWidget.Entry<Row> {
			final String id;

			Row(String id) {
				this.id = id;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
				NoKABOOMConfig.EnchantEntry entry = NoKABOOMConfig.get().enchantments.get(id);
				boolean on = entry != null && entry.enabled;
				int color = entry != null ? entry.color & 0xFFFFFF : 0xFFFFFF;
				int x = getX();
				int y = getY();
				int w = getWidth();
				context.drawText(NoKABOOMConfigScreen.this.textRenderer, Text.literal(prettyName(id)),
						x + 4, y + 2, 0xFFFFFF, false);
				context.drawText(NoKABOOMConfigScreen.this.textRenderer, Text.literal(id),
						x + 4, y + 11, 0x888888, false);
				String state = on ? "ON" : "OFF";
				context.drawText(NoKABOOMConfigScreen.this.textRenderer, Text.literal(state),
						x + w - 30, y + 6, on ? 0x55FF55 : 0xFF5555, false);
				context.fill(x + w - 52, y + 4, x + w - 36, y + 18, 0xFF000000 | color);
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
				return Text.literal(prettyName(id));
			}
		}
	}
}

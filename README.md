# NoKABOOM

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-44bd32?style=flat-square&logo=minecraft&logoColor=white)
![Loader](https://img.shields.io/badge/Fabric_Loader-%3E%3D0.19.5-0097e6?style=flat-square)
![Side](https://img.shields.io/badge/Side-Client_only-e1b12c?style=flat-square)
![Fabric API](https://img.shields.io/badge/Fabric_API-not_required-8e44ad?style=flat-square)
![Java](https://img.shields.io/badge/Java-21+-red?style=flat-square)
![License](https://img.shields.io/github/license/newroze/NoKABOOM?style=flat-square)

> **RU:** клиентский PvP-мод для Fabric — перекрашивает броню и оружие в руках в цвет их зачарований. Никаких боксов и плоских «коробок»: светится сама текстура предмета (как ванильное крашение кожаной брони). По умолчанию — **Blast Protection** красным и **Sharpness** оранжевым.
> **EN:** client-side Fabric PvP mod — recolors armor and held weapons into the color of their enchantments. No boxes, no flat shells: the item's own texture glows (like vanilla leather dyeing). Defaults — **Blast Protection** in red, **Sharpness** in orange.

---

## Requirements / Требования

| Component | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | >= 0.19.5 |
| Java | 21+ |
| Fabric API | **not required** / не нужен |
| Side | client only |

## Installation / Установка

1. Install [Fabric Loader](https://fabricmc.net/use/) **>= 0.19.5** for Minecraft **1.21.11** with Java **21+**.
   Установи Fabric Loader **>= 0.19.5** для Minecraft **1.21.11** и Java **21+**.
2. Download `nokaboom-1.0.0.jar` from [Releases](https://github.com/newroze/NoKABOOM/releases) and put it into your `mods` folder.
   Скачай `nokaboom-1.0.0.jar` из Releases и положи в папку `mods`.
3. No Fabric API required — the mod depends only on Fabric Loader.
   Fabric API не нужен — мод зависит только от Loader.

## Features / Возможности

- **Texture tint, not boxes.** Armor keeps its texture, trim and shading — only the color changes. Same for held weapons: the sword's own quads are re-drawn tinted, so the blade glows instead of getting a frame around it.
  **Цвет текстуры, а не боксы.** Броня сохраняет текстуру, узор и тримы — меняется только цвет. Оружие в руках тоже светится своей текстурой, а не рамкой вокруг.
- **Per-enchant colors.** Every enchantment has its own ON/OFF switch and color.
  **Свой цвет у каждого чара.** У каждого зачарования свой выключатель и цвет.
- **Pick an item, see its enchants.** Filter chips — Все / Броня / Оружие / Луки / Инструменты — show only enchants that go on that item. Each row also tells you where the enchant applies («Меч», «Ботинки», «Лук»...).
  **Выбери предмет — увидишь его чары.** Кнопки-фильтры показывают только чары для этого предмета, под каждым чаром подписано, на чём он бывает.
- **Full color picker.** Saturation/brightness square, hue strip, HEX field (`#FF2E2E`), RGB sliders, one-click presets, live preview.
  **Нормальная палитра.** Квадрат насыщенности/яркости, полоса оттенков, HEX-поле, RGB-слайдеры, пресеты, живой предпросмотр.
- **One opacity slider.** Single «Прозрачность» control (0 — barely visible, 255 — solid). No pulse/breathe gimmicks.
  **Одна прозрачность.** Один слайдер «Прозрачность», без пульсаций.
- **Players, armor stands, hands.** Works on other players (the PvP case), armor stands (kits, previews) and held items — each can be toggled separately.
  **Игроки, стойки, руки.** Подсветка других игроков, стоек для брони и оружия в руках — всё отключается по отдельности.
- **Fullbright option.** The mark reads even in the dark.
  **Опция «Свет».** Метка читается даже в темноте.
- **Zero shader cost.** No custom shaders — vanilla pipelines, one extra draw call per highlighted piece.
  **Ноль нагрузки.** Без кастомных шейдеров — ванильные пайплайны, один лишний draw call на подсвеченный предмет.

```
Enemy seen in PvP / Противник в PvP:

  Helmet / Шлем           Fire Protection IV      normal / обычный
  Chestplate / Нагрудник  Blast Protection IV    RED TINT / КРАСНЫЙ ТИНТ
  Leggings / Поножи        Protection IV           normal / обычный
  Boots / Ботинки          Blast Protection III   RED TINT / КРАСНЫЙ ТИНТ
  Sword / Меч              Sharpness V            ORANGE BLADE / ОРАНЖЕВЫЙ КЛИНОК
```

## Usage / Использование

Press **H** in-game or the **NoKABOOM...** button in the pause menu (Esc).
Нажми **H** в игре или кнопку **NoKABOOM...** в паузе (Esc).

1. Pick an item chip (e.g. **Оружие**) or type in search (name, id, or item — «меч» works).
   Выбери предмет или вбей в поиск (название, id или предмет).
2. Click an enchantment, set its color in the picker, make sure **Зачар: ВКЛ**.
   Кликни чар, выбери цвет, проверь **Зачар: ВКЛ**.
3. Tune **Прозрачность** and the **Мод / Игрок / Стенд / Руки / Свет** switches — every control has a hover hint.
   Настрой **Прозрачность** и переключатели — у каждой кнопки есть подсказка.
4. **Готово** saves to `config/nokaboom.json`.
   **Готово** сохраняет в `config/nokaboom.json`.

## Configuration / Конфиг — `config/nokaboom.json`

Created automatically on first launch. Every value is clamped on load, so a hand-edited broken file can never crash rendering.
Создаётся автоматически при первом запуске. Все значения проверяются при загрузке — битый конфиг не уронит рендер.

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch / мастер-выключатель |
| `highlightPlayers` | `true` | Tint on players / тинт на игроках |
| `highlightArmorStands` | `true` | Tint on armor stands / тинт на стойках |
| `highlightHeldItems` | `true` | Held weapon glow / свечение оружия в руках |
| `fullbright` | `true` | Always bright, visible in the dark / яркость в темноте |
| `maxAlpha` | `160` | Glow opacity 0–255 («Прозрачность») / сила свечения |
| `expandScale` | `1.03` | Tint shell inflation vs z-fighting (fixed) / раздутие тинта |
| `heldGlowScale` | `1.04` | Held glow inflation (fixed) / раздутие свечения |
| `enchantments` | see below | Per-enchant toggle + color / вкл/выкл + цвет каждого чара |
| `minAlpha`, `pulseSpeed` | legacy | Kept so old configs load; pulsing removed / остатки старых версий |

Defaults ON: `minecraft:blast_protection` (`#FF2E2E`), `minecraft:sharpness` (`#FF7A1A`).
По умолчанию включены Blast Protection и Sharpness, остальные чары перечислены и выключены.

## How it works / Как устроено

```
ArmorFeatureRenderer.renderArmor()  <- vanilla draws armor per slot
        |  @Redirect EquipmentRenderer.render()
        v
vanilla pass (untouched) + tint pass (same call, scaled x1.03)
        |  TINT_OVERRIDE set around the 2nd pass only
        v
EquipmentRendererMixin @Redirect submitModel()
  dye color -> enchant color (own texture x color, like leather dye)
  light     -> fullbright (if enabled)

HeldItemFeatureRenderer.renderItem() <- vanilla poses the hand, draws item
        |  @Redirect ItemRenderState.render()
        v
vanilla draw (untouched) + glow pass (same baked quads, tint slot 0,
  same transforms, scaled x1.04, fullbright)
```

- The enchant check is a pure function of `ItemStack` (`NoKABOOMConfig.colorFor`): no world/server access, render-thread safe.
  Проверка чара — чистая функция от `ItemStack`, без мира/сервера, безопасна в рендер-потоке.
- One armor injection point covers players and stands: both delegate armor to `ArmorFeatureRenderer` + `EquipmentRenderer`.
  Одна точка инъекции покрывает игроков и стойки.
- Rendering never crashes the frame: every mixin body is guarded, a missed tint beats a crashed game.
  Рендер никогда не роняет кадр: все миксины в try/catch.
- Client-only: no packets, the server never knows. Still, respect your server's rules.
  Только клиент: пакетов нет, сервер ничего не знает. Но сверяйся с правилами сервера.

## Project structure / Структура проекта

```
src/main/java/com/newroze/nokaboom/
├── NoKABOOM.java                          # MOD_ID + logger
├── NoKABOOMClient.java                    # ClientModInitializer: config load
├── config/
│   └── NoKABOOMConfig.java                # JSON config + per-enchant map + item groups
├── render/
│   ├── NoKABOOMArmorHighlight.java        # what glows: match, tint flag, opacity
│   └── NoKABOOMHeldHighlight.java         # held glow: tinted quad re-submit
├── gui/
│   ├── NoKABOOMConfigScreen.java          # vanilla screen: chips + search + picker
│   └── NoKABOOMKeybinds.java              # hotkey H without Fabric API (tick poll)
└── mixin/client/
    ├── ArmorFeatureRendererMixin.java     # redirect renderArmor -> vanilla + tint pass
    ├── EquipmentRendererMixin.java        # redirect submitModel -> dye swapped for enchant
    ├── HeldItemFeatureRendererMixin.java  # redirect item draw -> vanilla + glow pass
    ├── ItemRenderStateAccessor.java       # layers / layerCount / displayContext
    ├── ItemLayerAccessor.java             # renderLayer / transform / specialModel
    ├── MinecraftClientMixin.java          # H polling in tick
    └── GameMenuScreenMixin.java           # NoKABOOM... button in pause menu

src/main/resources/
├── fabric.mod.json                        # manifest (client-only, no Fabric API)
├── nokaboom.mixins.json                   # mixins config
└── assets/nokaboom/
    ├── textures/highlight.png             # legacy white texture (unused by tint)
    └── icon.png                           # mod icon

Mod.bat                                    # dev quick-launch: gradlew runClient
```

## Build from source / Сборка из исходников

```bash
# Requires JDK 21+ (JAVA_HOME), Gradle downloads the rest
./gradlew build        # Linux/macOS
gradlew.bat build      # Windows
# Output: build/libs/nokaboom-1.0.0.jar
```

Useful:

```bash
./gradlew runClient     # test client with the mod
Mod.bat                # same, double-click on Windows
./gradlew genSources    # Minecraft sources for IDE reading
```

## FAQ

**Cheats? Will I get banned? / Это читы? Меня забанят?**
The mod sends nothing and changes no mechanics — it only paints over what the client already sees.
Мод ничего не отправляет на сервер и не меняет механику — только рисует поверх того, что клиент и так видит. Но правила у серверов разные — уточни у администрации.

**Why Blast Protection and Sharpness? / Почему Blast Protection и Sharpness?**
Built for crystal PvP: instantly see the "blast" set and the sharp weapon. Everything else can be enabled in the settings screen (H).
Мод заточен под кристаллическое PvP: мгновенно видно «взрывной» сет и острую пушку. Остальное включается в настройках (H).

**Boxes over armor? / Коробки поверх брони?**
No — armor is re-rendered with its own texture multiplied by the enchant color (like vanilla leather dyeing). Diamond still looks like diamond, just red.
Нет — броня перерисовывается своей же текстурой, умноженной на цвет чара (как крашение кожи). Алмазка остаётся алмазкой, просто красной.

**FPS drops? / Просадки FPS?**
No custom shaders: one `instanceof` + enchant-component read per piece per frame, one extra draw call per highlighted piece.
Без шейдеров: один `instanceof` + чтение компонента чар на слот в кадре, один лишний draw call на подсвеченный предмет.

## Roadmap / Планы

- [x] Per-enchant tint with own texture (armor + held)
- [x] Settings screen (vanilla, no ModMenu): item chips + search + color picker
- [x] In-game hotkey (H) + pause-menu button
- [ ] Remappable hotkey in controls settings
- [ ] Per-item rules (different color for the same enchant on different items)

## License / Лицензия

MIT — see [LICENSE](LICENSE). Fork it, learn from it, reuse with credit.
MIT — см. [LICENSE](LICENSE). Делай форки, учись, тащи код в свои моды с указанием авторства.

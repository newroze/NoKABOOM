# NoKABOOM

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-44bd32?style=flat-square&logo=minecraft&logoColor=white)
![Loader](https://img.shields.io/badge/Loader-Fabric_0.19.5-0097e6?style=flat-square)
![Side](https://img.shields.io/badge/Side-Client_only-e1b12c?style=flat-square)
![Fabric API](https://img.shields.io/badge/Fabric_API-not_required-8e44ad?style=flat-square)
![Java](https://img.shields.io/badge/Java-21+-red?style=flat-square)
![License](https://img.shields.io/github/license/newroze/NoKABOOM?style=flat-square)

> **RU:** клиентский PvP-мод — подсвечивает настраиваемым цветом те части брони и оружие в руках, на которых есть отслеживаемые зачарования (по умолчанию — **Blast Protection** и **Sharpness**).
> **EN:** client-side PvP mod — highlights armor pieces and held weapons carrying tracked enchantments (defaults: **Blast Protection** + **Sharpness**) with per-enchant custom colors.

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

- Per-piece highlight: only the enchanted item glows, never the whole player.
  Подсветка послотно: светится только зачарованный предмет, а не весь игрок.
- Soft "breathing" overlay over the armor; the material stays readable (diamond still looks like diamond).
  Мягкая «дышащая» плёнка поверх брони, материал остаётся узнаваемым.
- Optional fullbright — the mark reads even in the dark.
  Опциональный fullbright — метка читается даже в темноте.
- Works on **players** and **armor stands** (great for kit previews), plus held weapons.
  Работает на **игроках**, **стойках для брони** и оружии в руках.

```
Enemy seen in PvP / Противник в PvP:

  Helmet / Шлем        Fire Protection IV    normal / обычный
  Chestplate / Нагрудник  Blast Protection IV  RED PULSE / КРАСНАЯ ПУЛЬСАЦИЯ
  Leggings / Поножи     Protection IV         normal / обычный
  Boots / Ботинки       Blast Protection III   RED PULSE / КРАСНАЯ ПУЛЬСАЦИЯ
```

## Usage / Использование

Press **H** in-game or the **NoKABOOM...** button in the pause menu (Esc).
Нажми **H** в игре или кнопку **NoKABOOM...** в паузе (Esc).

- Search enchantments by id or name / поиск по зачарованиям (id или название).
- Per-enchant ON/OFF + custom color: hex field (`#FF2E2E`), RGB sliders, presets.
  Вкл/выкл каждого чара + свой цвет: hex-поле, RGB-слайдеры, пресеты.

## Configuration / Конфиг — `config/nokaboom.json`

Created automatically on first launch / создаётся автоматически при первом запуске.
Legacy `highlightRgb` (1.0.0) auto-migrates into the `minecraft:blast_protection` color.

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch / мастер-выключатель |
| `highlightPlayers` | `true` | Highlight on players / подсветка на игроках |
| `highlightArmorStands` | `true` | Highlight on armor stands / подсветка на стойках |
| `highlightHeldItems` | `true` | Held weapon glow / подсветка оружия в руках |
| `enchantments` | see below | Per-enchant toggle + color / вкл/выкл + цвет каждого чара |
| `minAlpha` / `maxAlpha` | `0x55` / `0xA0` | Pulse opacity range / границы пульсации |
| `pulseSpeed` | `2.5` | Pulse speed, 0 = static / скорость пульсации, 0 — статично |
| `expandScale` | `1.03` | Shell inflation vs z-fighting / раздутие плёнки |
| `heldBoxSize` | `0.55` | Held-item glow box size / размер бокса вокруг оружия |
| `fullbright` | `true` | Glow in the dark / свечение в темноте |

Defaults ON: `minecraft:blast_protection` (`#FF2E2E`), `minecraft:sharpness` (`#FF7A1A`).
По умолчанию включены: Blast Protection и Sharpness, остальные чары перечислены и выключены.

## How it works / Как устроено

```
ArmorFeatureRenderer.renderArmor()   <- vanilla draws armor per slot
        |  TAIL mixin injection
        v
colorFor(stack)                      <- enabled enchant from config?
        |  yes
        v
renderModel(same model x expand,     <- translucent shell in enchant color,
            white 1x1 texture,          pulsing ARGB, fullbright)
            pulsing ARGB, fullbright)

HeldItemFeatureRenderer.renderItem() <- vanilla draws held item
        v  TAIL mixin injection
submitCustom(box heldBoxSize,        <- pulsing box in enchant color
             vanilla hand transform)
```

- The enchant check is a pure function of `ItemStack` (`NoKABOOMConfig.colorFor`): no world/server access, render-thread safe.
  Проверка чара — чистая функция от `ItemStack`, без мира/сервера, безопасна в рендер-потоке.
- One armor injection point covers players and stands: both delegate armor to `ArmorFeatureRenderer`.
  Одна точка инъекции покрывает игроков и стойки.
- Client-only: no packets, the server never knows. Still, respect your server's rules.
  Только клиент: пакетов нет, сервер ничего не знает. Но сверяйся с правилами сервера.

## Project structure / Структура проекта

```
src/main/java/com/newroze/nokaboom/
├── NoKABOOM.java                          # MOD_ID + logger
├── NoKABOOMClient.java                    # ClientModInitializer: config load
├── config/
│   └── NoKABOOMConfig.java                # JSON config: enchants + colors + migration
├── util/
│   └── BlastProtectionChecker.java        # Blast Protection check helper
├── render/
│   ├── NoKABOOMArmorHighlight.java        # armor overlay: color + pulse
│   └── NoKABOOMHeldHighlight.java         # held item: box via submitCustom
├── gui/
│   ├── NoKABOOMConfigScreen.java          # vanilla screen: search + hex/RGB/presets
│   └── NoKABOOMKeybinds.java              # hotkey H without Fabric API (tick poll)
└── mixin/client/
    ├── ArmorFeatureRendererMixin.java     # TAIL renderArmor -> shell
    ├── HeldItemFeatureRendererMixin.java  # TAIL renderItem -> box
    ├── MinecraftClientMixin.java          # H polling in tick
    └── GameMenuScreenMixin.java           # NoKABOOM... button in pause menu

src/main/resources/
├── fabric.mod.json                        # manifest (client-only, no Fabric API)
├── nokaboom.mixins.json                   # mixins config
└── assets/nokaboom/
    ├── textures/highlight.png             # white 1x1, tinted by code
    └── icon.png                           # mod icon
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
./gradlew genSources    # Minecraft sources for IDE reading
```

## FAQ

**Cheats? Will I get banned? / Это читы? Меня забанят?**
The mod sends nothing and changes no mechanics — it only paints over what the client already sees.
Мод ничего не отправляет на сервер и не меняет механику — только рисует поверх того, что клиент и так видит. Но правила у серверов разные — уточни у администрации.

**Why Blast Protection and Sharpness? / Почему Blast Protection и Sharpness?**
Built for crystal PvP: instantly see the "blast" set and the sharp weapon. Everything else can be enabled in the settings screen (H).
Мод заточен под кристаллическое PvP: мгновенно видно «взрывной» сет и острую пушку. Остальное включается в настройках (H).

**FPS drops? / Просадки FPS?**
No: one `instanceof` + enchant-component read per piece per frame, overlay is one extra draw call per highlighted slot.
Нет: один `instanceof` + чтение компонента чар на слот в кадре, оверлей — один лишний draw call на подсвеченный слот.

## Roadmap / Планы

- [x] Per-enchant highlight (configurable list + colors)
- [x] Settings screen (vanilla, no ModMenu) + search
- [x] In-game hotkey toggle (H) + pause-menu button
- [ ] Remappable hotkey in controls settings

## License / Лицензия

MIT — see [LICENSE](LICENSE). Fork it, learn from it, reuse with credit.
MIT — см. [LICENSE](LICENSE). Делай форки, учись, тащи код в свои моды с указанием авторства.

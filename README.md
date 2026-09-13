# 💥 NoKABOOM

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-44bd32?style=flat-square&logo=minecraft&logoColor=white)
![Loader](https://img.shields.io/badge/Loader-Fabric_0.18.4-0097e6?style=flat-square)
![Side](https://img.shields.io/badge/Side-Client_only-e1b12c?style=flat-square)
![Fabric API](https://img.shields.io/badge/Fabric_API-not_required-8e44ad?style=flat-square)
![License](https://img.shields.io/github/license/newroze/NoKABOOM?style=flat-square)

> **RU:** клиентский PvP-мод — подсвечивает **красным** только те части брони на игроках и стойках, на которых есть **«Взрывоустойчивость» (Blast Protection)**.
> **EN:** client-side PvP mod — highlights in **red** only the armor pieces (on players & armor stands) enchanted with **Blast Protection**.

---

## ✨ Как это выглядит / What it looks like

Подсветка работает **послотно**: светится только зачарованный предмет, а не весь игрок.

The highlight is **per-piece**: only the enchanted item glows, never the whole player.

```
Противник / Enemy seen in PvP:

  Шлем      Fire Protection IV       ⬜  обычный / normal
  Нагрудник Blast Protection IV      🟥  КРАСНАЯ ПУЛЬСАЦИЯ / RED PULSE
  Поножи    Protection IV            ⬜  обычный / normal
  Ботинки   Blast Protection III     🟥  КРАСНАЯ ПУЛЬСАЦИЯ / RED PULSE
```

- 🌊 Мягкая «дышащая» красная плёнка поверх брони, материал брони остаётся узнаваемым (алмазка выглядит как алмазка).
- 🌙 Опциональный fullbright — метка читается даже в темноте.
- 🧍 Работает на **игроках** и на **стойках для брони** (удобно для витрин китов).

- 🌊 Soft "breathing" red film over the armor; the material stays readable (diamond still looks like diamond).
- 🌙 Optional fullbright — the mark reads even in the dark.
- 🧍 Works on **players** and **armor stands** (great for kit previews).

---

## 🚀 Установка / Installation

1. Установи [Fabric Loader](https://fabricmc.net/use/) **0.18.4+** для Minecraft **1.21.11** и Java **21+**.
   Install Fabric Loader **0.18.4+** for Minecraft **1.21.11** with Java **21+**.
2. Скачай `nokaboom-1.0.0.jar` из [Releases](https://github.com/newroze/NoKABOOM/releases) и положи в папку `mods`.
   Download `nokaboom-1.0.0.jar` from Releases into your `mods` folder.
3. **Fabric API не нужен** — мод зависит только от Loader. No Fabric API required.

---

## ⚙️ Конфиг / Config — `config/nokaboom.json`

Создаётся автоматически при первом запуске / created automatically on first launch.

| Параметр / Option | По умолчанию / Default | Что делает / What it does |
|---|---|---|
| `enabled` | `true` | Мастер-выключатель / master switch |
| `highlightPlayers` | `true` | Подсветка на игроках / highlight on players |
| `highlightArmorStands` | `true` | Подсветка на стойках / highlight on armor stands |
| `highlightRgb` | `0xFF2E2E` | Цвет плёнки (RGB) / film color |
| `minAlpha` / `maxAlpha` | `0x55` / `0xA0` | Границы пульсации прозрачности / pulse range |
| `pulseSpeed` | `2.5` | Скорость пульсации (0 — статично) / pulse speed (0 — static) |
| `expandScale` | `1.03` | Раздутие плёнки против z-fighting / shell inflation vs z-fighting |
| `fullbright` | `true` | Свечение в темноте / glow in the dark |

---

## 🧠 Как устроено / How it works

```
ArmorFeatureRenderer.renderArmor()   ← ванилла рисует броню послотно
        │  (1 вызов на слот: шлем / грудь / ноги / ботинки)
        ▼  TAIL-инъекция миксина
shouldHighlight(state, stack)        ← игрок/стойка? + есть Blast Prot?
        │  да
        ▼
renderModel(та же модель × 1.03,     ← красная полупрозрачная оболочка
            белая 1×1 текстура,
            пульсирующий ARGB, fullbright)
```

- Проверка чара — **чистая функция от `ItemStack`** (`util/BlastProtectionChecker`): сравнивается ключ реестра `minecraft:blast_protection`, без мира/сервера — безопасно в рендер-потоке.
- Одна точка инъекции покрывает и игроков, и стойки: оба рендера делегируют броню в `ArmorFeatureRenderer`.
- Мод **client-only**: сервер ничего не знает и не банит за «лишний пакет» — пакетов просто нет. Тем не менее, сверяйся с правилами сервера.

- The enchant check is a **pure function of `ItemStack`**: it compares the `minecraft:blast_protection` registry key — no world/server access, render-thread safe.
- One injection point covers players and stands, since both delegate armor to `ArmorFeatureRenderer`.
- **Client-only**: no packets, the server never knows. Still, respect your server's rules.

---

## 🗂️ Структура проекта / Project structure

```
src/main/java/com/newroze/nokaboom/
├── NoKABOOM.java                          # MOD_ID + логгер / id + logger
├── NoKABOOMClient.java                    # вход ClientModInitializer: загрузка конфига
├── config/
│   └── NoKABOOMConfig.java                # JSON-конфиг на Gson (без зависимостей)
├── util/
│   └── BlastProtectionChecker.java        # есть ли Blast Protection на предмете
├── render/
│   └── NoKABOOMArmorHighlight.java        # ЧТО светится и КАК: цвет, пульс, свет
└── mixin/client/
    └── ArmorFeatureRendererMixin.java     # КОГДА: TAIL renderArmor → красная оболочка

src/main/resources/
├── fabric.mod.json                        # манифест (client-only, без Fabric API)
├── nokaboom.mixins.json                   # конфиг миксинов
└── assets/nokaboom/
    ├── textures/highlight.png             # белая 1×1 — красится в красный кодом
    └── icon.png                           # иконка мода
```

---

## 🔨 Сборка из исходников / Build from source

```bash
# Нужны JDK 21+ (JAVA_HOME), остальное скачает Gradle
./gradlew build        # Linux/macOS
gradlew.bat build      # Windows
# Готовый jar: build/libs/nokaboom-1.0.0.jar
```

Полезное / useful:

```bash
./gradlew runClient     # тестовый клиент с модом
./gradlew genSources    # исходники Minecraft для чтения в IDE
```

---

## ❓ FAQ

**Это читы? Меня забанят?**
Мод ничего не отправляет на сервер и не меняет механику — только рисует плёнку поверх того, что клиент и так видит (чары брони видны в инвентаре и так). Но правила у серверов разные — уточни у администрации.

**Is this cheats? Will I get banned?**
The mod sends nothing and changes no mechanics — it only paints over what the client already sees. Server rules differ though, so check with staff.

**Почему подсвечивается только Blast Protection?**
Мод заточен под кристаллическое PvP: мгновенно видно, кто в «взрывном» сете. Другие чары — в [Roadmap](#-планы--roadmap).

**Просадки FPS?**
Нет: один `instanceof` + чтение NBT-компонента на кусок брони в кадре, оверлей — один лишний draw call на зачарованный слот.

---

## 🗺️ Планы / Roadmap

- [ ] Подсветка других чар (настраиваемый список + цвета)
- [ ] Экран настроек (ModMenu)
- [ ] Переключатель хоткеем в игре

---

## 📄 Лицензия / License

MIT — см. [LICENSE](LICENSE). Делай форки, учись, тащи код в свои моды с указанием авторства.

MIT — see [LICENSE](LICENSE). Fork it, learn from it, reuse with credit.

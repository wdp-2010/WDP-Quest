# WDP Quest Resource Pack

**✅ REBUILT - January 2026:** This resource pack has been completely rebuilt using the modern Minecraft 1.21+ Item Model system. It now creates custom items in the `wdp_quest` namespace instead of overriding vanilla items.

---

## 🚀 What is this?

This resource pack provides visual progress bar textures and models used by the WDP-Quest plugin to show per-quest progress items inside GUIs.

**Compatible with Minecraft 1.21.3** (Pack Format 34)

---

## How It Works

The plugin uses the **NEW Item Model system** (1.21+) with custom items:
- Each progress bar is a custom item in the `wdp_quest` namespace
- Shows 0% through 100% progress in 6 levels (0-5)
- **Green bars** for normal quests
- **Red bars** for hard quests
- Does NOT override any vanilla Minecraft items

## Item Model Names

| Item Model | Quest Type | Progress |
|-----------|------------|----------|
| `wdp_quest:progress_normal_0` | Normal | 0% (Empty) |
| `wdp_quest:progress_normal_1` | Normal | 20% |
| `wdp_quest:progress_normal_2` | Normal | 40% |
| `wdp_quest:progress_normal_3` | Normal | 60% |
| `wdp_quest:progress_normal_4` | Normal | 80% |
| `wdp_quest:progress_normal_5` | Normal | 100% (Full) |
| `wdp_quest:progress_hard_0` | Hard | 0% (Empty) |
| `wdp_quest:progress_hard_1` | Hard | 20% |
| `wdp_quest:progress_hard_2` | Hard | 40% |
| `wdp_quest:progress_hard_3` | Hard | 60% |
| `wdp_quest:progress_hard_4` | Hard | 80% |
| `wdp_quest:progress_hard_5` | Hard | 100% (Full) |

## File Structure

```
resourcepack/
├── pack.mcmeta                   # Pack format 34 for MC 1.21.3
└── assets/
    └── wdp_quest/                # Our custom namespace (does NOT override minecraft!)
        ├── items/                # Item definitions (NEW!)
        │   ├── progress_normal_0.json
        │   ├── progress_normal_1.json
        │   ├── ... (all 12 items)
        │   └── progress_hard_5.json
        ├── models/item/
        │   ├── progress_normal_0.json
        │   ├── progress_normal_1.json
        │   ├── ... (all 12 models)
        │   └── progress_hard_5.json
        └── textures/item/
            ├── progress_normal_0.png
            ├── progress_normal_1.png
            ├── ... (all 12 textures)
            └── progress_hard_5.png
```

## Installation

### Server Resource Pack

1. Create a ZIP of this folder (include pack.mcmeta and assets/)
2. Host on a web server
3. Configure `server.properties`:
   ```properties
   resource-pack=https://your-host.com/WDPQuest-ResourcePack-NEW.zip
   resource-pack-sha1=<sha1-of-zip>
   require-resource-pack=true
   ```

### Client Installation

1. ZIP the folder contents
2. Place in `.minecraft/resourcepacks/`
3. Enable in Options > Resource Packs

## Regenerating Textures

```bash
python3 generate_textures.py
```

Edit the script to customize colors, size, or appearance.

## Fallback

Without the resource pack, players see a text-based progress bar in the item lore:
```
[███░░] 60%
```

# WDP Quest Resource Pack

Custom resource pack for the WDP Quest plugin providing visual progress bars in inventory slots.

## How It Works

The plugin uses **Custom Model Data** on paper items to display progress bars:
- Each progress bar is a single inventory slot
- Shows 0/5 through 5/5 progress increments
- **Green bars** for normal quests
- **Red bars** for hard quests

## Custom Model Data Values

| CMD Value | Quest Type | Progress |
|-----------|------------|----------|
| 1000 | Normal | 0/5 (Empty) |
| 1001 | Normal | 1/5 |
| 1002 | Normal | 2/5 |
| 1003 | Normal | 3/5 |
| 1004 | Normal | 4/5 |
| 1005 | Normal | 5/5 (Full) |
| 1010 | Hard | 0/5 (Empty) |
| 1011 | Hard | 1/5 |
| 1012 | Hard | 2/5 |
| 1013 | Hard | 3/5 |
| 1014 | Hard | 4/5 |
| 1015 | Hard | 5/5 (Full) |

## File Structure

```
resourcepack/
├── pack.mcmeta
├── assets/
│   ├── minecraft/
│   │   └── models/item/
│   │       └── paper.json          # Overrides for paper item
│   └── wdp_quest/
│       ├── models/item/
│       │   ├── progress_normal_0.json
│       │   ├── progress_normal_1.json
│       │   ├── ...
│       │   ├── progress_hard_0.json
│       │   └── ...
│       └── textures/item/
│           ├── progress_normal_0.png
│           ├── progress_normal_1.png
│           ├── ...
│           ├── progress_hard_0.png
│           └── ...
```

## Installation

### Server Resource Pack

1. Create a ZIP of this folder (include pack.mcmeta and assets/)
2. Host on a web server
3. Configure `server.properties`:
   ```properties
   resource-pack=https://your-host.com/WDPQuest-ResourcePack.zip
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

# WDP Quest Resource Pack - Installation & Testing Guide

## 🎨 What Changed

The resource pack has been completely rebuilt using the **modern Minecraft 1.21+ Item Model system**. Instead of overriding vanilla items, it now creates **brand new custom items** in the `wdp_quest` namespace.

### Old System (BROKEN) ❌
- Overrode `minecraft:items/red_stained_glass_pane.json` and other vanilla items
- Used `custom_model_data` integers (1000, 1001, etc.)
- Conflicted with vanilla items

### New System (WORKING) ✅
- Creates new items in `assets/wdp_quest/items/`
- Uses `minecraft:item_model` component (modern 1.21+ way)
- Does NOT override any vanilla items
- Based on the working example from "WDP Custom Models & Textures.zip"

## 📁 Resource Pack Structure

```
resourcepack/
├── pack.mcmeta                          # Pack format 34 (MC 1.21.3)
└── assets/
    └── wdp_quest/                       # OUR custom namespace
        ├── items/                       # Item definitions (NEW!)
        │   ├── progress_normal_0.json   # 0% normal quest
        │   ├── progress_normal_1.json   # 20% normal quest
        │   ├── progress_normal_2.json   # 40% normal quest
        │   ├── progress_normal_3.json   # 60% normal quest
        │   ├── progress_normal_4.json   # 80% normal quest
        │   ├── progress_normal_5.json   # 100% normal quest
        │   ├── progress_hard_0.json     # 0% hard quest
        │   ├── progress_hard_1.json     # 20% hard quest
        │   ├── progress_hard_2.json     # 40% hard quest
        │   ├── progress_hard_3.json     # 60% hard quest
        │   ├── progress_hard_4.json     # 80% hard quest
        │   └── progress_hard_5.json     # 100% hard quest
        ├── models/item/                 # 3D models
        │   ├── progress_normal_0.json
        │   ├── progress_normal_1.json
        │   ├── ... (same as above)
        │   └── progress_hard_5.json
        └── textures/item/               # PNG textures
            ├── progress_normal_0.png
            ├── progress_normal_1.png
            ├── ... (same as above)
            └── progress_hard_5.png
```

## 🚀 Installation

1. **Copy the resource pack**:
   ```bash
   cp WDPQuest-ResourcePack-NEW.zip ~/.minecraft/resourcepacks/
   ```
   Or on a server: Copy to the server's world folder or use server resource pack hosting

2. **Enable in Minecraft**:
   - Open Minecraft
   - Go to Options → Resource Packs
   - Find "WDP Quest - Custom Progress Bar Items"
   - Click the arrow to move it to "Selected"
   - Click "Done"

3. **Deploy the plugin**:
   ```bash
   cd /root/WDP-Rework/WDP-Quest
   ./deploy.sh
   ```

## 🧪 Testing

### In-Game Test Commands

After loading the resource pack, test if the items render correctly:

```mcfunction
# Give yourself a normal progress bar at 0% (empty)
/give @p paper[minecraft:item_model="wdp_quest:progress_normal_0"]

# Give yourself a normal progress bar at 40% (2/5 filled)
/give @p paper[minecraft:item_model="wdp_quest:progress_normal_2"]

# Give yourself a normal progress bar at 100% (full)
/give @p paper[minecraft:item_model="wdp_quest:progress_normal_5"]

# Give yourself a hard quest progress bar at 60% (3/5 filled)
/give @p paper[minecraft:item_model="wdp_quest:progress_hard_3"]
```

### What You Should See

- **Without resource pack**: Plain paper items
- **With resource pack**: Custom textured progress bars
  - Green bars for normal quests
  - Red bars for hard quests  
  - Each bar shows different fill levels

### Common Issues

1. **Items show as paper**:
   - Resource pack not loaded or not activated
   - Check F3 screen bottom-right for enabled resource packs
   - Reload with F3+T

2. **Items invisible/broken**:
   - Wrong pack format (must be 34 for 1.21.3)
   - JSON syntax errors (use https://jsonlint.com/ to validate)

3. **Server doesn't update resource pack**:
   - Run `./deploy.sh` to copy files
   - Players need to download/accept the server resource pack
   - Or manually copy to `~/.minecraft/resourcepacks/`

## 📝 For Developers

### Item Model Format (1.21+)

Each item definition (`assets/wdp_quest/items/progress_normal_0.json`):
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "wdp_quest:item/progress_normal_0"
  }
}
```

### Model Format

Each model (`assets/wdp_quest/models/item/progress_normal_0.json`):
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "wdp_quest:item/progress_normal_0"
  }
}
```

### Giving Items in Java

```java
// NEW 1.21+ way (used in the plugin now)
ItemStack item = Bukkit.getItemFactory().createItemStack(
    "minecraft:paper[minecraft:item_model=\"wdp_quest:progress_normal_2\"]"
);
```

### References

- [Simplexity Custom Model Data Guide](https://github.com/Simplexity-Development/Custom_Model_Data_Guide)
- [Minecraft Wiki - Item Model Definition](https://minecraft.wiki/w/Items_model_definition)
- [Resource Locations](https://minecraft.wiki/w/Resource_location)

## ✅ Changes Made

1. ✅ Created 12 new item definitions in `assets/wdp_quest/items/`
2. ✅ Removed incorrect `assets/minecraft/items/` overrides
3. ✅ Updated Java code to use `item_model` component instead of `custom_model_data`
4. ✅ Pack format set to 34 (Minecraft 1.21.3)
5. ✅ All textures and models already in correct locations

## 🎯 Result

Your quest menu will now show proper progress bars without breaking any vanilla Minecraft items!

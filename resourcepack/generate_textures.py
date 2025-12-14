#!/usr/bin/env python3
"""
Generate progress bar textures for WDP Quest.
Creates textures compatible with Minecraft 1.21.6 (pack_format 63).

In 1.21.4+, item rendering uses the NEW item model definition system:
- assets/<namespace>/items/<item>.json - item model DEFINITION (selects which model based on conditions)
- assets/<namespace>/models/item/<model>.json - the actual 3D model
- assets/<namespace>/textures/item/<texture>.png - texture file

For custom_model_data overrides on existing items, we use 'range_dispatch' in the items/<item>.json file.
"""

from PIL import Image, ImageDraw
import os
import json
import shutil

# Colors (RGBA)
NORMAL_COLOR_BG = (50, 50, 50, 255)       # Dark gray background
NORMAL_COLOR_FILL = (76, 175, 80, 255)    # Green fill
HARD_COLOR_BG = (50, 50, 50, 255)         # Dark gray background  
HARD_COLOR_FILL = (244, 67, 54, 255)      # Red fill
BORDER_COLOR = (30, 30, 30, 255)          # Darker border
TRANSPARENT = (0, 0, 0, 0)


def create_progress_texture(color_type, fill_level):
    """
    Create a single progress bar segment texture.
    The bar stretches from edge to edge (0 to 15 in x).
    Bar is thin (4 pixels high) and vertically centered.
    """
    img = Image.new('RGBA', (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    
    if color_type == 'normal':
        bg_color = NORMAL_COLOR_BG
        fill_color = NORMAL_COLOR_FILL
    else:
        bg_color = HARD_COLOR_BG
        fill_color = HARD_COLOR_FILL
    
    # Bar dimensions - thin horizontal bar, extends 1px beyond edges on each side
    bar_top = 6
    bar_bottom = 10
    bar_left = -1  # Extend 1 pixel to the left
    bar_right = 17  # Extend 1 pixel to the right
    
    # Draw border
    draw.rectangle([bar_left, bar_top, bar_right - 1, bar_bottom - 1], fill=BORDER_COLOR)
    
    # Inner bar area
    inner_left = bar_left
    inner_right = bar_right
    inner_top = bar_top + 1
    inner_bottom = bar_bottom - 1
    
    # Draw background
    draw.rectangle([inner_left, inner_top, inner_right - 1, inner_bottom - 1], fill=bg_color)
    
    # Calculate fill width
    total_width = inner_right - inner_left
    fill_fraction = fill_level / 5.0
    fill_width = int(total_width * fill_fraction)
    
    if fill_level > 0 and fill_width > 0:
        draw.rectangle([inner_left, inner_top, inner_left + fill_width - 1, inner_bottom - 1], fill=fill_color)
    
    return img


def create_model_json(color_type, fill_level):
    """Create a model JSON file for the actual 3D model."""
    return {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"wdp_quest:item/progress_{color_type}_{fill_level}"
        }
    }


def create_glass_pane_item_definition(glass_name):
    """
    Create an item model DEFINITION file for 1.21.4+ using select.
    This goes in assets/minecraft/items/<glass_pane>.json
    
    Uses the select type with custom_model_data property to choose models.
    """
    # Build cases for select
    # Normal progress bars (1000-1005), Hard progress bars (1010-1015)
    cases = []
    
    # Normal progress bars
    for fill_level in range(6):
        cases.append({
            "when": str(1000 + fill_level),
            "model": {
                "type": "minecraft:model",
                "model": f"wdp_quest:item/progress_normal_{fill_level}"
            }
        })
    
    # Hard progress bars  
    for fill_level in range(6):
        cases.append({
            "when": str(1010 + fill_level),
            "model": {
                "type": "minecraft:model",
                "model": f"wdp_quest:item/progress_hard_{fill_level}"
            }
        })
    
    return {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:custom_model_data",
            "fallback": {
                "type": "minecraft:model",
                "model": f"minecraft:item/{glass_name}"
            },
            "cases": cases
        }
    }


def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Clean up old directories
    assets_dir = os.path.join(base_dir, 'assets')
    if os.path.exists(assets_dir):
        shutil.rmtree(assets_dir)
    
    # Create directories for new structure
    texture_dir = os.path.join(base_dir, 'assets/wdp_quest/textures/item')
    model_dir = os.path.join(base_dir, 'assets/wdp_quest/models/item')
    # NEW: items folder for item model definitions
    minecraft_items_dir = os.path.join(base_dir, 'assets/minecraft/items')
    
    os.makedirs(texture_dir, exist_ok=True)
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(minecraft_items_dir, exist_ok=True)
    
    color_types = ['normal', 'hard']
    fill_levels = range(6)
    
    texture_count = 0
    model_count = 0
    
    # Generate textures and models
    for color_type in color_types:
        for fill_level in fill_levels:
            # Create texture
            texture_filename = f'progress_{color_type}_{fill_level}.png'
            texture_filepath = os.path.join(texture_dir, texture_filename)
            
            img = create_progress_texture(color_type, fill_level)
            img.save(texture_filepath)
            texture_count += 1
            print(f'Created texture: {texture_filename}')
            
            # Create model JSON
            model_filename = f'progress_{color_type}_{fill_level}.json'
            model_filepath = os.path.join(model_dir, model_filename)
            
            model_data = create_model_json(color_type, fill_level)
            with open(model_filepath, 'w') as f:
                json.dump(model_data, f, indent=2)
            model_count += 1
            print(f'Created model: {model_filename}')
    
    # Create item definition files using the NEW 1.21.4+ format
    glass_types = [
        "gray_stained_glass_pane",
        "lime_stained_glass_pane",
        "green_stained_glass_pane",
        "orange_stained_glass_pane",
        "red_stained_glass_pane",
    ]
    
    for glass_name in glass_types:
        item_def = create_glass_pane_item_definition(glass_name)
        filepath = os.path.join(minecraft_items_dir, f'{glass_name}.json')
        with open(filepath, 'w') as f:
            json.dump(item_def, f, indent=2)
        print(f'Created item definition: {glass_name}.json')
    
    # Update pack.mcmeta with correct format for 1.21.6
    pack_meta = {
        "pack": {
            "pack_format": 63,
            "description": "§eWDP Quest §7- Progress Bar Textures"
        }
    }
    
    pack_meta_path = os.path.join(base_dir, 'pack.mcmeta')
    with open(pack_meta_path, 'w') as f:
        json.dump(pack_meta, f, indent=2)
    print('Updated pack.mcmeta to format 63')
    
    print(f'\n=== Summary ===')
    print(f'Generated {texture_count} texture files')
    print(f'Generated {model_count} model files')
    print(f'Generated {len(glass_types)} item definition files (NEW 1.21.4+ format)')
    print(f'\nCustom Model Data mapping (uses floats[0]):')
    print(f'  Normal quests: 1000.0 (empty) to 1005.0 (full)')
    print(f'  Hard quests:   1010.0 (empty) to 1015.0 (full)')


if __name__ == '__main__':
    main()

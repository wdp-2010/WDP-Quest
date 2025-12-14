# WDP-Quest

A comprehensive quest plugin for Paper 1.21+ servers with 250 balanced quests, integrated with WDP-Progress and SkillCoins.

## Features

- **250 Quests** across 5 categories based on server progress
- **Clean GUI** with progress bars, category navigation, and quest details
- **WDP-Progress Integration** - Quests unlock based on server progress percentage
- **SkillCoins/Vault Integration** - Rewards in coins and tokens
- **Multiple Objective Types** - Mine, Kill, Craft, Collect, Place, Smelt, Fish, Breed, Enchant, Trade, Level Up, Visit, Advancement, Custom
- **SQLite Database** - Persistent player progress
- **Multi-Objective Quests** - Some quests require multiple objectives
- **Cooldown System** - Optional quest cooldowns

## Requirements

- Paper 1.21+
- Java 21
- WDP-Progress 1.2.0+
- Vault
- AuraSkills (optional, for token rewards)

## Installation

1. Place the JAR in your `plugins/` folder
2. Ensure WDP-Progress and Vault are installed
3. Restart the server
4. Edit `plugins/WDP-Quest/config.yml` if needed

## Commands

### Player Commands
- `/quest` - Open the quest menu
- `/quest list` - List available quests
- `/quest active` - Show active quests
- `/quest completed` - Show completed quests
- `/quest track <quest>` - Track a specific quest
- `/quest abandon <quest>` - Abandon a quest
- `/quest info <quest>` - View quest details

### Admin Commands
- `/questadmin reload` - Reload configuration
- `/questadmin give <player> <quest>` - Give a quest to a player
- `/questadmin complete <player> <quest>` - Complete a quest for a player
- `/questadmin reset <player> [quest]` - Reset quest progress
- `/questadmin progress <player> <quest>` - View player's quest progress
- `/questadmin list` - List all quests

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `wdpquest.use` | Access quest menu | true |
| `wdpquest.admin` | Admin commands | op |
| `wdpquest.admin.reload` | Reload config | op |
| `wdpquest.admin.give` | Give quests | op |
| `wdpquest.admin.complete` | Complete quests | op |
| `wdpquest.admin.reset` | Reset progress | op |

## Quest Categories

Quests are organized by required server progress percentage:

| Category | Progress Range | Quest Count |
|----------|---------------|-------------|
| **Beginner** | 0-20% | 50 quests |
| **Early** | 20-40% | 50 quests |
| **Intermediate** | 40-60% | 50 quests |
| **Advanced** | 60-80% | 50 quests |
| **Expert** | 80-100% | 50 quests |

## Quest Types

- **Mining** - Break specific blocks
- **Combat** - Kill specific entities
- **Crafting** - Craft items
- **Collection** - Collect items in inventory
- **Building** - Place blocks
- **Smelting** - Smelt items in furnaces
- **Fishing** - Catch fish
- **Breeding** - Breed animals
- **Enchanting** - Enchant items
- **Trading** - Trade with villagers
- **Leveling** - Gain experience levels
- **Advancements** - Earn specific advancements
- **Custom** - Server-specific objectives

## Configuration

See `config.yml` for all configurable options including:
- Reward multipliers
- GUI settings (title, colors)
- Maximum active quests
- Quest cooldowns
- Messages

## API

WDP-Quest provides an API for other plugins:

```java
// Get the API
QuestAPI api = QuestAPI.getInstance();

// Check if player has completed a quest
boolean completed = api.hasCompletedQuest(player, "first_wood");

// Get player's quest progress
double progress = api.getQuestProgress(player, "first_wood");

// Start a quest for a player
api.startQuest(player, "first_wood");

// Complete a quest
api.completeQuest(player, "first_wood");

// Update an objective programmatically
api.updateObjective(player, "first_wood", "mine_wood", 5);
```

## Building

```bash
# Clone and build
cd WDP-Quest
mvn clean package

# Deploy
./deploy.sh /path/to/plugins
```

## File Structure

```
WDP-Quest/
├── src/main/java/com/wdp/quest/
│   ├── WDPQuestPlugin.java       # Main plugin class
│   ├── api/
│   │   └── QuestAPI.java         # Public API
│   ├── commands/
│   │   ├── QuestCommand.java     # Player commands
│   │   └── QuestAdminCommand.java # Admin commands
│   ├── config/
│   │   └── ConfigManager.java    # Configuration handling
│   ├── data/
│   │   ├── DatabaseManager.java  # SQLite database
│   │   └── PlayerQuestData.java  # Player data model
│   ├── gui/
│   │   ├── QuestMenuHandler.java # GUI creation
│   │   └── QuestMenuListener.java # Click handling
│   ├── integrations/
│   │   ├── EconomyIntegration.java # Vault/SkillCoins
│   │   └── ProgressIntegration.java # WDP-Progress
│   ├── listeners/
│   │   ├── PlayerEventListener.java # Player events
│   │   └── QuestObjectiveListener.java # Objective tracking
│   └── quest/
│       ├── Quest.java            # Quest model
│       ├── QuestCategory.java    # Category enum
│       ├── QuestManager.java     # Quest loading
│       ├── QuestObjective.java   # Objective model
│       ├── QuestRewards.java     # Rewards model
│       └── PlayerQuestManager.java # Player tracking
└── src/main/resources/
    ├── plugin.yml
    ├── config.yml
    └── quests/
        ├── beginner_quests.yml   # 50 quests (0-20%)
        ├── early_quests.yml      # 50 quests (20-40%)
        ├── intermediate_quests.yml # 50 quests (40-60%)
        ├── advanced_quests.yml   # 50 quests (60-80%)
        └── expert_quests.yml     # 50 quests (80-100%)
```

## Support

This plugin integrates with:
- **WDP-Progress** - Server progress tracking
- **SkillCoins** - Custom economy via AuraSkills
- **Vault** - Standard economy integration

## License

MIT License

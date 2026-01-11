# WDP-Quest Permissions Guide

Complete permission system documentation for WDP-Quest - the comprehensive quest plugin with 250+ quests across 5 categories.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Base Player Permissions](#base-player-permissions)
3. [Category Access Permissions](#category-access-permissions)
4. [Special Quest Permissions](#special-quest-permissions)
5. [Bypass Permissions](#bypass-permissions)
6. [Admin Permissions](#admin-permissions)
7. [Notification Permissions](#notification-permissions)
8. [Permission Examples](#permission-examples)
9. [Category System Explained](#category-system-explained)
10. [Best Practices](#best-practices)

---

## Overview

WDP-Quest uses an advanced permission system that allows control over:
- Which quests players can access
- Which categories are available
- Special quest types (daily, repeatable, hard)
- Administrative functions

**Default Behavior:**
- All players can access quests matching their server progress
- Category access is tied to server progress percentage
- Operators get full admin access

---

## Base Player Permissions

### Master Permission
```yaml
wdp.quest.use
```
- **Description:** Base access to the quest system
- **Default:** true
- **Includes:** view, start, track, abandon
- **Usage:** Remove to completely disable quest system for a group

### Individual Base Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `wdp.quest.view` | true | View quest menu and details |
| `wdp.quest.start` | true | Start new quests |
| `wdp.quest.track` | true | Track active quests |
| `wdp.quest.abandon` | true | Abandon active quests |
| `wdp.quest.list` | true | List available quests |
| `wdp.quest.info` | true | View detailed quest information |
| `wdp.quest.active` | true | View active quest list |
| `wdp.quest.completed` | true | View completed quest list |

---

## Category Access Permissions

WDP-Quest organizes 250 quests into 5 categories based on server progress percentage. Control which categories players can access:

### Individual Category Permissions

| Permission | Default | Progress Required | Quest Range |
|------------|---------|-------------------|-------------|
| `wdp.quest.category.starter` | true | 0-19% | ~50 beginner quests |
| `wdp.quest.category.novice` | true | 20-39% | ~50 early-game quests |
| `wdp.quest.category.intermediate` | true | 40-59% | ~50 mid-game quests |
| `wdp.quest.category.advanced` | true | 60-79% | ~50 late-game quests |
| `wdp.quest.category.expert` | true | 80-100% | ~50 end-game quests |

### Master Category Permission
```yaml
wdp.quest.category.*
```
- **Description:** Access ALL quest categories
- **Default:** op
- **Usage:** Give to VIP players for unrestricted access

**Example - Restrict access to only starter quests:**
```yaml
permissions:
  - wdp.quest.use
  - wdp.quest.category.starter
  - -wdp.quest.category.novice
  - -wdp.quest.category.intermediate
  - -wdp.quest.category.advanced
  - -wdp.quest.category.expert
```

---

## Special Quest Permissions

Control access to special quest types:

| Permission | Default | Description |
|------------|---------|-------------|
| `wdp.quest.repeatable` | true | Access repeatable quests |
| `wdp.quest.daily` | true | Access daily quest system |
| `wdp.quest.hard` | true | Access hard/challenge quests |

**Use Case:** Create VIP-only repeatable quests:
```yaml
# Regular players
permissions:
  - wdp.quest.use
  - -wdp.quest.repeatable

# VIP players
permissions:
  - wdp.quest.use
  - wdp.quest.repeatable
```

---

## Bypass Permissions

Skip restrictions and requirements:

### Master Bypass
```yaml
wdp.quest.bypass
```
- **Description:** Bypass ALL quest restrictions
- **Default:** op
- **Includes:** All bypass.* permissions

### Individual Bypass Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `wdp.quest.bypass.requirements` | op | Skip level/progress requirements |
| `wdp.quest.bypass.cooldown` | op | Skip quest cooldowns |
| `wdp.quest.bypass.progress` | op | Skip server progress requirements |
| `wdp.quest.bypass.category` | op | Access all categories regardless of progress |

**Testing Use Case:**
```yaml
# Quest tester role
permissions:
  - wdp.quest.use
  - wdp.quest.bypass.requirements
  - wdp.quest.bypass.cooldown
  - wdp.quest.category.*
```

---

## Admin Permissions

### Master Admin Permission
```yaml
wdp.quest.admin
```
- **Description:** Full admin access
- **Default:** op
- **Includes:** All admin.* and bypass.* permissions

### Individual Admin Permissions

| Permission | Default | Description | Command |
|------------|---------|-------------|---------|
| `wdp.quest.admin.reload` | op | Reload configuration | `/questadmin reload` |
| `wdp.quest.admin.give` | op | Give quests to players | `/questadmin give <player> <quest>` |
| `wdp.quest.admin.reset` | op | Reset player progress | `/questadmin reset <player>` |
| `wdp.quest.admin.complete` | op | Force-complete quests | `/questadmin complete <player> <quest>` |
| `wdp.quest.admin.setprogress` | op | Set quest progress | `/questadmin setprogress <player> <quest> <progress>` |
| `wdp.quest.admin.list` | op | List all quests | `/questadmin list` |
| `wdp.quest.admin.viewdata` | op | View player data | N/A |
| `wdp.quest.admin.debug` | op | Debug information | `/quest debug` |

---

## Notification Permissions

Receive notifications about player quest activities:

### Master Notification
```yaml
wdp.quest.notify
```
- **Description:** Receive all quest notifications
- **Default:** op

### Individual Notifications

| Permission | Default | Description |
|------------|---------|-------------|
| `wdp.quest.notify.start` | op | Player starts quests |
| `wdp.quest.notify.complete` | op | Player completes quests |
| `wdp.quest.notify.abandon` | op | Player abandons quests |

**Monitoring Use Case:**
```yaml
# Analytics/monitoring role
permissions:
  - wdp.quest.notify.complete
```

---

## Permission Examples

### Example 1: Quest Helper Role
Helpers who can view and assist but not modify:
```yaml
permissions:
  - wdp.quest.admin.viewdata
  - wdp.quest.admin.list
  - wdp.quest.notify
```

### Example 2: VIP Player
VIP with access to all categories and special quests:
```yaml
permissions:
  - wdp.quest.use
  - wdp.quest.category.*
  - wdp.quest.repeatable
  - wdp.quest.daily
  - wdp.quest.hard
  - wdp.quest.bypass.cooldown
```

### Example 3: Quest Moderator
Can give quests and reset progress but not modify quest data:
```yaml
permissions:
  - wdp.quest.admin.give
  - wdp.quest.admin.reset
  - wdp.quest.admin.complete
  - wdp.quest.admin.viewdata
```

### Example 4: Testing Team
Full access to test all quest content:
```yaml
permissions:
  - wdp.quest.admin
  - wdp.quest.bypass
  - wdp.quest.category.*
```

### Example 5: Limited Starter Player
New player restricted to starter category only:
```yaml
permissions:
  - wdp.quest.use
  - wdp.quest.category.starter
  - -wdp.quest.category.novice
  - -wdp.quest.category.intermediate
  - -wdp.quest.category.advanced
  - -wdp.quest.category.expert
  - -wdp.quest.daily
  - -wdp.quest.hard
```

---

## Category System Explained

WDP-Quest's category system is designed to gradually unlock content as players progress:

### How It Works
1. Player joins server at 0% progress
2. Only **Starter** category (0-19%) quests available
3. As player completes quests/levels skills, progress increases
4. At 20% progress, **Novice** category unlocks
5. Progression continues through all 5 categories

### Category Progression Table

| Category | Progress | Quest Count | Difficulty | Example Quests |
|----------|----------|-------------|------------|----------------|
| **Starter** | 0-19% | ~50 | Easy | "Mine 10 Stone", "Craft a Pickaxe" |
| **Novice** | 20-39% | ~50 | Medium | "Mine 100 Iron", "Reach Mining 10" |
| **Intermediate** | 40-59% | ~50 | Moderate | "Craft Diamond Armor", "Build a House" |
| **Advanced** | 60-79% | ~50 | Hard | "Kill the Ender Dragon", "Max a Skill" |
| **Expert** | 80-100% | ~50 | Very Hard | "Max All Skills", "Build a Town" |

### Overriding Categories

Use category permissions to override the progress-based system:

```yaml
# Force access to advanced quests for donors
permissions:
  - wdp.quest.category.advanced
  - wdp.quest.category.expert
```

---

## Best Practices

### 1. **Respect Progress Gating**
The category system is designed to prevent overwhelming new players. Only bypass for VIPs or testing.

### 2. **Use Category Permissions Sparingly**
Let the natural progress system work. Category permissions should be for special cases only.

### 3. **Monitor Quest Completion Rates**
Use notification permissions to track which quests are being completed/abandoned:
```yaml
# Analytics role
permissions:
  - wdp.quest.notify.complete
  - wdp.quest.notify.abandon
```

### 4. **Create Role-Based Groups**
```yaml
# Example LuckPerms groups
/lp creategroup questhelper
/lp group questhelper permission set wdp.quest.admin.viewdata true
/lp group questhelper permission set wdp.quest.admin.reset true
```

### 5. **Test Quest Changes**
Always use bypass permissions to test quests before releasing to players:
```yaml
# Testing environment
permissions:
  - wdp.quest.admin
  - wdp.quest.bypass
```

### 6. **Balance Repeatable Quests**
If using VIP-only repeatable quests, monitor for economy imbalance:
```yaml
# VIP repeatable access
permissions:
  - wdp.quest.repeatable
```

---

## Integration with Other Plugins

WDP-Quest integrates with several plugins. Permissions may interact:

### WDP-Progress Integration
- Quest category access is automatically gated by progress percentage
- Use `wdp.quest.bypass.progress` to override

### Vault/Economy Integration
- No special permissions needed
- Quest rewards handled automatically

### AuraSkills Integration
- Quest objectives track skill levels
- No special permissions needed

---

## Troubleshooting

### Problem: "You don't have access to this quest"
**Solutions:**
1. Check if player has `wdp.quest.use`
2. Verify category permission matches quest category
3. Check if progress requirement is met
4. Use `/quest info <questId>` to see requirements

### Problem: Player can't see any quests
**Solutions:**
1. Verify `wdp.quest.view` permission
2. Check if player progress is too low for available categories
3. Use `/questadmin list` to see all quests
4. Grant `wdp.quest.category.starter` explicitly

### Problem: Admin commands not working
**Solutions:**
1. Ensure player has specific `wdp.quest.admin.<command>` permission
2. Check command syntax: `/questadmin <command> [args]`
3. Verify plugin is loaded: `/plugins`

### Problem: VIP can access all categories but shouldn't
**Solutions:**
1. Check for `wdp.quest.category.*` permission
2. Verify no `wdp.quest.bypass.category` permission
3. Use `/lp user <player> permission check wdp.quest.category.*`

---

## Need Help?

- Check [README.md](README.md) for plugin information
- Review quest configuration in `quests/` folder
- Join our Discord: https://dsc.gg/wdp-server
- Report issues on GitHub

---

**Last Updated:** January 10, 2026
**Plugin Version:** 1.0.0+
**Document Version:** 1.0

#!/bin/bash

# Script to switch between navbar implementations
# Usage: ./switch_navbar.sh [hardcoded|config|hybrid]

set -e

cd /root/WDP-Rework/WDP-Quest

HARDCODED_FILE="src/main/java/com/wdp/quest/ui/QuestMenuHandler.java.hardcoded"
CONFIG_FILE="src/main/java/com/wdp/quest/ui/QuestMenuHandler.java"
BACKUP_FILE="src/main/java/com/wdp/quest/ui/QuestMenuHandler.java.backup"

if [ "$1" == "hardcoded" ]; then
    echo "=== SWITCHING TO HARDCODED NAVBAR (Original Good) ==="
    
    # Save current as backup first
    if [ -f "$CONFIG_FILE" ]; then
        cp "$CONFIG_FILE" "$BACKUP_FILE"
        echo "✓ Backed up current version"
    fi
    
    # Copy hardcoded to current
    if [ ! -f "$HARDCODED_FILE" ]; then
        echo "Creating hardcoded backup from git..."
        git show 2420931:src/main/java/com/wdp/quest/ui/QuestMenuHandler.java > "$HARDCODED_FILE"
    fi
    
    cp "$HARDCODED_FILE" "$CONFIG_FILE"
    echo "✓ Switched to hardcoded navbar"
    
elif [ "$1" == "config" ]; then
    echo "=== SWITCHING TO CONFIG-BASED NAVBAR ==="
    
    # Restore from backup if exists
    if [ -f "$BACKUP_FILE" ]; then
        cp "$BACKUP_FILE" "$CONFIG_FILE"
        echo "✓ Restored from backup"
    else
        echo "No backup found. Current version is already config-based."
    fi
    
elif [ "$1" == "hybrid" ]; then
    echo "=== CURRENT: HYBRID NAVBAR (Config + Hardcoded Fallback) ==="
    echo "This is the recommended setup that uses navbar.yml with fallbacks."
    
else
    echo "Usage: $0 [hardcoded|config|hybrid]"
    echo ""
    echo "  hardcoded - Pure hardcoded navbar (original good version)"
    echo "  config    - Full config-based navbar (complex system)"
    echo "  hybrid    - Config with hardcoded fallbacks (RECOMMENDED)"
    echo ""
    echo "Current status:"
    if [ -f "$CONFIG_FILE" ]; then
        if grep -q "applyNavbar" "$CONFIG_FILE"; then
            echo "  ✓ Hybrid mode (uses navbar.yml)"
        elif grep -q "UnifiedMenuManager" "$CONFIG_FILE"; then
            echo "  ✗ Config mode (complex system)"
        else
            echo "  ✓ Hardcoded mode (original good)"
        fi
    fi
    
    echo ""
    echo "To test compilation: mvn clean compile"
    echo "To package: mvn clean package"
    exit 1
fi

echo ""
echo "File info:"
ls -lh "$CONFIG_FILE"
echo ""
echo "Lines: $(wc -l < "$CONFIG_FILE")"

echo ""
echo "Ready to compile. Run: mvn clean compile"
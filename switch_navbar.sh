#!/bin/bash

# Script to switch between navbar implementations
# Usage: ./switch_navbar.sh [backup|current|save-current]

set -e

cd /root/WDP-Rework/WDP-Quest

BACKUP_FILE="src/main/java/com/wdp/quest/ui/QuestMenuHandler.java.bak"
CURRENT_FILE="src/main/java/com/wdp/quest/ui/QuestMenuHandler.java"
TEMP_FILE="src/main/java/com/wdp/quest/ui/QuestMenuHandler.java.temp"

if [ "$1" == "backup" ]; then
    echo "=== SWITCHING TO BACKUP (Original Good Navbar) ==="
    
    # First, save current version if it's different from backup
    if [ -f "$CURRENT_FILE" ] && [ -f "$BACKUP_FILE" ]; then
        if ! diff -q "$CURRENT_FILE" "$BACKUP_FILE" > /dev/null 2>&1; then
            echo "Saving current version to temp..."
            cp "$CURRENT_FILE" "$TEMP_FILE"
        fi
    fi
    
    # Copy backup to current
    if [ ! -f "$BACKUP_FILE" ]; then
        echo "Error: Backup file not found at $BACKUP_FILE"
        exit 1
    fi
    cp "$BACKUP_FILE" "$CURRENT_FILE"
    echo "✓ Switched to backup version"
    
elif [ "$1" == "current" ]; then
    echo "=== SWITCHING TO CURRENT (Complex Navbar) ==="
    
    # Check if we have a saved current version
    if [ -f "$TEMP_FILE" ]; then
        echo "Restoring from temp file..."
        cp "$TEMP_FILE" "$CURRENT_FILE"
        rm "$TEMP_FILE"
        echo "✓ Switched to current version from temp"
    else
        # Try git as fallback
        echo "No temp file found, checking git history..."
        LATEST_COMMIT=$(git log -1 --oneline -- src/main/java/com/wdp/quest/ui/QuestMenuHandler.java | cut -d' ' -f1 2>/dev/null || echo "")
        if [ -n "$LATEST_COMMIT" ]; then
            echo "Restoring from commit $LATEST_COMMIT"
            git show "$LATEST_COMMIT:src/main/java/com/wdp/quest/ui/QuestMenuHandler.java" > "$CURRENT_FILE"
            echo "✓ Switched to current version from git"
        else
            echo "Error: Could not find current version (no temp file, no git history)"
            exit 1
        fi
    fi
    
elif [ "$1" == "save-current" ]; then
    echo "=== SAVING CURRENT VERSION AS BACKUP ==="
    if [ ! -f "$CURRENT_FILE" ]; then
        echo "Error: Current file not found"
        exit 1
    fi
    cp "$CURRENT_FILE" "$BACKUP_FILE"
    echo "✓ Current version saved as backup"
    
else
    echo "Usage: $0 [backup|current|save-current]"
    echo ""
    echo "  backup       - Switch to original good navbar (hardcoded)"
    echo "  current      - Switch back to complex navbar (config-based)"
    echo "  save-current - Save current version as backup before switching"
    echo ""
    echo "Example workflow:"
    echo "  1. ./switch_navbar.sh save-current  # Save current state"
    echo "  2. ./switch_navbar.sh backup        # Switch to good navbar"
    echo "  3. ./gradlew clean build -x test    # Test compilation"
    echo "  4. ./switch_navbar.sh current       # Switch back if needed"
    exit 1
fi

echo ""
echo "File info:"
ls -lh "$CURRENT_FILE"
echo ""
echo "Lines: $(wc -l < "$CURRENT_FILE")"

echo ""
echo "Ready to compile. Run: ./gradlew clean build -x test"
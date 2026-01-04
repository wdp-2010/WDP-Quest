#!/bin/bash

# WDP-Quest Deployment Script
# Builds and deploys the quest plugin to the Minecraft server

# === CONFIGURATION - edit these for your environment ===
CONTAINER_ID="b8f24891-b5be-4847-a96e-c705c500aece"
SERVER_DIR="/var/lib/pterodactyl/volumes/b8f24891-b5be-4847-a96e-c705c500aece"
PLUGINS_DIR="${SERVER_DIR}/plugins"
FILE_OWNER="pterodactyl:pterodactyl"
JAR_NAME="WDP Quest"  # Note: The actual JAR has a space in the name
PLUGIN_NAME="WDPQuest"  # Plugin internal name for verification
# === End configuration ===

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    print_error "This script must be run as root"
    exit 1
fi

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR" || exit 1

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║            WDP-Quest Deployment Script                     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Step 1: Build the project
print_step "Building WDP-Quest with Maven..."
MVN_LOG="/tmp/wdp_quest_build_$(date +%s).log"

mvn clean package -DskipTests > "$MVN_LOG" 2>&1
BUILD_EXIT=$?

if [ $BUILD_EXIT -ne 0 ]; then
    print_error "Maven build failed! Check $MVN_LOG"
    echo ""
    echo "Last 50 lines of build log:"
    tail -n 50 "$MVN_LOG"
    exit 1
fi
print_success "Maven build completed"

# Step 2: Find the built JAR
print_step "Locating built JAR..."
# Look for the shaded JAR (includes all dependencies)
JAR_FILE=$(find "$PROJECT_DIR/target" -name "*-SNAPSHOT.jar" -not -name "*-sources*" -not -name "*-javadoc*" -not -name "original-*" | head -1)

if [ -z "$JAR_FILE" ]; then
    print_error "Could not find built JAR file in target/"
    exit 1
fi

JAR_BASENAME=$(basename "$JAR_FILE")
print_success "Found JAR: ${JAR_BASENAME}"

# Step 3: Check server paths
if [ ! -d "$PLUGINS_DIR" ]; then
    print_warning "Plugins directory not found: ${PLUGINS_DIR}"
    print_warning "Creating directory..."
    mkdir -p "$PLUGINS_DIR"
fi

# Step 4: Stop container (if running)
print_step "Stopping Pterodactyl container ${CONTAINER_ID}..."

if docker ps -q --filter "id=${CONTAINER_ID}" | grep -q .; then
    docker stop "$CONTAINER_ID" > /dev/null 2>&1
    
    # Wait for container to fully stop
    STOP_TIMEOUT=30
    STOP_COUNTER=0
    
    while docker ps -q --filter "id=${CONTAINER_ID}" | grep -q .; do
        sleep 1
        STOP_COUNTER=$((STOP_COUNTER + 1))
        
        if [ $STOP_COUNTER -ge $STOP_TIMEOUT ]; then
            print_warning "Container did not stop within ${STOP_TIMEOUT}s, forcing..."
            docker kill "$CONTAINER_ID" > /dev/null 2>&1
            sleep 2
            break
        fi
        
        if [ $((STOP_COUNTER % 5)) -eq 0 ]; then
            echo -ne "  Waiting... ${STOP_COUNTER}s\r"
        fi
    done
    
    print_success "Container stopped (took ${STOP_COUNTER}s)"
else
    print_warning "Container was not running"
fi

# Extra safety wait
sleep 2

# Step 5: Remove old JAR and copy new one
print_step "Removing old plugin JAR..."
rm -f "${PLUGINS_DIR}/${JAR_NAME}"*.jar 2>/dev/null
print_success "Old JAR removed"

print_step "Copying new plugin JAR..."
cp "$JAR_FILE" "${PLUGINS_DIR}/"

if [ $? -eq 0 ] && [ -f "${PLUGINS_DIR}/${JAR_BASENAME}" ]; then
    print_success "JAR copied to ${PLUGINS_DIR}/"
else
    print_error "Failed to copy JAR file"
    exit 1
fi

# Step 6: Set proper ownership
print_step "Setting file ownership..."
chown $FILE_OWNER "${PLUGINS_DIR}/${JAR_BASENAME}" 2>/dev/null || print_warning "Failed to chown (may need to adjust FILE_OWNER)"
print_success "Ownership set"

# Step 6.5: Deploy resource pack
print_step "Deploying resource pack..."
RESOURCEPACK_SOURCE="${PROJECT_DIR}/resourcepack"
RESOURCEPACK_DIR="${PLUGINS_DIR}/ResourcePackManager/mixer"

if [ -d "$RESOURCEPACK_SOURCE" ]; then
    # Create the resource pack ZIP if it doesn't exist
    if [ ! -f "${RESOURCEPACK_SOURCE}/WDPQuest-ResourcePack.zip" ]; then
        print_step "Creating resource pack ZIP..."
        cd "$RESOURCEPACK_SOURCE"
        zip -q -r WDPQuest-ResourcePack.zip pack.mcmeta assets/
        cd "$PROJECT_DIR"
    fi
    
    # Deploy to ResourcePackManager mixer directory
    if [ -d "$RESOURCEPACK_DIR" ]; then
        cp "${RESOURCEPACK_SOURCE}/WDPQuest-ResourcePack.zip" "$RESOURCEPACK_DIR/"
        chown $FILE_OWNER "${RESOURCEPACK_DIR}/WDPQuest-ResourcePack.zip" 2>/dev/null
        print_success "Resource pack deployed to mixer directory"
    else
        print_warning "ResourcePackManager mixer directory not found, skipping resource pack deployment"
    fi
else
    print_warning "Resource pack source not found, skipping"
fi

# Step 7: Display file info
print_step "Deployment summary:"
echo "  Plugin JAR: ${PLUGINS_DIR}/${JAR_BASENAME}"
ls -lh "${PLUGINS_DIR}/${JAR_BASENAME}" 2>/dev/null | awk '{print "  Size: " $5}'

# Step 8: Start the container
print_step "Starting Pterodactyl container..."
docker start "$CONTAINER_ID" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    print_success "Container start command executed"
else
    print_warning "Failed to execute container start command (container may not exist)"
fi

# Step 9: Verify container is running
print_step "Verifying container startup..."
START_TIMEOUT=15
START_COUNTER=0

while ! docker ps -q --filter "id=${CONTAINER_ID}" | grep -q .; do
    sleep 1
    START_COUNTER=$((START_COUNTER + 1))
    
    if [ $START_COUNTER -ge $START_TIMEOUT ]; then
        print_warning "Container did not start within ${START_TIMEOUT}s"
        print_warning "Check container logs with: docker logs ${CONTAINER_ID}"
        break
    fi
    
    if [ $((START_COUNTER % 2)) -eq 0 ]; then
        echo -ne "  Waiting for container... ${START_COUNTER}s\r"
    fi
done

if docker ps -q --filter "id=${CONTAINER_ID}" | grep -q .; then
    print_success "Container is running (verified in ${START_COUNTER}s)"
    
    # Step 10: Wait for plugin to load and verify
    print_step "Waiting for plugin to load (this may take 30-60 seconds)..."
    VERIFY_TIMEOUT=60
    VERIFY_COUNTER=0
    PLUGIN_LOADED=false
    
    while [ $VERIFY_COUNTER -lt $VERIFY_TIMEOUT ]; do
        sleep 2
        VERIFY_COUNTER=$((VERIFY_COUNTER + 2))
        
        # Check if plugin is loaded in logs
        if docker logs "$CONTAINER_ID" 2>&1 | grep -q "\[${PLUGIN_NAME}\] Enabling ${PLUGIN_NAME}"; then
            PLUGIN_LOADED=true
            break
        fi
        
        if [ $((VERIFY_COUNTER % 10)) -eq 0 ]; then
            echo -ne "  Waiting for plugin load... ${VERIFY_COUNTER}s\r"
        fi
    done
    
    echo "" # New line after waiting message
    
    if [ "$PLUGIN_LOADED" = true ]; then
        # Get quest count from logs
        QUEST_COUNT=$(docker logs "$CONTAINER_ID" 2>&1 | grep -o "Loaded [0-9]* quests" | tail -1)
        print_success "Plugin loaded successfully! (${QUEST_COUNT:-Quest system active})"
        
        # Check for any errors
        ERROR_COUNT=$(docker logs "$CONTAINER_ID" 2>&1 | grep -i "\[${PLUGIN_NAME}\]" | grep -i "error" | wc -l)
        if [ "$ERROR_COUNT" -gt 0 ]; then
            print_warning "Found ${ERROR_COUNT} error(s) in logs - check console for details"
        fi
    else
        print_warning "Could not verify plugin load within ${VERIFY_TIMEOUT}s"
        print_warning "Plugin may still be loading - check logs with: docker logs ${CONTAINER_ID}"
    fi
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          DEPLOYMENT COMPLETED SUCCESSFULLY!                ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "Next steps:"
echo -e "  1. Wait for server to fully start (check Pterodactyl console)"
echo -e "  2. Join the server and test with: ${YELLOW}/quest${NC}"
echo -e "  3. Admin commands: ${YELLOW}/questadmin reload${NC}"
echo ""
echo -e "Plugin Dependencies:"
echo -e "  - Paper 1.21.x"
echo -e "  - WDP-Progress 1.2.0+"
echo -e "  - Vault"
echo -e "  - AuraSkills (optional, for token rewards)"
echo ""
echo -e "Log files:"
echo -e "  Build log: ${YELLOW}${MVN_LOG}${NC}"
echo -e "  Server log: ${YELLOW}${SERVER_DIR}/logs/latest.log${NC}"
echo ""

exit 0

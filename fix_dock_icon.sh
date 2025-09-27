#!/bin/bash

# YunFx AutoShell Dock Icon Fix Script
# Helps identify and fix dock icon issues

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    echo -e "${PURPLE}$1${NC}"
}

print_header "🔧 YunFx AutoShell Dock Icon Fix"
echo "======================================"

print_status "This script will help identify and fix dock icon issues."
echo ""

# Check for any pinned shortcuts or desktop files
print_status "1. Checking for pinned shortcuts and desktop files..."
DESKTOP_FILES=(
    "$HOME/Desktop/yunfx-autoshell.desktop"
    "$HOME/.local/share/applications/yunfx-autoshell.desktop"
    "/usr/share/applications/yunfx-autoshell.desktop"
)

for file in "${DESKTOP_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_warning "Found desktop file: $file"
        echo "Icon path in file:"
        grep "Icon=" "$file" || echo "  No Icon= line found"
        echo ""
    fi
done

# Check for any running processes with old icons
print_status "2. Checking for running YunFx AutoShell processes..."
RUNNING_PROCESSES=$(pgrep -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" 2>/dev/null || true)
if [ -n "$RUNNING_PROCESSES" ]; then
    print_warning "Found running processes:"
    echo "$RUNNING_PROCESSES"
    echo ""
    print_status "Stopping running processes..."
    pkill -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" || true
    sleep 2
    print_success "Processes stopped"
else
    print_success "No running processes found"
fi

# Check icon files
print_status "3. Checking icon files..."
ICON_FILES=(
    "$HOME/.local/share/icons/yunfx-autoshell.png"
    "/usr/share/pixmaps/yunfx-autoshell.png"
)

for icon in "${ICON_FILES[@]}"; do
    if [ -f "$icon" ]; then
        print_success "Found icon: $icon"
        echo "  Size: $(stat -c%s "$icon") bytes"
        echo "  Modified: $(stat -c%y "$icon")"
    else
        print_warning "Icon not found: $icon"
    fi
done

# Check GNOME favorites
print_status "4. Checking GNOME favorites..."
FAVORITES=$(gsettings get org.gnome.shell favorite-apps 2>/dev/null || echo "[]")
if echo "$FAVORITES" | grep -q "yunfx"; then
    print_warning "YunFx AutoShell found in GNOME favorites"
    echo "Current favorites: $FAVORITES"
else
    print_success "YunFx AutoShell not in GNOME favorites (this is normal)"
fi

# Force refresh everything
print_status "5. Performing complete refresh..."
update-desktop-database ~/.local/share/applications 2>/dev/null || true
update-desktop-database /usr/share/applications 2>/dev/null || true
gtk-update-icon-cache -f -t ~/.local/share/icons 2>/dev/null || true
gtk-update-icon-cache -f -t /usr/share/icons 2>/dev/null || true
gtk-update-icon-cache -f -t /usr/share/pixmaps 2>/dev/null || true
killall -HUP gnome-shell 2>/dev/null || true
print_success "Refresh completed"

# Check if the issue might be a pinned shortcut
print_status "6. Checking for potential solutions..."
echo ""
print_warning "If you still see an old icon in your dock, try these steps:"
echo ""
echo "1. Right-click on the old icon in your dock and select 'Remove from Favorites'"
echo "2. Search for 'autoshell' or 'yunfx' in your application launcher"
echo "3. Right-click on the correct app and select 'Add to Favorites'"
echo "4. If that doesn't work, try logging out and back in"
echo ""
print_status "The new icon should be the red Java coffee cup with green '>' symbol."

# Offer to add to favorites
echo ""
read -p "Would you like to add YunFx AutoShell to your favorites now? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_status "Adding YunFx AutoShell to favorites..."
    CURRENT_FAVORITES=$(gsettings get org.gnome.shell favorite-apps)
    NEW_FAVORITES=$(echo "$CURRENT_FAVORITES" | sed 's/]$/, "yunfx-autoshell.desktop"]/')
    gsettings set org.gnome.shell favorite-apps "$NEW_FAVORITES"
    print_success "Added to favorites"
fi

echo ""
print_header "🎉 Dock icon fix completed!"
echo ""
print_status "The dock should now display the correct YunFx AutoShell icon."
print_status "If the issue persists, the old icon might be from a different application or a custom shortcut."

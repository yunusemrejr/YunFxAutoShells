#!/bin/bash

# YunFx AutoShell System Icon Update Script
# Run this with sudo to fix the system icon

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

print_header "🔄 YunFx AutoShell System Icon Update"
echo "============================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    print_error "This script must be run with sudo"
    echo "Usage: sudo $0"
    exit 1
fi

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

print_status "Updating system icon from: $SCRIPT_DIR/media/icon.png"

# Copy the correct icon
print_status "Copying correct icon to system location..."
cp "$SCRIPT_DIR/media/icon.png" "/usr/share/pixmaps/yunfx-autoshell.png"
print_success "System icon updated"

# Update desktop entry with absolute path
print_status "Updating desktop entry..."
cat > "/usr/share/applications/yunfx-autoshell.desktop" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=YunFx AutoShell
Comment=JavaFX Script Manager for Linux
Exec=/usr/local/bin/yunfx-autoshell
Icon=/usr/share/pixmaps/yunfx-autoshell.png
Terminal=false
StartupNotify=true
Categories=Development;System;Utility;
Keywords=autoshell;yunfx;script;shell;automation;java;javafx;
StartupWMClass=YunFx AutoShell
MimeType=application/x-shellscript;
EOF
print_success "Desktop entry updated"

# Update desktop database
print_status "Updating desktop database..."
update-desktop-database "/usr/share/applications"
print_success "Desktop database updated"

# Update icon cache
print_status "Updating icon cache..."
gtk-update-icon-cache -f -t "/usr/share/pixmaps"
gtk-update-icon-cache -f -t "/usr/share/icons"
print_success "Icon cache updated"

print_header "🎉 System icon update completed!"
echo ""
print_status "The application menu should now show the correct YunFx AutoShell icon."
print_status "You may need to refresh your application launcher or restart your desktop environment."
echo ""
print_success "Icon file: /usr/share/pixmaps/yunfx-autoshell.png"
print_success "Desktop entry: /usr/share/applications/yunfx-autoshell.desktop"

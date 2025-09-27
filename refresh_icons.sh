#!/bin/bash

# YunFx AutoShell Icon Refresh Script
# Forces desktop environment to use the correct icon

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

print_header "🔄 YunFx AutoShell Icon Refresh"
echo "======================================"

print_status "This script will force refresh all icon caches and desktop databases"
echo "to ensure the correct YunFx AutoShell icon is displayed."
echo ""

# Update desktop databases
print_status "1. Updating desktop databases..."
update-desktop-database ~/.local/share/applications 2>/dev/null || true
update-desktop-database /usr/share/applications 2>/dev/null || true
print_success "Desktop databases updated"

# Update icon caches
print_status "2. Updating icon caches..."
gtk-update-icon-cache -f -t ~/.local/share/icons 2>/dev/null || true
gtk-update-icon-cache -f -t /usr/share/icons 2>/dev/null || true
gtk-update-icon-cache -f -t /usr/share/pixmaps 2>/dev/null || true
print_success "Icon caches updated"

# Update MIME database
print_status "3. Updating MIME database..."
update-mime-database ~/.local/share/mime 2>/dev/null || true
print_success "MIME database updated"

# XDG desktop menu refresh
if command -v xdg-desktop-menu &> /dev/null; then
    print_status "4. Refreshing XDG desktop menu..."
    xdg-desktop-menu forceupdate 2>/dev/null || true
    print_success "XDG desktop menu refreshed"
fi

# Desktop environment specific refresh
print_status "5. Refreshing desktop environment..."
case "$XDG_CURRENT_DESKTOP" in
    *GNOME*|*gnome*)
        print_status "Refreshing GNOME desktop..."
        gsettings set org.gnome.desktop.background show-desktop-icons true 2>/dev/null || true
        killall -HUP gnome-shell 2>/dev/null || true
        ;;
    *KDE*|*kde*)
        print_status "Refreshing KDE desktop..."
        kbuildsycoca5 2>/dev/null || true
        killall -HUP plasmashell 2>/dev/null || true
        ;;
    *XFCE*|*xfce*)
        print_status "Refreshing XFCE desktop..."
        xfce4-panel -r 2>/dev/null || true
        ;;
    *CINNAMON*|*cinnamon*)
        print_status "Refreshing Cinnamon desktop..."
        killall -HUP cinnamon 2>/dev/null || true
        ;;
    *MATE*|*mate*)
        print_status "Refreshing MATE desktop..."
        killall -HUP mate-panel 2>/dev/null || true
        ;;
    *)
        print_status "Generic desktop refresh..."
        killall -HUP gtk-launch 2>/dev/null || true
        killall -HUP gtk-application 2>/dev/null || true
        ;;
esac

print_success "Desktop environment refreshed"

# Check if YunFx AutoShell icon exists
print_status "6. Verifying icon files..."
if [ -f ~/.local/share/icons/yunfx-autoshell.png ]; then
    print_success "User icon found: ~/.local/share/icons/yunfx-autoshell.png"
elif [ -f /usr/share/pixmaps/yunfx-autoshell.png ]; then
    print_success "System icon found: /usr/share/pixmaps/yunfx-autoshell.png"
else
    print_warning "No YunFx AutoShell icon found. Please run the install script first."
fi

# Check desktop entries
print_status "7. Verifying desktop entries..."
if [ -f ~/.local/share/applications/yunfx-autoshell.desktop ]; then
    print_success "User desktop entry found: ~/.local/share/applications/yunfx-autoshell.desktop"
elif [ -f /usr/share/applications/yunfx-autoshell.desktop ]; then
    print_success "System desktop entry found: /usr/share/applications/yunfx-autoshell.desktop"
else
    print_warning "No YunFx AutoShell desktop entry found. Please run the install script first."
fi

echo ""
print_header "🎉 Icon refresh completed!"
echo ""
print_status "The desktop environment should now display the correct YunFx AutoShell icon."
print_status "If the icon still doesn't appear correctly, try:"
echo "  • Logging out and back in"
echo "  • Restarting your desktop environment"
echo "  • Running: sudo ./install.sh (for system-wide refresh)"
echo ""
print_success "You can now search for 'autoshell' or 'yunfx' in your applications menu."

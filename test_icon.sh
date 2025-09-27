#!/bin/bash

# YunFx AutoShell Icon Test Script
# Tests and displays the correct icon

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

print_header "🧪 YunFx AutoShell Icon Test"
echo "================================"

print_status "Testing icon files and desktop entries..."
echo ""

# Check project icon
print_status "1. Checking project icon..."
PROJECT_ICON="media/icon.png"
if [ -f "$PROJECT_ICON" ]; then
    print_success "Project icon found: $PROJECT_ICON"
    echo "  Size: $(stat -c%s "$PROJECT_ICON") bytes"
    echo "  Modified: $(stat -c%y "$PROJECT_ICON")"
else
    print_error "Project icon not found: $PROJECT_ICON"
fi

# Check user desktop entry
print_status "2. Checking user desktop entry..."
USER_DESKTOP="$HOME/.local/share/applications/yunfx-autoshell.desktop"
if [ -f "$USER_DESKTOP" ]; then
    print_success "User desktop entry found: $USER_DESKTOP"
    echo "  Icon path: $(grep "Icon=" "$USER_DESKTOP" | cut -d'=' -f2)"
    if grep -q "Icon=" "$USER_DESKTOP"; then
        ICON_PATH=$(grep "Icon=" "$USER_DESKTOP" | cut -d'=' -f2)
        if [ -f "$ICON_PATH" ]; then
            print_success "Icon file exists: $ICON_PATH"
        else
            print_warning "Icon file not found: $ICON_PATH"
        fi
    fi
else
    print_warning "User desktop entry not found: $USER_DESKTOP"
fi

# Check system desktop entry
print_status "3. Checking system desktop entry..."
SYSTEM_DESKTOP="/usr/share/applications/yunfx-autoshell.desktop"
if [ -f "$SYSTEM_DESKTOP" ]; then
    print_success "System desktop entry found: $SYSTEM_DESKTOP"
    echo "  Icon path: $(grep "Icon=" "$SYSTEM_DESKTOP" | cut -d'=' -f2)"
else
    print_warning "System desktop entry not found: $SYSTEM_DESKTOP"
fi

# Check user icon
print_status "4. Checking user icon..."
USER_ICON="$HOME/.local/share/icons/yunfx-autoshell.png"
if [ -f "$USER_ICON" ]; then
    print_success "User icon found: $USER_ICON"
    echo "  Size: $(stat -c%s "$USER_ICON") bytes"
else
    print_warning "User icon not found: $USER_ICON"
fi

# Check system icon
print_status "5. Checking system icon..."
SYSTEM_ICON="/usr/share/pixmaps/yunfx-autoshell.png"
if [ -f "$SYSTEM_ICON" ]; then
    print_success "System icon found: $SYSTEM_ICON"
    echo "  Size: $(stat -c%s "$SYSTEM_ICON") bytes"
else
    print_warning "System icon not found: $SYSTEM_ICON"
fi

# Test desktop entry
print_status "6. Testing desktop entry..."
if command -v desktop-file-validate &> /dev/null; then
    if [ -f "$USER_DESKTOP" ]; then
        desktop-file-validate "$USER_DESKTOP" && print_success "User desktop entry is valid" || print_warning "User desktop entry has issues"
    fi
    if [ -f "$SYSTEM_DESKTOP" ]; then
        desktop-file-validate "$SYSTEM_DESKTOP" && print_success "System desktop entry is valid" || print_warning "System desktop entry has issues"
    fi
else
    print_warning "desktop-file-validate not available"
fi

echo ""
print_header "🎯 Next Steps:"
echo ""
print_status "1. Search for 'autoshell' or 'yunfx' in your application launcher"
print_status "2. The correct icon should be the red Java coffee cup with green '>' symbol"
print_status "3. If you still see the old icon, try:"
echo "   • Logging out and back in"
echo "   • Running: sudo ./update_system_icon.sh"
echo "   • Clearing application cache in your desktop environment"
echo ""
print_status "The user-level desktop entry should take precedence over the system one."

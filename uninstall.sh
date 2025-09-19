#!/bin/bash

# YunFx AutoShell Simple Uninstaller
# This script removes the application from the system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

echo "🗑️  YunFx AutoShell Uninstaller"
echo "================================"

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    print_status "Running as root - removing system-wide installation"
    SYSTEM_INSTALL=true
else
    print_status "Running as user - removing user installation only"
    print_warning "For system-wide removal, run: sudo $0"
    SYSTEM_INSTALL=false
fi

# Remove user-level installation
print_status "Removing user-level installation..."

# Remove desktop entry from user applications
USER_APPS_DIR="$HOME/.local/share/applications"
if [ -f "$USER_APPS_DIR/yunfx-autoshell.desktop" ]; then
    rm -f "$USER_APPS_DIR/yunfx-autoshell.desktop"
    print_success "Removed user desktop entry"
fi

# Remove user icon
USER_ICON_DIR="$HOME/.local/share/icons"
if [ -f "$USER_ICON_DIR/yunfx-autoshell.png" ]; then
    rm -f "$USER_ICON_DIR/yunfx-autoshell.png"
    print_success "Removed user icon"
fi

# Remove desktop shortcut
DESKTOP_SHORTCUT="$HOME/Desktop/yunfx-autoshell.desktop"
if [ -f "$DESKTOP_SHORTCUT" ]; then
    rm -f "$DESKTOP_SHORTCUT"
    print_success "Removed desktop shortcut"
fi

# Remove project desktop entry
if [ -f "yunfx-autoshell.desktop" ]; then
    rm -f "yunfx-autoshell.desktop"
    print_success "Removed project desktop entry"
fi

# Remove launcher script
if [ -f "launch_autoshell.sh" ]; then
    rm -f "launch_autoshell.sh"
    print_success "Removed launcher script"
fi

# Remove systemd user service
USER_SERVICE="$HOME/.config/systemd/user/yunfx-autoshell.service"
if [ -f "$USER_SERVICE" ]; then
    systemctl --user stop yunfx-autoshell.service 2>/dev/null || true
    systemctl --user disable yunfx-autoshell.service 2>/dev/null || true
    rm -f "$USER_SERVICE"
    print_success "Removed user systemd service"
fi

# Reload systemd user daemon
systemctl --user daemon-reload 2>/dev/null || true

# Remove system-wide installation if running as root
if [ "$SYSTEM_INSTALL" = true ]; then
    print_status "Removing system-wide installation..."
    
    # Remove application directory
    if [ -d "/opt/yunfx-autoshell" ]; then
        rm -rf "/opt/yunfx-autoshell"
        print_success "Removed application directory"
    fi
    
    # Remove launcher script
    if [ -f "/usr/local/bin/yunfx-autoshell" ]; then
        rm -f "/usr/local/bin/yunfx-autoshell"
        print_success "Removed system launcher"
    fi
    
    # Remove icon
    if [ -f "/usr/share/pixmaps/yunfx-autoshell.png" ]; then
        rm -f "/usr/share/pixmaps/yunfx-autoshell.png"
        print_success "Removed system icon"
    fi
    
    # Remove system desktop entry
    if [ -f "/usr/share/applications/yunfx-autoshell.desktop" ]; then
        rm -f "/usr/share/applications/yunfx-autoshell.desktop"
        print_success "Removed system desktop entry"
    fi
    
    # Remove systemd service
    if [ -f "/etc/systemd/system/yunfx-autoshell.service" ]; then
        systemctl stop yunfx-autoshell.service 2>/dev/null || true
        systemctl disable yunfx-autoshell.service 2>/dev/null || true
        rm -f "/etc/systemd/system/yunfx-autoshell.service"
        systemctl daemon-reload 2>/dev/null || true
        print_success "Removed systemd service"
    fi
fi

# Update desktop database
print_status "Updating desktop database..."
if command -v update-desktop-database &> /dev/null; then
    if [ -d "$HOME/.local/share/applications" ]; then
        update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
    fi
    
    if [ "$SYSTEM_INSTALL" = true ] && [ -d "/usr/share/applications" ]; then
        update-desktop-database "/usr/share/applications" 2>/dev/null || true
    fi
    
    print_success "Desktop database updated"
fi

# Check for running processes
if pgrep -f "yunfx-autoshell\|YunFx AutoShell" > /dev/null; then
    print_warning "Found running YunFx AutoShell processes"
    echo "The following processes are still running:"
    pgrep -f "yunfx-autoshell\|YunFx AutoShell" | while read pid; do
        ps -p "$pid" -o pid,ppid,cmd --no-headers
    done
    echo ""
    read -p "Do you want to kill these processes? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        pkill -f "yunfx-autoshell\|YunFx AutoShell" || true
        print_success "Killed running processes"
    else
        print_warning "Processes left running - you may need to restart your desktop environment"
    fi
fi

echo ""
print_success "YunFx AutoShell has been successfully uninstalled!"
echo ""
print_status "You may need to restart your desktop environment for all changes to take effect."
echo ""

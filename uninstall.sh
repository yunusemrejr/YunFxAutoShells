#!/bin/bash

# YunFx AutoShell Uninstaller
# This script removes all traces of the application from the system

set -e

echo "🗑️  YunFx AutoShell Uninstaller"
echo "================================"

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

# Function to check if running as root
check_root() {
    if [ "$EUID" -eq 0 ]; then
        return 0
    else
        return 1
    fi
}

# Function to remove user-level installations
remove_user_installation() {
    print_status "Removing user-level installation..."
    
    # Remove desktop entry from user applications
    USER_APPS_DIR="$HOME/.local/share/applications"
    if [ -f "$USER_APPS_DIR/yunfx-autoshell.desktop" ]; then
        rm -f "$USER_APPS_DIR/yunfx-autoshell.desktop"
        print_success "Removed user desktop entry"
    fi
    
    # Remove desktop shortcut
    DESKTOP_SHORTCUT="$HOME/Desktop/yunfx-autoshell.desktop"
    if [ -f "$DESKTOP_SHORTCUT" ]; then
        rm -f "$DESKTOP_SHORTCUT"
        print_success "Removed desktop shortcut"
    fi
    
    # Remove any desktop entries in the project directory
    PROJECT_DESKTOP="$HOME/Desktop/YunFxAutoShell/yunfx-autoshell.desktop"
    if [ -f "$PROJECT_DESKTOP" ]; then
        rm -f "$PROJECT_DESKTOP"
        print_success "Removed project desktop entry"
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
}

# Function to remove system-wide installations
remove_system_installation() {
    print_status "Removing system-wide installation..."
    
    # Stop and disable systemd service
    if systemctl is-active --quiet yunfx-autoshell.service 2>/dev/null; then
        systemctl stop yunfx-autoshell.service
        print_success "Stopped systemd service"
    fi
    
    if systemctl is-enabled --quiet yunfx-autoshell.service 2>/dev/null; then
        systemctl disable yunfx-autoshell.service
        print_success "Disabled systemd service"
    fi
    
    # Remove systemd service file
    if [ -f "/etc/systemd/system/yunfx-autoshell.service" ]; then
        rm -f "/etc/systemd/system/yunfx-autoshell.service"
        print_success "Removed systemd service file"
    fi
    
    # Remove application directory
    if [ -d "/opt/yunfx-autoshell" ]; then
        rm -rf "/opt/yunfx-autoshell"
        print_success "Removed application directory"
    fi
    
    # Remove launcher script
    if [ -f "/usr/local/bin/yunfx-autoshell" ]; then
        rm -f "/usr/local/bin/yunfx-autoshell"
        print_success "Removed launcher script"
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
    
    # Reload systemd daemon
    systemctl daemon-reload 2>/dev/null || true
}

# Function to perform comprehensive cleanup
comprehensive_cleanup() {
    print_status "Performing comprehensive cleanup..."
    
    # Search for and remove any remaining desktop entries
    find "$HOME" -name "*yunfx*autoshell*.desktop" -type f 2>/dev/null | while read -r file; do
        if [ -f "$file" ]; then
            rm -f "$file"
            print_success "Removed: $file"
        fi
    done
    
    # Search for and remove any remaining desktop entries in system directories (if root)
    if check_root; then
        find /usr/share/applications /opt -name "*yunfx*autoshell*.desktop" -type f 2>/dev/null | while read -r file; do
            if [ -f "$file" ]; then
                rm -f "$file"
                print_success "Removed: $file"
            fi
        done
    fi
    
    # Remove any cached desktop entries
    if [ -f "$HOME/.local/share/applications/mimeinfo.cache" ]; then
        sed -i '/yunfx-autoshell/d' "$HOME/.local/share/applications/mimeinfo.cache" 2>/dev/null || true
    fi
    
    if check_root && [ -f "/usr/share/applications/mimeinfo.cache" ]; then
        sed -i '/yunfx-autoshell/d' "/usr/share/applications/mimeinfo.cache" 2>/dev/null || true
    fi
}

# Function to clean up desktop database
cleanup_desktop_database() {
    print_status "Updating desktop database..."
    
    # Update user desktop database
    if command -v update-desktop-database &> /dev/null; then
        if [ -d "$HOME/.local/share/applications" ]; then
            update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
        fi
        
        # Update system desktop database if running as root
        if check_root && [ -d "/usr/share/applications" ]; then
            update-desktop-database "/usr/share/applications" 2>/dev/null || true
        fi
        
        print_success "Desktop database updated"
    fi
}

# Function to check for running processes
check_running_processes() {
    print_status "Checking for running processes..."
    
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
    else
        print_success "No running processes found"
    fi
}

# Function to show what was removed
show_removal_summary() {
    echo ""
    echo "📋 Removal Summary:"
    echo "=================="
    echo "✅ User desktop entry removed"
    echo "✅ Desktop shortcut removed"
    echo "✅ User systemd service removed"
    
    if check_root; then
        echo "✅ System-wide installation removed"
        echo "✅ System launcher removed"
        echo "✅ System icon removed"
        echo "✅ System desktop entry removed"
        echo "✅ System systemd service removed"
    fi
    
    echo "✅ Desktop database updated"
    echo ""
}

# Main execution
main() {
    echo ""
    print_status "Starting uninstallation process..."
    echo ""
    
    # Check for running processes first
    check_running_processes
    echo ""
    
    # Remove user-level installation
    remove_user_installation
    echo ""
    
    # Check if running as root for system-wide removal
    if check_root; then
        print_status "Running as root - removing system-wide installation"
        remove_system_installation
        echo ""
    else
        print_warning "Not running as root - only removing user-level installation"
        print_warning "To remove system-wide installation, run: sudo $0"
        echo ""
    fi
    
    # Perform comprehensive cleanup
    comprehensive_cleanup
    echo ""
    
    # Clean up desktop database
    cleanup_desktop_database
    echo ""
    
    # Show summary
    show_removal_summary
    
    print_success "YunFx AutoShell has been successfully uninstalled!"
    echo ""
    print_status "You may need to restart your desktop environment for all changes to take effect."
    echo ""
}

# Handle command line arguments
case "${1:-}" in
    --help|-h)
        echo "YunFx AutoShell Uninstaller"
        echo ""
        echo "Usage: $0 [options]"
        echo ""
        echo "Options:"
        echo "  --help, -h     Show this help message"
        echo "  --user-only    Only remove user-level installation"
        echo "  --system-only  Only remove system-wide installation (requires root)"
        echo ""
        echo "Examples:"
        echo "  $0                    # Remove user installation"
        echo "  sudo $0               # Remove both user and system installations"
        echo "  $0 --user-only        # Only remove user installation"
        echo "  sudo $0 --system-only # Only remove system installation"
        exit 0
        ;;
    --user-only)
        print_status "User-only removal mode"
        remove_user_installation
        cleanup_desktop_database
        show_removal_summary
        print_success "User installation removed!"
        exit 0
        ;;
    --system-only)
        if ! check_root; then
            print_error "System-only removal requires root privileges"
            echo "Usage: sudo $0 --system-only"
            exit 1
        fi
        print_status "System-only removal mode"
        remove_system_installation
        cleanup_desktop_database
        show_removal_summary
        print_success "System installation removed!"
        exit 0
        ;;
    "")
        main
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
esac

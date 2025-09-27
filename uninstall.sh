#!/bin/bash

# YunFx AutoShell Smart Uninstaller
# Multi-distro support for complete removal of all traces

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
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

print_header() {
    echo -e "${PURPLE}$1${NC}"
}

print_step() {
    echo -e "${CYAN}🔧 $1${NC}"
}

print_header "🗑️  YunFx AutoShell Smart Uninstaller"
echo "=============================================="
echo "Multi-distro support: Debian, Ubuntu, Pop!_OS, Fedora, Arch, openSUSE"
echo ""

# Detect Linux distribution
detect_distro() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        DISTRO_ID="$ID"
        DISTRO_VERSION="$VERSION_ID"
        DISTRO_NAME="$NAME"
    elif [ -f /etc/debian_version ]; then
        DISTRO_ID="debian"
        DISTRO_VERSION="$(cat /etc/debian_version)"
        DISTRO_NAME="Debian"
    elif [ -f /etc/redhat-release ]; then
        DISTRO_ID="rhel"
        DISTRO_VERSION="$(cat /etc/redhat-release)"
        DISTRO_NAME="Red Hat"
    elif [ -f /etc/arch-release ]; then
        DISTRO_ID="arch"
        DISTRO_NAME="Arch Linux"
    else
        DISTRO_ID="unknown"
        DISTRO_NAME="Unknown"
    fi
    
    print_status "Detected: $DISTRO_NAME ($DISTRO_ID)"
}

# Check if running as root
check_sudo() {
if [ "$EUID" -eq 0 ]; then
        print_warning "Running as root - will remove system-wide installation"
    SYSTEM_INSTALL=true
else
        print_status "Running as user - will remove user installation only"
    print_warning "For system-wide removal, run: sudo $0"
    SYSTEM_INSTALL=false
fi
}

# Stop all running processes
stop_processes() {
    print_step "Stopping YunFx AutoShell processes..."
    
    # Find and kill all related processes
    PIDS=$(pgrep -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" 2>/dev/null || true)
    
    if [ -n "$PIDS" ]; then
        print_warning "Found running YunFx AutoShell processes:"
        for pid in $PIDS; do
            ps -p "$pid" -o pid,ppid,cmd --no-headers 2>/dev/null || true
        done
        
        print_status "Stopping processes..."
        pkill -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" || true
        
        # Wait a moment for graceful shutdown
        sleep 3
        
        # Force kill if still running
        REMAINING_PIDS=$(pgrep -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" 2>/dev/null || true)
        if [ -n "$REMAINING_PIDS" ]; then
            print_warning "Force stopping remaining processes..."
            pkill -9 -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" || true
        fi
        
        print_success "All processes stopped"
    else
        print_success "No running processes found"
    fi
}

# Stop and disable systemd services
stop_services() {
    print_step "Stopping systemd services..."
    
    # Stop and disable system-wide service
    if [ "$SYSTEM_INSTALL" = true ]; then
        if systemctl is-active yunfx-autoshell.service >/dev/null 2>&1; then
            print_status "Stopping system-wide systemd service..."
            systemctl stop yunfx-autoshell.service || true
            systemctl disable yunfx-autoshell.service || true
            print_success "System-wide service stopped and disabled"
        fi
    fi
    
    # Stop and disable user service
    if systemctl --user is-active yunfx-autoshell.service >/dev/null 2>&1; then
        print_status "Stopping user systemd service..."
        systemctl --user stop yunfx-autoshell.service || true
        systemctl --user disable yunfx-autoshell.service || true
        print_success "User service stopped and disabled"
    fi
    
    # Reload systemd daemon
    systemctl daemon-reload 2>/dev/null || true
systemctl --user daemon-reload 2>/dev/null || true
}

# Remove user-level files
remove_user_files() {
    print_step "Removing user-level files..."
    
    USER_PATHS=(
        "$HOME/.local/share/applications/yunfx-autoshell.desktop"
        "$HOME/.local/share/icons/yunfx-autoshell.png"
        "$HOME/Desktop/yunfx-autoshell.desktop"
        "$HOME/.config/systemd/user/yunfx-autoshell.service"
        "$HOME/.local/share/yunfx-autoshell"
        "$HOME/.cache/yunfx-autoshell"
        "$HOME/.config/yunfx-autoshell"
    )
    
    for path in "${USER_PATHS[@]}"; do
        if [ -e "$path" ]; then
            print_status "Removing: $path"
            rm -rf "$path"
        fi
    done
    
    print_success "User-level files removed"
}

# Remove system-level files
remove_system_files() {
    print_step "Removing system-level files..."
    
    SYSTEM_PATHS=(
        "/opt/yunfx-autoshell"
        "/usr/local/bin/yunfx-autoshell"
        "/usr/share/pixmaps/yunfx-autoshell.png"
        "/usr/share/applications/yunfx-autoshell.desktop"
        "/etc/systemd/system/yunfx-autoshell.service"
        "/var/cache/yunfx-autoshell"
        "/var/lib/yunfx-autoshell"
        "/var/log/yunfx-autoshell"
    )
    
    for path in "${SYSTEM_PATHS[@]}"; do
        if [ -e "$path" ]; then
            print_status "Removing: $path"
            rm -rf "$path"
        fi
    done
    
    print_success "System-level files removed"
}

# Remove project files
remove_project_files() {
    print_step "Removing project files..."
    
    # Get script directory
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    
    PROJECT_FILES=(
        "$SCRIPT_DIR/launch_autoshell.sh"
        "$SCRIPT_DIR/yunfx-autoshell.desktop"
        "$SCRIPT_DIR/launch_fixed.sh"
    )
    
    for file in "${PROJECT_FILES[@]}"; do
        if [ -f "$file" ]; then
            print_status "Removing: $file"
            rm -f "$file"
        fi
    done
    
    print_success "Project files removed"
}

# Clean up package manager cache (optional)
cleanup_package_cache() {
    print_step "Cleaning up package manager cache..."
    
    # Detect package manager
    if command -v apt &> /dev/null; then
        PACKAGE_MANAGER="apt"
    elif command -v dnf &> /dev/null; then
        PACKAGE_MANAGER="dnf"
    elif command -v pacman &> /dev/null; then
        PACKAGE_MANAGER="pacman"
    elif command -v zypper &> /dev/null; then
        PACKAGE_MANAGER="zypper"
    else
        PACKAGE_MANAGER="unknown"
    fi
    
    case "$PACKAGE_MANAGER" in
        apt)
            print_status "Cleaning apt cache..."
            apt autoremove -y 2>/dev/null || true
            apt autoclean 2>/dev/null || true
            ;;
        dnf)
            print_status "Cleaning dnf cache..."
            dnf autoremove -y 2>/dev/null || true
            dnf clean all 2>/dev/null || true
            ;;
        pacman)
            print_status "Cleaning pacman cache..."
            pacman -Sc --noconfirm 2>/dev/null || true
            ;;
        zypper)
            print_status "Cleaning zypper cache..."
            zypper clean --all 2>/dev/null || true
            ;;
    esac
    
    print_success "Package cache cleaned"
}

# Refresh desktop environment
refresh_desktop() {
    print_step "Refreshing desktop environment..."

# Update desktop database
    if command -v update-desktop-database &> /dev/null; then
print_status "Updating desktop database..."
        if [ "$SYSTEM_INSTALL" = true ]; then
            update-desktop-database "/usr/share/applications" 2>/dev/null || true
        fi
        update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
    fi
    
    # Update icon cache
    if command -v gtk-update-icon-cache &> /dev/null; then
        print_status "Updating icon cache..."
        gtk-update-icon-cache -f -t "$HOME/.local/share/icons" 2>/dev/null || true
        if [ "$SYSTEM_INSTALL" = true ]; then
            gtk-update-icon-cache -f -t "/usr/share/pixmaps" 2>/dev/null || true
        fi
    fi
    
    # Update MIME database
    if command -v update-mime-database &> /dev/null; then
        print_status "Updating MIME database..."
        update-mime-database "$HOME/.local/share/mime" 2>/dev/null || true
    fi
    
    # Refresh desktop environment specific
    case "$XDG_CURRENT_DESKTOP" in
        *GNOME*|*gnome*)
            print_status "Refreshing GNOME desktop..."
            gsettings set org.gnome.desktop.background show-desktop-icons true 2>/dev/null || true
            ;;
        *KDE*|*kde*)
            print_status "Refreshing KDE desktop..."
            kbuildsycoca5 2>/dev/null || true
            ;;
        *XFCE*|*xfce*)
            print_status "Refreshing XFCE desktop..."
            xfce4-panel -r 2>/dev/null || true
            ;;
    esac
    
    print_success "Desktop environment refreshed"
}

# Verify complete removal
verify_removal() {
    print_step "Verifying complete removal..."
    
    REMAINING_FILES=()
    
    # Check for remaining files
    CHECK_PATHS=(
        "$HOME/.local/share/applications/yunfx-autoshell.desktop"
        "$HOME/.local/share/icons/yunfx-autoshell.png"
        "$HOME/Desktop/yunfx-autoshell.desktop"
        "$HOME/.config/systemd/user/yunfx-autoshell.service"
        "$HOME/.local/share/yunfx-autoshell"
    )
    
    if [ "$SYSTEM_INSTALL" = true ]; then
        SYSTEM_CHECK_PATHS=(
            "/opt/yunfx-autoshell"
            "/usr/local/bin/yunfx-autoshell"
            "/usr/share/pixmaps/yunfx-autoshell.png"
            "/usr/share/applications/yunfx-autoshell.desktop"
            "/etc/systemd/system/yunfx-autoshell.service"
        )
        CHECK_PATHS+=("${SYSTEM_CHECK_PATHS[@]}")
    fi
    
    for path in "${CHECK_PATHS[@]}"; do
        if [ -e "$path" ]; then
            REMAINING_FILES+=("$path")
        fi
    done
    
    # Check for remaining processes
    REMAINING_PROCESSES=$(pgrep -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" 2>/dev/null || true)
    
    if [ ${#REMAINING_FILES[@]} -eq 0 ] && [ -z "$REMAINING_PROCESSES" ]; then
        print_success "Complete removal verified - no traces found"
        return 0
    else
        print_warning "Some traces remain:"
        for file in "${REMAINING_FILES[@]}"; do
            print_warning "  • $file"
        done
        if [ -n "$REMAINING_PROCESSES" ]; then
            print_warning "  • Running processes detected"
        fi
        return 1
    fi
}

# Main execution
main() {
    # Detect distribution
    detect_distro
    
    # Check sudo status
    check_sudo
    
    # Confirm uninstallation
    echo ""
    print_warning "This will completely remove YunFx AutoShell and all its data."
    if [ "$SYSTEM_INSTALL" = true ]; then
        print_warning "System-wide installation will be removed."
    else
        print_warning "User installation will be removed."
    fi
    echo ""
    
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Uninstallation cancelled."
        exit 0
    fi
    
    # Stop all processes
    stop_processes
    
    # Stop services
    stop_services
    
    # Remove user files
    remove_user_files
    
    # Remove system files if running as root
    if [ "$SYSTEM_INSTALL" = true ]; then
        remove_system_files
    fi
    
    # Remove project files
    remove_project_files
    
    # Clean up package cache (optional)
    read -p "Clean up package manager cache? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cleanup_package_cache
    fi
    
    # Refresh desktop environment
    refresh_desktop
    
    # Verify removal
    if verify_removal; then
        print_header "🎉 YunFx AutoShell has been completely removed!"
        echo ""
        print_success "All traces of YunFx AutoShell have been removed from your system."
        print_status "You may need to restart your desktop environment for all changes to take effect."
    else
        print_warning "Some traces may remain. Please check the warnings above."
        print_status "You may need to manually remove remaining files."
fi

echo ""
    print_status "Thank you for using YunFx AutoShell!"
}

# Run main function
main
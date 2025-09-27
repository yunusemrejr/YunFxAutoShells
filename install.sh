#!/bin/bash

# YunFx AutoShell Smart Installer
# Multi-distro support for Debian, Ubuntu, Pop!_OS, Fedora, Arch, and more
# Handles complete cleanup and fresh installation from source

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

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

print_header "🚀 YunFx AutoShell Smart Installer"
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

# Package manager detection and installation commands
get_package_manager() {
    case "$DISTRO_ID" in
        ubuntu|debian|pop|elementary|linuxmint)
            PACKAGE_MANAGER="apt"
            UPDATE_CMD="apt update"
            INSTALL_CMD="apt install -y"
            ;;
        fedora|rhel|centos|rocky|almalinux)
            PACKAGE_MANAGER="dnf"
            UPDATE_CMD="dnf check-update || true"
            INSTALL_CMD="dnf install -y"
            ;;
        arch|manjaro|endeavouros)
            PACKAGE_MANAGER="pacman"
            UPDATE_CMD="pacman -Sy"
            INSTALL_CMD="pacman -S --noconfirm"
            ;;
        opensuse*|suse)
            PACKAGE_MANAGER="zypper"
            UPDATE_CMD="zypper refresh"
            INSTALL_CMD="zypper install -y"
            ;;
        *)
            print_warning "Unknown distribution, trying apt as fallback"
            PACKAGE_MANAGER="apt"
            UPDATE_CMD="apt update"
            INSTALL_CMD="apt install -y"
            ;;
    esac
    
    print_status "Using package manager: $PACKAGE_MANAGER"
}

# Check if running as root
check_sudo() {
    if [ "$EUID" -eq 0 ]; then
        print_warning "Running as root - will install system-wide"
        SYSTEM_INSTALL=true
    else
        print_status "Running as user - will install for current user only"
        print_warning "For system-wide installation, run: sudo $0"
        SYSTEM_INSTALL=false
    fi
}

# Complete cleanup of existing installations
cleanup_existing() {
    print_step "Cleaning up existing installations..."
    
    # Kill running processes
    if pgrep -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" > /dev/null; then
        print_warning "Found running YunFx AutoShell processes, stopping them..."
        pkill -f "yunfx-autoshell\|YunFx AutoShell\|com.yunfx.autoshell.Main" || true
        sleep 2
    fi
    
    # User-level cleanup
    USER_CLEANUP_PATHS=(
        "$HOME/.local/share/applications/yunfx-autoshell.desktop"
        "$HOME/.local/share/icons/yunfx-autoshell.png"
        "$HOME/Desktop/yunfx-autoshell.desktop"
        "$HOME/.config/systemd/user/yunfx-autoshell.service"
        "$HOME/.local/share/yunfx-autoshell"
    )
    
    for path in "${USER_CLEANUP_PATHS[@]}"; do
        if [ -e "$path" ]; then
            print_status "Removing: $path"
            rm -rf "$path"
        fi
    done
    
    # System-level cleanup (only if running as root)
    if [ "$SYSTEM_INSTALL" = true ]; then
        SYSTEM_CLEANUP_PATHS=(
            "/opt/yunfx-autoshell"
            "/usr/local/bin/yunfx-autoshell"
            "/usr/share/pixmaps/yunfx-autoshell.png"
            "/usr/share/applications/yunfx-autoshell.desktop"
            "/etc/systemd/system/yunfx-autoshell.service"
        )
        
        for path in "${SYSTEM_CLEANUP_PATHS[@]}"; do
            if [ -e "$path" ]; then
                print_status "Removing: $path"
                rm -rf "$path"
            fi
        done
        
        # Stop and disable systemd service if exists
        if systemctl is-active yunfx-autoshell.service >/dev/null 2>&1; then
            print_status "Stopping systemd service..."
            systemctl stop yunfx-autoshell.service || true
            systemctl disable yunfx-autoshell.service || true
        fi
    fi
    
    # Stop and disable user systemd service if exists
    if systemctl --user is-active yunfx-autoshell.service >/dev/null 2>&1; then
        print_status "Stopping user systemd service..."
        systemctl --user stop yunfx-autoshell.service || true
        systemctl --user disable yunfx-autoshell.service || true
    fi
    
    # Reload systemd daemon
    systemctl daemon-reload 2>/dev/null || true
    systemctl --user daemon-reload 2>/dev/null || true
    
    print_success "Cleanup completed"
}

# Check and install dependencies
install_dependencies() {
    print_step "Installing dependencies..."
    
    # Update package lists
    print_status "Updating package lists..."
    if [ "$PACKAGE_MANAGER" = "apt" ]; then
        $UPDATE_CMD
    else
        $UPDATE_CMD || true
    fi
    
    # Java installation based on distro
    case "$PACKAGE_MANAGER" in
        apt)
            JAVA_PACKAGE="openjdk-17-jdk"
            JAVAFX_PACKAGE="openjfx"
            MAVEN_PACKAGE="maven"
            ;;
        dnf)
            JAVA_PACKAGE="java-17-openjdk-devel"
            JAVAFX_PACKAGE="java-17-openjfx"
            MAVEN_PACKAGE="maven"
            ;;
        pacman)
            JAVA_PACKAGE="jdk17-openjdk"
            JAVAFX_PACKAGE="java17-openjfx"
            MAVEN_PACKAGE="maven"
            ;;
        zypper)
            JAVA_PACKAGE="java-17-openjdk-devel"
            JAVAFX_PACKAGE="java-17-openjfx"
            MAVEN_PACKAGE="maven"
            ;;
    esac
    
    # Install Java if not present
    if ! command -v java &> /dev/null; then
        print_status "Installing Java..."
        $INSTALL_CMD $JAVA_PACKAGE
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -lt 17 ]; then
            print_warning "Java version $JAVA_VERSION is too old. Installing Java 17..."
            $INSTALL_CMD $JAVA_PACKAGE
        else
            print_success "Java $JAVA_VERSION found"
        fi
    fi
    
    # Install Maven if not present
    if ! command -v mvn &> /dev/null; then
        print_status "Installing Maven..."
        $INSTALL_CMD $MAVEN_PACKAGE
    else
        print_success "Maven found"
    fi
    
    # Install JavaFX if not present
    JAVAFX_FOUND=false
    JAVAFX_PATHS=(
        "/usr/share/openjfx/lib"
        "/usr/lib/jvm/java-17-openjdk-amd64/lib"
        "/usr/lib/jvm/java-17-openjdk/lib"
        "/usr/lib/jvm/default-java/lib"
        "/usr/lib/jvm/java-17-openjdk-arm64/lib"
    )
    
    for path in "${JAVAFX_PATHS[@]}"; do
        if [ -d "$path" ] && [ -f "$path/javafx.controls.jar" ]; then
            JAVAFX_FOUND=true
            break
        fi
    done
    
    if [ "$JAVAFX_FOUND" = false ]; then
        print_status "Installing JavaFX..."
        $INSTALL_CMD $JAVAFX_PACKAGE
        
        # Check again after installation
        JAVAFX_FOUND=false
        for path in "${JAVAFX_PATHS[@]}"; do
            if [ -d "$path" ] && [ -f "$path/javafx.controls.jar" ]; then
                JAVAFX_FOUND=true
                break
            fi
        done
        
        if [ "$JAVAFX_FOUND" = false ]; then
            print_error "JavaFX installation failed. Please install manually."
            exit 1
        fi
    fi
    
    print_success "All dependencies installed"
}

# Compile application from source
compile_application() {
    print_step "Compiling application from source..."
    
    # Check if pom.xml exists
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Please run this script from the project root directory."
        exit 1
    fi
    
    # Clean previous build
    print_status "Cleaning previous build..."
    mvn clean -q
    
    # Compile
    print_status "Compiling application..."
    mvn compile -q
    
    if [ $? -ne 0 ]; then
        print_error "Compilation failed. Please check the error messages above."
        exit 1
    fi
    
    # Package
    print_status "Packaging application..."
    mvn package -DskipTests -q
    
    if [ $? -ne 0 ]; then
        print_error "Packaging failed. Please check the error messages above."
        exit 1
    fi
    
    # Ensure dependencies are available
    if [ ! -d "target/dependency" ]; then
        print_status "Downloading dependencies..."
        mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
    fi
    
    print_success "Compilation completed"
}

# Create scripts directory
setup_scripts_directory() {
    DEFAULT_SCRIPTS_DIR="/home/$USER/SCRIPTS"
    if [ ! -d "$DEFAULT_SCRIPTS_DIR" ]; then
        print_status "Creating default scripts directory: $DEFAULT_SCRIPTS_DIR"
        mkdir -p "$DEFAULT_SCRIPTS_DIR"
        
        # Create a sample script
        cat > "$DEFAULT_SCRIPTS_DIR/sample_script.sh" << 'EOF'
#!/bin/bash
# Sample script for YunFx AutoShell
# Tag: sample, demo

echo "Hello from YunFx AutoShell!"
echo "This is a sample script to demonstrate the application."
echo "Current time: $(date)"
echo "Current user: $(whoami)"
echo "Current directory: $(pwd)"
EOF
        
        chmod +x "$DEFAULT_SCRIPTS_DIR/sample_script.sh"
        print_success "Created sample script: $DEFAULT_SCRIPTS_DIR/sample_script.sh"
    fi
}

# Install system-wide
install_system_wide() {
    print_step "Installing system-wide..."
    
    APP_NAME="YunFx AutoShell"
    APP_DIR="/opt/yunfx-autoshell"
    BIN_DIR="/usr/local/bin"
    ICON_DIR="/usr/share/pixmaps"
    DESKTOP_DIR="/usr/share/applications"
    
    # Create application directory
    mkdir -p "$APP_DIR"
    mkdir -p "$APP_DIR/media"
    
    # Copy application files
    print_status "Copying application files..."
    cp -r src "$APP_DIR/"
    cp -r target "$APP_DIR/"
    cp pom.xml "$APP_DIR/"
    cp README.md "$APP_DIR/" 2>/dev/null || true
    cp INSTALL.md "$APP_DIR/" 2>/dev/null || true
    cp media/icon.png "$APP_DIR/media/"
    
    # Create system launcher
    cat > "$BIN_DIR/yunfx-autoshell" << EOF
#!/bin/bash
# YunFx AutoShell System Launcher

APP_DIR="$APP_DIR"
USER_DATA_DIR="\$HOME/.local/share/yunfx-autoshell"

# Create user data directory if it doesn't exist
mkdir -p "\$USER_DATA_DIR"

# Copy application files to user directory if needed
if [ ! -d "\$USER_DATA_DIR/target" ] || [ "\$APP_DIR/target" -nt "\$USER_DATA_DIR/target" ]; then
    echo "📦 Updating application files..."
    cp -r "\$APP_DIR/target" "\$USER_DATA_DIR/"
    cp -r "\$APP_DIR/src" "\$USER_DATA_DIR/"
    cp "\$APP_DIR/pom.xml" "\$USER_DATA_DIR/"
fi

# Change to user data directory for write permissions
cd "\$USER_DATA_DIR"

# Find JavaFX modules
JAVAFX_PATHS=(
    "/usr/share/openjfx/lib"
    "/usr/lib/jvm/java-17-openjdk-amd64/lib"
    "/usr/lib/jvm/java-17-openjdk/lib"
    "/usr/lib/jvm/default-java/lib"
    "/usr/lib/jvm/java-17-openjdk-arm64/lib"
)

JAVAFX_PATH=""
for path in "\${JAVAFX_PATHS[@]}"; do
    if [ -d "\$path" ] && [ -f "\$path/javafx.controls.jar" ]; then
        JAVAFX_PATH="\$path"
        break
    fi
done

if [ -z "\$JAVAFX_PATH" ]; then
    echo "❌ JavaFX modules not found. Please install OpenJDK with JavaFX support."
    exit 1
fi

# Build classpath
CLASSPATH="target/classes"
CLASSPATH="\$CLASSPATH:target/dependency/*"

# Run the application
exec java --module-path "\$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml \\
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED \\
     --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \\
     --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \\
     --add-opens javafx.base/com.sun.javafx.binding=ALL-UNNAMED \\
     --add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED \\
     --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \\
     -cp "\$CLASSPATH" com.yunfx.autoshell.Main
EOF
    
    chmod +x "$BIN_DIR/yunfx-autoshell"
    
    # Copy icon
    cp "$APP_DIR/media/icon.png" "$ICON_DIR/yunfx-autoshell.png"
    
    # Create desktop entry
    cat > "$DESKTOP_DIR/yunfx-autoshell.desktop" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=$APP_NAME
Comment=JavaFX Script Manager for Linux
Exec=$BIN_DIR/yunfx-autoshell
Icon=$ICON_DIR/yunfx-autoshell.png
Terminal=false
StartupNotify=true
Categories=Development;System;Utility;
Keywords=autoshell;yunfx;script;shell;automation;java;javafx;
StartupWMClass=YunFx AutoShell
MimeType=application/x-shellscript;
EOF
    
    print_success "System-wide installation completed"
}

# Install for current user only
install_user_only() {
    print_step "Installing for current user..."
    
    APP_NAME="YunFx AutoShell"
    USER_APPS_DIR="$HOME/.local/share/applications"
    USER_ICON_DIR="$HOME/.local/share/icons"
    DESKTOP_FILE="yunfx-autoshell.desktop"
    
    # Create directories
    mkdir -p "$USER_APPS_DIR"
    mkdir -p "$USER_ICON_DIR"
    
    # Copy icon
    cp media/icon.png "$USER_ICON_DIR/yunfx-autoshell.png"
    
    # Create launcher script
    cat > "launch_autoshell.sh" << 'EOF'
#!/bin/bash
# YunFx AutoShell Launcher

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
USER_DATA_DIR="$HOME/.local/share/yunfx-autoshell"

# Create user data directory if it doesn't exist
mkdir -p "$USER_DATA_DIR"

# Copy application files to user directory if needed
if [ ! -d "$USER_DATA_DIR/target" ] || [ "$SCRIPT_DIR/target" -nt "$USER_DATA_DIR/target" ]; then
    echo "📦 Updating application files..."
    cp -r "$SCRIPT_DIR/target" "$USER_DATA_DIR/"
    cp -r "$SCRIPT_DIR/src" "$USER_DATA_DIR/"
    cp "$SCRIPT_DIR/pom.xml" "$USER_DATA_DIR/"
fi

# Change to user data directory for write permissions
cd "$USER_DATA_DIR"

# Find JavaFX modules
JAVAFX_PATHS=(
    "/usr/share/openjfx/lib"
    "/usr/lib/jvm/java-17-openjdk-amd64/lib"
    "/usr/lib/jvm/java-17-openjdk/lib"
    "/usr/lib/jvm/default-java/lib"
    "/usr/lib/jvm/java-17-openjdk-arm64/lib"
)

JAVAFX_PATH=""
for path in "${JAVAFX_PATHS[@]}"; do
    if [ -d "$path" ] && [ -f "$path/javafx.controls.jar" ]; then
        JAVAFX_PATH="$path"
        break
    fi
done

if [ -z "$JAVAFX_PATH" ]; then
    echo "❌ JavaFX modules not found. Please install OpenJDK with JavaFX support."
    exit 1
fi

# Build classpath
CLASSPATH="target/classes"
CLASSPATH="$CLASSPATH:target/dependency/*"

# Run the application
exec java --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml \
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
     --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
     --add-opens javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
     --add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED \
     --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \
     -cp "$CLASSPATH" com.yunfx.autoshell.Main
EOF
    
    chmod +x "launch_autoshell.sh"
    
    # Create desktop entry
    cat > "$DESKTOP_FILE" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=$APP_NAME
Comment=JavaFX Script Manager for Linux
Exec=$SCRIPT_DIR/launch_autoshell.sh
Icon=$USER_ICON_DIR/yunfx-autoshell.png
Terminal=false
StartupNotify=true
Categories=Development;System;Utility;
Keywords=autoshell;yunfx;script;shell;automation;java;javafx;
StartupWMClass=YunFx AutoShell
MimeType=application/x-shellscript;
EOF
    
    # Install desktop entry
    cp "$DESKTOP_FILE" "$USER_APPS_DIR/"
    
    print_success "User installation completed"
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

# Main execution
main() {
    # Detect distribution
    detect_distro
    
    # Get package manager
    get_package_manager
    
    # Check sudo status
    check_sudo
    
    # Complete cleanup
    cleanup_existing
    
    # Install dependencies
    install_dependencies
    
    # Setup scripts directory
    setup_scripts_directory
    
    # Compile application
    compile_application
    
    # Install based on sudo status
    if [ "$SYSTEM_INSTALL" = true ]; then
        install_system_wide
        print_header "🎉 System-wide installation completed!"
        echo ""
        echo "📋 What was installed:"
        echo "   • Application: /opt/yunfx-autoshell"
        echo "   • Launcher: /usr/local/bin/yunfx-autoshell"
        echo "   • Icon: /usr/share/pixmaps/yunfx-autoshell.png"
        echo "   • Desktop entry: /usr/share/applications/yunfx-autoshell.desktop"
        echo ""
        echo "🚀 How to use:"
        echo "   • Run: yunfx-autoshell"
        echo "   • Find 'YunFx AutoShell' in applications menu"
        echo "   • Search for 'autoshell' or 'yunfx' in your app launcher"
        echo ""
    else
        install_user_only
        print_header "🎉 User installation completed!"
        echo ""
        echo "📋 What was installed:"
        echo "   • Desktop entry: $HOME/.local/share/applications/yunfx-autoshell.desktop"
        echo "   • Icon: $HOME/.local/share/icons/yunfx-autoshell.png"
        echo "   • Launcher: $SCRIPT_DIR/launch_autoshell.sh"
        echo ""
        echo "🚀 How to use:"
        echo "   • Find 'YunFx AutoShell' in applications menu"
        echo "   • Search for 'autoshell' or 'yunfx' in your app launcher"
        echo "   • Or run: $SCRIPT_DIR/launch_autoshell.sh"
        echo ""
        print_warning "For system-wide installation, run: sudo $0"
        echo ""
    fi
    
    # Refresh desktop environment
    refresh_desktop
    
    print_success "YunFx AutoShell is now ready to use!"
    echo ""
    print_status "You can now search for 'autoshell' or 'yunfx' in your applications menu."
    print_status "The application should appear immediately without requiring a restart."
}

# Run main function
main

#!/bin/bash

# YunFx AutoShell Complete Setup and Installation Script
# This script compiles from source, installs system-wide, and ensures proper desktop integration

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

print_header "🚀 YunFx AutoShell Complete Setup and Installation"
echo "========================================================"

# Check if running as root
check_sudo() {
    if [ "$EUID" -eq 0 ]; then
        print_warning "Running as root - this will install system-wide"
        return 0
    else
        print_status "Not running as root - will install for current user only"
        print_warning "For system-wide installation, run: sudo $0"
        return 1
    fi
}

# Check dependencies
check_dependencies() {
    print_status "Checking dependencies..."
    
    # Check if Java is installed
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install OpenJDK 17 or later."
        echo "Run: sudo apt update && sudo apt install openjdk-17-jdk"
        exit 1
    fi

    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java version $JAVA_VERSION is too old. Please install Java 17 or later."
        exit 1
    fi

    print_success "Java $JAVA_VERSION found"

    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        print_warning "Maven is not installed. Installing Maven..."
        sudo apt update
        sudo apt install -y maven
    fi

    print_success "Maven found"

    # Check if the project directory exists
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Please run this script from the project root directory."
        exit 1
    fi

    print_success "Project structure found"

    # Check JavaFX
    JAVAFX_PATHS=(
        "/usr/share/openjfx/lib"
        "/usr/lib/jvm/java-17-openjdk-amd64/lib"
        "/usr/lib/jvm/java-17-openjdk/lib"
        "/usr/lib/jvm/default-java/lib"
    )

    JAVAFX_PATH=""
    for path in "${JAVAFX_PATHS[@]}"; do
        if [ -d "$path" ] && [ -f "$path/javafx.controls.jar" ]; then
            JAVAFX_PATH="$path"
            break
        fi
    done

    if [ -z "$JAVAFX_PATH" ]; then
        print_warning "JavaFX modules not found. Installing JavaFX..."
        sudo apt install -y openjfx
        # Try to find JavaFX again
        for path in "${JAVAFX_PATHS[@]}"; do
            if [ -d "$path" ] && [ -f "$path/javafx.controls.jar" ]; then
                JAVAFX_PATH="$path"
                break
            fi
        done
        if [ -z "$JAVAFX_PATH" ]; then
            print_error "JavaFX installation failed. Please install manually: sudo apt install openjfx"
            exit 1
        fi
    fi

    print_success "JavaFX found at: $JAVAFX_PATH"
}

# Create default scripts directory
setup_scripts_directory() {
    DEFAULT_SCRIPTS_DIR="/home/yunfx/SCRIPTS"
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
    print_success "Scripts directory ready"
}

# Clean and compile application
compile_application() {
    print_status "Cleaning previous build..."
    mvn clean -q

    print_status "Compiling application from source..."
    mvn compile -q

    if [ $? -ne 0 ]; then
        print_error "Compilation failed. Please check the error messages above."
        exit 1
    fi

    print_success "Compilation successful"

    print_status "Packaging application..."
    mvn package -DskipTests -q

    if [ $? -ne 0 ]; then
        print_error "Packaging failed. Please check the error messages above."
        exit 1
    fi

    print_success "Packaging successful"

    # Ensure dependencies are available
    if [ ! -d "target/dependency" ]; then
        print_status "Downloading dependencies..."
        mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
    fi

    print_success "Dependencies ready"
}

# Install system-wide
install_system_wide() {
    print_status "Installing system-wide..."
    
    APP_NAME="YunFx AutoShell"
    APP_DIR="/opt/yunfx-autoshell"
    BIN_DIR="/usr/local/bin"
    ICON_DIR="/usr/share/pixmaps"
    DESKTOP_DIR="/usr/share/applications"

    # Clean up existing installations
    print_status "Cleaning up existing installations..."
    if [ -d "$APP_DIR" ]; then
        rm -rf "$APP_DIR"
    fi
    if [ -f "$BIN_DIR/yunfx-autoshell" ]; then
        rm -f "$BIN_DIR/yunfx-autoshell"
    fi
    if [ -f "$ICON_DIR/yunfx-autoshell.png" ]; then
        rm -f "$ICON_DIR/yunfx-autoshell.png"
    fi
    if [ -f "$DESKTOP_DIR/yunfx-autoshell.desktop" ]; then
        rm -f "$DESKTOP_DIR/yunfx-autoshell.desktop"
    fi

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
    echo "Run: sudo apt install openjfx"
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
Keywords=script;shell;automation;java;javafx;
StartupWMClass=YunFx AutoShell
EOF

    # Update desktop database
    if command -v update-desktop-database &> /dev/null; then
        update-desktop-database "$DESKTOP_DIR"
    fi

    print_success "System-wide installation completed"
}

# Install for current user only
install_user_only() {
    print_status "Installing for current user..."
    
    APP_NAME="YunFx AutoShell"
    USER_APPS_DIR="$HOME/.local/share/applications"
    USER_ICON_DIR="$HOME/.local/share/icons"
    DESKTOP_FILE="yunfx-autoshell.desktop"

    # Create directories
    mkdir -p "$USER_APPS_DIR"
    mkdir -p "$USER_ICON_DIR"

    # Clean up existing installations
    if [ -f "$USER_APPS_DIR/$DESKTOP_FILE" ]; then
        rm -f "$USER_APPS_DIR/$DESKTOP_FILE"
    fi
    if [ -f "$USER_ICON_DIR/yunfx-autoshell.png" ]; then
        rm -f "$USER_ICON_DIR/yunfx-autoshell.png"
    fi

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
    echo "Run: sudo apt install openjfx"
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
Keywords=script;shell;automation;java;javafx;
StartupWMClass=YunFx AutoShell
EOF

    # Install desktop entry
    cp "$DESKTOP_FILE" "$USER_APPS_DIR/"

    # Update desktop database
    if command -v update-desktop-database &> /dev/null; then
        update-desktop-database "$USER_APPS_DIR"
    fi

    print_success "User installation completed"
}

# Run the application
run_application() {
    print_status "Starting YunFx AutoShell..."
    echo "Default scripts directory: /home/yunfx/SCRIPTS"
    echo "You can change this directory from within the application."
    echo ""

    # Find JavaFX modules
    JAVAFX_PATHS=(
        "/usr/share/openjfx/lib"
        "/usr/lib/jvm/java-17-openjdk-amd64/lib"
        "/usr/lib/jvm/java-17-openjdk/lib"
        "/usr/lib/jvm/default-java/lib"
    )

    JAVAFX_PATH=""
    for path in "${JAVAFX_PATHS[@]}"; do
        if [ -d "$path" ] && [ -f "$path/javafx.controls.jar" ]; then
            JAVAFX_PATH="$path"
            break
        fi
    done

    if [ -z "$JAVAFX_PATH" ]; then
        print_error "JavaFX modules not found. Please install OpenJDK with JavaFX support."
        echo "Run: sudo apt install openjfx"
        exit 1
    fi

    print_success "Using JavaFX modules from: $JAVAFX_PATH"

    # Build classpath
    CLASSPATH="target/classes"
    CLASSPATH="$CLASSPATH:target/dependency/*"

    # Run the application
    java --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml \
         --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
         --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
         --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
         --add-opens javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
         --add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED \
         --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \
         -cp "$CLASSPATH" com.yunfx.autoshell.Main

    echo ""
    print_success "YunFx AutoShell has exited. Thank you for using the application!"
}

# Main execution
main() {
    # Check dependencies
    check_dependencies
    
    # Setup scripts directory
    setup_scripts_directory
    
    # Compile application
    compile_application
    
    # Check if running as root and install accordingly
    if check_sudo; then
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
        echo "   • Or run: $SCRIPT_DIR/launch_autoshell.sh"
        echo ""
        print_warning "For system-wide installation, run: sudo $0"
        echo ""
    fi
    
    # Ask if user wants to run the application
    read -p "Do you want to run the application now? (Y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Nn]$ ]]; then
        print_success "Setup completed! You can run the application from the applications menu."
    else
        run_application
    fi
}

# Run main function
main

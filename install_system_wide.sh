#!/bin/bash

# YunFx AutoShell System-Wide Installation Script
# This script installs the application system-wide for all users

set -e

echo "🚀 YunFx AutoShell System-Wide Installation"
echo "============================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "❌ This script must be run as root for system-wide installation"
    echo "Usage: sudo ./install_system_wide.sh"
    exit 1
fi

# Get the current directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_NAME="YunFx AutoShell"
APP_DIR="/opt/yunfx-autoshell"
BIN_DIR="/usr/local/bin"
ICON_DIR="/usr/share/pixmaps"
DESKTOP_DIR="/usr/share/applications"

echo "📁 Installing to: $APP_DIR"

# Create application directory
mkdir -p "$APP_DIR"
mkdir -p "$APP_DIR/media"

# Copy application files
echo "📦 Copying application files..."
cp -r "$SCRIPT_DIR/src" "$APP_DIR/"
cp -r "$SCRIPT_DIR/target" "$APP_DIR/"
cp "$SCRIPT_DIR/pom.xml" "$APP_DIR/"
cp "$SCRIPT_DIR/README.md" "$APP_DIR/"
cp "$SCRIPT_DIR/INSTALL.md" "$APP_DIR/"
cp "$SCRIPT_DIR/run_app.sh" "$APP_DIR/"
cp "$SCRIPT_DIR/launch_autoshell.sh" "$APP_DIR/"
cp "$SCRIPT_DIR/media/icon.png" "$APP_DIR/media/"

# Make scripts executable
chmod +x "$APP_DIR/run_app.sh"
chmod +x "$APP_DIR/launch_autoshell.sh"

# Create system-wide launcher script
cat > "$BIN_DIR/yunfx-autoshell" << EOF
#!/bin/bash
# YunFx AutoShell System Launcher

APP_DIR="$APP_DIR"
cd "\$APP_DIR"

# Check dependencies
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install OpenJDK 17 or later."
    echo "Run: sudo apt install openjdk-17-jdk"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Installing Maven..."
    apt update && apt install -y maven
fi

if [ ! -d "/usr/share/openjfx/lib" ]; then
    echo "❌ JavaFX not found. Installing JavaFX..."
    apt install -y openjfx
fi

# Ensure dependencies are available
if [ ! -d "target/dependency" ]; then
    echo "📦 Downloading dependencies..."
    mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
fi

# Compile if needed
if [ ! -f "target/classes/com/yunfx/autoshell/Main.class" ]; then
    echo "🔨 Compiling application..."
    mvn compile -q
fi

# Launch the application
exec "\$APP_DIR/run_app.sh"
EOF

chmod +x "$BIN_DIR/yunfx-autoshell"

# Copy icon to system directory
echo "🎨 Installing icon..."
cp "$APP_DIR/media/icon.png" "$ICON_DIR/yunfx-autoshell.png"

# Create system-wide desktop entry
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
    echo "✅ Desktop database updated"
fi

# Create uninstaller script
cat > "$APP_DIR/uninstall.sh" << 'EOF'
#!/bin/bash
# YunFx AutoShell Uninstaller

echo "🗑️  Uninstalling YunFx AutoShell..."

# Remove application files
rm -rf /opt/yunfx-autoshell

# Remove launcher
rm -f /usr/local/bin/yunfx-autoshell

# Remove icon
rm -f /usr/share/pixmaps/yunfx-autoshell.png

# Remove desktop entry
rm -f /usr/share/applications/yunfx-autoshell.desktop

# Update desktop database
if command -v update-desktop-database &> /dev/null; then
    update-desktop-database /usr/share/applications
fi

echo "✅ YunFx AutoShell uninstalled successfully"
EOF

chmod +x "$APP_DIR/uninstall.sh"

# Create systemd service for auto-start
cat > "/etc/systemd/system/yunfx-autoshell.service" << EOF
[Unit]
Description=YunFx AutoShell Script Manager
After=graphical-session.target

[Service]
Type=simple
ExecStart=$BIN_DIR/yunfx-autoshell
WorkingDirectory=$APP_DIR
Restart=on-failure
RestartSec=5
User=root
Environment=DISPLAY=:0

[Install]
WantedBy=multi-user.target
EOF

echo "✅ Systemd service created: /etc/systemd/system/yunfx-autoshell.service"

echo ""
echo "🎉 System-wide installation completed!"
echo ""
echo "📋 What was installed:"
echo "   • Application: $APP_DIR"
echo "   • Launcher: $BIN_DIR/yunfx-autoshell"
echo "   • Icon: $ICON_DIR/yunfx-autoshell.png"
echo "   • Desktop entry: $DESKTOP_DIR/yunfx-autoshell.desktop"
echo "   • Uninstaller: $APP_DIR/uninstall.sh"
echo ""
echo "🚀 How to use:"
echo "   • Run: yunfx-autoshell"
echo "   • Find 'YunFx AutoShell' in applications menu"
echo "   • Enable auto-start: systemctl enable yunfx-autoshell.service"
echo ""
echo "🗑️  To uninstall:"
echo "   sudo $APP_DIR/uninstall.sh"
echo ""
echo "✅ YunFx AutoShell is now installed system-wide!"

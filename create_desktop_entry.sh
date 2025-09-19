#!/bin/bash

# YunFx AutoShell Desktop Entry Creator
# This script creates a desktop entry for the application

set -e

echo "🚀 Creating YunFx AutoShell Desktop Entry..."

# Get the current directory (where the script is located)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_NAME="YunFx AutoShell"
DESKTOP_FILE="yunfx-autoshell.desktop"
ICON_FILE="icon.png"

# Check if icon exists
if [ ! -f "$SCRIPT_DIR/media/$ICON_FILE" ]; then
    echo "❌ Icon file not found: $SCRIPT_DIR/media/$ICON_FILE"
    echo "Please ensure the icon.png file is in the media/ directory"
    exit 1
fi

# Create media directory if it doesn't exist
mkdir -p "$SCRIPT_DIR/media"

# Copy icon to a standard location
ICON_PATH="$SCRIPT_DIR/media/$ICON_FILE"
echo "✅ Using icon: $ICON_PATH"

# Create the desktop entry file
cat > "$DESKTOP_FILE" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=$APP_NAME
Comment=JavaFX Script Manager for Linux
Exec=$SCRIPT_DIR/run_app.sh
Icon=$ICON_PATH
Terminal=false
StartupNotify=true
Categories=Development;System;Utility;
Keywords=script;shell;automation;java;javafx;
StartupWMClass=YunFx AutoShell
EOF

echo "✅ Desktop entry created: $DESKTOP_FILE"

# Make the desktop entry executable
chmod +x "$DESKTOP_FILE"

# Install to user applications directory
USER_APPS_DIR="$HOME/.local/share/applications"
mkdir -p "$USER_APPS_DIR"

# Copy desktop entry to applications directory
cp "$DESKTOP_FILE" "$USER_APPS_DIR/"
echo "✅ Installed to: $USER_APPS_DIR/$DESKTOP_FILE"

# Update desktop database
if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$USER_APPS_DIR"
    echo "✅ Desktop database updated"
fi

# Create desktop shortcut (optional)
DESKTOP_SHORTCUT="$HOME/Desktop/$DESKTOP_FILE"
if [ -d "$HOME/Desktop" ]; then
    cp "$DESKTOP_FILE" "$DESKTOP_SHORTCUT"
    chmod +x "$DESKTOP_SHORTCUT"
    echo "✅ Desktop shortcut created: $DESKTOP_SHORTCUT"
fi

# Create a systemd user service for auto-start (optional)
SERVICE_FILE="$HOME/.config/systemd/user/yunfx-autoshell.service"
mkdir -p "$HOME/.config/systemd/user"

cat > "$SERVICE_FILE" << EOF
[Unit]
Description=YunFx AutoShell Script Manager
After=graphical-session.target

[Service]
Type=simple
ExecStart=$SCRIPT_DIR/run_app.sh
WorkingDirectory=$SCRIPT_DIR
Restart=on-failure
RestartSec=5
Environment=DISPLAY=:0

[Install]
WantedBy=default.target
EOF

echo "✅ Systemd service created: $SERVICE_FILE"
echo "   To enable auto-start: systemctl --user enable yunfx-autoshell.service"

# Create a launcher script that handles dependencies
cat > "$SCRIPT_DIR/launch_autoshell.sh" << 'EOF'
#!/bin/bash

# YunFx AutoShell Launcher
# This script ensures all dependencies are available before launching

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 YunFx AutoShell Launcher"
echo "Checking dependencies..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install OpenJDK 17 or later."
    echo "Run: sudo apt install openjdk-17-jdk"
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Installing Maven..."
    sudo apt update && sudo apt install -y maven
fi

# Check JavaFX
if [ ! -d "/usr/share/openjfx/lib" ]; then
    echo "❌ JavaFX not found. Installing JavaFX..."
    sudo apt install -y openjfx
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
echo "🎯 Launching YunFx AutoShell..."
exec "$SCRIPT_DIR/run_app.sh"
EOF

chmod +x "$SCRIPT_DIR/launch_autoshell.sh"

# Update the desktop entry to use the launcher
sed -i "s|Exec=$SCRIPT_DIR/run_app.sh|Exec=$SCRIPT_DIR/launch_autoshell.sh|g" "$DESKTOP_FILE"
sed -i "s|Exec=$SCRIPT_DIR/run_app.sh|Exec=$SCRIPT_DIR/launch_autoshell.sh|g" "$USER_APPS_DIR/$DESKTOP_FILE"

if [ -f "$DESKTOP_SHORTCUT" ]; then
    sed -i "s|Exec=$SCRIPT_DIR/run_app.sh|Exec=$SCRIPT_DIR/launch_autoshell.sh|g" "$DESKTOP_SHORTCUT"
fi

echo ""
echo "🎉 Desktop entry creation completed!"
echo ""
echo "📋 What was created:"
echo "   • Desktop entry: $DESKTOP_FILE"
echo "   • Installed to: $USER_APPS_DIR/$DESKTOP_FILE"
echo "   • Desktop shortcut: $DESKTOP_SHORTCUT"
echo "   • Launcher script: $SCRIPT_DIR/launch_autoshell.sh"
echo "   • Systemd service: $SERVICE_FILE"
echo ""
echo "🚀 How to use:"
echo "   • Find 'YunFx AutoShell' in your applications menu"
echo "   • Double-click the desktop shortcut"
echo "   • Or run: $SCRIPT_DIR/launch_autoshell.sh"
echo ""
echo "⚙️  Optional - Enable auto-start:"
echo "   systemctl --user enable yunfx-autoshell.service"
echo "   systemctl --user start yunfx-autoshell.service"
echo ""
echo "✅ YunFx AutoShell is now integrated into your desktop environment!"

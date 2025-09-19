#!/bin/bash
# YunFx AutoShell Fixed Launcher

USER_DATA_DIR="$HOME/.local/share/yunfx-autoshell"

# Create user data directory if it doesn't exist
mkdir -p "$USER_DATA_DIR"

# Copy application files to user directory if needed
if [ ! -d "$USER_DATA_DIR/target" ] || [ "/opt/yunfx-autoshell/target" -nt "$USER_DATA_DIR/target" ]; then
    echo "📦 Updating application files..."
    cp -r "/opt/yunfx-autoshell/target" "$USER_DATA_DIR/"
    cp -r "/opt/yunfx-autoshell/src" "$USER_DATA_DIR/"
    cp "/opt/yunfx-autoshell/pom.xml" "$USER_DATA_DIR/"
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

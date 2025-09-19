#!/bin/bash

# Simple run script for YunFx AutoShell
# This script runs the application without Maven dependency resolution

echo "🚀 Starting YunFx AutoShell..."

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

echo "✅ Using JavaFX modules from: $JAVAFX_PATH"

# Build classpath with dependencies
CLASSPATH="target/classes"
CLASSPATH="$CLASSPATH:target/dependency/*"

# Create dependency directory if it doesn't exist
mkdir -p target/dependency

# Copy dependencies if they don't exist
if [ ! -f "target/dependency/sqlite-jdbc-3.44.1.0.jar" ]; then
    echo "📦 Copying dependencies..."
    mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
fi

# Run the application with JVM arguments for JFoenix compatibility
echo "🎯 Launching YunFx AutoShell..."
java --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml \
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
     --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
     --add-opens javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
     --add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED \
     --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \
     -cp "$CLASSPATH" com.yunfx.autoshell.Main

echo "👋 YunFx AutoShell has exited."

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

#!/bin/bash

# YunFx AutoShell Setup and Run Script
# This script compiles and runs the JavaFX desktop application

set -e  # Exit on any error

echo "=== YunFx AutoShell Setup and Run ==="
echo "Checking dependencies..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install OpenJDK 17 or later."
    echo "Run: sudo apt update && sudo apt install openjdk-17-jdk"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java version $JAVA_VERSION is too old. Please install Java 17 or later."
    exit 1
fi

echo "✅ Java $JAVA_VERSION found"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Installing Maven..."
    sudo apt update
    sudo apt install -y maven
fi

echo "✅ Maven found"

# Check if the project directory exists
if [ ! -f "pom.xml" ]; then
    echo "❌ pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

echo "✅ Project structure found"

# Create default scripts directory if it doesn't exist
DEFAULT_SCRIPTS_DIR="/home/yunfx/SCRIPTS"
if [ ! -d "$DEFAULT_SCRIPTS_DIR" ]; then
    echo "📁 Creating default scripts directory: $DEFAULT_SCRIPTS_DIR"
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
    echo "✅ Created sample script: $DEFAULT_SCRIPTS_DIR/sample_script.sh"
fi

echo "✅ Scripts directory ready"

# Clean previous build
echo "🧹 Cleaning previous build..."
mvn clean

# Compile the application
echo "🔨 Compiling application..."
mvn compile

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed. Please check the error messages above."
    exit 1
fi

echo "✅ Compilation successful"

# Package the application
echo "📦 Packaging application..."
mvn package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Packaging failed. Please check the error messages above."
    exit 1
fi

echo "✅ Packaging successful"

# Run the application
echo "🚀 Starting YunFx AutoShell..."
echo "Default scripts directory: $DEFAULT_SCRIPTS_DIR"
echo "You can change this directory from within the application."
echo ""

# Run with JavaFX modules
# Try different JavaFX module paths
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

# Build classpath
CLASSPATH="target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)"

# Run the application with JVM arguments for JFoenix compatibility
java --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml \
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
     --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
     --add-opens javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
     --add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED \
     --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \
     -cp "$CLASSPATH" com.yunfx.autoshell.Main

echo ""
echo "👋 YunFx AutoShell has exited. Thank you for using the application!"

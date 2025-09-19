#!/bin/bash

# Test script to demonstrate GUI sudo functionality
# This script simulates missing dependencies to test the GUI prompts

echo "🧪 Testing YunFx AutoShell GUI Sudo Integration"
echo "=============================================="

# Temporarily rename maven to simulate missing dependency
if command -v mvn &> /dev/null; then
    echo "📦 Temporarily renaming mvn to test GUI prompt..."
    sudo mv /usr/bin/mvn /usr/bin/mvn.backup 2>/dev/null || true
fi

# Temporarily rename javafx to simulate missing dependency
if [ -d "/usr/share/openjfx" ]; then
    echo "🎨 Temporarily renaming JavaFX to test GUI prompt..."
    sudo mv /usr/share/openjfx /usr/share/openjfx.backup 2>/dev/null || true
fi

echo ""
echo "🚀 Starting YunFx AutoShell..."
echo "The application should now detect missing dependencies and prompt you through the GUI to install them."
echo ""

# Launch the application
./launch_autoshell.sh

echo ""
echo "🔄 Restoring dependencies..."

# Restore maven
if [ -f "/usr/bin/mvn.backup" ]; then
    sudo mv /usr/bin/mvn.backup /usr/bin/mvn
    echo "✅ Maven restored"
fi

# Restore javafx
if [ -d "/usr/share/openjfx.backup" ]; then
    sudo mv /usr/share/openjfx.backup /usr/share/openjfx
    echo "✅ JavaFX restored"
fi

echo "✅ Test completed!"

#!/bin/bash

# Test script to demonstrate terminal sudo functionality
# This script simulates missing dependencies to test the terminal opening

echo "🧪 Testing YunFx AutoShell Terminal Sudo Integration"
echo "=================================================="

# Check if we can open a terminal
if ! command -v gnome-terminal &> /dev/null; then
    echo "❌ gnome-terminal not found. Installing..."
    sudo apt update && sudo apt install -y gnome-terminal
fi

echo ""
echo "🚀 Starting YunFx AutoShell..."
echo "The application should now detect missing dependencies and open terminal windows for sudo commands."
echo ""

# Launch the application
./launch_autoshell.sh

echo ""
echo "✅ Test completed!"

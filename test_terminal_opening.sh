#!/bin/bash

# Simple test to verify terminal opening works
echo "🧪 Testing Terminal Opening Functionality"
echo "========================================"

# Test opening a terminal with a simple command
echo "Opening test terminal..."
gnome-terminal --title="YunFx AutoShell Test" -- bash -c 'echo "✅ Terminal opened successfully!"; echo "This is a test terminal for YunFx AutoShell"; echo ""; echo "Press any key to close..."; read -n 1'

echo "✅ Terminal test completed!"

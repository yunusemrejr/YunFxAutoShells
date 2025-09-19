# Desktop Integration Guide

This guide explains how to integrate YunFx AutoShell into your Linux desktop environment.

## 🚀 Quick Start

### User Installation (Recommended)
```bash
./create_desktop_entry.sh
```

### System-Wide Installation (All Users)
```bash
sudo ./install_system_wide.sh
```

## 📋 What Gets Created

### User Installation
- **Desktop Entry**: `~/.local/share/applications/yunfx-autoshell.desktop`
- **Desktop Shortcut**: `~/Desktop/yunfx-autoshell.desktop`
- **Launcher Script**: `launch_autoshell.sh`
- **Systemd Service**: `~/.config/systemd/user/yunfx-autoshell.service`

### System-Wide Installation
- **Application Directory**: `/opt/yunfx-autoshell/`
- **System Launcher**: `/usr/local/bin/yunfx-autoshell`
- **System Icon**: `/usr/share/pixmaps/yunfx-autoshell.png`
- **System Desktop Entry**: `/usr/share/applications/yunfx-autoshell.desktop`
- **Systemd Service**: `/etc/systemd/system/yunfx-autoshell.service`
- **Uninstaller**: `/opt/yunfx-autoshell/uninstall.sh`

## 🎯 How to Use

### After User Installation
1. **Applications Menu**: Find "YunFx AutoShell" in your applications menu
2. **Desktop Shortcut**: Double-click the desktop shortcut
3. **Command Line**: Run `./launch_autoshell.sh`

### After System-Wide Installation
1. **Applications Menu**: Find "YunFx AutoShell" in your applications menu
2. **Command Line**: Run `yunfx-autoshell` from anywhere
3. **Desktop Shortcut**: Double-click the desktop shortcut

## ⚙️ Auto-Start Configuration

### User Auto-Start
```bash
# Enable auto-start for current user
systemctl --user enable yunfx-autoshell.service
systemctl --user start yunfx-autoshell.service

# Disable auto-start
systemctl --user disable yunfx-autoshell.service
```

### System Auto-Start
```bash
# Enable auto-start for all users
sudo systemctl enable yunfx-autoshell.service
sudo systemctl start yunfx-autoshell.service

# Disable auto-start
sudo systemctl disable yunfx-autoshell.service
```

## 🔧 Features

### Automatic Dependency Management
The launcher scripts automatically:
- ✅ Check for Java 17+
- ✅ Install Maven if missing
- ✅ Install JavaFX if missing
- ✅ Download project dependencies
- ✅ Compile the application if needed
- ✅ Launch with proper JVM arguments

### Desktop Integration
- ✅ **Application Menu**: Appears in Development/System/Utility categories
- ✅ **Desktop Shortcut**: Double-click to launch
- ✅ **System Tray**: Can be configured for background operation
- ✅ **File Associations**: Can be extended for .sh file associations
- ✅ **Search Integration**: Appears in desktop search results

### Icon and Branding
- ✅ **Custom Icon**: Uses the provided icon.png
- ✅ **Professional Appearance**: Material Design styling
- ✅ **Consistent Branding**: YunFx AutoShell identity

## 🗑️ Uninstallation

### User Installation
```bash
# Remove desktop entries
rm -f ~/.local/share/applications/yunfx-autoshell.desktop
rm -f ~/Desktop/yunfx-autoshell.desktop

# Remove systemd service
rm -f ~/.config/systemd/user/yunfx-autoshell.service

# Disable auto-start
systemctl --user disable yunfx-autoshell.service
```

### System-Wide Installation
```bash
# Use the provided uninstaller
sudo /opt/yunfx-autoshell/uninstall.sh
```

## 🔍 Troubleshooting

### Application Won't Start
1. **Check Dependencies**:
   ```bash
   java -version  # Should be 17+
   mvn -version   # Should be 3.6+
   ls /usr/share/openjfx/lib/  # Should exist
   ```

2. **Check Permissions**:
   ```bash
   ls -la launch_autoshell.sh  # Should be executable
   ls -la run_app.sh          # Should be executable
   ```

3. **Check Logs**:
   ```bash
   journalctl --user -u yunfx-autoshell.service  # User service logs
   journalctl -u yunfx-autoshell.service          # System service logs
   ```

### Desktop Entry Issues
1. **Refresh Desktop Database**:
   ```bash
   update-desktop-database ~/.local/share/applications
   ```

2. **Check Desktop Entry**:
   ```bash
   cat ~/.local/share/applications/yunfx-autoshell.desktop
   ```

3. **Test Execution**:
   ```bash
   ./launch_autoshell.sh
   ```

### Icon Not Showing
1. **Check Icon Path**:
   ```bash
   ls -la media/icon.png
   ```

2. **Update Icon Cache**:
   ```bash
   gtk-update-icon-cache -f -t ~/.local/share/icons
   ```

## 📱 Desktop Environment Support

### Tested On
- ✅ **GNOME**: Full integration with applications menu
- ✅ **KDE Plasma**: Full integration with applications menu
- ✅ **XFCE**: Full integration with applications menu
- ✅ **MATE**: Full integration with applications menu
- ✅ **Cinnamon**: Full integration with applications menu

### Features by Desktop
- **GNOME**: Search integration, notifications
- **KDE**: System tray, advanced notifications
- **XFCE**: Panel integration, lightweight
- **MATE**: Traditional desktop integration
- **Cinnamon**: Modern desktop with traditional feel

## 🎨 Customization

### Changing the Icon
1. Replace `media/icon.png` with your preferred icon
2. Run `./create_desktop_entry.sh` again
3. The new icon will be used automatically

### Modifying Categories
Edit the desktop entry file and change the `Categories` line:
```ini
Categories=Development;System;Utility;Office;
```

### Adding Keywords
Edit the desktop entry file and modify the `Keywords` line:
```ini
Keywords=script;shell;automation;java;javafx;development;
```

## 🚀 Advanced Configuration

### Custom Launch Arguments
Edit `launch_autoshell.sh` to add custom JVM arguments:
```bash
# Add custom memory settings
JAVA_OPTS="-Xmx2g -Xms1g"
java $JAVA_OPTS --module-path "$JAVAFX_PATH" ...
```

### Environment Variables
Set custom environment variables in the launcher:
```bash
export YUNFX_SCRIPTS_DIR="/custom/scripts/path"
export YUNFX_LOG_LEVEL="DEBUG"
```

### Multiple Instances
To allow multiple instances, modify the desktop entry:
```ini
StartupWMClass=YunFx AutoShell
# Remove or comment out the above line
```

This comprehensive desktop integration makes YunFx AutoShell a first-class citizen in your Linux desktop environment! 🎉

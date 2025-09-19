# Installation Guide

## Prerequisites

Before running YunFx AutoShell, you need to install the following dependencies:

### 1. Java 17 or later
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Maven (for building the project)
```bash
sudo apt install maven
```

### 3. JavaFX (for the UI)
```bash
sudo apt install openjfx
```

## Quick Installation

Run the setup script which will check and install dependencies automatically:

```bash
./setup_and_run.sh
```

## Manual Installation

If you prefer to install dependencies manually:

1. **Install Java 17**:
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jdk
   ```

2. **Install Maven**:
   ```bash
   sudo apt install maven
   ```

3. **Install JavaFX**:
   ```bash
   sudo apt install openjfx
   ```

4. **Build the project**:
   ```bash
   mvn clean compile package
   ```

5. **Run the application**:
   ```bash
   java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) com.yunfx.autoshell.Main
   ```

## Troubleshooting

### JavaFX Module Not Found
If you get a "module not found" error for JavaFX:

1. Check if JavaFX is installed:
   ```bash
   ls /usr/share/openjfx/lib/
   ```

2. If not installed, install it:
   ```bash
   sudo apt install openjfx
   ```

3. If still having issues, try alternative paths:
   ```bash
   find /usr -name "javafx.controls.jar" 2>/dev/null
   ```

### Maven Not Found
If Maven is not available:

1. Install Maven:
   ```bash
   sudo apt install maven
   ```

2. Or use the system package manager to install Maven

### Permission Denied
If you get permission errors:

1. Make the setup script executable:
   ```bash
   chmod +x setup_and_run.sh
   ```

2. Ensure you have write permissions in the project directory

## Default Scripts Directory

The application will create a default scripts directory at `/home/yunfx/SCRIPTS` with a sample script if it doesn't exist.

You can change this directory from within the application using the "Select Script Directory" button.

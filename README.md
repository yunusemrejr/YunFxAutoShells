# YunFx AutoShell

A JavaFX desktop application for managing and executing shell scripts on Linux Ubuntu Pop_OS with Material Design Components.

## Features

- **Script Discovery**: Automatically discover shell scripts (*.sh) in nested directories
- **Script Management**: Create, edit, delete, and organize scripts through the UI
- **Group Management**: Create groups/tags for organizing related scripts
- **Sequential Execution**: Run groups of scripts sequentially with visual feedback
- **Database Persistence**: Save groups, tags, and script metadata in SQLite
- **Search & Filter**: Find scripts by name, description, or group
- **Material Design**: Modern UI using JavaFX with Material Design Components

## Requirements

- Java 17 or later
- Maven 3.6 or later
- Linux Ubuntu Pop_OS (or compatible Linux distribution)
- OpenJDK with JavaFX modules

## Quick Start

1. **Run the application**:
   ```bash
   ./setup_and_run.sh
   ```

2. **Select a directory** containing your shell scripts (default: `/home/yunfx/SCRIPTS`)

3. **Create groups** to organize your scripts

4. **Add scripts to groups** for easy management

5. **Execute scripts** individually or as groups

## Project Structure

```
YunFxAutoShell/
├── src/main/java/com/yunfx/autoshell/
│   ├── Main.java                          # Application entry point
│   ├── model/
│   │   ├── Script.java                    # Script data model
│   │   └── ScriptGroup.java               # Script group data model
│   ├── database/
│   │   └── DatabaseManager.java           # SQLite database operations
│   ├── service/
│   │   ├── ScriptDiscoveryService.java    # Script discovery and analysis
│   │   └── ScriptExecutionService.java    # Script execution with feedback
│   └── ui/
│       └── MainController.java            # Main UI controller
├── src/main/resources/
│   └── styles.css                         # Material Design styling
├── pom.xml                                # Maven dependencies
├── setup_and_run.sh                      # Setup and run script
└── README.md                              # This file
```

## Usage

### Script Discovery
- The application automatically scans the selected directory for `.sh` files
- Scripts are analyzed for metadata (description, tags) from comments
- Example script with metadata:
  ```bash
  #!/bin/bash
  # This script updates the system packages
  # Tag: system, update, maintenance
  
  sudo apt update && sudo apt upgrade -y
  ```

### Group Management
- Create groups to organize related scripts
- Add scripts to multiple groups
- Execute entire groups sequentially
- Groups are persisted in SQLite database

### Script Execution
- Execute individual scripts with real-time feedback
- Run groups of scripts sequentially
- Visual indicators show success/failure status
- Output and error messages are displayed

## Database Schema

The application uses SQLite with the following tables:
- `script_groups`: Stores group information
- `scripts`: Stores script metadata
- `group_scripts`: Many-to-many relationship between groups and scripts
- `script_tags`: Stores script tags

## Development

### Building from source:
```bash
mvn clean compile package
```

### Running without the setup script:
```bash
java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) com.yunfx.autoshell.Main
```

## Dependencies

- **JavaFX 21.0.1**: UI framework
- **JFoenix 9.0.10**: Material Design Components
- **SQLite JDBC 3.44.1.0**: Database connectivity

## License

This project is part of the YunFx AutoShell suite.

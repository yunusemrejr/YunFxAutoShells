package com.yunfx.autoshell.ui;

import com.jfoenix.controls.*;
import com.yunfx.autoshell.database.DatabaseManager;
import com.yunfx.autoshell.model.Script;
import com.yunfx.autoshell.model.ScriptGroup;
import com.yunfx.autoshell.service.ScriptDiscoveryService;
import com.yunfx.autoshell.service.ScriptExecutionService;
import com.yunfx.autoshell.service.SudoService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController {
    private Stage primaryStage;
    private DatabaseManager dbManager;
    private ScriptDiscoveryService discoveryService;
    private ScriptExecutionService executionService;
    private SudoService sudoService;
    
    // UI Components
    private JFXTextField searchField;
    private JFXComboBox<String> groupFilterCombo;
    private TableView<Script> scriptTable;
    private TableView<ScriptGroup> groupTable;
    private JFXButton refreshButton;
    private JFXButton addGroupButton;
    private JFXButton removeGroupButton;
    private JFXButton executeGroupButton;
    private JFXButton selectDirectoryButton;
    private Label statusLabel;
    private ProgressBar progressBar;
    
    // Data
    private ObservableList<Script> scripts;
    private ObservableList<ScriptGroup> groups;
    private String currentDirectory = "/home/yunfx/SCRIPTS";
    
    public void initialize(Stage stage) {
        this.primaryStage = stage;
        this.dbManager = DatabaseManager.getInstance();
        this.discoveryService = new ScriptDiscoveryService();
        this.executionService = new ScriptExecutionService();
        this.sudoService = new SudoService();
        
        initializeData();
        createUI();
        loadData();
        
        primaryStage.setTitle("YunFx AutoShell - Script Manager");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }
    
    private void initializeData() {
        scripts = FXCollections.observableArrayList();
        groups = FXCollections.observableArrayList();
    }
    
    private void createUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #313131;");
        
        // Top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Main content area
        SplitPane mainContent = new SplitPane();
        mainContent.setDividerPositions(0.3, 0.7);
        mainContent.setStyle("-fx-background-color: #313131;");
        
        // Left panel - Groups
        VBox groupPanel = createGroupPanel();
        mainContent.getItems().add(groupPanel);
        
        // Right panel - Scripts
        VBox scriptPanel = createScriptPanel();
        mainContent.getItems().add(scriptPanel);
        
        root.setCenter(mainContent);
        
        // Bottom status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #313131; -fx-border-color: #666666; -fx-border-width: 0 0 1 0;");
        
        // Directory selection
        selectDirectoryButton = new JFXButton("Select Script Directory");
        selectDirectoryButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-font-weight: bold; -fx-padding: 8 16;");
        selectDirectoryButton.setOnAction(this::selectDirectory);
        
        // Search field
        searchField = new JFXTextField();
        searchField.setPromptText("Search scripts...");
        searchField.setPrefWidth(200);
        searchField.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-prompt-text-fill: #666666; -fx-padding: 8 12;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterScripts());
        
        // Refresh button
        refreshButton = new JFXButton("Refresh");
        refreshButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-font-weight: bold; -fx-padding: 8 16;");
        refreshButton.setOnAction(e -> refreshScripts());
        
        // Add group button
        addGroupButton = new JFXButton("Add Group");
        addGroupButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-font-weight: bold; -fx-padding: 8 16;");
        addGroupButton.setOnAction(this::addGroup);
        
        // Remove group button
        removeGroupButton = new JFXButton("Remove Group");
        removeGroupButton.setStyle("-fx-background-color: #ec503b; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 8 16;");
        removeGroupButton.setOnAction(this::removeSelectedGroup);
        removeGroupButton.setDisable(true);
        
        // Execute group button
        executeGroupButton = new JFXButton("Execute Group");
        executeGroupButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-font-weight: bold; -fx-padding: 8 16;");
        executeGroupButton.setOnAction(this::executeSelectedGroup);
        executeGroupButton.setDisable(true);
        
        toolbar.getChildren().addAll(
            selectDirectoryButton, searchField, refreshButton, 
            addGroupButton, removeGroupButton, executeGroupButton
        );
        
        return toolbar;
    }
    
    private VBox createGroupPanel() {
        VBox groupPanel = new VBox(10);
        groupPanel.setPadding(new Insets(15));
        groupPanel.setStyle("-fx-background-color: #313131; -fx-border-color: #666666; -fx-border-width: 1; -fx-border-radius: 5;");
        
        Label groupLabel = new Label("Script Groups");
        groupLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        // Group filter combo
        groupFilterCombo = new JFXComboBox<>();
        groupFilterCombo.setPromptText("Filter by group");
        groupFilterCombo.getItems().add("All Scripts");
        groupFilterCombo.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-prompt-text-fill: #666666; -fx-padding: 8 12;");
        groupFilterCombo.setOnAction(e -> filterScripts());
        
        // Groups table
        groupTable = new TableView<>();
        groupTable.setPrefHeight(400);
        groupTable.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131;");
        
        TableColumn<ScriptGroup, String> groupNameCol = new TableColumn<>("Group Name");
        groupNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        groupNameCol.setPrefWidth(150);
        groupNameCol.setStyle("-fx-background-color: #666666; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        TableColumn<ScriptGroup, Integer> scriptCountCol = new TableColumn<>("Scripts");
        scriptCountCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getScripts().size()).asObject());
        scriptCountCol.setPrefWidth(80);
        scriptCountCol.setStyle("-fx-background-color: #666666; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        groupTable.getColumns().addAll(groupNameCol, scriptCountCol);
        groupTable.setItems(groups);
        
        // Group selection listener
        groupTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                groupFilterCombo.setValue(newVal.getName());
                filterScripts();
                executeGroupButton.setDisable(false);
                removeGroupButton.setDisable(false);
            } else {
                executeGroupButton.setDisable(true);
                removeGroupButton.setDisable(true);
            }
        });
        
        // Group filter combo listener
        groupFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            filterScripts();
            // Enable execute button if a specific group is selected (not "All Scripts")
            boolean isGroupSelected = newVal != null && !newVal.equals("All Scripts");
            executeGroupButton.setDisable(!isGroupSelected);
            removeGroupButton.setDisable(!isGroupSelected);
        });
        
        groupPanel.getChildren().addAll(groupLabel, groupFilterCombo, groupTable);
        return groupPanel;
    }
    
    private VBox createScriptPanel() {
        VBox scriptPanel = new VBox(10);
        scriptPanel.setPadding(new Insets(15));
        scriptPanel.setStyle("-fx-background-color: #313131; -fx-border-color: #666666; -fx-border-width: 1; -fx-border-radius: 5;");
        
        Label scriptLabel = new Label("Scripts");
        scriptLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        // Scripts table
        scriptTable = new TableView<>();
        scriptTable.setPrefHeight(400);
        scriptTable.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131;");
        
        TableColumn<Script, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        nameCol.setStyle("-fx-background-color: #666666; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        TableColumn<Script, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setPrefWidth(250);
        descriptionCol.setStyle("-fx-background-color: #666666; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        TableColumn<Script, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilePath().toString()));
        pathCol.setPrefWidth(300);
        pathCol.setStyle("-fx-background-color: #666666; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        TableColumn<Script, Boolean> executableCol = new TableColumn<>("Executable");
        executableCol.setCellValueFactory(new PropertyValueFactory<>("executable"));
        executableCol.setPrefWidth(100);
        executableCol.setStyle("-fx-background-color: #666666; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        
        TableColumn<Script, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setStyle("-fx-background-color: #666666; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        actionsCol.setCellFactory(param -> new TableCell<Script, Void>() {
            private final JFXButton executeBtn = new JFXButton("Execute");
            private final JFXButton terminalBtn = new JFXButton("Terminal");
            private final JFXButton addToGroupBtn = new JFXButton("Add to Group");
            
            {
                executeBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8;");
                terminalBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #313131; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8;");
                addToGroupBtn.setStyle("-fx-background-color: #ec503b; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8;");
                
                executeBtn.setOnAction(e -> executeScript(getTableView().getItems().get(getIndex())));
                terminalBtn.setOnAction(e -> executeScriptInTerminal(getTableView().getItems().get(getIndex())));
                addToGroupBtn.setOnAction(e -> addScriptToGroup(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(3);
                    buttons.getChildren().addAll(executeBtn, terminalBtn, addToGroupBtn);
                    setGraphic(buttons);
                }
            }
        });
        
        scriptTable.getColumns().addAll(nameCol, descriptionCol, pathCol, executableCol, actionsCol);
        scriptTable.setItems(scripts);
        
        scriptPanel.getChildren().addAll(scriptLabel, scriptTable);
        return scriptPanel;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #313131; -fx-border-color: #666666; -fx-border-width: 1 0 0 0;");
        
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);
        progressBar.setStyle("-fx-accent: #ec503b;");
        
        statusBar.getChildren().addAll(statusLabel, progressBar);
        return statusBar;
    }
    
    private void loadData() {
        try {
            // Check and install dependencies first
            checkAndInstallDependencies();
            
            // Load groups from database
            groups.clear();
            groups.addAll(dbManager.getAllGroups());
            
            // Update group filter combo
            groupFilterCombo.getItems().clear();
            groupFilterCombo.getItems().add("All Scripts");
            groups.forEach(group -> groupFilterCombo.getItems().add(group.getName()));
            
            // Load scripts from current directory
            refreshScripts();
            
        } catch (Exception e) {
            showError("Error loading data", e.getMessage());
        }
    }
    
    private void checkAndInstallDependencies() {
        statusLabel.setText("Checking dependencies...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        // Check Java
        sudoService.checkDependencyAsync("java").thenAccept(result -> {
            if (!result.isSuccess()) {
                Platform.runLater(() -> {
                    showDependencyInstallDialog("Java", "openjdk-17-jdk");
                });
            }
        });
        
        // Check Maven
        sudoService.checkDependencyAsync("mvn").thenAccept(result -> {
            if (!result.isSuccess()) {
                Platform.runLater(() -> {
                    showDependencyInstallDialog("Maven", "maven");
                });
            }
        });
        
        // Check JavaFX
        sudoService.executeCommandAsync("ls /usr/share/openjfx/lib/").thenAccept(result -> {
            if (!result.isSuccess()) {
                Platform.runLater(() -> {
                    showDependencyInstallDialog("JavaFX", "openjfx");
                });
            }
        });
    }
    
    private void showDependencyInstallDialog(String dependencyName, String packageName) {
        Alert installDialog = new Alert(Alert.AlertType.CONFIRMATION);
        installDialog.setTitle("Install " + dependencyName);
        installDialog.setHeaderText(dependencyName + " is required but not installed");
        installDialog.setContentText("Would you like to install " + dependencyName + " now?\n\nA terminal window will open where you can enter your password for administrator privileges.");
        
        Optional<ButtonType> result = installDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            installDependency(packageName);
        } else {
            showError("Missing Dependency", dependencyName + " is required to run this application. Please install it manually or restart the application to install it automatically.");
        }
    }
    
    private void installDependency(String packageName) {
        statusLabel.setText("Opening terminal for " + packageName + " installation...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        sudoService.installDependencyAsync(packageName).thenAccept(result -> {
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                if (result.isSuccess()) {
                    statusLabel.setText("Terminal opened for " + packageName + " installation");
                    showInfo("Terminal Opened", "A terminal window has opened for installing " + packageName + ".\nPlease follow the instructions in the terminal and enter your password when prompted.\n\nThe terminal will close automatically when the installation is complete.");
                } else {
                    statusLabel.setText("Failed to open terminal for " + packageName);
                    showError("Terminal Error", "Failed to open terminal for " + packageName + ":\n" + result.getError());
                }
            });
        });
    }
    
    private void selectDirectory(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Script Directory");
        chooser.setInitialDirectory(new File(currentDirectory));
        
        File selectedDirectory = chooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            currentDirectory = selectedDirectory.getAbsolutePath();
            refreshScripts();
        }
    }
    
    private void refreshScripts() {
        try {
            statusLabel.setText("Discovering scripts...");
            progressBar.setVisible(true);
            progressBar.setProgress(-1); // Indeterminate progress
            
            List<Script> discoveredScripts = discoveryService.discoverScripts(currentDirectory);
            
            // Save scripts to database
            for (Script script : discoveredScripts) {
                dbManager.saveScript(script);
            }
            
            scripts.clear();
            scripts.addAll(discoveredScripts);
            
            statusLabel.setText("Found " + scripts.size() + " scripts");
            progressBar.setVisible(false);
            
        } catch (Exception e) {
            showError("Error discovering scripts", e.getMessage());
            statusLabel.setText("Error discovering scripts");
            progressBar.setVisible(false);
        }
    }
    
    private void refreshGroups() {
        try {
            // Reload groups from database
            groups.clear();
            groups.addAll(dbManager.getAllGroups());
            
            // Update group filter combo
            groupFilterCombo.getItems().clear();
            groupFilterCombo.getItems().add("All Scripts");
            groups.forEach(group -> groupFilterCombo.getItems().add(group.getName()));
            
        } catch (Exception e) {
            showError("Error refreshing groups", e.getMessage());
        }
    }
    
    private void filterScripts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedGroup = groupFilterCombo.getValue();
        
        scripts.clear();
        
        try {
            List<Script> scriptsToShow;
            
            if (selectedGroup == null || selectedGroup.equals("All Scripts")) {
                // Show all scripts
                scriptsToShow = dbManager.getAllScripts();
            } else {
                // Find the selected group and get its scripts
                ScriptGroup selectedGroupObj = null;
                for (ScriptGroup group : groups) {
                    if (group.getName().equals(selectedGroup)) {
                        selectedGroupObj = group;
                        break;
                    }
                }
                
                if (selectedGroupObj != null) {
                    // Get scripts for this specific group from database
                    scriptsToShow = dbManager.getScriptsByGroup(selectedGroupObj.getId());
                } else {
                    scriptsToShow = new ArrayList<>();
                }
            }
            
            // Apply search filter
            for (Script script : scriptsToShow) {
                boolean matchesSearch = searchText.isEmpty() || 
                    script.getName().toLowerCase().contains(searchText) ||
                    script.getDescription().toLowerCase().contains(searchText);
                
                if (matchesSearch) {
                    scripts.add(script);
                }
            }
        } catch (Exception e) {
            showError("Error filtering scripts", e.getMessage());
        }
    }
    
    private void addGroup(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add New Group");
        dialog.setHeaderText("Create a new script group");
        dialog.setContentText("Group name:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                ScriptGroup group = new ScriptGroup(result.get().trim());
                dbManager.saveGroup(group);
                groups.add(group);
                groupFilterCombo.getItems().add(group.getName());
                statusLabel.setText("Group created: " + group.getName());
            } catch (Exception e) {
                showError("Error creating group", e.getMessage());
            }
        }
    }
    
    private void removeSelectedGroup(ActionEvent event) {
        String selectedGroupName = groupFilterCombo.getValue();
        if (selectedGroupName == null || selectedGroupName.equals("All Scripts")) {
            showInfo("No Group Selected", "Please select a specific group to remove.");
            return;
        }
        
        // Find the selected group object
        ScriptGroup selectedGroup = null;
        for (ScriptGroup group : groups) {
            if (group.getName().equals(selectedGroupName)) {
                selectedGroup = group;
                break;
            }
        }
        
        if (selectedGroup == null) {
            showInfo("Group Not Found", "The selected group could not be found.");
            return;
        }
        
        // Confirm deletion
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Remove Group");
        confirmDialog.setHeaderText("Remove group: " + selectedGroup.getName());
        confirmDialog.setContentText("This will remove the group but keep all scripts in the system.\n\nAre you sure you want to continue?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Remove group from database
                dbManager.removeGroup(selectedGroup.getId());
                
                // Remove from UI
                groups.remove(selectedGroup);
                
                // Update group filter combo
                groupFilterCombo.getItems().remove(selectedGroupName);
                groupFilterCombo.setValue("All Scripts");
                
                // Clear selection and refresh
                groupTable.getSelectionModel().clearSelection();
                filterScripts();
                
                // Disable buttons
                executeGroupButton.setDisable(true);
                removeGroupButton.setDisable(true);
                
                statusLabel.setText("Group removed: " + selectedGroup.getName());
                
            } catch (Exception e) {
                showError("Error removing group", e.getMessage());
            }
        }
    }
    
    private void addScriptToGroup(Script script) {
        if (groups.isEmpty()) {
            showInfo("No Groups", "Please create a group first.");
            return;
        }
        
        ChoiceDialog<ScriptGroup> dialog = new ChoiceDialog<>(groups.get(0), groups);
        dialog.setTitle("Add Script to Group");
        dialog.setHeaderText("Select a group for: " + script.getName());
        dialog.setContentText("Choose group:");
        
        Optional<ScriptGroup> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                ScriptGroup group = result.get();
                group.addScript(script);
                dbManager.saveGroup(group);
                dbManager.addScriptToGroup(group.getId(), script.getFilePath().toString());
                
                // Refresh the groups to show updated script count
                refreshGroups();
                
                // Update the group filter to show the updated group
                groupFilterCombo.setValue(group.getName());
                filterScripts();
                
                statusLabel.setText("Script added to group: " + group.getName());
            } catch (Exception e) {
                showError("Error adding script to group", e.getMessage());
            }
        }
    }
    
    private void executeScript(Script script) {
        statusLabel.setText("Executing: " + script.getName());
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        executionService.executeScriptAsync(script).thenAccept(result -> {
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                if (result.isSuccess()) {
                    statusLabel.setText("Script executed successfully: " + script.getName());
                    showInfo("Execution Success", "Script executed successfully.\nOutput:\n" + result.getOutput());
                } else {
                    statusLabel.setText("Script execution failed: " + script.getName());
                    showError("Execution Failed", "Script execution failed.\nError:\n" + result.getError());
                }
            });
        });
    }
    
    private void executeScriptInTerminal(Script script) {
        statusLabel.setText("Opening terminal for: " + script.getName());
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        // Execute in a separate thread to avoid blocking UI
        new Thread(() -> {
            ScriptExecutionService.ExecutionResult result = executionService.executeScriptInTerminal(script);
            
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                if (result.isSuccess()) {
                    statusLabel.setText("Terminal opened for: " + script.getName());
                    showInfo("Terminal Opened", "A terminal window has opened and is executing: " + script.getName() + 
                            "\n\nThe terminal will remain open so you can see the script output and interact with it.");
                } else {
                    statusLabel.setText("Failed to open terminal for: " + script.getName());
                    showError("Terminal Error", "Failed to open terminal for " + script.getName() + ":\n" + result.getError());
                }
            });
        }).start();
    }
    
    private void executeSelectedGroup(ActionEvent event) {
        String selectedGroupName = groupFilterCombo.getValue();
        if (selectedGroupName == null || selectedGroupName.equals("All Scripts")) {
            showInfo("No Group Selected", "Please select a specific group to execute.");
            return;
        }
        
        // Find the selected group object
        ScriptGroup selectedGroup = null;
        for (ScriptGroup group : groups) {
            if (group.getName().equals(selectedGroupName)) {
                selectedGroup = group;
                break;
            }
        }
        
        if (selectedGroup == null) {
            showInfo("Group Not Found", "The selected group could not be found.");
            return;
        }
        
        // Get scripts for this group from database
        try {
            List<Script> groupScripts = dbManager.getScriptsByGroup(selectedGroup.getId());
            if (groupScripts.isEmpty()) {
                showInfo("Empty Group", "The selected group contains no scripts.");
                return;
            }
            
            // Create custom dialog with execution choice
            Alert executionChoiceDialog = new Alert(Alert.AlertType.CONFIRMATION);
            executionChoiceDialog.setTitle("Execute Group");
            executionChoiceDialog.setHeaderText("Execute all scripts in group: " + selectedGroup.getName());
            executionChoiceDialog.setContentText("This will execute " + groupScripts.size() + " scripts sequentially.\n\nHow would you like to execute them?");
            
            // Create custom buttons
            ButtonType guiButton = new ButtonType("GUI (Background)");
            ButtonType terminalButton = new ButtonType("Terminal Windows");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            executionChoiceDialog.getButtonTypes().setAll(guiButton, terminalButton, cancelButton);
            
            Optional<ButtonType> result = executionChoiceDialog.showAndWait();
            if (result.isPresent()) {
                if (result.get() == guiButton) {
                    executeGroupSequentially(selectedGroup, false);
                } else if (result.get() == terminalButton) {
                    executeGroupSequentially(selectedGroup, true);
                }
            }
        } catch (Exception e) {
            showError("Error loading group scripts", e.getMessage());
        }
    }
    
    private void executeGroupSequentially(ScriptGroup group, boolean useTerminal) {
        if (useTerminal) {
            executeGroupInTerminals(group);
        } else {
            executeGroupInGUI(group);
        }
    }
    
    private void executeGroupInGUI(ScriptGroup group) {
        statusLabel.setText("Executing group: " + group.getName());
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        
        executionService.executeGroupSequentially(group, 
            event -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Group execution in progress...");
                });
            },
            event -> {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    statusLabel.setText("Group execution completed: " + group.getName());
                });
            }
        );
    }
    
    private void executeGroupInTerminals(ScriptGroup group) {
        statusLabel.setText("Opening terminals for group: " + group.getName());
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        // Execute in a separate thread to avoid blocking UI
        new Thread(() -> {
            final List<Script> scripts = group.getScripts();
            final int[] successCount = {0};
            final int[] failCount = {0};
            
            for (int i = 0; i < scripts.size(); i++) {
                final Script script = scripts.get(i);
                final int currentIndex = i;
                
                Platform.runLater(() -> {
                    statusLabel.setText("Opening terminal " + (currentIndex + 1) + " of " + scripts.size() + ": " + script.getName());
                });
                
                ScriptExecutionService.ExecutionResult result = executionService.executeScriptInTerminal(script);
                
                if (result.isSuccess()) {
                    successCount[0]++;
                } else {
                    failCount[0]++;
                }
                
                // Small delay between opening terminals
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            final int finalSuccessCount = successCount[0];
            final int finalFailCount = failCount[0];
            
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                statusLabel.setText("Group execution completed: " + group.getName() + 
                    " (" + finalSuccessCount + " terminals opened, " + finalFailCount + " failed)");
                
                if (finalFailCount > 0) {
                    showInfo("Group Execution Summary", 
                        "Group execution completed with " + finalSuccessCount + " terminals opened successfully and " + 
                        finalFailCount + " failures.\n\nCheck the status messages for details about any failures.");
                }
            });
        }).start();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

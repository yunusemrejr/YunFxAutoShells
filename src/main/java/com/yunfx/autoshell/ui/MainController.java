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
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
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
        
        // Top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Main content area
        SplitPane mainContent = new SplitPane();
        mainContent.setDividerPositions(0.3, 0.7);
        
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
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5));
        
        // Directory selection
        selectDirectoryButton = new JFXButton("Select Script Directory");
        selectDirectoryButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        selectDirectoryButton.setOnAction(this::selectDirectory);
        
        // Search field
        searchField = new JFXTextField();
        searchField.setPromptText("Search scripts...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterScripts());
        
        // Refresh button
        refreshButton = new JFXButton("Refresh");
        refreshButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        refreshButton.setOnAction(e -> refreshScripts());
        
        // Add group button
        addGroupButton = new JFXButton("Add Group");
        addGroupButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        addGroupButton.setOnAction(this::addGroup);
        
        // Execute group button
        executeGroupButton = new JFXButton("Execute Group");
        executeGroupButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        executeGroupButton.setOnAction(this::executeSelectedGroup);
        executeGroupButton.setDisable(true);
        
        toolbar.getChildren().addAll(
            selectDirectoryButton, searchField, refreshButton, 
            addGroupButton, executeGroupButton
        );
        
        return toolbar;
    }
    
    private VBox createGroupPanel() {
        VBox groupPanel = new VBox(10);
        groupPanel.setPadding(new Insets(10));
        
        Label groupLabel = new Label("Script Groups");
        groupLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Group filter combo
        groupFilterCombo = new JFXComboBox<>();
        groupFilterCombo.setPromptText("Filter by group");
        groupFilterCombo.getItems().add("All Scripts");
        groupFilterCombo.setOnAction(e -> filterScripts());
        
        // Groups table
        groupTable = new TableView<>();
        groupTable.setPrefHeight(400);
        
        TableColumn<ScriptGroup, String> groupNameCol = new TableColumn<>("Group Name");
        groupNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        groupNameCol.setPrefWidth(150);
        
        TableColumn<ScriptGroup, Integer> scriptCountCol = new TableColumn<>("Scripts");
        scriptCountCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getScripts().size()).asObject());
        scriptCountCol.setPrefWidth(80);
        
        groupTable.getColumns().addAll(groupNameCol, scriptCountCol);
        groupTable.setItems(groups);
        
        // Group selection listener
        groupTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                groupFilterCombo.setValue(newVal.getName());
                filterScripts();
                executeGroupButton.setDisable(false);
            } else {
                executeGroupButton.setDisable(true);
            }
        });
        
        groupPanel.getChildren().addAll(groupLabel, groupFilterCombo, groupTable);
        return groupPanel;
    }
    
    private VBox createScriptPanel() {
        VBox scriptPanel = new VBox(10);
        scriptPanel.setPadding(new Insets(10));
        
        Label scriptLabel = new Label("Scripts");
        scriptLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Scripts table
        scriptTable = new TableView<>();
        scriptTable.setPrefHeight(400);
        
        TableColumn<Script, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        
        TableColumn<Script, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setPrefWidth(250);
        
        TableColumn<Script, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilePath().toString()));
        pathCol.setPrefWidth(300);
        
        TableColumn<Script, Boolean> executableCol = new TableColumn<>("Executable");
        executableCol.setCellValueFactory(new PropertyValueFactory<>("executable"));
        executableCol.setPrefWidth(100);
        
        TableColumn<Script, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setCellFactory(param -> new TableCell<Script, Void>() {
            private final JFXButton executeBtn = new JFXButton("Execute");
            private final JFXButton addToGroupBtn = new JFXButton("Add to Group");
            
            {
                executeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10px;");
                addToGroupBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px;");
                
                executeBtn.setOnAction(e -> executeScript(getTableView().getItems().get(getIndex())));
                addToGroupBtn.setOnAction(e -> addScriptToGroup(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.getChildren().addAll(executeBtn, addToGroupBtn);
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
        statusBar.setPadding(new Insets(5));
        
        statusLabel = new Label("Ready");
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);
        
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
        installDialog.setContentText("Would you like to install " + dependencyName + " now? This will require administrator privileges.");
        
        Optional<ButtonType> result = installDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            installDependency(packageName);
        } else {
            showError("Missing Dependency", dependencyName + " is required to run this application. Please install it manually or restart the application to install it automatically.");
        }
    }
    
    private void installDependency(String packageName) {
        statusLabel.setText("Installing " + packageName + "...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        
        sudoService.installDependencyAsync(packageName).thenAccept(result -> {
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                if (result.isSuccess()) {
                    statusLabel.setText(packageName + " installed successfully");
                    showInfo("Installation Complete", packageName + " has been installed successfully.");
                } else {
                    statusLabel.setText("Failed to install " + packageName);
                    showError("Installation Failed", "Failed to install " + packageName + ":\n" + result.getError());
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
    
    private void filterScripts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedGroup = groupFilterCombo.getValue();
        
        scripts.clear();
        
        try {
            List<Script> allScripts = dbManager.getAllScripts();
            
            for (Script script : allScripts) {
                boolean matchesSearch = searchText.isEmpty() || 
                    script.getName().toLowerCase().contains(searchText) ||
                    script.getDescription().toLowerCase().contains(searchText);
                
                boolean matchesGroup = selectedGroup == null || 
                    selectedGroup.equals("All Scripts") ||
                    (groupTable.getSelectionModel().getSelectedItem() != null &&
                     groupTable.getSelectionModel().getSelectedItem().containsScript(script));
                
                if (matchesSearch && matchesGroup) {
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
    
    private void executeSelectedGroup(ActionEvent event) {
        ScriptGroup selectedGroup = groupTable.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            showInfo("No Group Selected", "Please select a group to execute.");
            return;
        }
        
        if (selectedGroup.getScripts().isEmpty()) {
            showInfo("Empty Group", "The selected group contains no scripts.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Execute Group");
        confirmDialog.setHeaderText("Execute all scripts in group: " + selectedGroup.getName());
        confirmDialog.setContentText("This will execute " + selectedGroup.getScripts().size() + " scripts sequentially.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            executeGroupSequentially(selectedGroup);
        }
    }
    
    private void executeGroupSequentially(ScriptGroup group) {
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

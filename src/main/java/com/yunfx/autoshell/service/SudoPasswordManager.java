package com.yunfx.autoshell.service;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Optional;

public class SudoPasswordManager {
    private static SudoPasswordManager instance;
    private String cachedPassword;
    private boolean passwordValidated;
    
    private SudoPasswordManager() {
        this.cachedPassword = null;
        this.passwordValidated = false;
    }
    
    public static synchronized SudoPasswordManager getInstance() {
        if (instance == null) {
            instance = new SudoPasswordManager();
        }
        return instance;
    }
    
    /**
     * Requests sudo password from user if not already cached
     * @param reason Reason for requesting sudo password
     * @return true if password was obtained (either cached or newly entered), false if cancelled
     */
    public boolean requestSudoPassword(String reason) {
        if (passwordValidated && cachedPassword != null) {
            return true; // Password already cached and validated
        }
        
        // Clear any invalid password
        clearPassword();
        
        return Platform.isFxApplicationThread() ? 
            requestPasswordOnFXThread(reason) : 
            requestPasswordOnBackgroundThread(reason);
    }
    
    private boolean requestPasswordOnFXThread(String reason) {
        while (true) {
            Alert passwordDialog = new Alert(Alert.AlertType.CONFIRMATION);
            passwordDialog.setTitle("Sudo Password Required");
            passwordDialog.setHeaderText("Sudo Password Required");
            passwordDialog.setContentText("Some scripts require sudo privileges to execute.\n\nReason: " + reason);
            
            // Create password field
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Enter sudo password");
            passwordField.setPrefColumnCount(20);
            
            // Create grid pane for layout
            GridPane gridPane = new GridPane();
            gridPane.setHgap(10);
            gridPane.setVgap(10);
            gridPane.setMaxWidth(Double.MAX_VALUE);
            
            Text label = new Text("Sudo Password:");
            gridPane.add(label, 0, 0);
            gridPane.add(passwordField, 1, 0);
            
            GridPane.setHgrow(passwordField, Priority.ALWAYS);
            
            passwordDialog.getDialogPane().setContent(gridPane);
            
            // Customize buttons
            ButtonType okButton = new ButtonType("OK");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            passwordDialog.getButtonTypes().setAll(okButton, cancelButton);
            
            // Focus on password field
            Platform.runLater(passwordField::requestFocus);
            
            Optional<ButtonType> result = passwordDialog.showAndWait();
            if (result.isPresent() && result.get() == okButton) {
                String password = passwordField.getText();
                if (password != null && !password.trim().isEmpty()) {
                    cachedPassword = password.trim();
                    
                    // Validate the password
                    if (validatePassword()) {
                        return true;
                    } else {
                        // Show error and retry
                        Alert errorDialog = new Alert(Alert.AlertType.ERROR);
                        errorDialog.setTitle("Invalid Password");
                        errorDialog.setHeaderText("Invalid Sudo Password");
                        errorDialog.setContentText("The password you entered is incorrect. Please try again.");
                        errorDialog.showAndWait();
                        // Continue the loop to retry
                    }
                } else {
                    // Show error for empty password
                    Alert errorDialog = new Alert(Alert.AlertType.ERROR);
                    errorDialog.setTitle("Empty Password");
                    errorDialog.setHeaderText("Password Required");
                    errorDialog.setContentText("Please enter a password.");
                    errorDialog.showAndWait();
                    // Continue the loop to retry
                }
            } else {
                // User cancelled
                return false;
            }
        }
    }
    
    private boolean requestPasswordOnBackgroundThread(String reason) {
        // This should not be called on background thread, but handle gracefully
        System.err.println("Sudo password requested on background thread: " + reason);
        
        // Use Platform.runLater to switch to FX thread
        final boolean[] result = {false};
        Platform.runLater(() -> {
            result[0] = requestPasswordOnFXThread(reason);
        });
        
        // Wait for the result (with timeout)
        long startTime = System.currentTimeMillis();
        while (!result[0] && (System.currentTimeMillis() - startTime) < 30000) { // 30 second timeout
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return result[0];
    }
    
    /**
     * Gets the cached sudo password
     * @return The cached password, or null if not available
     */
    public String getCachedPassword() {
        return passwordValidated ? cachedPassword : null;
    }
    
    /**
     * Checks if a valid password is cached
     * @return true if password is available and validated
     */
    public boolean hasValidPassword() {
        return passwordValidated && cachedPassword != null;
    }
    
    /**
     * Clears the cached password (for security)
     */
    public void clearPassword() {
        cachedPassword = null;
        passwordValidated = false;
    }
    
    /**
     * Validates the cached password by testing it with sudo
     * @return true if password is valid, false otherwise
     */
    public boolean validatePassword() {
        if (cachedPassword == null) {
            return false;
        }
        
        try {
            // Test password with a simple sudo command
            ProcessBuilder pb = new ProcessBuilder("sudo", "-S", "echo", "test");
            Process process = pb.start();
            
            // Send password to sudo
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
                writer.write(cachedPassword + "\n");
                writer.flush();
            }
            
            // Read any error output
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            passwordValidated = (exitCode == 0);
            
            if (!passwordValidated) {
                System.err.println("Sudo password validation failed. Error: " + errorOutput.toString());
                clearPassword();
            } else {
                System.out.println("Sudo password validated successfully");
            }
            
            return passwordValidated;
        } catch (Exception e) {
            System.err.println("Error validating sudo password: " + e.getMessage());
            clearPassword();
            return false;
        }
    }
    
    /**
     * Gets a command prefix for executing scripts with sudo if needed
     * @param requiresSudo Whether the script requires sudo
     * @return Command prefix (e.g., "sudo -S" or empty string)
     */
    public String getSudoPrefix(boolean requiresSudo) {
        if (!requiresSudo || !hasValidPassword()) {
            return "";
        }
        return "sudo -S";
    }
    
    /**
     * Gets the password input for sudo command
     * @param requiresSudo Whether the script requires sudo
     * @return Password string to pipe to sudo, or empty string
     */
    public String getPasswordInput(boolean requiresSudo) {
        if (!requiresSudo || !hasValidPassword()) {
            return "";
        }
        return cachedPassword + "\n";
    }
}

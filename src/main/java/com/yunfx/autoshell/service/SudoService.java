package com.yunfx.autoshell.service;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SudoService {
    
    public static class SudoResult {
        private final boolean success;
        private final String output;
        private final String error;
        private final int exitCode;
        
        public SudoResult(boolean success, String output, String error, int exitCode) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
        }
        
        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public int getExitCode() { return exitCode; }
    }
    
    public CompletableFuture<SudoResult> executeSudoCommandAsync(String command) {
        return CompletableFuture.supplyAsync(() -> {
            return executeSudoCommand(command);
        });
    }
    
    public SudoResult executeSudoCommand(String command) {
        try {
            // First check if sudo is needed
            if (!isSudoRequired(command)) {
                return executeCommandSync(command);
            }
            
            // Prompt for sudo password
            String password = promptForSudoPassword();
            if (password == null) {
                return new SudoResult(false, "", "User cancelled sudo password entry", -1);
            }
            
            // Execute with sudo
            return executeSudoCommandWithPassword(command, password);
            
        } catch (Exception e) {
            return new SudoResult(false, "", "Error executing command: " + e.getMessage(), -1);
        }
    }
    
    private boolean isSudoRequired(String command) {
        // Commands that typically require sudo
        String[] sudoCommands = {
            "apt", "apt-get", "aptitude", "dpkg",
            "systemctl", "service", "systemd",
            "usermod", "groupmod", "adduser", "deluser",
            "chown", "chmod", "chgrp",
            "mount", "umount", "fdisk", "parted",
            "iptables", "ufw", "firewall",
            "visudo", "passwd", "su",
            "install", "update", "upgrade"
        };
        
        String lowerCommand = command.toLowerCase();
        for (String sudoCmd : sudoCommands) {
            if (lowerCommand.contains(sudoCmd)) {
                return true;
            }
        }
        
        return false;
    }
    
    private String promptForSudoPassword() {
        final String[] password = {null};
        
        Platform.runLater(() -> {
            Alert passwordDialog = new Alert(Alert.AlertType.CONFIRMATION);
            passwordDialog.setTitle("Administrator Access Required");
            passwordDialog.setHeaderText("This operation requires administrator privileges");
            passwordDialog.setContentText("Please enter your password to continue:");
            
            // Create password field
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Enter your password");
            
            GridPane grid = new GridPane();
            grid.add(new Text("Password:"), 0, 0);
            grid.add(passwordField, 1, 0);
            grid.setHgap(10);
            grid.setVgap(10);
            
            passwordDialog.getDialogPane().setContent(grid);
            
            // Customize buttons
            passwordDialog.getButtonTypes().clear();
            passwordDialog.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            Optional<ButtonType> result = passwordDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                password[0] = passwordField.getText();
            }
        });
        
        // Wait for user input (with timeout)
        long startTime = System.currentTimeMillis();
        while (password[0] == null && (System.currentTimeMillis() - startTime) < 30000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return password[0];
    }
    
    private SudoResult executeSudoCommandWithPassword(String command, String password) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sudo", "-S", "bash", "-c", command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Send password to sudo
            try (PrintWriter writer = new PrintWriter(process.getOutputStream())) {
                writer.println(password);
                writer.flush();
            }
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for completion
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SudoResult(false, output.toString(), "Command timed out", -1);
            }
            
            int exitCode = process.exitValue();
            boolean success = exitCode == 0;
            
            return new SudoResult(success, output.toString(), "", exitCode);
            
        } catch (Exception e) {
            return new SudoResult(false, "", "Error executing sudo command: " + e.getMessage(), -1);
        }
    }
    
    public CompletableFuture<SudoResult> executeCommandAsync(String command) {
        return CompletableFuture.supplyAsync(() -> {
            return executeCommandSync(command);
        });
    }
    
    private SudoResult executeCommandSync(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for completion
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SudoResult(false, output.toString(), "Command timed out", -1);
            }
            
            int exitCode = process.exitValue();
            boolean success = exitCode == 0;
            
            return new SudoResult(success, output.toString(), "", exitCode);
            
        } catch (Exception e) {
            return new SudoResult(false, "", "Error executing command: " + e.getMessage(), -1);
        }
    }
    
    public CompletableFuture<SudoResult> installDependencyAsync(String dependency) {
        return CompletableFuture.supplyAsync(() -> {
            String command = "apt update && apt install -y " + dependency;
            return executeSudoCommand(command);
        });
    }
    
    public CompletableFuture<SudoResult> checkDependencyAsync(String dependency) {
        return CompletableFuture.supplyAsync(() -> {
            String command = "which " + dependency + " || dpkg -l | grep -q " + dependency;
            return executeCommandSync(command);
        });
    }
}

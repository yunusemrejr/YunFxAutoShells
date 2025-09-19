package com.yunfx.autoshell.service;

import com.yunfx.autoshell.model.Script;
import com.yunfx.autoshell.model.ScriptGroup;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ScriptExecutionService {
    private final ScriptAnalysisService analysisService;
    private final SudoPasswordManager sudoManager;
    
    public ScriptExecutionService() {
        this.analysisService = new ScriptAnalysisService();
        this.sudoManager = SudoPasswordManager.getInstance();
    }
    
    public static class ExecutionResult {
        private final boolean success;
        private final String output;
        private final String error;
        private final int exitCode;
        private final long executionTimeMs;
        
        public ExecutionResult(boolean success, String output, String error, int exitCode, long executionTimeMs) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
            this.executionTimeMs = executionTimeMs;
        }
        
        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
        public int getExitCode() { return exitCode; }
        public long getExecutionTimeMs() { return executionTimeMs; }
    }
    
    public CompletableFuture<ExecutionResult> executeScriptAsync(Script script) {
        return CompletableFuture.supplyAsync(() -> {
            return executeScript(script);
        });
    }
    
    public ExecutionResult executeScript(Script script) {
        long startTime = System.currentTimeMillis();
        
        try {
            Path scriptPath = script.getFilePath();
            if (!scriptPath.toFile().exists()) {
                return new ExecutionResult(false, "", "Script file not found: " + scriptPath, -1, 0);
            }
            
            if (!scriptPath.toFile().canExecute()) {
                return new ExecutionResult(false, "", "Script is not executable: " + scriptPath, -1, 0);
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder("bash", scriptPath.toString());
            processBuilder.directory(scriptPath.getParent().toFile());
            
            Process process = processBuilder.start();
            
            // Read output and error streams
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            
            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                
                String line;
                while ((line = outputReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }
            
            // Wait for process to complete with timeout
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (!finished) {
                process.destroyForcibly();
                return new ExecutionResult(false, output.toString(), 
                    "Script execution timed out after 30 seconds", -1, executionTime);
            }
            
            int exitCode = process.exitValue();
            boolean success = exitCode == 0;
            
            return new ExecutionResult(success, output.toString(), error.toString(), exitCode, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new ExecutionResult(false, "", "Execution error: " + e.getMessage(), -1, executionTime);
        }
    }
    
    public Task<ExecutionResult> createExecutionTask(Script script) {
        return new Task<ExecutionResult>() {
            @Override
            protected ExecutionResult call() throws Exception {
                return executeScript(script);
            }
        };
    }
    
    public void executeGroupSequentially(ScriptGroup group, 
                                       EventHandler<WorkerStateEvent> onScriptComplete,
                                       EventHandler<WorkerStateEvent> onGroupComplete) {
        
        Task<Void> groupTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<Script> scripts = group.getScripts();
                
                for (int i = 0; i < scripts.size(); i++) {
                    Script script = scripts.get(i);
                    
                    // Update progress
                    updateProgress(i, scripts.size());
                    updateMessage("Executing: " + script.getName());
                    
                    // Execute script
                    ExecutionResult result = executeScript(script);
                    
                    // Notify completion
                    if (onScriptComplete != null) {
                        javafx.application.Platform.runLater(() -> {
                            onScriptComplete.handle(new WorkerStateEvent(this, null));
                        });
                    }
                    
                    // If script failed and we want to stop on first failure
                    if (!result.isSuccess()) {
                        updateMessage("Script failed: " + script.getName());
                        break;
                    }
                }
                
                updateProgress(scripts.size(), scripts.size());
                updateMessage("Group execution completed");
                
                return null;
            }
        };
        
        if (onGroupComplete != null) {
            groupTask.setOnSucceeded(onGroupComplete);
            groupTask.setOnFailed(onGroupComplete);
        }
        
        Thread executionThread = new Thread(groupTask);
        executionThread.setDaemon(true);
        executionThread.start();
    }
    
    public void makeScriptExecutable(Script script) {
        try {
            Path scriptPath = script.getFilePath();
            scriptPath.toFile().setExecutable(true);
            script.setExecutable(true);
        } catch (Exception e) {
            // Handle error silently or log it
            System.err.println("Failed to make script executable: " + e.getMessage());
        }
    }
    
    public ExecutionResult executeScriptWithSudo(Script script) {
        long startTime = System.currentTimeMillis();
        
        try {
            Path scriptPath = script.getFilePath();
            System.out.println("Executing script: " + script.getName() + " at " + scriptPath);
            
            if (!scriptPath.toFile().exists()) {
                System.err.println("Script file not found: " + scriptPath);
                return new ExecutionResult(false, "", "Script file not found: " + scriptPath, -1, 0);
            }
            
            if (!scriptPath.toFile().canExecute()) {
                System.err.println("Script is not executable: " + scriptPath);
                return new ExecutionResult(false, "", "Script is not executable: " + scriptPath, -1, 0);
            }
            
            // Check if script requires sudo
            boolean requiresSudo = analysisService.requiresSudo(script);
            System.out.println("Script " + script.getName() + " requires sudo: " + requiresSudo);
            
            ProcessBuilder processBuilder;
            if (requiresSudo) {
                // Execute with sudo
                System.out.println("Executing with sudo: " + scriptPath);
                processBuilder = new ProcessBuilder("sudo", "-S", scriptPath.toString());
            } else {
                // Execute normally
                System.out.println("Executing normally: " + scriptPath);
                processBuilder = new ProcessBuilder(scriptPath.toString());
            }
            
            processBuilder.directory(scriptPath.getParent().toFile());
            
            System.out.println("Starting process...");
            Process process = processBuilder.start();
            
            // If sudo is required, send the password
            if (requiresSudo) {
                System.out.println("Sending sudo password...");
                try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
                    writer.write(sudoManager.getPasswordInput(requiresSudo));
                    writer.flush();
                }
                System.out.println("Sudo password sent");
            }
            
            // Read output
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            
            System.out.println("Reading process output...");
            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                
                String line;
                while ((line = outputReader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("Output: " + line);
                }
                
                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                    System.err.println("Error: " + line);
                }
            }
            
            System.out.println("Waiting for process to complete...");
            int exitCode = process.waitFor();
            long executionTime = System.currentTimeMillis() - startTime;
            
            boolean success = exitCode == 0;
            System.out.println("Script " + script.getName() + " completed with exit code: " + exitCode + " (success: " + success + ")");
            
            String resultMessage = success ? 
                "Script executed successfully" + (requiresSudo ? " (with sudo)" : "") : 
                "Script execution failed";
            
            return new ExecutionResult(success, output.toString(), error.toString(), exitCode, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            System.err.println("Exception executing script " + script.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new ExecutionResult(false, "", "Failed to execute script: " + e.getMessage(), -1, executionTime);
        }
    }

    public ExecutionResult executeScriptInTerminal(Script script) {
        long startTime = System.currentTimeMillis();
        
        try {
            Path scriptPath = script.getFilePath();
            if (!scriptPath.toFile().exists()) {
                return new ExecutionResult(false, "", "Script file not found: " + scriptPath, -1, 0);
            }
            
            if (!scriptPath.toFile().canExecute()) {
                return new ExecutionResult(false, "", "Script is not executable: " + scriptPath, -1, 0);
            }
            
            // Check if script requires sudo
            boolean requiresSudo = analysisService.requiresSudo(script);
            String sudoPrefix = sudoManager.getSudoPrefix(requiresSudo);
            
            // Build command with sudo if needed
            String scriptCommand;
            if (requiresSudo && !sudoPrefix.isEmpty()) {
                scriptCommand = "cd \"" + scriptPath.getParent() + "\" && echo '" + 
                    sudoManager.getPasswordInput(requiresSudo).trim() + "' | " + 
                    sudoPrefix + " \"" + scriptPath + "\"";
            } else {
                scriptCommand = "cd \"" + scriptPath.getParent() + "\" && \"" + scriptPath + "\"";
            }
            
            // Try different terminal emulators
            String[] terminalCommands = {
                "gnome-terminal -- bash -c '" + scriptCommand + "; exec bash'",
                "xterm -e 'bash -c \"" + scriptCommand + "; exec bash\"'",
                "konsole -e 'bash -c \"" + scriptCommand + "; exec bash\"'",
                "xfce4-terminal -e 'bash -c \"" + scriptCommand + "; exec bash\"'",
                "mate-terminal -e 'bash -c \"" + scriptCommand + "; exec bash\"'",
                "lxterminal -e 'bash -c \"" + scriptCommand + "; exec bash\"'"
            };
            
            ProcessBuilder processBuilder = null;
            for (String cmd : terminalCommands) {
                try {
                    processBuilder = new ProcessBuilder("bash", "-c", cmd);
                    Process testProcess = processBuilder.start();
                    testProcess.destroy(); // Just test if the command works
                    break;
                } catch (Exception e) {
                    // Try next terminal
                    continue;
                }
            }
            
            if (processBuilder == null) {
                return new ExecutionResult(false, "", "No suitable terminal emulator found. Please install gnome-terminal, xterm, or another terminal emulator.", -1, 0);
            }
            
            // Execute the terminal command
            Process process = processBuilder.start();
            
            // Wait a bit to see if the terminal opens successfully
            Thread.sleep(1000);
            
            long executionTime = System.currentTimeMillis() - startTime;
            return new ExecutionResult(true, "Terminal opened successfully" + 
                (requiresSudo ? " (with sudo)" : ""), "", 0, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new ExecutionResult(false, "", "Failed to open terminal: " + e.getMessage(), -1, executionTime);
        }
    }
}

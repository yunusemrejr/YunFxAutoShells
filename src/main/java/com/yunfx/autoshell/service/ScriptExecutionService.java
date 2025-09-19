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
}

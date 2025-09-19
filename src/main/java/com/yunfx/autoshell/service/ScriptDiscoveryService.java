package com.yunfx.autoshell.service;

import com.yunfx.autoshell.model.Script;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ScriptDiscoveryService {
    
    public List<Script> discoverScripts(String rootPath) throws IOException {
        List<Script> scripts = new ArrayList<>();
        Path root = Paths.get(rootPath);
        
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IOException("Invalid directory path: " + rootPath);
        }
        
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().toLowerCase().endsWith(".sh")) {
                    Script script = createScriptFromFile(file);
                    if (script != null) {
                        scripts.add(script);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return scripts;
    }
    
    private Script createScriptFromFile(Path file) {
        try {
            Script script = new Script();
            script.setName(file.getFileName().toString());
            script.setFilePath(file);
            script.setExecutable(Files.isExecutable(file));
            
            // Get last modified time
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), 
                ZoneId.systemDefault()
            );
            script.setLastModified(lastModified);
            
            // Read file content for analysis
            try {
                String content = Files.readString(file);
                script.setContent(content);
                
                // Extract description from comments
                String description = extractDescriptionFromContent(content);
                script.setDescription(description);
                
                // Extract tags from comments
                List<String> tags = extractTagsFromContent(content);
                script.setTags(tags);
                
            } catch (IOException e) {
                // If we can't read the file, still create the script object
                script.setContent("");
                script.setDescription("Unable to read file content");
            }
            
            return script;
            
        } catch (IOException e) {
            return null;
        }
    }
    
    private String extractDescriptionFromContent(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") && !line.startsWith("#!/")) {
                String description = line.substring(1).trim();
                if (!description.isEmpty() && description.length() > 3) {
                    return description;
                }
            }
        }
        return "No description available";
    }
    
    private List<String> extractTagsFromContent(String content) {
        List<String> tags = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") && line.toLowerCase().contains("tag")) {
                String tagLine = line.substring(1).trim();
                if (tagLine.toLowerCase().startsWith("tag")) {
                    String[] parts = tagLine.split(":");
                    if (parts.length > 1) {
                        String[] tagList = parts[1].split(",");
                        for (String tag : tagList) {
                            String cleanTag = tag.trim();
                            if (!cleanTag.isEmpty()) {
                                tags.add(cleanTag);
                            }
                        }
                    }
                }
            }
        }
        
        return tags;
    }
    
    public boolean isScriptExecutable(Path scriptPath) {
        return Files.exists(scriptPath) && Files.isExecutable(scriptPath);
    }
    
    public void makeScriptExecutable(Path scriptPath) throws IOException {
        if (Files.exists(scriptPath)) {
            // Set executable permission for owner, group, and others
            Files.setPosixFilePermissions(scriptPath, 
                java.util.Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE
                )
            );
        }
    }
}

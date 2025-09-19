package com.yunfx.autoshell.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Script {
    private String name;
    private String description;
    private Path filePath;
    private LocalDateTime lastModified;
    private List<String> tags;
    private boolean executable;
    private String content;

    public Script() {
        this.tags = new ArrayList<>();
    }

    public Script(String name, Path filePath) {
        this();
        this.name = name;
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Path getFilePath() { return filePath; }
    public void setFilePath(Path filePath) { this.filePath = filePath; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public boolean isExecutable() { return executable; }
    public void setExecutable(boolean executable) { this.executable = executable; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    @Override
    public String toString() {
        return name;
    }
}

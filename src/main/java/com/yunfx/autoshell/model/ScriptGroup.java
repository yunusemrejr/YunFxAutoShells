package com.yunfx.autoshell.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScriptGroup {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private List<Script> scripts;

    public ScriptGroup() {
        this.scripts = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public ScriptGroup(String name) {
        this();
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Script> getScripts() { return scripts; }
    public void setScripts(List<Script> scripts) { this.scripts = scripts; }

    public void addScript(Script script) {
        if (!scripts.contains(script)) {
            scripts.add(script);
        }
    }

    public void removeScript(Script script) {
        scripts.remove(script);
    }

    public boolean containsScript(Script script) {
        return scripts.contains(script);
    }

    @Override
    public String toString() {
        return name;
    }
}

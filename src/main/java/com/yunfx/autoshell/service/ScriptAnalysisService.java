package com.yunfx.autoshell.service;

import com.yunfx.autoshell.model.Script;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

public class ScriptAnalysisService {
    
    // Regex patterns to detect sudo requirements
    private static final Pattern[] SUDO_PATTERNS = {
        // Direct sudo commands
        Pattern.compile("\\bsudo\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsudo\\s+\\$", Pattern.CASE_INSENSITIVE),
        
        // System administration commands that typically need sudo
        Pattern.compile("\\b(apt|yum|dnf|pacman|zypper)\\s+(install|remove|update|upgrade)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(systemctl|service)\\s+(start|stop|restart|enable|disable)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(usermod|useradd|userdel|groupadd|groupdel)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(mount|umount|fdisk|parted|mkfs)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(ifconfig|ip\\s+link|ip\\s+addr|ip\\s+route)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(ufw|iptables|firewall-cmd)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(crontab|at)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(visudo|passwd|chpasswd)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(rsync|scp|ssh)\\s+.*root@", Pattern.CASE_INSENSITIVE),
        
        // File operations that typically need sudo
        Pattern.compile("\\b(chmod|chown|chgrp)\\s+.*[0-7]{3,4}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(rm|rmdir|mkdir|touch|cp|mv)\\s+.*/(etc|var|usr|opt|root)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(echo|cat|tee)\\s+.*>\\s*/etc/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(echo|cat|tee)\\s+.*>\\s*/var/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(echo|cat|tee)\\s+.*>\\s*/usr/", Pattern.CASE_INSENSITIVE),
        
        // Network operations that typically need sudo
        Pattern.compile("\\b(netstat|ss|lsof)\\s+-[a-z]*p", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(tcpdump|wireshark|tshark)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(nmap|masscan|zmap)", Pattern.CASE_INSENSITIVE),
        
        // Docker and container operations
        Pattern.compile("\\bdocker\\s+(run|start|stop|restart|rm|rmi|build|push|pull)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(podman|docker-compose)", Pattern.CASE_INSENSITIVE),
        
        // Log operations that might need sudo
        Pattern.compile("\\b(journalctl|logrotate)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(tail|head|grep|awk|sed)\\s+.*/var/log/", Pattern.CASE_INSENSITIVE),
        
        // Hardware and kernel operations
        Pattern.compile("\\b(modprobe|insmod|rmmod|lsmod)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(lspci|lsusb|lscpu|lsblk)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(hdparm|smartctl|badblocks)", Pattern.CASE_INSENSITIVE)
    };
    
    // Patterns that indicate the script might NOT need sudo (false positives)
    private static final Pattern[] NON_SUDO_PATTERNS = {
        Pattern.compile("\\b(sudo\\s+-n|sudo\\s+--non-interactive)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b#.*sudo", Pattern.CASE_INSENSITIVE), // Commented out sudo
        Pattern.compile("\\becho.*sudo", Pattern.CASE_INSENSITIVE), // Just echoing sudo
        Pattern.compile("\\b(which|whereis|type)\\s+sudo", Pattern.CASE_INSENSITIVE) // Just checking if sudo exists
    };
    
    /**
     * Analyzes a script to determine if it requires sudo privileges
     * @param script The script to analyze
     * @return true if the script likely needs sudo, false otherwise
     */
    public boolean requiresSudo(Script script) {
        try {
            String content = getScriptContent(script);
            if (content == null || content.trim().isEmpty()) {
                return false;
            }
            
            // Check for non-sudo patterns first (to avoid false positives)
            for (Pattern pattern : NON_SUDO_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    continue; // This might be a false positive, check other patterns
                }
            }
            
            // Check for sudo patterns
            for (Pattern pattern : SUDO_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error analyzing script for sudo requirements: " + e.getMessage());
            return false; // Default to not requiring sudo if analysis fails
        }
    }
    
    /**
     * Gets the content of a script file
     * @param script The script to read
     * @return The script content as a string, or null if unable to read
     */
    private String getScriptContent(Script script) {
        try {
            if (script.getContent() != null && !script.getContent().trim().isEmpty()) {
                return script.getContent();
            }
            
            // If content is not cached, read from file
            if (script.getFilePath() != null && script.getFilePath().toFile().exists()) {
                return Files.readString(script.getFilePath());
            }
            
            return null;
        } catch (IOException e) {
            System.err.println("Error reading script content: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Analyzes multiple scripts and returns a summary of sudo requirements
     * @param scripts List of scripts to analyze
     * @return AnalysisResult containing sudo requirements info
     */
    public AnalysisResult analyzeScripts(java.util.List<Script> scripts) {
        int totalScripts = scripts.size();
        int sudoRequired = 0;
        int nonSudo = 0;
        
        for (Script script : scripts) {
            if (requiresSudo(script)) {
                sudoRequired++;
            } else {
                nonSudo++;
            }
        }
        
        return new AnalysisResult(totalScripts, sudoRequired, nonSudo);
    }
    
    /**
     * Result of script analysis
     */
    public static class AnalysisResult {
        private final int totalScripts;
        private final int sudoRequired;
        private final int nonSudo;
        
        public AnalysisResult(int totalScripts, int sudoRequired, int nonSudo) {
            this.totalScripts = totalScripts;
            this.sudoRequired = sudoRequired;
            this.nonSudo = nonSudo;
        }
        
        public int getTotalScripts() { return totalScripts; }
        public int getSudoRequired() { return sudoRequired; }
        public int getNonSudo() { return nonSudo; }
        
        public boolean hasSudoScripts() { return sudoRequired > 0; }
        public boolean hasNonSudoScripts() { return nonSudo > 0; }
        
        @Override
        public String toString() {
            return String.format("Total: %d, Sudo required: %d, Non-sudo: %d", 
                totalScripts, sudoRequired, nonSudo);
        }
    }
}

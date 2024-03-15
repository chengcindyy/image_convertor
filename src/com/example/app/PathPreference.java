package com.example.app;

import java.util.prefs.Preferences;

public class PathPreference {
    private final Preferences prefs;

    public PathPreference() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
    }

    public void saveLastUsedFolder(String path) {
        prefs.put("LAST_USED_FOLDER", path);
    }

    public String getLastUsedFolder() {
        return prefs.get("LAST_USED_FOLDER", "");
    }

    public void saveLastUsedFilePath(String filePath) {
        prefs.put("LAST_USED_FILE_PATH", filePath);
    }

    public String getLastUsedFilePath() {
        return prefs.get("LAST_USED_FILE_PATH", "");
    }
}


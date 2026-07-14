package com.ericfortis.tabulareye;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@State(name = "TabularEyeSettings", storages = @Storage("TabularEye.xml"))
public class DetectorSettings implements PersistentStateComponent<DetectorSettings.State> {

    public static class State {
        public Map<String, Boolean> enabledDetectors = new LinkedHashMap<>();
    }

    private State myState = new State();
    private final List<Runnable> myListeners = new ArrayList<>();

    public static DetectorSettings getInstance() {
        return ApplicationManager.getApplication().getService(DetectorSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public boolean isDetectorEnabled(String className) {
        return myState.enabledDetectors.getOrDefault(className, true);
    }

    public void setDetectorEnabled(String className, boolean enabled) {
        myState.enabledDetectors.put(className, enabled);
    }

    public void addListener(Runnable listener) {
        myListeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        myListeners.remove(listener);
    }

    public void notifyListeners() {
        for (Runnable listener : myListeners)
            listener.run();
    }
}

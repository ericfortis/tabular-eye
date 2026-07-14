package com.ericfortis.syntaxeye;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.*;

@State(name = "SyntaxEyeSettings", storages = @Storage("SyntaxEye.xml"))
public class SyntaxEyeSettings implements PersistentStateComponent<SyntaxEyeSettings.State> {

    public static class State {
        public boolean enabled = false;
        public String wordsText = "";
    }

    private State myState = new State();
    private final List<Runnable> myListeners = new ArrayList<>();

    public static SyntaxEyeSettings getInstance() {
        return ApplicationManager.getApplication().getService(SyntaxEyeSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public boolean isEnabled() {
        return myState.enabled;
    }

    public void setEnabled(boolean enabled) {
        myState.enabled = enabled;
    }

    public String getWordsText() {
        return myState.wordsText;
    }

    public void setWordsText(String text) {
        myState.wordsText = text;
    }

    public Set<String> getWordSet() {
        return parseWords(myState.wordsText);
    }

    public static Set<String> parseWords(String text) {
        if (text == null || text.isBlank())
            return Set.of();
        return stream(text.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void addListener(Runnable listener) {
        myListeners.add(listener);
    }

    public void notifyListeners() {
        for (Runnable listener : myListeners)
            listener.run();
    }
}

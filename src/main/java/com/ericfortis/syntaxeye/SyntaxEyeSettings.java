package com.ericfortis.syntaxeye;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@State(name = "SyntaxEyeSettings", storages = @Storage("SyntaxEye.xml"))
public class SyntaxEyeSettings implements PersistentStateComponent<SyntaxEyeSettings.State> {

  public static class State {
    public boolean enabled = false;
    public String wordsText = "";
  }

  private record Parsed(String source, Set<String> words, List<Pattern> patterns) {
  }

  private State myState = new State();
  private final List<Runnable> myListeners = new ArrayList<>();
  private volatile Parsed myParsed = new Parsed("", Set.of(), List.of());

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
    return parsed().words();
  }

  public List<Pattern> getWordPatterns() {
    return parsed().patterns();
  }

  private Parsed parsed() {
    var cached = myParsed;
    if (!Objects.equals(cached.source(), myState.wordsText)) {
      cached = new Parsed(myState.wordsText, parseWords(myState.wordsText), parsePatterns(myState.wordsText));
      myParsed = cached;
    }
    return cached;
  }

  private static List<Pattern> parsePatterns(String text) {
    return parseWords(text).stream()
       .filter(word -> word.length() >= 2)
       .map(word -> Pattern.compile(Pattern.quote(word)))
       .toList();
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

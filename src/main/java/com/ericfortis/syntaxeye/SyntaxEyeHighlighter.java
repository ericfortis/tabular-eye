package com.ericfortis.syntaxeye;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.intellij.util.concurrency.AppExecutorUtil.*;


public class SyntaxEyeHighlighter implements EditorFactoryListener {

    private final Map<Editor, EditorHighlightSession> sessions = new HashMap<>();

    public SyntaxEyeHighlighter() {
        SyntaxEyeSettings.getInstance().addListener(this::refreshAll);
    }

    private void refreshAll() {
        var snapshot = new HashMap<>(sessions);
        for (var entry : snapshot.entrySet()) {
            var editor = entry.getKey();
            var session = entry.getValue();
            if (editor.isDisposed())
                continue;
            session.scheduleUpdate();
        }
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        var editor = event.getEditor();
        if (editor.getEditorKind() != EditorKind.MAIN_EDITOR)
            return;
        var p = editor.getProject();
        if (p == null || p.isDisposed())
            return;
        sessions.put(editor, new EditorHighlightSession(editor, p));
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        var session = sessions.remove(event.getEditor());
        if (session != null)
            session.dispose();
    }

    private static class EditorHighlightSession implements Disposable {

        private final Editor myEditor;
        private final Project myProject;
        private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        private RangeHighlighter[] myHighlighters = new RangeHighlighter[0];

        EditorHighlightSession(Editor editor, Project project) {
            myEditor = editor;
            myProject = project;

            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    scheduleUpdate();
                }
            }, this);

            scheduleUpdate();
        }

        void scheduleUpdate() {
            myAlarm.cancelAllRequests();
            myAlarm.addRequest(() -> {
                if (myEditor.isDisposed() || myProject.isDisposed())
                    return;
                ReadAction.nonBlocking((java.util.concurrent.Callable<Void>) () -> {
                            updateHighlights();
                            return null;
                        })
                        .expireWhen(() -> myEditor.isDisposed() || myProject.isDisposed())
                        .submit(getAppExecutorService());
            }, 300);
        }

        private void updateHighlights() {
            for (var h : myHighlighters)
                myEditor.getMarkupModel().removeHighlighter(h);
            myHighlighters = new RangeHighlighter[0];

            var settings = SyntaxEyeSettings.getInstance();
            if (!settings.isEnabled())
                return;

            var words = settings.getWordSet();
            if (words.isEmpty())
                return;

            var text = myEditor.getDocument().getText();
            var highlighters = new ArrayList<RangeHighlighter>();
            var attrs = SyntaxEyeColors.getFaintTextAttributes();

            for (var word : words) {
                if (word.length() < 2)
                    continue;
                var pattern = Pattern.compile(Pattern.quote(word));
                var matcher = pattern.matcher(text);
                while (matcher.find()) {
                    var h = myEditor.getMarkupModel().addRangeHighlighter(
                            matcher.start(),
                            matcher.end(),
                            HighlighterLayer.WARNING,
                            attrs,
                            HighlighterTargetArea.EXACT_RANGE
                    );
                    if (h instanceof RangeHighlighterEx ex)
                        ex.setTextAttributes(attrs);
                    highlighters.add(h);
                }
            }
            myHighlighters = highlighters.toArray(new RangeHighlighter[0]);
        }

        @Override
        public void dispose() {
            for (var h : myHighlighters)
                if (!myEditor.isDisposed())
                    myEditor.getMarkupModel().removeHighlighter(h);
            myHighlighters = new RangeHighlighter[0];
        }
    }
}

package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;


class EditorSession implements Disposable {
    private final Editor editor;
    private final List<AlignmentDetector> detectors;
    private final Spacers spacers;
    private final Alarm alarm;
    private static final int ON_CHANGE_DELAY = 300;

    EditorSession(Editor ed, Project p, List<AlignmentDetector> detectors) {
        this.editor = ed;
        this.detectors = detectors;
        this.spacers = new Spacers(ed);
        this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

        // On highlighting finished (bold fonts, etc. settled)
        p.getMessageBus().connect(this).subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonCodeAnalyzer.DaemonListener() {
            @Override
            public void daemonFinished(@NotNull Collection<? extends FileEditor> fileEditors) {
                for (var fe : fileEditors)
                    if (fe instanceof TextEditor te && te.getEditor().equals(editor)) {
                        refresh(p, ON_CHANGE_DELAY);
                        break;
                    }
            }
        });

        // On font-size or color-scheme change
        p.getMessageBus().connect(this).subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> refresh(p, ON_CHANGE_DELAY));

        // On content change (user edit)
        ed.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent ev) {
                refresh(p, ON_CHANGE_DELAY);
            }
        }, this);

        refresh(p);
    }

    @Override
    public void dispose() {
        spacers.clearAll();
    }

    void refresh(Project p) {
        refresh(p, 0);
    }

    void refresh(Project p, int delay) {
        if (p.isDisposed())
            return;
        alarm.cancelAllRequests();
        alarm.addRequest(() -> doRefresh(p), delay);
    }

    private void doRefresh(Project p) {
        if (p.isDisposed() || editor.isDisposed())
            return;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (p.isDisposed() || editor.isDisposed())
                return;
            PsiDocumentManager.getInstance(p).performWhenAllCommitted(() -> ReadAction.nonBlocking(() -> {
                        if (p.isDisposed() || editor.isDisposed())
                            return null;
                        var doc = editor.getDocument();
                        var psiFile = PsiDocumentManager.getInstance(p).getPsiFile(doc);
                        if (psiFile == null)
                            return null;
                        return spacers.calcAlignments(detectors, psiFile, doc);
                    })
                    .expireWhen(editor::isDisposed)
                    .finishOnUiThread(ModalityState.defaultModalityState(), allBlocks -> {
                        if (allBlocks != null && !editor.isDisposed())
                            spacers.refresh(allBlocks);
                    })
                    .submit(AppExecutorUtil.getAppExecutorService()));
        }, ModalityState.defaultModalityState());
    }
}

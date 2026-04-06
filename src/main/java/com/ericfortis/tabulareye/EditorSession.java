package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder;
import com.ericfortis.tabulareye.finders.AlignmentFinder.AlignmentBlock;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


class EditorSession implements Disposable {
	private final Editor editor;
	private final List<AlignmentFinder> finders;
	private final Spacers spacers;
	private final Disposable disposable;
	private final Alarm alarm;
	private final int DEFAULT_DELAY = 40;
	private final int DOCUMENT_DELAY = 300;

	EditorSession(Editor ed, Project p, List<AlignmentFinder> finders) {
		this.editor = ed;
		this.finders = finders;
		this.spacers = new Spacers(ed);
		this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
		disposable = Disposer.newDisposable("tabulareye-" + ed.hashCode());


		// On opening file
		// On returning to an already-open tab
		p.getMessageBus().connect(disposable).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
			@Override
			public void selectionChanged(@NotNull FileEditorManagerEvent event) {
				if (event.getNewFile() != null
					 && event.getNewEditor() instanceof TextEditor textEditor
					 && textEditor.getEditor().equals(editor)
				)
					refresh(p);
			}
		});


		// On content change (user edit)
		ed.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void documentChanged(@NotNull DocumentEvent ev) {
				refresh(p, DOCUMENT_DELAY);
			}
		}, disposable);


		// On font-size / color-scheme change
		p.getMessageBus().connect(disposable).subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> {
			spacers.invalidateFontMetricsCache();
			refresh(p);
		});
	}

	@Override
	public void dispose() {
		Disposer.dispose(disposable);
		spacers.clearAll();
	}


	void refresh(Project p) {
		refresh(p, DEFAULT_DELAY);
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

		var doc = editor.getDocument();
		ApplicationManager.getApplication().invokeLater(() -> {
			if (p.isDisposed() || editor.isDisposed())
				return;
			var psiDocManager = PsiDocumentManager.getInstance(p);

			ReadAction.nonBlocking(() -> {
					 if (p.isDisposed() || editor.isDisposed())
						 return null;

					 var psiFile = psiDocManager.getPsiFile(doc);
					 if (psiFile == null)
						 return null;

					 psiDocManager.commitDocument(doc);

					 List<AlignmentBlock> allBlocks = new ArrayList<>();
					 for (var finder : finders) {
						 if (p.isDisposed() || editor.isDisposed()) 
							 return null;
						 var blocks = finder.findBlocks(psiFile, doc);
						 if (!blocks.isEmpty())
							 allBlocks.addAll(blocks);
					 }
					 return allBlocks;
				 })
				 .finishOnUiThread(ModalityState.any(), allBlocks -> {
					 if (allBlocks != null && !p.isDisposed() && !editor.isDisposed())
						 spacers.refresh(allBlocks);
				 })
				 .expireWith(this)
				 .expireWith(p)
				 .submit(AppExecutorUtil.getAppExecutorService());
		}, ModalityState.any());
	}
}

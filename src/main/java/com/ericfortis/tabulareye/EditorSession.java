package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder;
import com.ericfortis.tabulareye.finders.AlignmentFinder.AlignmentGroup;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns all per-editor state for TabularEye: the applicable finders,
 * the spacer renderer, and the event subscriptions that drive refreshes.
 * Disposing this object cleans up everything associated with the editor.
 */
class EditorSession implements Disposable {

	private final Editor editor;
	private final List<AlignmentFinder> finders;
	private final Spacers spacers;
	private final Disposable disposable;

	EditorSession(Editor ed, Project p, List<AlignmentFinder> finders) {
		this.editor = ed;
		this.finders = finders;
		this.spacers = new Spacers(ed);

		disposable = Disposer.newDisposable("tabulareye-" + ed.hashCode());

		ed.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void documentChanged(@NotNull DocumentEvent ev) {
				refresh(p);
			}
		}, disposable);

		p.getMessageBus()
			 .connect(disposable)
			 .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
				 @Override
				 public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
					 refresh(p);
				 }

				 // Handles returning to an already-open tab
				 @Override
				 public void selectionChanged(@NotNull com.intellij.openapi.fileEditor.FileEditorManagerEvent event) {
					 if (event.getNewFile() == null) return;
					 var psiFile = PsiDocumentManager.getInstance(p).getPsiFile(ed.getDocument());
					 if (psiFile != null && event.getNewFile().equals(psiFile.getVirtualFile()))
						 refresh(p);
				 }
			 });

		// Handles font size / color-scheme changes; invalidates the cached FontMetrics
		p.getMessageBus()
			 .connect(disposable)
			 .subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> {
				 spacers.invalidateFontMetricsCache();
				 refresh(p);
			 });
	}

	void refresh(Project p) {
		if (p.isDisposed()) return;
		var doc = editor.getDocument();
		var psiDocManager = PsiDocumentManager.getInstance(p);

		psiDocManager.performForCommittedDocument(doc, () -> {
			if (editor.isDisposed()) return;
			var psiFile = psiDocManager.getPsiFile(doc);
			if (psiFile == null) return;

			List<AlignmentGroup> allGroups = new ArrayList<>();
			for (var finder : finders)
				allGroups.addAll(finder.findGroups(psiFile, doc));
			spacers.refresh(allGroups);
		});
	}

	@Override
	public void dispose() {
		Disposer.dispose(disposable);
		spacers.clearAll();
	}
}

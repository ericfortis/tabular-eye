package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder;
import com.ericfortis.tabulareye.finders.AlignmentFinder.AlignmentGroup;
import com.ericfortis.tabulareye.finders.CssPropertyFinder;
import com.ericfortis.tabulareye.finders.JsObjectLiteralFinder;
import com.ericfortis.tabulareye.finders.JsTupleArrayFinder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TabularEyeListener implements EditorFactoryListener {

	private final List<AlignmentFinder> finders = List.of(
		 new JsObjectLiteralFinder(),
		 new JsTupleArrayFinder(),
		 new CssPropertyFinder()
	);

	private final Map<Editor, SpacerManager> managers = new HashMap<>();
	private final Map<Editor, Disposable> disposables = new HashMap<>();


	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		var editor = event.getEditor();
		var project = editor.getProject();
		if (project == null) return;

		var document = editor.getDocument();
		PsiDocumentManager.getInstance(project).performForCommittedDocument(document, () -> {
			if (!editor.isDisposed()) {
				var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
				if (psiFile != null && isSupported(psiFile))
					initializeManager(editor, project);
			}
		});
	}

	private boolean isSupported(@NotNull PsiFile file) {
		return finders.stream().anyMatch(f -> f.isApplicable(file));
	}

	private void initializeManager(Editor editor, Project project) {
		if (managers.containsKey(editor))
			return;

		var manager = new SpacerManager(editor);
		managers.put(editor, manager);

		var parentDisposable = Disposer.newDisposable("tabulareye-" + editor.hashCode());
		disposables.put(editor, parentDisposable);

		editor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void documentChanged(@NotNull DocumentEvent e) {
				scheduleRefresh(editor, manager);
			}
		}, parentDisposable);


		project.getMessageBus()
			 .connect(parentDisposable)
			 .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
				 @Override
				 public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
					 scheduleRefresh(editor, manager);
				 }

				 // Handles returning to an open tab
				 @Override
				 public void selectionChanged(@NotNull com.intellij.openapi.fileEditor.FileEditorManagerEvent event) {
					 if (event.getNewFile() != null) {
						 var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
						 if (psiFile != null && event.getNewFile().equals(psiFile.getVirtualFile())) {
							 scheduleRefresh(editor, manager);
						 }
					 }
				 }
			 });
	}

	@Override
	public void editorReleased(@NotNull EditorFactoryEvent event) {
		var editor = event.getEditor();

		// Disposing the Disposable automatically unregisters the document listener.
		var d = disposables.remove(editor);
		if (d != null)
			Disposer.dispose(d);

		var manager = managers.remove(editor);
		if (manager != null)
			manager.clearAll();
	}


	private void scheduleRefresh(Editor editor, SpacerManager manager) {
		var project = editor.getProject();
		if (project == null || project.isDisposed()) {
			for (Project p : ProjectManager.getInstance().getOpenProjects())
				if (!p.isDisposed()) {
					doRefresh(editor, manager, p);
					return;
				}
			return;
		}
		doRefresh(editor, manager, project);
	}

	private void doRefresh(Editor editor, SpacerManager manager, Project project) {
		var document = editor.getDocument();
		var psiDocManager = PsiDocumentManager.getInstance(project);

		psiDocManager.performForCommittedDocument(document, () -> {
			if (editor.isDisposed())
				return;

			var psiFile = psiDocManager.getPsiFile(document);
			if (psiFile != null) {
				List<AlignmentGroup> allGroups = new ArrayList<>();
				for (AlignmentFinder finder : finders)
					if (finder.isApplicable(psiFile))
						allGroups.addAll(finder.findGroups(psiFile, document));
				manager.refresh(allGroups);
			}
		});
	}
}

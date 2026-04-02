package com.ericfortis.tabulareye;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point
 * Lifecycle per editor:
 * <p>
 * editorCreated → guard JS file, create a Disposable, attach DocumentListener
 * via the non-deprecated (listener, Disposable) overload, initial render
 * <p>
 * documentChanged→ re-run PSI parse + inlay refresh (via performForCommittedDocument)
 * <p>
 * editorReleased → dispose the per-editor Disposable (auto-removes listener) + clear inlays
 * <p>
 * One AlignmentInlayManager and one Disposable are kept per editor instance.
 */
public class TabularEyeListener implements EditorFactoryListener {

	private final Map<Editor, AlignmentInlayManager> managers = new HashMap<>();
	private final Map<Editor, Disposable> disposables = new HashMap<>();


	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		var editor = event.getEditor();
		if (!isJsEditor(editor))
			return;

		var manager = new AlignmentInlayManager(editor);
		managers.put(editor, manager);

		// A dedicated Disposable whose sole job is to own the document listener.
		// When we dispose of it in editorReleased(), the platform automatically
		// removes the listener — no manual removeDocumentListener() needed.
		var parentDisposable = Disposer.newDisposable("tabulareye-" + editor.hashCode());
		disposables.put(editor, parentDisposable);

		editor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void documentChanged(@NotNull DocumentEvent e) {
				scheduleRefresh(editor, manager);
			}
		}, parentDisposable);

		
		// Initial setup
		var project = editor.getProject();
		if (project == null) return;
		project.getMessageBus()
			 .connect(parentDisposable)
			 .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
				 @Override
				 public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
					 scheduleRefresh(editor, manager);
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


	private boolean isJsEditor(Editor editor) {
		var vf = FileDocumentManager.getInstance().getFile(editor.getDocument());
		return vf != null && vf.getFileType() instanceof JavaScriptFileType;
	}

	private void scheduleRefresh(Editor editor, AlignmentInlayManager manager) {
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

	private void doRefresh(Editor editor, AlignmentInlayManager manager, Project project) {
		var document = editor.getDocument();
		var psiDocManager = PsiDocumentManager.getInstance(project);

		psiDocManager.performForCommittedDocument(document, () -> {
			// FIXME when opening a file the editor isDisposed(released)
			if (editor.isDisposed())
				return;

			var psiFile = psiDocManager.getPsiFile(document);
			if (psiFile == null)
				return;
			manager.refresh(ObjectLiteralFinder.findGroups(psiFile, document));
		});
	}
}

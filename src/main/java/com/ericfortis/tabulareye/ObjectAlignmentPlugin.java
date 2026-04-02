package com.ericfortis.tabulareye;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
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
public class ObjectAlignmentPlugin implements EditorFactoryListener {

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
		// When we dispose it in editorReleased(), the platform automatically
		// removes the listener — no manual removeDocumentListener() needed.
		var listenerDisposable = Disposer.newDisposable("tabulareye-" + editor.hashCode());
		disposables.put(editor, listenerDisposable);

		editor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void documentChanged(@NotNull DocumentEvent e) {
				scheduleRefresh(editor, manager);
			}
		}, listenerDisposable);

		// Initial render.
		scheduleRefresh(editor, manager);
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

	private void scheduleRefresh(Editor editor, AlignmentInlayManager manager) {
		var project = editor.getProject();
		if (project == null || project.isDisposed()) {
			for (Project p : ProjectManager.getInstance().getOpenProjects()) {
				if (!p.isDisposed()) {
					doRefresh(editor, manager, p);
					return;
				}
			}
			return;
		}
		doRefresh(editor, manager, project);
	}

	private void doRefresh(Editor editor, AlignmentInlayManager manager, Project project) {
		var document = editor.getDocument();
		var psiDocManager = PsiDocumentManager.getInstance(project);

		psiDocManager.performForCommittedDocument(document, () -> {
			if (editor.isDisposed())
				return;

			PsiFile psiFile = psiDocManager.getPsiFile(document);
			if (psiFile == null)
				return;

			manager.refresh(ObjectLiteralFinder.findGroups(psiFile, document));
		});
	}

	private boolean isJsEditor(Editor editor) {
		var vf = FileDocumentManager.getInstance()
			 .getFile(editor.getDocument());
		if (vf == null)
			return false;
		return vf.getFileType() instanceof JavaScriptFileType;
	}
}

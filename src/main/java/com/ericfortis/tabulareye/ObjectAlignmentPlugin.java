package com.ericfortis.tabulareye;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ObjectAlignmentPlugin
 * <p>
 * Entry point registered as an editorFactoryListener in plugin.xml.
 * <p>
 * Lifecycle per editor:
 * editorCreated  → guard JS file, create a Disposable, attach DocumentListener
 * via the non-deprecated (listener, Disposable) overload, initial render
 * documentChanged→ re-run PSI parse + inlay refresh (via performForCommittedDocument)
 * editorReleased → dispose the per-editor Disposable (auto-removes listener) + clear inlays
 * <p>
 * One AlignmentInlayManager and one Disposable are kept per editor instance.
 */
public class ObjectAlignmentPlugin implements EditorFactoryListener {

	private final Map<Editor, AlignmentInlayManager> managers = new HashMap<>();
	private final Map<Editor, Disposable> disposables = new HashMap<>();


	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		Editor editor = event.getEditor();

		if (!isJsEditor(editor)) 
			return;

		AlignmentInlayManager manager = new AlignmentInlayManager(editor);
		managers.put(editor, manager);

		// A dedicated Disposable whose sole job is to own the document listener.
		// When we dispose it in editorReleased(), the platform automatically
		// removes the listener — no manual removeDocumentListener() needed.
		Disposable listenerDisposable = Disposer.newDisposable("tabulareye-" + editor.hashCode());
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
		Editor editor = event.getEditor();

		// Disposing the Disposable automatically unregisters the document listener.
		Disposable d = disposables.remove(editor);
		if (d != null) Disposer.dispose(d);

		AlignmentInlayManager manager = managers.remove(editor);
		if (manager != null) {
			manager.clearAll();
		}
	}

	private void scheduleRefresh(Editor editor, AlignmentInlayManager manager) {
		Project project = editor.getProject();
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
		Document document = editor.getDocument();
		PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

		psiDocManager.performForCommittedDocument(document, () -> {
			if (editor.isDisposed()) return;

			PsiFile psiFile = psiDocManager.getPsiFile(document);
			if (psiFile == null) return;

			List<ObjectLiteralFinder.ObjectGroup> groups =
				 ObjectLiteralFinder.findGroups(psiFile, document);

			manager.refresh(groups);
		});
	}

	private boolean isJsEditor(Editor editor) {
		VirtualFile vf = FileDocumentManager.getInstance()
			 .getFile(editor.getDocument());
		if (vf == null) return false;
		return vf.getFileType() instanceof JavaScriptFileType;
	}
}

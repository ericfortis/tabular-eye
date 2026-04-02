package com.ericfortis.tabulareye;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
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
 * editorCreated  → guard JS file, attach DocumentListener, initial render
 * documentChanged→ re-run PSI parse + inlay refresh (debounced via
 * PsiDocumentManager.performForCommittedDocument so we
 * always work on a fully parsed PSI tree)
 * editorReleased → dispose all inlays for that editor
 * <p>
 * One AlignmentInlayManager is kept per editor instance.
 */
public class ObjectAlignmentPlugin implements EditorFactoryListener {

	private final Map<Editor, AlignmentInlayManager> managers = new HashMap<>();

	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		Editor editor = event.getEditor();

		if (!isJsEditor(editor)) 
			return;

		AlignmentInlayManager manager = new AlignmentInlayManager(editor);
		managers.put(editor, manager);

		// Listen for document changes.
		editor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void documentChanged(@NotNull DocumentEvent e) {
				scheduleRefresh(editor, manager);
			}
		});

		// Initial render.
		scheduleRefresh(editor, manager);
	}

	@Override
	public void editorReleased(@NotNull EditorFactoryEvent event) {
		Editor editor = event.getEditor();
		AlignmentInlayManager manager = managers.remove(editor);
		if (manager != null) {
			manager.clearAll();
		}
	}

	
	/**
	 * Waits for PSI to be committed (so the tree reflects the latest edits),
	 * then re-parses object literals and rebuilds inlays.
	 * <p>
	 * PsiDocumentManager.performForCommittedDocument ensures we don't read
	 * a stale PSI tree while the user is mid-typing.
	 */
	private void scheduleRefresh(Editor editor, AlignmentInlayManager manager) {
		Project project = editor.getProject();
		if (project == null || project.isDisposed()) {
			// Fall back: try all open projects (happens for editors opened
			// before a project is fully loaded).
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

		// performForCommittedDocument runs the lambda only when the PSI is
		// in sync with the document — after any pending commits are flushed.
		psiDocManager.performForCommittedDocument(document, () -> {
			if (editor.isDisposed()) return;

			PsiFile psiFile = psiDocManager.getPsiFile(document);
			if (psiFile == null) return;

			List<ObjectLiteralFinder.ObjectGroup> groups =
				 ObjectLiteralFinder.findGroups(psiFile, document);

			manager.refresh(groups);
		});
	}

	/**
	 * Returns true only for editors backed by a .js file with the
	 * ECMAScript 6 language association used by WebStorm/IntelliJ.
	 */
	private boolean isJsEditor(Editor editor) {
		VirtualFile vf = FileDocumentManager.getInstance()
			 .getFile(editor.getDocument());
		if (vf == null) return false;

		// FileType check covers .js, .mjs etc. registered as JavaScriptFileType.
		return vf.getFileType() instanceof JavaScriptFileType;
	}
}

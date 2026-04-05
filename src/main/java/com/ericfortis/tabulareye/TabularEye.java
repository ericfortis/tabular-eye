package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;



public class TabularEye implements EditorFactoryListener {

	// This way CSS and JS plugins (bundled) could be optional
	// Although, not sure if they can be disabled, at least in WebStorm I can't.
	private static final ExtensionPointName<AlignmentFinder> EP_NAME =
		 ExtensionPointName.create("com.ericfortis.tabulareye.alignmentFinder");

	private final Map<Editor, EditorSession> sessions = new HashMap<>();


	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		var editor = event.getEditor();
		var project = editor.getProject();
		if (project == null) return;

		var document = editor.getDocument();
		var psiDocManager = PsiDocumentManager.getInstance(project);
		if (psiDocManager.getPsiFile(document) == null || editor.isViewer()) return;

		psiDocManager.performForCommittedDocument(document, () -> ReadAction.runBlocking(() -> {
			if (!editor.isDisposed())
				openSession(editor, project);
		}));
	}

	@Override
	public void editorReleased(@NotNull EditorFactoryEvent event) {
		var session = sessions.remove(event.getEditor());
		if (session != null)
			Disposer.dispose(session);
	}


	private void openSession(Editor editor, com.intellij.openapi.project.Project project) {
		if (sessions.containsKey(editor))
			return;

		var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
		if (psiFile == null) return;

		var applicable = EP_NAME.getExtensionList().stream().filter(f -> f.isApplicable(psiFile)).toList();
		if (applicable.isEmpty())
			return;

		var session = new EditorSession(editor, project, applicable);
		sessions.put(editor, session);

		// Trigger an initial render for files that are open on startup
		session.refresh(project);
	}
}

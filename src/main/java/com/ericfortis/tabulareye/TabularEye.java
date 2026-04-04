package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder;
import com.ericfortis.tabulareye.finders.CssPropertyFinder;
import com.ericfortis.tabulareye.finders.JsObjectLiteralFinder;
import com.ericfortis.tabulareye.finders.Js2dArrayFinder;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabularEye implements EditorFactoryListener {

	private final List<AlignmentFinder> finders = List.of(
		 new JsObjectLiteralFinder(),
		 new Js2dArrayFinder(),
		 new CssPropertyFinder()
	);

	private final Map<Editor, EditorSession> sessions = new HashMap<>();


	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		var editor = event.getEditor();
		var project = editor.getProject();
		if (project == null) return;

		var document = editor.getDocument();
		PsiDocumentManager.getInstance(project).performForCommittedDocument(document, () -> ReadAction.runBlocking(() -> {
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

		var applicable = finders.stream().filter(f -> f.isApplicable(psiFile)).toList();
		if (applicable.isEmpty()) return;

		var session = new EditorSession(editor, project, applicable);
		sessions.put(editor, session);

		// Trigger an initial render for files that are open on startup
		session.refresh(project);
	}
}

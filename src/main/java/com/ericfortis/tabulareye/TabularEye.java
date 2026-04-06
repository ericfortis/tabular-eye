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
import com.intellij.util.concurrency.AppExecutorUtil;

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
		if (project == null)
			return;

		var document = editor.getDocument();
		var psiDocManager = PsiDocumentManager.getInstance(project);
		var psiFile = psiDocManager.getPsiFile(document);
		// Check for virtual file to avoid editors not backed by real files (like terminal)
		if (psiFile == null || psiFile.getVirtualFile() == null || editor.isViewer())
			return;

		psiDocManager.performForCommittedDocument(document, () -> {
			if (project.isDisposed() || editor.isDisposed()) 
				return;
			ReadAction.nonBlocking(() -> openSession(editor, project))
				 .expireWith(project)
				 .submit(AppExecutorUtil.getAppExecutorService());
		});
	}

	@Override
	public void editorReleased(@NotNull EditorFactoryEvent event) {
		var session = sessions.remove(event.getEditor());
		if (session != null)
			Disposer.dispose(session);
	}


	private EditorSession openSession(Editor editor, com.intellij.openapi.project.Project project) {
		if (project.isDisposed() || editor.isDisposed() || sessions.get(editor) != null)
			return null;

		var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
		if (psiFile == null || psiFile.getVirtualFile() == null)
			return null;

		var extensionList = EP_NAME.getExtensionList();
		var applicable = extensionList.stream().filter(f -> f.isApplicable(psiFile)).toList();
		if (applicable.isEmpty())
			return null;

		var session = new EditorSession(editor, project, applicable);
		sessions.put(editor, session);

		// Trigger an initial render for files that are open on startup
		session.refresh(project);
		return session;
	}
}

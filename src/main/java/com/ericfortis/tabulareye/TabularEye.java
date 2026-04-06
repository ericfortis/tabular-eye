package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


public class TabularEye implements EditorFactoryListener {

	// This way the bundled plugins we depend on could be optional
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
		// Avoids editors not backed by real files (e.g., the Terminal)
		if (psiFile == null || psiFile.getVirtualFile() == null || editor.isViewer())
			return;

		psiDocManager.performForCommittedDocument(document, () -> {
			if (!project.isDisposed() && !editor.isDisposed())
				ReadAction.nonBlocking(() -> openSession(editor, project))
					 .expireWhen(() -> project.isDisposed() || editor.isDisposed())
					 .submit(AppExecutorUtil.getAppExecutorService());
		});
	}

	@Override
	public void editorReleased(@NotNull EditorFactoryEvent event) {
		var session = sessions.remove(event.getEditor());
		if (session != null)
			Disposer.dispose(session);
	}


	private EditorSession openSession(Editor editor, Project project) {
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

package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TabularEye implements EditorFactoryListener {

	// This way the bundled plugins we depend on could be optional
	private static final ExtensionPointName<AlignmentDetector> EPN =
		 ExtensionPointName.create("com.ericfortis.tabulareye.alignmentDetector");

	private List<AlignmentDetector> allDetectors;

	private final Map<Editor, EditorSession> sessions = new HashMap<>();


	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		var editor = event.getEditor();
		if (editor.getEditorKind() != EditorKind.MAIN_EDITOR)
			return;

		var project = editor.getProject();
		if (project == null)
			return;

		PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.getDocument(), () -> {
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
		if (sessions.get(editor) != null)
			return null;

		var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
		if (psiFile == null)
			return null;

		if (allDetectors == null)
			allDetectors = EPN.getExtensionList();

		var detectors = allDetectors.stream().filter(f -> f.isApplicable(psiFile)).toList();
		if (detectors.isEmpty())
			return null;

		var session = new EditorSession(editor, project, detectors);
		sessions.put(editor, session);
		return session;
	}
}

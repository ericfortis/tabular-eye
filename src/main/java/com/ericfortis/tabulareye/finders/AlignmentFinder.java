package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Visitor for finding the column spacing needed for tabularizing.
 */
public abstract class AlignmentFinder {
	public abstract boolean isApplicable(@NotNull PsiFile file);

	public static boolean isJs(@NotNull PsiFile file) {
		var type = file.getFileType();
		return type instanceof com.intellij.lang.javascript.JavaScriptFileType
			 || type instanceof com.intellij.lang.javascript.TypeScriptFileType
			 || type instanceof com.intellij.lang.javascript.TypeScriptJSXFileType
			 || type instanceof com.intellij.lang.javascript.JSXFileType;
	}

	public static boolean isCss(@NotNull PsiFile file) {
		var type = file.getFileType();
		return type instanceof com.intellij.psi.css.CssFileType;
	}

	static boolean isMultiline(PsiElement elem, Document doc) {
		int startLine = doc.getLineNumber(elem.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(elem.getTextRange().getEndOffset());
		return endLine > startLine;
	}

	protected static int findSeparatorOffset(PsiElement elem, String separator) {
		var child = elem.getFirstChild();
		while (child != null) {
			if (separator.equals(child.getText()))
				return child.getTextRange().getStartOffset();
			child = child.getNextSibling();
		}
		return -1;
	}

	@NotNull
	public abstract List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document);

	public static class AlignmentGroup {
		private final List<PropInfo> props = new ArrayList<>();

		public int getStartOffset() {
			return props.isEmpty()
				 ? -1
				 : props.getFirst().keyStartOffset();
		}

		public int getEndOffset() {
			return props.isEmpty()
				 ? -1
				 : props.getLast().colonOffset();
		}

		public void add(PropInfo p) {
			props.add(p);
		}

		public boolean isValid() {
			return props.size() > 1;
		}

		public List<PropInfo> props() {
			return Collections.unmodifiableList(props);
		}
	}

	public record PropInfo(String keyText, int keyStartOffset, int colonOffset) {
	}
}

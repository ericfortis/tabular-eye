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

	static boolean isMultiline(PsiElement elem, Document doc) {
		int startLine = doc.getLineNumber(elem.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(elem.getTextRange().getEndOffset());
		return endLine > startLine;
	}

	/**
	 * Walks the immediate children of {@code elem} looking for a child whose
	 * text equals {@code tokenText} and returns its start offset, or -1 if not found.
	 */
	protected static int findTokenOffset(PsiElement elem, String tokenText) {
		var child = elem.getFirstChild();
		while (child != null) {
			if (tokenText.equals(child.getText())) 
				return child.getTextRange().getStartOffset();
			child = child.getNextSibling();
		}
		return -1;
	}

	@NotNull
	public abstract List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document);

	public static class AlignmentGroup {
		private final List<PropInfo> props = new ArrayList<>();

		public void add(PropInfo p) {
			props.add(p);
		}

		/**
		 * A group is only meaningful when at least two properties can be aligned.
		 */
		public boolean isValid() {
			return props.size() > 1;
		}

		public List<PropInfo> props() {
			return Collections.unmodifiableList(props);
		}
	}

	public record PropInfo(String keyText, int colonOffset) {
	}
}

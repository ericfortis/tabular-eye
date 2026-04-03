package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

	@NotNull
	public abstract List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document);

	public static class AlignmentGroup {
		public final List<PropInfo> props = new ArrayList<>();
	}

	public record PropInfo(String keyText, int colonOffset) {
	}
}


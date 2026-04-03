package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AlignmentFinder {
	/**
	 * Returns true if this finder can handle the given file.
	 */
	boolean isApplicable(@NotNull PsiFile file);

	/**
	 * Finds groups for alignment in the given file.
	 */
	@NotNull
	List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document);
}

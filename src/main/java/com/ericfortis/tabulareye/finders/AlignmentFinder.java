package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Visitor for finding the column spacing needed for tabularizing. */
public interface AlignmentFinder {
	boolean isApplicable(@NotNull PsiFile file);

	@NotNull
	List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document);

	class AlignmentGroup {
		public final List<PropInfo> props = new ArrayList<>();
	}

	record PropInfo(String keyText, int colonOffset) {
	}
}

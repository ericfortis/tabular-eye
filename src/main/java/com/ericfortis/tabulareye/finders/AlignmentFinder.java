package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AlignmentFinder {
	boolean isApplicable(@NotNull PsiFile file);

	@NotNull
	List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document);
}

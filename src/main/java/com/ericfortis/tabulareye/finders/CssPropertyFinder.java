package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.css.CssBlock;
import com.intellij.psi.css.CssDeclaration;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds CSS properties to align.
 * <p>
 * header {
 * display: flex;
 * flex:    0 0 100%;
 * padding: 16px;
 * }
 */
public class CssPropertyFinder implements AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return "CSS".equals(file.getLanguage().getID());
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();
		Class<? extends PsiElement> declarationClass = CssDeclaration.class;
		Class<? extends PsiElement> blockClass = CssBlock.class;

		for (var block : PsiTreeUtil.collectElementsOfType(file, blockClass)) {
			if (!isMultiline(block, doc))
				continue;

			var group = new AlignmentGroup();
			for (var decl : PsiTreeUtil.getChildrenOfTypeAsList(block, declarationClass)) {
				int colonOffset = findColonOffset(decl);
				if (colonOffset > 0) 
					group.props.add(new PropInfo(getPropertyName(decl), colonOffset));
			}

			if (group.props.size() > 1)
				groups.add(group);
		}

		return groups;
	}

	private String getPropertyName(PsiElement decl) {
		// Usually the first child is the property
		PsiElement firstChild = decl.getFirstChild();
		return firstChild == null
			 ? ""
			 : firstChild.getText();
	}

	private int findColonOffset(PsiElement decl) {
		var child = decl.getFirstChild();
		while (child != null) {
			if (":".equals(child.getText()))
				return child.getTextRange().getStartOffset();
			child = child.getNextSibling();
		}
		return -1;
	}

	private static boolean isMultiline(PsiElement elem, Document doc) {
		int startLine = doc.getLineNumber(elem.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(elem.getTextRange().getEndOffset());
		return endLine > startLine;
	}
}

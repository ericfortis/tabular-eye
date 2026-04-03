package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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
		String id = file.getLanguage().getID();
		return "CSS".equals(id) || "SCSS".equals(id) || "LESS".equals(id);
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends PsiElement> declarationClass = (Class<? extends PsiElement>) Class.forName("com.intellij.psi.css.CssDeclaration");
			@SuppressWarnings("unchecked")
			Class<? extends PsiElement> blockClass = (Class<? extends PsiElement>) Class.forName("com.intellij.psi.css.CssBlock");

			var allBlocks = PsiTreeUtil.collectElementsOfType(file, blockClass);
			for (var block : allBlocks) {
				if (!isMultiline(block, doc)) continue;

				var group = new AlignmentGroup();
				var declarations = PsiTreeUtil.getChildrenOfTypeAsList(block, declarationClass);

				for (var decl : declarations) {
					int colonOffset = findColonOffset(decl);
					if (colonOffset > 0) {
						// For CssDeclaration, we can try to get the property name
						// but AlignmentGroup uses PropInfo which takes (keyText, colonOffset).
						// We can get text before colon or just use decl.getFirstChild().getText() if it's the property.
						group.props.add(new PropInfo(getPropertyName(decl), colonOffset));
					}
				}

				if (group.props.size() > 1)
					groups.add(group);
			}
		} catch (ClassNotFoundException e) {
			// Fallback or log? If the classes aren't there, we can't do much.
			// But they SHOULD be there if we added the dependency.
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

	private static boolean isMultiline(PsiElement element, Document doc) {
		int startLine = doc.getLineNumber(element.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(element.getTextRange().getEndOffset());
		return endLine > startLine;
	}
}

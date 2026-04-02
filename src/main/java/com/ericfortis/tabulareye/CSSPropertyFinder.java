package com.ericfortis.tabulareye;

import com.intellij.lang.Language;
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
public class CSSPropertyFinder implements AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return isCssLanguage(file.getLanguage());
	}

	private boolean isCssLanguage(Language language) {
		String id = language.getID();
		return "CSS".equals(id) || "SCSS".equals(id) || "LESS".equals(id);
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(
		 @NotNull PsiFile file,
		 @NotNull Document document
	) {
		List<AlignmentGroup> groups = new ArrayList<>();

		// We'll look for CSS declaration blocks or similar containers.
		// In CSS PSI, CssDeclaration is usually the property: value; pair.
		// They are often grouped inside a CssRuleset or CssBlock.

		// Let's try to find all elements that look like a property: value pair.
		// We want to group those that are siblings or in the same block.

		// To keep it simple and consistent with other finders, we'll find blocks
		// and group properties within them.

		// Since I don't have the exact class names at hand, I'll use a more generic approach
		// or try to guess the common ones: CssDeclaration, CssBlock.

		// I'll search for "CssDeclaration" in the project if possible, but it's a library.
		// Assuming CssDeclaration exists and has a colon.

		// Let's try to find all "CssBlock" or similar and group their "CssDeclaration" children.
		// If "CssBlock" is not available, I'll look for declarations and group them by parent.

		// I will use reflection or common knowledge of CSS PSI:
		// CssDeclaration
		// CssBlock
		// CssRuleset

		// Actually, a safer way without knowing exact class names is to look for elements
		// that contain a colon and have siblings that also contain a colon.

		// But let's try to be specific.
		try {
			@SuppressWarnings("unchecked")
			Class<? extends PsiElement> declarationClass = (Class<? extends PsiElement>) Class.forName("com.intellij.psi.css.CssDeclaration");
			@SuppressWarnings("unchecked")
			Class<? extends PsiElement> blockClass = (Class<? extends PsiElement>) Class.forName("com.intellij.psi.css.CssBlock");

			var allBlocks = PsiTreeUtil.collectElementsOfType(file, blockClass);
			for (var block : allBlocks) {
				if (!isMultiline(block, document)) continue;

				var group = new AlignmentGroup();
				var declarations = PsiTreeUtil.getChildrenOfTypeAsList(block, declarationClass);

				for (var decl : declarations) {
					// Find colon offset in declaration
					int colonOffset = findColonOffset(decl);
					if (colonOffset > 0) {
						// For CssDeclaration, we can try to get the property name
						// but AlignmentGroup uses PropInfo which takes (keyText, colonOffset).
						// We can get text before colon or just use decl.getFirstChild().getText() if it's the property.
						String propName = getPropertyName(decl);
						group.props.add(new PropInfo(propName, colonOffset));
					}
				}

				if (group.props.size() > 1) {
					groups.add(group);
				}
			}
		} catch (ClassNotFoundException e) {
			// Fallback or log? If the classes aren't there, we can't do much.
			// But they SHOULD be there if we added the dependency.
		}

		return groups;
	}

	private String getPropertyName(PsiElement decl) {
		// Usually the first child is the property
		return decl.getFirstChild().getText();
	}

	private int findColonOffset(PsiElement decl) {
		var child = decl.getFirstChild();
		while (child != null) {
			if (":".equals(child.getText())) {
				return child.getTextRange().getStartOffset();
			}
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

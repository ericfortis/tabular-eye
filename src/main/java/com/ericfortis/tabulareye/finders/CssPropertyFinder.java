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

public class CssPropertyFinder extends AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return "CSS".equals(file.getLanguage().getID());
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var block : PsiTreeUtil.findChildrenOfType(file, CssBlock.class))
			if (isMultiline(block, doc)) {
				var group = new AlignmentGroup();
				for (var child = block.getFirstChild(); child != null; child = child.getNextSibling())
					if (child instanceof CssDeclaration decl) {
						int colonOffset = findSeparatorOffset(decl, ":");
						if (colonOffset > 0)
							group.add(new PropInfo(getPropertyName(decl), colonOffset));
					}

				if (group.isValid())
					groups.add(group);
			}

		return groups;
	}

	private String getPropertyName(PsiElement decl) {
		var firstChild = decl.getFirstChild();
		return firstChild == null
			 ? ""
			 : firstChild.getText();
	}
}

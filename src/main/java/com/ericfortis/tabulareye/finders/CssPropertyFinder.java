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
	CssPropertyFinder() {
		super(CSS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc, int startOffset, int endOffset) {
		List<AlignmentBlock> groups = new ArrayList<>();

		for (var el : PsiTreeUtil.findChildrenOfType(file, CssBlock.class)) {
			var range = el.getTextRange();
			if (range.getEndOffset() < startOffset || range.getStartOffset() > endOffset)
				continue;

			if (isMultiline(el, doc)) {
				var block = new AlignmentBlock();
				for (var child = el.getFirstChild(); child != null; child = child.getNextSibling())
					if (child instanceof CssDeclaration decl) {
						int colonOffset = findSeparatorOffset(decl, ":");
						if (colonOffset > 0) {
							int firstOffset = decl.getFirstChild() != null 
								 ? decl.getFirstChild().getTextRange().getStartOffset() 
								 : colonOffset;
							block.add(new PropInfo(getPropertyName(decl), firstOffset, colonOffset));
						}
					}
				if (block.isValid())
					groups.add(block);
			}
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

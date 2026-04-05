package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.psi.JSArrayLiteralExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Only aligns the second column
 */
public class Js2dArrayFinder extends AlignmentFinder {
	Js2dArrayFinder() {
		super(JS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc, int startOffset, int endOffset) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		for (var el : PsiTreeUtil.collectElementsOfType(file, JSArrayLiteralExpression.class)) {
			var range = el.getTextRange();
			if (range.getEndOffset() < startOffset || range.getStartOffset() > endOffset)
				continue;

			if (isMultiline(el, doc) && is2dArray(el)) {
				var g = buildBlock(el);
				if (g.isValid())
					blocks.add(g);
			}
		}
		return blocks;
	}


	private static boolean is2dArray(JSArrayLiteralExpression array) {
		for (var el : array.getExpressions())
			if (el instanceof JSArrayLiteralExpression inner)
				if (inner.getExpressions().length >= 2)
					return true;
		return false;
	}

	private static AlignmentBlock buildBlock(JSArrayLiteralExpression arr) {
		var block = new AlignmentBlock();
		for (var el : arr.getExpressions())
			if (el instanceof JSArrayLiteralExpression inner) {
				var innerElements = inner.getExpressions();
				if (innerElements.length >= 2) {
					var first = innerElements[0];
					int commaOffset = findSeparatorOffset(inner, ",");
					if (commaOffset > 0)
						block.add(new PropInfo(first.getText(), first.getTextRange().getStartOffset(), commaOffset));
				}
			}
		return block;
	}
}

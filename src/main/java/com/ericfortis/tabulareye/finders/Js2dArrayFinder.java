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
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> groups = new ArrayList<>();
		for (var arr : PsiTreeUtil.collectElementsOfType(file, JSArrayLiteralExpression.class))
			if (isMultiline(arr, doc) && is2dArray(arr)) {
				var g = buildGroup(arr);
				if (g.isValid())
					groups.add(g);
			}
		return groups;
	}


	private static boolean is2dArray(JSArrayLiteralExpression array) {
		for (var elem : array.getExpressions())
			if (elem instanceof JSArrayLiteralExpression inner)
				if (inner.getExpressions().length >= 2)
					return true;
		return false;
	}

	private static AlignmentBlock buildGroup(JSArrayLiteralExpression arr) {
		var group = new AlignmentBlock();
		for (var elem : arr.getExpressions())
			if (elem instanceof JSArrayLiteralExpression inner) {
				var innerElements = inner.getExpressions();
				if (innerElements.length >= 2) {
					var first = innerElements[0];
					int commaOffset = findSeparatorOffset(inner, ",");
					if (commaOffset > 0)
						group.add(new PropInfo(first.getText(), first.getTextRange().getStartOffset(), commaOffset));
				}
			}
		return group;
	}
}

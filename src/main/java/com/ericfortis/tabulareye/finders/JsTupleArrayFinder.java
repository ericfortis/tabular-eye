package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.psi.JSArrayLiteralExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class JsTupleArrayFinder extends AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return file.getFileType() instanceof JavaScriptFileType;
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var arr : PsiTreeUtil.collectElementsOfType(file, JSArrayLiteralExpression.class))
			if (isMultiline(arr, doc) && isTupleArray(arr)) {
				var g = buildGroup(arr);
				if (g != null && g.isValid())
					groups.add(g);
			}

		return groups;
	}

	/**
	 * A "tuple-array" is an array whose elements are themselves 2-element arrays,
	 * e.g. [[key1, val1], [key2, val2]].
	 * <p>
	 * We require at least one such child tuple to qualify. A flat [key, value]
	 * pair is NOT a tuple-array — it is a single tuple, not a container of them.
	 * Note: a top-level array with exactly 2 elements could itself be a single
	 * tuple, but if those elements happen to be 2-element arrays, we still treat
	 * it as a tuple-array (e.g. [[a, 1], [b, 2]]).
	 */
	private static boolean isTupleArray(JSArrayLiteralExpression array) {
		for (var elem : array.getExpressions())
			if (elem instanceof JSArrayLiteralExpression tuple)
				if (tuple.getExpressions().length == 2)
					return true;
		return false;
	}

	private static AlignmentGroup buildGroup(JSArrayLiteralExpression arr) {
		var group = new AlignmentGroup();

		for (var elem : arr.getExpressions())
			if (elem instanceof JSArrayLiteralExpression tuple) {
				var tupleElements = tuple.getExpressions();
				if (tupleElements.length == 2) {
					var first = tupleElements[0];
					int commaOffset = findTokenOffset(tuple, ",");
					if (commaOffset > 0)
						group.add(new PropInfo(first.getText(), commaOffset));
				}
			}

		return group.props().isEmpty()
			 ? null
			 : group;
	}
}

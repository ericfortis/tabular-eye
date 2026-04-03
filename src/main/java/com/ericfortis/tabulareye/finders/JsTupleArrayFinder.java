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
					int commaOffset = findSeparatorOffset(tuple, ",");
					if (commaOffset > 0)
						group.add(new PropInfo(first.getText(), commaOffset));
				}
			}

		return group.props().isEmpty()
			 ? null
			 : group;
	}
}

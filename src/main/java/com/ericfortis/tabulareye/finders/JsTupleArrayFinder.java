package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.psi.JSArrayLiteralExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class JsTupleArrayFinder implements AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return file.getFileType() instanceof JavaScriptFileType;
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var array : PsiTreeUtil.collectElementsOfType(file, JSArrayLiteralExpression.class))
			if (hasTuples(array) && isMultiline(array, document)) {
				var group = buildGroup(array);
				if (group != null && group.props.size() > 1)
					groups.add(group);
			}

		return groups;
	}

	private static boolean hasTuples(JSArrayLiteralExpression array) {
		if (array.getExpressions().length == 2)
			return false;

		for (var element : array.getExpressions())
			if (element instanceof JSArrayLiteralExpression tuple)
				if (tuple.getExpressions().length == 2)
					return true;
		return false;
	}

	private static boolean isMultiline(JSArrayLiteralExpression array, Document doc) {
		int startLine = doc.getLineNumber(array.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(array.getTextRange().getEndOffset());
		return endLine > startLine;
	}

	private static AlignmentGroup buildGroup(JSArrayLiteralExpression array) {
		var group = new AlignmentGroup();

		for (var element : array.getExpressions())
			if (element instanceof JSArrayLiteralExpression tuple) {
				var tupleElements = tuple.getExpressions();
				if (tupleElements.length == 2) {
					var first = tupleElements[0];
					int commaOffset = findCommaOffset(tuple);
					if (commaOffset > 0)
						group.props.add(new PropInfo(first.getText(), commaOffset));
				}
			}

		return group.props.isEmpty()
			 ? null
			 : group;
	}

	private static int findCommaOffset(JSArrayLiteralExpression tuple) {
		var child = tuple.getFirstChild();
		while (child != null) {
			if (",".equals(child.getText()))
				return child.getTextRange().getStartOffset();
			child = child.getNextSibling();
		}
		return -1;
	}
}

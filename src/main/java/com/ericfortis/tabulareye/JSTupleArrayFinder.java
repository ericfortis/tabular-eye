package com.ericfortis.tabulareye;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.psi.JSArrayLiteralExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds all multiline arrays containing subarrays (tuples) in a JS file.
 * We want to align the second element of each tuple [a, b].
 * <p>
 * Example:
 * [
 * [1, 2],
 * [123, 456]
 * ]
 */
public class JSTupleArrayFinder implements AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return file.getFileType() instanceof JavaScriptFileType;
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(
		 @NotNull PsiFile file,
		 @NotNull Document document
	) {
		List<AlignmentGroup> groups = new ArrayList<>();

		// Collect every JSArrayLiteralExpression in the file.
		var allArrays = PsiTreeUtil.collectElementsOfType(file, JSArrayLiteralExpression.class);

		for (var array : allArrays) {
			// Check if this array contains at least one tuple (array of 2 elements).
			// We want to align these tuples.
			if (!hasTuples(array))
				continue;

			// Must span more than one line.
			if (!isMultiline(array, document))
				continue;

			var group = buildGroup(array);
			if (group != null && group.props.size() > 1) {
				groups.add(group);
			}
		}

		return groups;
	}

	private static boolean hasTuples(JSArrayLiteralExpression array) {
		// If the array itself is a tuple, we don't want to process it as a container.
		// We only want containers that HOLD tuples.
		if (array.getExpressions().length == 2) {
			return false;
		}

		for (JSExpression element : array.getExpressions()) {
			if (element instanceof JSArrayLiteralExpression tuple) {
				if (tuple.getExpressions().length == 2) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isMultiline(JSArrayLiteralExpression array, Document doc) {
		int startLine = doc.getLineNumber(array.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(array.getTextRange().getEndOffset());
		return endLine > startLine;
	}

	private static AlignmentGroup buildGroup(JSArrayLiteralExpression array) {
		var group = new AlignmentGroup();

		for (JSExpression element : array.getExpressions()) {
			if (element instanceof JSArrayLiteralExpression tuple) {
				JSExpression[] tupleElements = tuple.getExpressions();
				if (tupleElements.length == 2) {
					JSExpression first = tupleElements[0];
					int commaOffset = findCommaOffset(tuple);
					if (commaOffset > 0) {
						group.props.add(new PropInfo(first.getText(), commaOffset));
					}
				}
			}
		}

		return group.props.isEmpty() ? null : group;
	}

	/**
	 * Locates the first ',' token inside a JSArrayLiteralExpression.
	 */
	private static int findCommaOffset(JSArrayLiteralExpression tuple) {
		var child = tuple.getFirstChild();
		while (child != null) {
			if (",".equals(child.getText())) {
				return child.getTextRange().getStartOffset();
			}
			child = child.getNextSibling();
		}
		return -1;
	}
}

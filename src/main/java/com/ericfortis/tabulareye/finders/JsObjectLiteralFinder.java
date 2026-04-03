package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds all multiline object literals in a JS file and returns
 * the plain key:value properties we want to align.
 * <p>
 * Excluded intentionally:
 * - Shorthand properties  ({ foo })
 * - Spread elements       ({ ...rest })
 * - Single-line objects   ({ a: 1, b: 2 })
 */
public class JsObjectLiteralFinder implements AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return file.getFileType() instanceof JavaScriptFileType;
	}

	/**
	 * Walks the PSI tree of the given file and returns one AlignmentGroup
	 * per qualifying multiline object literal found.
	 *
	 * @param file     the PSI file (already confirmed to be JS/ES6)
	 * @param document the backing document (used for line-number checks)
	 */
	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(
		 @NotNull PsiFile file,
		 @NotNull Document document
	) {
		List<AlignmentGroup> groups = new ArrayList<>();

		// Collect every JSObjectLiteralExpression in the file.
		var allObjects = PsiTreeUtil.collectElementsOfType(file, JSObjectLiteralExpression.class);

		for (var obj : allObjects) {
			// Must span more than one line.
			if (!isMultiline(obj, document))
				continue;

			var group = buildGroup(obj);
			if (group != null && group.props.size() > 1) {
				// Nothing to align if there's only one property.
				groups.add(group);
			}
		}

		return groups;
	}

	private static boolean isMultiline(JSObjectLiteralExpression obj, Document doc) {
		int startLine = doc.getLineNumber(obj.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(obj.getTextRange().getEndOffset());
		return endLine > startLine;
	}

	/**
	 * Builds an AlignmentGroup from a qualifying JSObjectLiteralExpression.
	 * Returns null if no alignable properties were found.
	 */
	private static AlignmentGroup buildGroup(JSObjectLiteralExpression obj) {
		var group = new AlignmentGroup();

		for (var prop : obj.getProperties()) {
			if (prop == null || prop.isShorthanded()) 
				continue; 

			int colonOffset = findColonOffset(prop);
			if (colonOffset < 0)
				continue;

			// Extract all text before the colon to form the key text.
			// This covers normal, quoted, and computed keys ([expr]).
			var keyBuilder = new StringBuilder();
			var child = prop.getFirstChild();
			while (child != null && !":".equals(child.getText())) {
				keyBuilder.append(child.getText());
				child = child.getNextSibling();
			}
			var keyText = keyBuilder.toString().trim();

			if (keyText.isEmpty())
				continue;

			group.props.add(new PropInfo(keyText, colonOffset));
		}

		return group.props.isEmpty()
			 ? null
			 : group;
	}

	/**
	 * Locates the ':' token inside a JSProperty element by scanning its
	 * children for the COLON token type.
	 * <p>
	 * IntelliJ's JSProperty PSI doesn't expose the colon directly via API,
	 * so we walk child tokens.
	 */
	private static int findColonOffset(JSProperty prop) {
		var child = prop.getFirstChild();
		while (child != null) {
			if (":".equals(child.getText()))
				return child.getTextRange().getStartOffset();
			child = child.getNextSibling();
		}
		return -1;
	}
}
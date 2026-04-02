package com.ericfortis.tabulareye;

import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Finds all multiline object literals in a JS file and returns
 * the plain key:value properties we want to align.
 * <p>
 * Excluded intentionally:
 * - Shorthand properties  ({ foo })
 * - Spread elements       ({ ...rest })
 * - Computed keys         ({ [expr]: val })
 * - Single-line objects   ({ a: 1, b: 2 })
 * - Nested object values  (handled as separate objects if multiline)
 */
public class ObjectLiteralFinder {

	/**
	 * One alignable property: the key name text and the document offset
	 * of the colon character (we'll place the inlay at colonOffset + 1).
	 *
	 * @param keyText     The key's display text, e.g. "anotherLongProp"
	 * @param colonOffset Document offset of the ':' token for this property
	 */
	public record PropInfo(String keyText, int colonOffset) {
	}

	/**
	 * One object literal whose properties should be aligned together.
	 */
	public static class ObjectGroup {
		public final List<PropInfo> props = new ArrayList<>();
	}

	/**
	 * Walks the PSI tree of the given file and returns one ObjectGroup
	 * per qualifying multiline object literal found at the top level
	 * (not nested inside another object literal that is itself multiline).
	 *
	 * @param file     the PSI file (already confirmed to be JS/ES6)
	 * @param document the backing document (used for line-number checks)
	 */
	@NotNull
	public static List<ObjectGroup> findGroups(
		 @NotNull PsiFile file,
		 @NotNull Document document
	) {
		List<ObjectGroup> groups = new ArrayList<>();

		// Collect every JSObjectLiteralExpression in the file.
		Collection<JSObjectLiteralExpression> allObjects =
			 PsiTreeUtil.collectElementsOfType(file, JSObjectLiteralExpression.class);

		for (JSObjectLiteralExpression obj : allObjects) {
			// Only top-level objects (not nested inside another object value).
			if (isNestedInObject(obj))
				continue;

			// Must span more than one line.
			if (!isMultiline(obj, document))
				continue;

			ObjectGroup group = buildGroup(obj, document);
			if (group != null && group.props.size() > 1) {
				// Nothing to align if there's only one property.
				groups.add(group);
			}
		}

		return groups;
	}

	/**
	 * Returns true if this object expression is directly inside a property
	 * value of another object expression (i.e., it is a nested object).
	 */
	private static boolean isNestedInObject(JSObjectLiteralExpression obj) {
		PsiElement parent = obj.getParent();
		// The parent of a property value is JSProperty; its parent is JSObjectLiteralExpression.
		if (parent instanceof JSProperty) {
			PsiElement grandParent = parent.getParent();
			return grandParent instanceof JSObjectLiteralExpression;
		}
		return false;
	}

	/**
	 * Returns true if the object literal spans more than one line.
	 */
	private static boolean isMultiline(JSObjectLiteralExpression obj, Document doc) {
		int startLine = doc.getLineNumber(obj.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(obj.getTextRange().getEndOffset());
		return endLine > startLine;
	}

	/**
	 * Builds an ObjectGroup from a qualifying JSObjectLiteralExpression.
	 * Returns null if no alignable properties were found.
	 */
	private static ObjectGroup buildGroup(
		 JSObjectLiteralExpression obj,
		 Document document
	) {
		ObjectGroup group = new ObjectGroup();

		for (JSProperty prop : obj.getProperties()) {
			// Skip spread elements (JSSpreadExpression appears as a child, not JSProperty,
			// but guard defensively).
			if (prop == null)
				continue;

			// Skip shorthand: shorthand props have no value distinct from name.
			if (prop.isShorthanded())
				continue;

			// Skip computed keys: { [expr]: val }
			if (prop.getName() == null)
				continue;

			// Skip if the value is itself a JSObjectLiteralExpression (nested object).
			JSExpression value = prop.getValue();
			if (value instanceof JSObjectLiteralExpression)
				continue;

			// Find the colon offset.
			int colonOffset = findColonOffset(prop);
			if (colonOffset < 0)
				continue;

			group.props.add(new PropInfo(prop.getName(), colonOffset));
		}

		return group.props.isEmpty() ? null : group;
	}

	/**
	 * Locates the ':' token inside a JSProperty element by scanning its
	 * children for the COLON token type.
	 * <p>
	 * IntelliJ's JSProperty PSI doesn't expose the colon directly via API,
	 * so we walk child tokens.
	 */
	private static int findColonOffset(JSProperty prop) {
		PsiElement child = prop.getFirstChild();
		while (child != null) {
			if (":".equals(child.getText()))
				return child.getTextRange().getStartOffset();
			child = child.getNextSibling();
		}
		return -1;
	}
}
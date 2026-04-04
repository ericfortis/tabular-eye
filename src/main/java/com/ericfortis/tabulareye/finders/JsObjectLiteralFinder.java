package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsObjectLiteralFinder extends AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		var type = file.getFileType();
		return type instanceof com.intellij.lang.javascript.JavaScriptFileType
			 || type instanceof com.intellij.lang.javascript.TypeScriptFileType
			 || type instanceof com.intellij.lang.javascript.TypeScriptJSXFileType
			 || type instanceof com.intellij.lang.javascript.JSXFileType;
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var obj : PsiTreeUtil.collectElementsOfType(file, JSObjectLiteralExpression.class))
			if (isMultiline(obj, doc)) {
				var group = buildGroup(obj);
				if (group != null && group.isValid())
					groups.add(group);
			}

		return groups;
	}


	private AlignmentGroup buildGroup(JSObjectLiteralExpression obj) {
		var group = new AlignmentGroup();

		for (var prop : obj.getProperties()) {
			if (prop == null || prop.isShorthanded())
				continue;

			int colonOffset = findSeparatorOffset(prop, ":");
			if (colonOffset < 0)
				continue;

			var keyBuilder = new StringBuilder();
			var child = prop.getFirstChild();
			while (child != null && !":".equals(child.getText())) {
				keyBuilder.append(child.getText());
				child = child.getNextSibling();
			}

			var keyText = keyBuilder.toString().trim();
			if (!keyText.isEmpty()) {
				int startOffset = Objects.requireNonNull(prop.getFirstChild()).getTextRange().getStartOffset();
				group.add(new PropInfo(keyText, startOffset, colonOffset));
			}
		}

		return group.props().isEmpty()
			 ? null
			 : group;
	}
}

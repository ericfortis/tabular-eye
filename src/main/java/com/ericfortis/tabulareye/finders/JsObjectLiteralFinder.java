package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class JsObjectLiteralFinder extends AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return file.getFileType() instanceof JavaScriptFileType;
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

			int colonOffset = findTokenOffset(prop, ":");
			if (colonOffset < 0)
				continue;

			var keyBuilder = new StringBuilder();
			var child = prop.getFirstChild();
			while (child != null && !":".equals(child.getText())) {
				keyBuilder.append(child.getText());
				child = child.getNextSibling();
			}

			var keyText = keyBuilder.toString().trim();
			if (!keyText.isEmpty())
				group.add(new PropInfo(keyText, colonOffset));
		}

		return group.props().isEmpty()
			 ? null
			 : group;
	}
}

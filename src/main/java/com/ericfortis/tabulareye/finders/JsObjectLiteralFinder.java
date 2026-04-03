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
				if (group != null && group.props.size() > 1)
					groups.add(group);
			}

		return groups;
	}

	
	private static AlignmentGroup buildGroup(JSObjectLiteralExpression obj) {
		var group = new AlignmentGroup();

		for (var prop : obj.getProperties()) {
			if (prop == null || prop.isShorthanded())
				continue;

			int colonOffset = findColonOffset(prop);
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
				group.props.add(new PropInfo(keyText, colonOffset));
		}

		return group.props.isEmpty()
			 ? null
			 : group;
	}
	

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
package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class JsObjectLiteralFinder extends AlignmentFinder {
	JsObjectLiteralFinder() {
		super(JS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		return findGroups(file, doc, JSObjectLiteralExpression.class, this::buildGroup);
	}

	private AlignmentGroup buildGroup(JSObjectLiteralExpression obj) {
		var group = new AlignmentGroup();
		for (var prop : obj.getProperties())
			if (prop != null && !prop.isShorthanded()) {
				var kv = describeKV(findSeparatorOffset(prop, ":"), prop.getFirstChild());
				if (kv != null)
					group.add(kv);
			}
		return group.props().isEmpty() ? null : group;
	}

	static PropInfo describeKV(int separatorOffset, PsiElement firstChild) {
		if (separatorOffset < 0)
			return null;

		var keyBuilder = new StringBuilder();
		var child = firstChild;
		while (child != null && !":".equals(child.getText())) {
			keyBuilder.append(child.getText());
			child = child.getNextSibling();
		}

		var keyText = keyBuilder.toString().trim();
		if (!keyText.isEmpty()) {
			int startOffset = Objects.requireNonNull(firstChild).getTextRange().getStartOffset();
			return new PropInfo(keyText, startOffset, separatorOffset);
		}

		return null;
	}
}

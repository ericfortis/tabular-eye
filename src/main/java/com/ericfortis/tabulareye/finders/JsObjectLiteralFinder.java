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
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, JSObjectLiteralExpression.class, this::buildGroup);
	}

	private AlignmentBlock buildGroup(JSObjectLiteralExpression obj) {
		var group = new AlignmentBlock();
		for (var prop : obj.getProperties())
			if (prop != null && !prop.isShorthanded()) {
				var kv = describeKV(prop);
				if (kv != null)
					group.add(kv);
			}
		return group;
	}

	static PropInfo describeKV(PsiElement prop) {
		var separatorOffset = findSeparatorOffset(prop, ":");
		if (separatorOffset < 0)
			return null;
		
		var firstChild = prop.getFirstChild();
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

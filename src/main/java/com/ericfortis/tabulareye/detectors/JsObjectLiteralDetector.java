package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JsObjectLiteralDetector extends AlignmentDetector {
	JsObjectLiteralDetector() {
		super(JS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, JSObjectLiteralExpression.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(JSObjectLiteralExpression obj) {
		var block = new AlignmentBlock();
		for (var prop : obj.getProperties())
			if (prop != null && !prop.isShorthanded()) {
				var kv = describeKV(prop);
				if (kv != null)
					block.add(kv);
			}
		return block;
	}

	@Nullable
	static PropInfo describeKV(JSProperty prop) {
		var keyElement = prop.getIdentifyingElement();
		if (keyElement == null)
			return null;

		PsiElement colonElement = null;
		var child = keyElement.getNextSibling();
		while (child != null) {
			if (":".equals(child.getText())) {
				colonElement = child;
				break;
			}
			child = child.getNextSibling();
		}

		if (colonElement == null)
			return null;

		return new PropInfo(
			 keyElement.getText(),
			 keyElement.getTextRange().getStartOffset(),
			 colonElement.getTextRange().getStartOffset()
		);
	}
}

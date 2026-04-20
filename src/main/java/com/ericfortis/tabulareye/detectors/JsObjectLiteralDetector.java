package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

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
				var kv = describeKV(prop, JSProperty::getIdentifyingElement);
				if (kv != null)
					block.add(kv);
			}
		return block;
	}
}

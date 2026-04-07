package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptObjectTypeImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class TsInterfaceDetector extends AlignmentDetector {
	TsInterfaceDetector() {
		super(TS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, TypeScriptObjectTypeImpl.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(PsiElement tsInterface) {
		var block = new AlignmentBlock();
		for (var prop : tsInterface.getChildren()) {
			var kv = describeKV(prop);
			if (kv != null)
				block.add(kv);
		}
		return block;
	}

	static PropInfo describeKV(PsiElement prop) {
		var separatorOffset = findSeparatorOffset(prop, ":");
		if (separatorOffset < 0)
			return null;

		var keyBuilder = new StringBuilder();
		var firstChild = prop.getFirstChild();
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

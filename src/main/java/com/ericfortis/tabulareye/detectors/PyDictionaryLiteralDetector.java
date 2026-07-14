package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyDictLiteralExpression;
import com.jetbrains.python.psi.PyKeyValueExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PyDictionaryLiteralDetector extends AlignmentDetector {
	public PyDictionaryLiteralDetector() {
		super(PY_EXT);
	}

	@Override
	public String getDisplayName() {
		return "Python Dictionary";
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, PyDictLiteralExpression.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(PyDictLiteralExpression dict) {
		var block = new AlignmentBlock();
		for (var element : dict.getElements())
			if (element instanceof PyKeyValueExpression kv) {
				var key = kv.getKey();
				var separatorOffset = findSeparatorOffset(kv, ":");
				if (separatorOffset >= 0) {
					var keyText = key.getText().trim();
					if (!keyText.isEmpty()) {
						int startOffset = key.getTextRange().getStartOffset();
						block.add(new PropInfo(keyText, startOffset, separatorOffset));
					}
				}
			}
		return block;
	}
}

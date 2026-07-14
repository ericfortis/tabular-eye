package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyKeywordArgument;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PyKeywordArgsDetector extends AlignmentDetector {
	public PyKeywordArgsDetector() {
		super(PY_EXT);
	}

	@Override
	public String getDisplayName() {
		return "Python Keyword Args";
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, PyCallExpression.class, this::buildBlock);
	}

	// TODO it would be nice to add some more width to these spacers
	private AlignmentBlock buildBlock(PyCallExpression call) {
		var block = new AlignmentBlock();
		for (var arg : call.getArguments())
			if (arg instanceof PyKeywordArgument kw) {
				var key = kw.getKeyword();
				var separatorOffset = findSeparatorOffset(kw, "=");
				if (key != null && separatorOffset >= 0) {
					int startOffset = kw.getTextRange().getStartOffset();
					block.add(new PropInfo(key, startOffset, separatorOffset));
				}
			}
		return block;
	}
}

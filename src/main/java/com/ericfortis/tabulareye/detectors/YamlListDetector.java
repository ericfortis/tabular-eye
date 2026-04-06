package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLSequence;

import java.util.ArrayList;
import java.util.List;

public class YamlListDetector extends AlignmentDetector {
	YamlListDetector() {
		super(YML_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();

		for (var el : PsiTreeUtil.collectElementsOfType(file, YAMLSequence.class))
			for (var item : el.getItems()) {
				var hyphen = item.getFirstChild();
				if (hyphen == null || !"-".equals(hyphen.getText()))
					continue;

				var firstValue = item.getValue();
				if (firstValue == null)
					continue;

				int hyphenStart = hyphen.getTextRange().getStartOffset();
				int contentStart = firstValue.getTextRange().getStartOffset();

				if (doc.getLineNumber(hyphenStart) != doc.getLineNumber(contentStart))
					continue;

				var block = new AlignmentBlock();
				block.add(new PropInfo("-", hyphenStart, hyphenStart));
				block.add(new PropInfo(" ", -1, -1));
				blocks.add(block);
			}

		return blocks;
	}
}

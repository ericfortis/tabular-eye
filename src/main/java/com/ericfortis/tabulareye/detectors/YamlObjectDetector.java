package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.List;

// TODO maybe we can improve this by excluding the keylength of a nested obj

public class YamlObjectDetector extends AlignmentDetector {
	YamlObjectDetector() {
		super(YML_EXT);
	}

	@Override
	public String getDisplayName() {
		return "YAML Mapping";
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, YAMLMapping.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(YAMLMapping mapping) {
		var block = new AlignmentBlock();
		for (var kv : mapping.getKeyValues()) {
			var key = kv.getKey();
			if (key == null)
				continue;

			int colonOffset = findSeparatorOffset(kv, ":");
			if (colonOffset < 0)
				continue;

			var keyText = key.getText().trim();
			if (!keyText.isEmpty()) {
				int startOffset = key.getTextRange().getStartOffset();
				block.add(new PropInfo(keyText, startOffset, colonOffset));
			}
		}
		return block;
	}
}

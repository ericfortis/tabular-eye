package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.List;

// TODO maybe we can improve this by excluding the keylength of a nested obj

public class YamlObjectFinder extends AlignmentFinder {
	YamlObjectFinder() {
		super(YML_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, YAMLMapping.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(YAMLMapping mapping) {
		var block = new AlignmentBlock();
		for (var keyValue : mapping.getKeyValues()) {
			var key = keyValue.getKey();
			if (key == null)
				continue;

			int colonOffset = findSeparatorOffset(keyValue, ":");
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

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
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		return findGroups(file, doc, YAMLMapping.class, this::buildGroup);
	}

	private AlignmentGroup buildGroup(YAMLMapping mapping) {
		var group = new AlignmentGroup();

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
				group.add(new PropInfo(keyText, startOffset, colonOffset));
			}
		}

		return group.props().isEmpty() ? null : group;
	}
}

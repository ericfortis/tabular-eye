package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.ArrayList;
import java.util.List;

// TODO maybe we can improve this by excluding the keylength of a nested obj

public class YamlObjectFinder extends AlignmentFinder {

	@Override
	protected List<String> getExtensions() {
		return YML_EXT;
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var mapping : PsiTreeUtil.collectElementsOfType(file, YAMLMapping.class))
			if (isMultiline(mapping, doc)) {
				var group = buildGroup(mapping);
				if (group != null && group.isValid())
					groups.add(group);
			}

		return groups;
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

		return group.props().isEmpty()
			 ? null
			 : group;
	}
}

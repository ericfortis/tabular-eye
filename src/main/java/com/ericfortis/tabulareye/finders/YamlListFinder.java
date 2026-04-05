package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLSequence;

import java.util.ArrayList;
import java.util.List;

public class YamlListFinder extends AlignmentFinder {
	YamlListFinder() {
		super(YML_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var sequence : PsiTreeUtil.collectElementsOfType(file, YAMLSequence.class))
			for (var item : sequence.getItems()) {
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

				var group = new AlignmentGroup();
				group.add(new PropInfo("-", hyphenStart, hyphenStart));
				group.add(new PropInfo(" ", -1, -1));
				groups.add(group);
			}

		return groups;
	}
}

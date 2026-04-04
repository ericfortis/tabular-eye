package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TsInterfaceFinder extends AlignmentFinder {

	@Override
	public boolean isApplicable(@NotNull PsiFile file) {
		return isTs(file);
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var el : PsiTreeUtil.collectElements(file, el -> "TypeScriptObjectTypeImpl".equals(el.getClass().getSimpleName()))) {
			if (isMultiline(el, doc)) {
				var group = buildGroup(el);
				if (group != null && group.isValid())
					groups.add(group);
			}
		}

		return groups;
	}

	private AlignmentGroup buildGroup(PsiElement tsInterface) {
		var group = new AlignmentGroup();

		for (var member : tsInterface.getChildren()) {
			int colonOffset = findSeparatorOffset(member, ":");
			if (colonOffset < 0)
				continue;

			var keyBuilder = new StringBuilder();
			var child = member.getFirstChild();
			while (child != null && !":".equals(child.getText())) {
				keyBuilder.append(child.getText());
				child = child.getNextSibling();
			}

			var keyText = keyBuilder.toString().trim();
			if (!keyText.isEmpty()) {
				int startOffset = Objects.requireNonNull(member.getFirstChild()).getTextRange().getStartOffset();
				group.add(new PropInfo(keyText, startOffset, colonOffset));
			}
		}

		return group.props().isEmpty()
			 ? null
			 : group;
	}
}

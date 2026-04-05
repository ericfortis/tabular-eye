package com.ericfortis.tabulareye.finders;

import com.intellij.json.psi.JsonObject;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonAlignmentFinder extends AlignmentFinder {

	@Override
	protected List<String> getExtensions() {
		return JSON_EXT;
	}

	@Override
	@NotNull
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentGroup> groups = new ArrayList<>();

		for (var obj : PsiTreeUtil.collectElementsOfType(file, JsonObject.class))
			if (isMultiline(obj, doc)) {
				var group = buildGroup(obj);
				if (group != null && group.isValid())
					groups.add(group);
			}

		return groups;
	}

	private AlignmentGroup buildGroup(JsonObject obj) {
		var group = new AlignmentGroup();

		for (var prop : obj.getPropertyList()) {
			int colonOffset = findSeparatorOffset(prop, ":");
			if (colonOffset < 0)
				continue;

			var keyText = prop.getName();
			if (!keyText.isEmpty()) {
				int startOffset = Objects.requireNonNull(prop.getFirstChild()).getTextRange().getStartOffset();
				group.add(new PropInfo(keyText, startOffset, colonOffset));
			}
		}

		return group.props().isEmpty()
			 ? null
			 : group;
	}
}

package com.ericfortis.tabulareye.finders;

import com.intellij.json.psi.JsonObject;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class JsonAlignmentFinder extends AlignmentFinder {
	JsonAlignmentFinder() {
		super(JSON_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, JsonObject.class, this::buildGroup);
	}

	private AlignmentBlock buildGroup(JsonObject obj) {
		var group = new AlignmentBlock();
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
		return group;
	}
}

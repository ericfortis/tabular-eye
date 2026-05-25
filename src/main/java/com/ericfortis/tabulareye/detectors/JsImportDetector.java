package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.ecmascript6.psi.ES6ImportDeclaration;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// TODO Proportional Fonts. When at the left of the spacer (ie. import {foo}…) there are 
//  mixed bold and normal font-weights the alignment is not perfect (it's close enough to be good for now)

public class JsImportDetector extends AlignmentDetector {
	JsImportDetector() {
		super(JS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		boolean isHtml = isHtmlFile(file);

		var imports = PsiTreeUtil.collectElementsOfType(file, ES6ImportDeclaration.class);
		if (imports.isEmpty())
			return blocks;

		List<List<ES6ImportDeclaration>> groups = new ArrayList<>();
		List<ES6ImportDeclaration> currentGroup = new ArrayList<>();
		int prevEndLine = -2;

		for (var imp : imports) {
			if (isHtml && !isInScriptTag(imp))
				continue;
			if (isMultiline(imp, doc))
				continue;

			int startLine = doc.getLineNumber(imp.getTextRange().getStartOffset());
			if (currentGroup.isEmpty() || startLine == prevEndLine + 1) {
				currentGroup.add(imp);
			} else {
				if (currentGroup.size() > 1)
					groups.add(currentGroup);
				currentGroup = new ArrayList<>();
				currentGroup.add(imp);
			}
			prevEndLine = doc.getLineNumber(imp.getTextRange().getEndOffset() - 1);
		}
		if (currentGroup.size() > 1)
			groups.add(currentGroup);

		for (var group : groups) {
			var block = new AlignmentBlock();
			for (var imp : group) {
				var prop = buildProp(imp);
				if (prop != null)
					block.add(prop);
			}
			if (block.isValid())
				blocks.add(block);
		}

		return blocks;
	}

	private PropInfo buildProp(ES6ImportDeclaration imp) {
		int keyStart = imp.getTextRange().getStartOffset();
		String text = imp.getText();
		int fromIdx = text.lastIndexOf(" from ");
		if (fromIdx < 0)
			return null;

		int separatorOffset = keyStart + fromIdx;
		String key = text.substring(0, fromIdx);

		return new PropInfo(key, keyStart, separatorOffset);
	}
}

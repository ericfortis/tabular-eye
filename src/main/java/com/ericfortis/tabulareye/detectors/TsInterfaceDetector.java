package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptObjectType;
import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptObjectTypeImpl;
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TsInterfaceDetector extends AlignmentDetector {
	TsInterfaceDetector() {
		super(TS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, TypeScriptObjectTypeImpl.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(TypeScriptObjectType tsInterface) {
		var block = new AlignmentBlock();
		for (var prop : tsInterface.getTypeMembers()) {
			var kv = describeKV(prop);
			if (kv != null)
				block.add(kv);
		}
		return block;
	}

	@Nullable
	static PropInfo describeKV(PsiElement prop) {
		PsiElement keyElement = null;
		PsiElement colonElement = null;

		var child = prop.getFirstChild();
		while (child != null) {
			var node = child.getNode();
			if (node != null) {
				var typeName = node.getElementType().toString();
				if ("JS:IDENTIFIER".equals(typeName) || "JS:STRING_LITERAL".equals(typeName)) {
					keyElement = child;
				} else if ("JS:COLON".equals(typeName)) {
					colonElement = child;
				}
			}
			child = child.getNextSibling();
		}

		if (keyElement == null || colonElement == null)
			return null;

		return new PropInfo(
			 keyElement.getText(),
			 keyElement.getTextRange().getStartOffset(),
			 colonElement.getTextRange().getStartOffset()
		);
	}
}

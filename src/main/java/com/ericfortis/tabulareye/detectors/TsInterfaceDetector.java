package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptObjectType;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptPropertySignature;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeMember;
import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptObjectTypeImpl;
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
	static PropInfo describeKV(TypeScriptTypeMember prop) {
		if (!(prop instanceof TypeScriptPropertySignature))
			return null;

		var keyElem = ((TypeScriptPropertySignature) prop).getIdentifyingElement();
		if (keyElem == null)
			return null;

		PsiElement colonElem = null;
		var child = keyElem.getNextSibling();
		while (child != null) {
			if (":".equals(child.getText())) {
				colonElem = child;
				break;
			}
			child = child.getNextSibling();
		}

		if (colonElem == null)
			return null;

		return new PropInfo(
			 keyElem.getText(),
			 keyElem.getTextRange().getStartOffset(),
			 colonElem.getTextRange().getStartOffset()
		);
	}
}

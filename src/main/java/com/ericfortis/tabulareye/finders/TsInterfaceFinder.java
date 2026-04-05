package com.ericfortis.tabulareye.finders;

import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptObjectTypeImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TsInterfaceFinder extends AlignmentFinder {
	TsInterfaceFinder() {
		super(TS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, TypeScriptObjectTypeImpl.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(PsiElement tsInterface) {
		var block = new AlignmentBlock();
		for (var prop : tsInterface.getChildren()) {
			var kv = JsObjectLiteralFinder.describeKV(prop);
			if (kv != null)
				block.add(kv);
		}
		return block;
	}
}

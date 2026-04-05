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
	public List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document doc) {
		return findGroups(file, doc, TypeScriptObjectTypeImpl.class, this::buildGroup);
	}

	private AlignmentGroup buildGroup(PsiElement tsInterface) {
		var group = new AlignmentGroup();
		for (var prop : tsInterface.getChildren()) {
			var kv = JsObjectLiteralFinder.describeKV(prop);
			if (kv != null)
				group.add(kv);
		}
		return group.props().isEmpty() ? null : group;
	}
}

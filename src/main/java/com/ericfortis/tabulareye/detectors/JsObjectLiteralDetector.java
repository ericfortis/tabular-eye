package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.ecmascript6.psi.ES6ComputedName;
import com.intellij.lang.javascript.psi.JSFunctionProperty;
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class JsObjectLiteralDetector extends AlignmentDetector {
	JsObjectLiteralDetector() {
		super(JS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		for (var el : PsiTreeUtil.collectElementsOfType(file, JSObjectLiteralExpression.class))
			if (isMultiline(el, doc))
				blocks.addAll(buildBlocks(el, doc));
		return blocks;
	}

	private List<AlignmentBlock> buildBlocks(JSObjectLiteralExpression obj, Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		var block = new AlignmentBlock();
		JSProperty prevProp = null;
		for (var prop : obj.getProperties())
			if (prop != null && !prop.isShorthanded()) {
				var kv = prop instanceof JSFunctionProperty functionProperty
					 ? describeFunctionProperty(functionProperty)
					 : describeKV(prop, JsObjectLiteralDetector::getKeyElement);
				if (kv != null) {
					if (prevProp != null && hasBlankLineBetween(prevProp, prop, doc)) {
						if (block.isValid())
							blocks.add(block);
						block = new AlignmentBlock();
					}
					block.add(kv);
					prevProp = prop;
				}
			}
		if (block.isValid())
			blocks.add(block);
		return blocks;
	}

	private static PropInfo describeFunctionProperty(JSFunctionProperty prop) {
		var keyElem = getKeyElement(prop);
		var parameterList = prop.getParameterList();
		if (keyElem == null || parameterList == null)
			return null;

		return new PropInfo(
			 keyElem.getText(),
			 keyElem.getTextRange().getStartOffset(),
			 parameterList.getTextRange().getEndOffset() - 1
		);
	}

	private static PsiElement getKeyElement(JSProperty prop) {
		var computed = PsiTreeUtil.findChildOfType(prop, ES6ComputedName.class);
		return computed != null
			 ? computed
			 : prop.getIdentifyingElement();
	}

	private static boolean hasBlankLineBetween(JSProperty prev, JSProperty current, Document doc) {
		int prevEndLine = doc.getLineNumber(prev.getTextRange().getEndOffset());
		int currentStartLine = doc.getLineNumber(current.getTextRange().getStartOffset());
		return currentStartLine - prevEndLine > 1;
	}
}

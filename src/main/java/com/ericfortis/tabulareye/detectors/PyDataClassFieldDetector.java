package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyTargetExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PyDataClassFieldDetector extends AlignmentDetector {
	public PyDataClassFieldDetector() {
		super(PY_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		for (var pyClass : PsiTreeUtil.collectElementsOfType(file, PyClass.class))
			if (isDataClass(pyClass) && isMultiline(pyClass, doc)) {
				var block = buildBlock(pyClass);
				if (block.isValid())
					blocks.add(block);
			}
		return blocks;
	}

	private boolean isDataClass(PyClass pyClass) {
		var decoratorList = pyClass.getDecoratorList();
		if (decoratorList == null) return false;
		for (var decorator : decoratorList.getDecorators()) {
			String name = decorator.getName();
			if ("dataclass".equals(name) || (name != null && name.endsWith(".dataclass")))
				return true;
		}
		return false;
	}

	private AlignmentBlock buildBlock(PyClass pyClass) {
		var block = new AlignmentBlock();
		for (var statement : pyClass.getStatementList().getStatements())
			for (var target : PsiTreeUtil.findChildrenOfType(statement, PyTargetExpression.class)) {
				var kv = describeTarget(target);
				if (kv != null)
					block.add(kv);
			}
		return block;
	}

	private PropInfo describeTarget(PyTargetExpression target) {
		var separatorOffset = findSeparatorOffset(target, ":");
		if (separatorOffset < 0) 
			separatorOffset = findSeparatorOffset(target.getParent(), ":");

		if (separatorOffset < 0)
			return null;

		String key = target.getName();
		if (key == null || key.isEmpty())
			return null;

		int startOffset = target.getTextRange().getStartOffset();
		return new PropInfo(key, startOffset, separatorOffset);
	}
}

package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyAssignmentStatement;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyTargetExpression;
import com.jetbrains.python.psi.PyTypeDeclarationStatement;
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
		for (var pyClass : PsiTreeUtil.collectElementsOfType(file, PyClass.class)) {
			if (isDataClass(pyClass)) {
				var statementList = pyClass.getStatementList();
				if (isMultiline(statementList, doc)) {
					var block = buildBlock(pyClass);
					if (block.isValid())
						blocks.add(block);
				}
			}
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
		for (var statement : pyClass.getStatementList().getStatements()) {
			PyTargetExpression target = null;
			PsiElement annotation = null;

			if (statement instanceof PyTargetExpression t)
				target = t;
			else if (statement instanceof PyTypeDeclarationStatement typeDecl) {
				if (typeDecl.getTarget() instanceof PyTargetExpression t) {
					target = t;
					annotation = typeDecl.getAnnotation();
				}
			} else if (statement instanceof PyAssignmentStatement assignment) {
				var targets = assignment.getTargets();
				if (targets.length == 1 && targets[0] instanceof PyTargetExpression t) {
					target = t;
					annotation = t.getAnnotation();
				}
			}

			if (target != null) {
				var kv = describeTarget(target, annotation);
				if (kv != null)
					block.add(kv);
			}
		}
		return block;
	}

	private PropInfo describeTarget(PyTargetExpression target, PsiElement annotation) {
		int separatorOffset = -1;
		if (annotation != null)
			separatorOffset = findSeparatorOffset(annotation, ":");
		if (separatorOffset < 0)
			separatorOffset = findSeparatorOffset(target, ":");
		if (separatorOffset < 0) { // Try to find colon in the statement children if it's not in target/annotation
			var parent = target.getParent();
			if (parent != null)
				separatorOffset = findSeparatorOffset(parent, ":");
		}

		if (separatorOffset < 0)
			return null;

		var key = target.getName();
		if (key == null || key.isEmpty())
			return null;

		int startOffset = target.getTextRange().getStartOffset();
		return new PropInfo(key, startOffset, separatorOffset);
	}
}

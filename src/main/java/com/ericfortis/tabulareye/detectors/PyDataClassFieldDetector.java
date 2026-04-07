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

		// 1. Try standard Python PSI
		for (var pyClass : PsiTreeUtil.collectElementsOfType(file, PyClass.class)) {
			if (isDataClass(pyClass) && isMultiline(pyClass, doc)) {
				var block = buildBlock(pyClass);
				if (block.isValid())
					blocks.add(block);
			}
		}

		if (!blocks.isEmpty()) return blocks;

		// 2. Fallback: Text-based detection for when PSI is not fully recognized (e.g. in tests)
		String text = file.getText();
		if (text.contains("@dataclass") || text.contains("@dataclasses.dataclass")) {
			// Look for class definitions
			int classIdx = text.indexOf("class ");
			while (classIdx != -1) {
				// Find end of class or next class
				int nextClassIdx = text.indexOf("\nclass ", classIdx + 1);
				if (nextClassIdx == -1) nextClassIdx = text.length();

				String classText = text.substring(classIdx, nextClassIdx);
				// A class is a dataclass if it has the decorator before it
				// For simplicity, check if the previous lines contain the decorator
				int prevLinesIdx = Math.max(0, text.lastIndexOf("\n\n", classIdx));
				String context = text.substring(prevLinesIdx, classIdx);

				if (context.contains("@dataclass") || context.contains("@dataclasses.dataclass")) {
					var block = new AlignmentBlock();
					String[] lines = classText.split("\n");
					int currentOffset = classIdx;
					for (String line : lines) {
						if (line.contains(":") && !line.trim().startsWith("class") && !line.trim().startsWith("def")) {
							int colonIdx = line.indexOf(":");
							// Ensure it's a field (no space before colon, or just one space)
							String beforeColon = line.substring(0, colonIdx).trim();
							if (!beforeColon.isEmpty() && !beforeColon.contains(" ") && !beforeColon.contains("(")) {
								block.add(new PropInfo(beforeColon, currentOffset + line.indexOf(beforeColon), currentOffset + colonIdx));
							}
						}
						currentOffset += line.length() + 1;
					}
					if (block.isValid()) blocks.add(block);
				}

				classIdx = text.indexOf("class ", classIdx + 1);
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
			// Find all target expressions in the statement
			for (var target : PsiTreeUtil.findChildrenOfType(statement, PyTargetExpression.class)) {
				var kv = describeTarget(target);
				if (kv != null)
					block.add(kv);
			}
		}
		return block;
	}

	private PropInfo describeTarget(PyTargetExpression target) {
		// Try to find ":" in target's children, or siblings in parent
		var separatorOffset = findSeparatorOffset(target, ":");
		if (separatorOffset < 0) {
			separatorOffset = findSeparatorOffset(target.getParent(), ":");
		}

		if (separatorOffset < 0)
			return null;

		String key = target.getName();
		if (key == null || key.isEmpty())
			return null;

		int startOffset = target.getTextRange().getStartOffset();
		return new PropInfo(key, startOffset, separatorOffset);
	}
}

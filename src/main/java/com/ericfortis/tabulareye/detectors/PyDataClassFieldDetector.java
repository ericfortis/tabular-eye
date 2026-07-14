package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PyDataClassFieldDetector extends AlignmentDetector {
	public PyDataClassFieldDetector() {
		super(PY_EXT);
	}

	@Override
	public String getDisplayName() {
		return "Python Dataclass";
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		for (var pyClass : PsiTreeUtil.collectElementsOfType(file, PyClass.class))
			if (isDataClass(pyClass) && isMultiline(pyClass.getStatementList(), doc))
				blocks.add(buildBlock(pyClass));
		return blocks;
	}

	private boolean isDataClass(PyClass pyClass) {
		var dl = pyClass.getDecoratorList();
		if (dl != null)
			for (var d : dl.getDecorators())
				if ("dataclass".equals(d.getName()))
					return true;
		return false;
	}

	private AlignmentBlock buildBlock(PyClass pyClass) {
		var block = new AlignmentBlock();
		for (var attr : pyClass.getClassAttributes()) {
			var text = attr.getText();
			var offset = attr.getTextOffset();
			var separatorOffset = text.length() + offset + 1;
			block.add(new PropInfo(text, offset, separatorOffset));
		}
		return block;
	}
}

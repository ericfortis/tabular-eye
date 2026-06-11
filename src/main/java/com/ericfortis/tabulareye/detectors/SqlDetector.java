package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.sql.psi.SqlColumnDefinition;
import com.intellij.sql.psi.SqlTableDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlDetector extends AlignmentDetector {
	SqlDetector() {
		super(SQL_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		return findBlocks(file, doc, SqlTableDefinition.class, this::buildBlock);
	}

	private AlignmentBlock buildBlock(SqlTableDefinition tableDef) {
		var block = new AlignmentBlock();
		for (var col : PsiTreeUtil.findChildrenOfType(tableDef, SqlColumnDefinition.class)) {
			var nameIdent = col.getNameIdentifier();
			if (nameIdent == null)
				continue;

			var name = nameIdent.getText();
			if (name == null || name.isEmpty())
				continue;

			block.add(new PropInfo(
				 name,
				 nameIdent.getTextRange().getStartOffset(),
				 nameIdent.getTextRange().getEndOffset()
			));
		}
		return block;
	}
}

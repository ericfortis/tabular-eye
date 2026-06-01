package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.javascript.psi.JSVarStatement;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsVarsDetector extends AlignmentDetector {
	JsVarsDetector() {
		super(JS_EXT);
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		var statements = PsiTreeUtil.collectElementsOfType(file, JSVarStatement.class);
		if (statements.isEmpty())
			return blocks;

		var consts = findCandidates(statements, isHtmlFile(file), doc);
		if (consts.isEmpty())
			return blocks;

		List<List<JSVarStatement>> groups = new ArrayList<>();
		List<JSVarStatement> currentGroup = new ArrayList<>();
		int prevEndLine = -2;

		for (var stmt : consts) {
			int startLine = doc.getLineNumber(stmt.getTextRange().getStartOffset());
			if (currentGroup.isEmpty() || startLine == prevEndLine + 1)
				currentGroup.add(stmt);
			else {
				if (currentGroup.size() > 1)
					groups.add(currentGroup);
				currentGroup = new ArrayList<>();
				currentGroup.add(stmt);
			}
			prevEndLine = doc.getLineNumber(stmt.getTextRange().getEndOffset() - 1);
		}
		if (currentGroup.size() > 1)
			groups.add(currentGroup);

		for (var group : groups) {
			var block = new AlignmentBlock();
			for (var stmt : group) {
				var prop = buildProp(stmt);
				if (prop != null)
					block.add(prop);
			}
			if (block.isValid())
				blocks.add(block);
		}

		return blocks;
	}

	private static List<JSVarStatement> findCandidates(
		 Collection<JSVarStatement> statements,
		 boolean isHtml,
		 Document doc
	) {
		List<JSVarStatement> vars = new ArrayList<>();
		for (var stmt : statements) {
			if (isHtml && !isInScriptTag(stmt))
				continue;
			if (isMultiline(stmt, doc))
				continue;
			if (isDestructuring(stmt))
				continue;
			vars.add(stmt);
		}
		return vars;
	}

	private static boolean isDestructuring(JSVarStatement stmt) {
		var variable = PsiTreeUtil.findChildOfType(stmt, JSVariable.class);
		if (variable == null)
			return true;
		var nameId = variable.getNameIdentifier();
		if (nameId == null)
			return true;
		var name = nameId.getText();
		return name.isEmpty() || name.startsWith("{") || name.startsWith("[");
	}

	private PropInfo buildProp(JSVarStatement stmt) {
		var variable = PsiTreeUtil.findChildOfType(stmt, JSVariable.class);
		if (variable == null)
			return null;

		var separatorOffset = findSeparatorOffset(variable, "=");
		if (separatorOffset < 0)
			return null;

		var keyStart = stmt.getTextRange().getStartOffset();
		var key = stmt.getText().substring(0, separatorOffset - keyStart);
		return new PropInfo(key, keyStart, separatorOffset);
	}
}

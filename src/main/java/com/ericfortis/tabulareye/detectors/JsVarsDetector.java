package com.ericfortis.tabulareye.detectors;

import com.intellij.lang.javascript.psi.JSLoopStatement;
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
	public String getDisplayName() {
		return "JS Variables";
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		var vStatements = PsiTreeUtil.collectElementsOfType(file, JSVarStatement.class);
		if (vStatements.isEmpty())
			return new ArrayList<>();

		var candidates = findCandidates(vStatements, doc, isHtmlFile(file));
		if (candidates.isEmpty())
			return new ArrayList<>();

		return alignmentBlocksFor(doc, candidates);
	}

	private static List<JSVarStatement> findCandidates(Collection<JSVarStatement> statements, Document doc, boolean isHtml) {
		List<JSVarStatement> candidates = new ArrayList<>();
		for (var stmt : statements) {
			if (isHtml && !isInScriptTag(stmt))
				continue;
			if (isMultiline(stmt, doc))
				continue;
			if (stmt.getParent() instanceof JSLoopStatement)
				continue;
			candidates.add(stmt);
		}
		return candidates;
	}

	private @NotNull List<AlignmentBlock> alignmentBlocksFor(@NotNull Document doc, List<JSVarStatement> candidates) {
		var groups = groupContiguous(doc, candidates);
		List<AlignmentBlock> blocks = new ArrayList<>();
		for (var g : groups) {
			var block = new AlignmentBlock();
			for (var stmt : g) {
				var prop = buildProp(stmt);
				if (prop != null)
					block.add(prop);
			}
			if (block.isValid())
				blocks.add(block);
		}
		return blocks;
	}

	private static @NotNull List<List<JSVarStatement>> groupContiguous(@NotNull Document doc, List<JSVarStatement> candidates) {
		List<List<JSVarStatement>> groups = new ArrayList<>();
		List<JSVarStatement> currentGroup = new ArrayList<>();
		int prevEndLine = -2;
		for (var c : candidates) {
			int startLine = doc.getLineNumber(c.getTextRange().getStartOffset());
			if (currentGroup.isEmpty() || startLine == prevEndLine + 1)
				currentGroup.add(c);
			else {
				if (currentGroup.size() > 1)
					groups.add(currentGroup);
				currentGroup = new ArrayList<>();
				currentGroup.add(c);
			}
			prevEndLine = doc.getLineNumber(c.getTextRange().getEndOffset() - 1);
		}
		if (currentGroup.size() > 1)
			groups.add(currentGroup);
		return groups;
	}

	private PropInfo buildProp(JSVarStatement stmt) {
		var variable = PsiTreeUtil.findChildOfType(stmt, JSVariable.class);
		if (variable == null) // e.g., JsDestructuringElement
			return null;

		var sepOffset = findSeparatorOffset(variable, "=");
		if (sepOffset == -1)
			return null;

		var keyStart = stmt.getTextRange().getStartOffset();
		var key = stmt.getText().substring(0, sepOffset - keyStart);
		return new PropInfo(key, keyStart, sepOffset);
	}
}

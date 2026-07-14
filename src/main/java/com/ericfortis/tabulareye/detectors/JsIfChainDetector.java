package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class JsIfChainDetector extends AlignmentDetector {
	private static final Pattern LINE_PATTERN = Pattern.compile(
		 "^(\\s*)(if|else\\s+if|else(?=\\s|\\(|$))"
	);

	JsIfChainDetector() {
		super(JS_EXT);
	}

	@Override
	public String getDisplayName() {
		return "JS If Chain";
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		int lineCount = doc.getLineCount();

		List<LineInfo> chain = new ArrayList<>();

		for (int i = 0; i < lineCount; i++) {
			int lineStart = doc.getLineStartOffset(i);
			int lineEnd = doc.getLineEndOffset(i);
			String text = doc.getText(new TextRange(lineStart, lineEnd));
			while (!text.isEmpty() && (text.charAt(text.length() - 1) == '\n'
				 || text.charAt(text.length() - 1) == '\r'))
				text = text.substring(0, text.length() - 1);

			if (text.trim().endsWith("{")) {
				flushChain(chain, blocks);
				continue;
			}

			var m = LINE_PATTERN.matcher(text);
			if (!m.find()) {
				flushChain(chain, blocks);
				continue;
			}

			String keyword = m.group(2);
			int keywordStart = lineStart + m.start(2);
			int keywordEnd = lineStart + m.end(2) - 1;
			int pos = m.end(2);

			// skip whitespace after keyword
			while (pos < text.length() && text.charAt(pos) == ' ') pos++;

			// check for condition — a '(' that may contain nested parens and strings
			boolean hasCondition = false;
			if (pos < text.length() && text.charAt(pos) == '(') {
				int depth = 1;
				pos++;
				while (pos < text.length() && depth > 0) {
					char c = text.charAt(pos);
					if (c == '\'' || c == '"' || c == '`') {
						pos = skipStringLiteral(text, pos, c);
						if (pos >= text.length()) break;
					} else if (c == '(') {
						depth++;
					} else if (c == ')') {
						depth--;
					}
					if (depth > 0) pos++;
				}
				if (depth == 0) {
					hasCondition = true;
					pos++;
				}
			}

			// skip whitespace after condition
			while (pos < text.length() && text.charAt(pos) == ' ') pos++;

			if (pos >= text.length()) {
				flushChain(chain, blocks);
				continue;
			}

			String prefix = text.substring(0, pos);
			int bodyStart = lineStart + pos;
			String body = text.substring(pos);
			if (body.trim().isEmpty()) {
				flushChain(chain, blocks);
				continue;
			}

			chain.add(new LineInfo(
				 lineStart, bodyStart, prefix,
				 keyword, keywordStart, keywordEnd, hasCondition
			));
		}
		flushChain(chain, blocks);

		return blocks;
	}

	private static int skipStringLiteral(String text, int pos, char quote) {
		pos++;
		while (pos < text.length()) {
			char c = text.charAt(pos);
			if (c == '\\') {
				pos += 2;
				continue;
			}
			if (c == quote) return pos;
			pos++;
		}
		return pos;
	}

	private void flushChain(List<LineInfo> chain, List<AlignmentBlock> blocks) {
		if (chain.size() > 1) {
			var kwBlock = new AlignmentBlock();
			var bodyBlock = new AlignmentBlock();

			for (var info : chain) {
				String bodyKey = info.prefix;
				if (info.hasCondition && "if".equals(info.keyword))
					bodyKey = "else " + bodyKey;
				bodyBlock.add(new PropInfo(bodyKey, info.lineStart, info.bodyStart - 1));

				if (info.hasCondition) {
					kwBlock.add(new PropInfo(info.keyword, info.keywordStart, info.keywordStart + 1));
				}
			}

			if (kwBlock.isValid())
				blocks.add(kwBlock);
			if (bodyBlock.isValid())
				blocks.add(bodyBlock);
		}
		chain.clear();
	}

	private record LineInfo(
		 int lineStart,
		 int bodyStart,
		 String prefix,
		 String keyword,
		 int keywordStart,
		 int keywordEnd,
		 boolean hasCondition
	) {
	}
}

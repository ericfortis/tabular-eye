package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects code that was manually tabularized using repeated spaces/tabs,
 * e.g. C or Go source files. A block is a continuous run of lines that
 * each contain a run of 2+ spaces/tabs. The key is everything on the left
 * up to the last such run.
 * <p>
 * Note: this ignores that a line could already have multiple aligned
 * columns; only the first run of whitespace is considered.
 */
public class WhitespaceAlignmentDetector extends AlignmentDetector {
	private static final Pattern WHITESPACE_RUN = Pattern.compile("[ \t]{2,}");

	WhitespaceAlignmentDetector() {
		super(C_LIKE_EXT);
	}

	@Override
	public String getDisplayName() {
		return "Whitespace";
	}

	@Override
	@NotNull
	public List<AlignmentBlock> findBlocks(@NotNull PsiFile file, @NotNull Document doc) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		var current = new AlignmentBlock();
		var lineCount = doc.getLineCount();
		var text = doc.getText();

		for (int i = 0; i < lineCount; i++) {
			var start = doc.getLineStartOffset(i);
			var end = doc.getLineEndOffset(i);
			var line = text.substring(start, end);

			var prop = describeLine(line, start);
			if (prop == null) {
				if (current.isValid())
					blocks.add(current);
				current = new AlignmentBlock();
				continue;
			}
			current.add(prop);
		}

		if (current.isValid())
			blocks.add(current);

		return blocks;
	}

	private PropInfo describeLine(String line, int lineStart) {
		if (line.isBlank())
			return null;

		if (line.endsWith("\\"))
			return null;

		var matcher = WHITESPACE_RUN.matcher(line);
		int matchStart = -1;
		int matchEnd = -1;
		while (matcher.find()) {
			if (!line.substring(0, matcher.start()).stripLeading().isEmpty()) {
				matchStart = matcher.start();
				matchEnd = matcher.end();
				break;
			}
		}

		if (matchStart == -1)
			return null;

		var keyRaw = line.substring(0, matchStart);
		var trimmedKey = keyRaw.stripLeading();
		if (trimmedKey.isEmpty())
			return null;

		// The key includes the trailing whitespace run, so the spacer
		// gets inserted right after it ends, not in the middle of it.
		var key = line.substring(0, matchEnd).stripLeading();
		var leadingWsLen = keyRaw.length() - trimmedKey.length();
		var keyOffset = lineStart + leadingWsLen;
		var separatorOffset = lineStart + matchEnd - 1;
		return new PropInfo(key, keyOffset, separatorOffset);
	}
}

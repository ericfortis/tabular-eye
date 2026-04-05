package com.ericfortis.tabulareye.finders;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Visitor for finding the column spacing needed for tabularizing.
 */
public abstract class AlignmentFinder {
	final List<String> extensions;

	AlignmentFinder(List<String> extensions) {
		this.extensions = extensions;
	}

	static final List<String> JS_EXT = List.of("js", "jsx", "ts", "tsx");
	static final List<String> TS_EXT = List.of("ts", "tsx");
	static final List<String> CSS_EXT = List.of("css");
	static final List<String> YML_EXT = List.of("yml", "yaml");
	static final List<String> JSON_EXT = List.of("json");

	public final boolean isApplicable(@NotNull PsiFile file) {
		return extensions.contains(file.getVirtualFile().getExtension());
	}

	static boolean isMultiline(PsiElement elem, Document doc) {
		int startLine = doc.getLineNumber(elem.getTextRange().getStartOffset());
		int endLine = doc.getLineNumber(elem.getTextRange().getEndOffset());
		return endLine > startLine;
	}

	static int findSeparatorOffset(PsiElement elem, String separator) {
		var child = elem.getFirstChild();
		while (child != null) {
			if (separator.equals(child.getText()))
				return child.getTextRange().getStartOffset();
			child = child.getNextSibling();
		}
		return -1;
	}

	@NotNull
	public abstract List<AlignmentGroup> findGroups(@NotNull PsiFile file, @NotNull Document document);

	public static class AlignmentGroup {
		private final List<PropInfo> props = new ArrayList<>();

		public int getStartOffset() {
			return props.isEmpty()
				 ? -1
				 : props.getFirst().keyStartOffset();
		}

		public int getEndOffset() {
			return props.isEmpty()
				 ? -1
				 : props.getLast().separatorOffset();
		}

		public void add(PropInfo p) {
			props.add(p);
		}

		public boolean isValid() {
			return props.size() > 1;
		}

		public List<PropInfo> props() {
			return Collections.unmodifiableList(props);
		}
	}

	public record PropInfo(String keyText, int keyStartOffset, int separatorOffset) {
	}
}

package com.ericfortis.tabulareye.detectors;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Visitor for finding the column spacing needed for tabularizing.
 */
public abstract class AlignmentDetector {
	final List<String> extensions;

	AlignmentDetector(List<String> extensions) {
		this.extensions = extensions;
	}

	static final List<String> JS_EXT = List.of("js", "jsx", "ts", "tsx", "html");
	static final List<String> TS_EXT = List.of("ts", "tsx");
	static final List<String> PY_EXT = List.of("py");
	static final List<String> CSS_EXT = List.of("css", "html");
	static final List<String> YML_EXT = List.of("yml", "yaml");
	static final List<String> JSON_EXT = List.of("json");
	static final List<String> SQL_EXT = List.of("sql");

	public boolean isApplicable(@NotNull PsiFile file) {
		var vFile = file.getVirtualFile();
		if (vFile == null) return false;
		var ext = vFile.getExtension();
		return ext != null && extensions.contains(ext);
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

	static boolean isInHtmlTag(PsiElement elem, String tagName) {
		var tag = PsiTreeUtil.getParentOfType(elem, XmlTag.class);
		return tag != null && tagName.equalsIgnoreCase(tag.getName());
	}

	static boolean isInStyleTag(PsiElement elem) {
		return isInHtmlTag(elem, "style");
	}

	static boolean isInScriptTag(PsiElement elem) {
		return isInHtmlTag(elem, "script");
	}

	protected static boolean isHtmlFile(PsiFile file) {
		var vFile = file.getVirtualFile();
		return vFile != null && "html".equals(vFile.getExtension());
	}

	@Nullable
	static <T> PropInfo describeKV(T prop, Function<T, PsiElement> keyExtractor) {
		var keyElem = keyExtractor.apply(prop);
		if (keyElem == null)
			return null;

		PsiElement colonElem = null;
		var child = keyElem.getNextSibling();
		while (child != null) {
			if (":".equals(child.getText())) {
				colonElem = child;
				break;
			}
			child = child.getNextSibling();
		}

		if (colonElem == null)
			return null;

		return new PropInfo(
				keyElem.getText(),
				keyElem.getTextRange().getStartOffset(),
				colonElem.getTextRange().getStartOffset()
		);
	}


	@NotNull
	public abstract List<AlignmentBlock> findBlocks(
		 @NotNull PsiFile file,
		 @NotNull Document document
	);

	@NotNull
	protected <T extends PsiElement> List<AlignmentBlock> findBlocks(
		 @NotNull PsiFile file,
		 @NotNull Document doc,
		 @NotNull Class<T> clazz,
		 @NotNull Function<T, AlignmentBlock> builder
	) {
		List<AlignmentBlock> blocks = new ArrayList<>();
		for (var el : PsiTreeUtil.collectElementsOfType(file, clazz))
			if (isMultiline(el, doc)) {
				var block = builder.apply(el);
				if (block != null && block.isValid())
					blocks.add(block);
			}
		return blocks;
	}


	public static class AlignmentBlock {
		private final List<PropInfo> props = new ArrayList<>();

		public PropInfo get(int index) {
			return props.get(index);
		}

		public int size() {
			return props.size();
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

	public static class PropInfo {
		private final String key;
		private final int keyOffset;
		private final int separatorOffset;
		private int keyWidth;

		public PropInfo(String key, int keyOffset, int separatorOffset) {
			this.key = key;
			this.keyOffset = keyOffset;
			this.separatorOffset = separatorOffset;
		}

		public String key() {
			return key;
		}

		public int keyOffset() {
			return keyOffset;
		}

		public int separatorOffset() {
			return separatorOffset;
		}

		public int keyWidth() {
			return keyWidth;
		}

		public void setKeyWidth(int width) {
			this.keyWidth = width;
		}
	}
}

package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector;
import com.ericfortis.tabulareye.detectors.AlignmentDetector.AlignmentBlock;
import com.ericfortis.tabulareye.detectors.AlignmentDetector.PropInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Spacers {
	/**
	 * A fully transparent inlay that occupies exactly {@code widthPx} pixels.
	 * Note: antialiasing (user setting) greyscale makes columns not align 100% perfectly.
	 */
	private record Spacer(int widthPx) implements EditorCustomElementRenderer {
		@Override
		public int calcWidthInPixels(@NotNull Inlay inlay) {
			return widthPx;
		}

		@Override
		public int calcHeightInPixels(@NotNull Inlay inlay) {
			return 1;
		}
	}

	private boolean isRefreshing = false;
	private final Editor editor;
	private final List<Inlay<Spacer>> activeInlays = new ArrayList<>();

	// This is mainly because we want to handle non-monospace fonts.
	// i.e., we could later implement an optimized path for mono (I don't use mono, so…)
	private final Map<Integer, FontMetrics> fontMetricsCache = new HashMap<>();

	public Spacers(Editor editor) {
		this.editor = editor;
	}

	public void refresh(List<AlignmentBlock> blocks) {
		if (isRefreshing || editor.isDisposed())
			return;
		isRefreshing = true;
		try {
			clearAll();
			for (var b : blocks)
				render(b);
		} finally {
			isRefreshing = false;
		}
	}

	public void clearAll() {
		for (var inlay : activeInlays)
			if (inlay.isValid())
				Disposer.dispose(inlay);
		activeInlays.clear();
	}

	public List<AlignmentBlock> calcAlignments(List<AlignmentDetector> detectors, PsiFile psiFile, Document doc) {
		List<AlignmentBlock> allBlocks = new ArrayList<>();
		for (var d : detectors) {
			var blocks = d.findBlocks(psiFile, doc);
			for (var b : blocks)
				for (var prop : b.props())
					setKeyWidth(prop);

			if (!blocks.isEmpty())
				allBlocks.addAll(blocks);
		}
		return allBlocks;
	}

	// Handles proportional fonts. 
	// TODO create a fast path for monospace fonts. With a cache of a 1-char width.
	private void setKeyWidth(PropInfo prop) {
		var fm = getFontMetrics(prop.keyOffset());
		if (fm != null)
			prop.setKeyWidth(fm.stringWidth(prop.key()));
	}

	private void render(AlignmentBlock block) {
		var props = block.props();

		int maxWidth = 0;
		for (var p : props)
			maxWidth = Math.max(maxWidth, p.keyWidth());

		// Insert spacers
		var model = editor.getInlayModel();
		for (var p : props) {
			int spacerWidth = maxWidth - p.keyWidth();
			if (spacerWidth > 0) { // skips longest
				int placeAt = p.separatorOffset() + 1;
				var inlay = model.addInlineElement(placeAt, true, new Spacer(spacerWidth));
				if (inlay != null)
					activeInlays.add(inlay);
			}
		}
	}

	public void invalidateFontMetricsCache() {
		fontMetricsCache.clear();
	}

	public FontMetrics getFontMetrics(int offset) {
		if (editor.isDisposed())
			return null;

		var iterator = editor.getHighlighter().createIterator(offset);
		int fontStyleBitmask = iterator.getTextAttributes().getFontType();
		
		return fontMetricsCache.computeIfAbsent(fontStyleBitmask, style -> {
			var type = EditorFontType.forJavaStyle(style);
			var font = editor.getColorsScheme().getFont(type);
			return editor.getContentComponent().getFontMetrics(font);
		});
	}
}

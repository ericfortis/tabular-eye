package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.detectors.AlignmentDetector.AlignmentBlock;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.util.Disposer;
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
	private final Map<Integer, FontMetrics> metricsCache = new HashMap<>();

	public Spacers(Editor editor) {
		this.editor = editor;
	}

	public void refresh(List<AlignmentBlock> blocks) {
		if (isRefreshing)
			return;

		isRefreshing = true;
		try {
			ReadAction.runBlocking(() -> {
				if (!editor.isDisposed()) {
					clearAll();
					for (var b : blocks)
						renderGroup(b);
				}
			});
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


	private void renderGroup(AlignmentBlock block) {
		var props = block.props();

		// measure left-side tokens (keys)
		int[] widths = new int[props.size()];
		int maxWidth = 0;
		for (int i = 0; i < props.size(); i++) {
			var prop = props.get(i);
			var fm = getFontMetrics(prop.keyOffset());
			if (fm != null) {
				widths[i] = fm.stringWidth(prop.key()); // supports proportional fonts
				maxWidth = Math.max(maxWidth, widths[i]);
			}
		}

		// insert spacers
		var model = editor.getInlayModel();
		for (int i = 0; i < props.size(); i++) {
			int spacerWidth = maxWidth - widths[i];
			if (spacerWidth > 0) { // skips longest
				int placeAt = props.get(i).separatorOffset() + 1;
				var inlay = model.addInlineElement(placeAt, true, new Spacer(spacerWidth));
				if (inlay != null)
					activeInlays.add(inlay);
			}
		}
	}

	public void invalidateFontMetricsCache() {
		metricsCache.clear();
	}

	private FontMetrics getFontMetrics(int offset) {
		if (editor.isDisposed())
			return null;

		var iterator = editor.getHighlighter().createIterator(offset);
		int fontStyle = iterator.getTextAttributes().getFontType();

		return metricsCache.computeIfAbsent(fontStyle, style -> {
			var type = EditorFontType.forJavaStyle(style);
			var font = editor.getColorsScheme().getFont(type);
			return editor.getContentComponent().getFontMetrics(font);
		});
	}
}

package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder.AlignmentGroup;
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
	private boolean isRefreshing = false;
	private final List<Inlay<Spacer>> activeInlays = new ArrayList<>();
	private final Editor editor;

	private final Map<Integer, FontMetrics> metricsCache = new HashMap<>();

	public Spacers(Editor editor) {
		this.editor = editor;
	}

	public void refresh(List<AlignmentGroup> groups) {
		if (isRefreshing) return;
		isRefreshing = true;
		try {
			ReadAction.runBlocking(() -> {
				if (editor.isDisposed()) 
					return;
				clearAll();

				var visibleArea = editor.getScrollingModel().getVisibleArea();
				int startVisualLine = editor.xyToLogicalPosition(new Point(0, visibleArea.y)).line;
				int endVisualLine = editor.xyToLogicalPosition(new Point(0, visibleArea.y + visibleArea.height)).line;

				for (var g : groups) {
					int groupStartLine = editor.offsetToLogicalPosition(g.getStartOffset()).line;
					int groupEndLine = editor.offsetToLogicalPosition(g.getEndOffset()).line;

					if (groupEndLine < startVisualLine || groupStartLine > endVisualLine)
						continue;

					renderGroup(g);
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

	public void invalidateFontMetricsCache() {
		metricsCache.clear();
	}


	private void renderGroup(AlignmentGroup group) {
		var props = group.props();

		// measure left-side tokens
		int[] widths = new int[props.size()];
		int maxWidth = 0;
		for (int i = 0; i < props.size(); i++) {
			var prop = props.get(i);
			var fm = getFontMetrics(prop.keyStartOffset());
			if (fm == null) continue;

			int w = fm.stringWidth(prop.keyText()); // supports proportional fonts
			widths[i] = w;
			if (w > maxWidth)
				maxWidth = w;
		}

		// insert spacers
		var model = editor.getInlayModel();
		for (int i = 0; i < props.size(); i++) {
			int spacerWidth = maxWidth - widths[i];
			if (spacerWidth > 0) { // skips longest
				int placeAt = props.get(i).colonOffset() + 1;
				var inlay = model.addInlineElement(placeAt, true, new Spacer(spacerWidth));
				if (inlay != null)
					activeInlays.add(inlay);
			}
		}
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
}

package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder.AlignmentGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Spacers {
	private static final Logger LOG = Logger.getInstance(Spacers.class);

	private final List<Inlay<Spacer>> activeInlays = new ArrayList<>();
	private final Editor editor;

	/**
	 * Cached to avoid repeated Graphics-context acquisition on every refresh.
	 */
	private FontMetrics cachedFontMetrics;

	public Spacers(Editor editor) {
		this.editor = editor;
	}

	public void refresh(List<AlignmentGroup> groups) {
		clearAll();
		var fm = getFontMetrics();
		if (fm != null)
			for (var g : groups)
				renderGroup(g, fm);
	}

	public void clearAll() {
		for (var inlay : activeInlays)
			if (inlay.isValid())
				Disposer.dispose(inlay);
		activeInlays.clear();
	}

	/**
	 * Called when the editor color scheme changes (font size, typeface, etc.)
	 * so that the next refresh re-acquires correct metrics.
	 */
	public void invalidateFontMetricsCache() {
		cachedFontMetrics = null;
	}


	private void renderGroup(AlignmentGroup group, FontMetrics fm) {
		var props = group.props();

		// measure keys
		int[] widths = new int[props.size()];
		int maxWidth = 0;
		for (int i = 0; i < props.size(); i++) {
			int w = fm.stringWidth(props.get(i).keyText()); // supports proportional fonts
			widths[i] = w;
			if (w > maxWidth)
				maxWidth = w;
		}

		// insert spacers
		var model = editor.getInlayModel();
		for (int i = 0; i < props.size(); i++) {
			int spacerWidth = maxWidth - widths[i];
			if (spacerWidth > 0) { // skip the longest key — it needs no padding
				int placeAt = props.get(i).colonOffset() + 1;
				var inlay = model.addInlineElement(placeAt, true, new Spacer(spacerWidth));
				if (inlay != null)
					activeInlays.add(inlay);
			}
		}
	}


	private FontMetrics getFontMetrics() {
		if (editor.isDisposed())
			return null;

		if (cachedFontMetrics != null)
			return cachedFontMetrics;

		var g = editor.getContentComponent().getGraphics();
		if (g == null) {
			LOG.warn("TabularEye: could not acquire Graphics context from editor component; skipping render.");
			return null;
		}

		try {
			cachedFontMetrics = g.getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
			return cachedFontMetrics;
		} finally {
			g.dispose();
		}
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

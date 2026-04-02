package com.ericfortis.tabulareye;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AlignmentInlayManager
 * Given a list of ObjectGroups (from ObjectLiteralFinder), this class:
 * 1. Measures the pixel width of each property key in the editor's current font.
 * 2. Finds the maximum key width per group.
 * 3. Places a transparent inline inlay at (`colonOffset + 1`) for each property
 * whose key is narrower than the max. The inlay `width = maxWidth - thisWidth`,
 * so combined with the existing space char, all values start at the same column.
 * Inlays are tracked and disposed before every re-render so stale spacers
 * never accumulate.
 */
public class ElasticInlayManager {

	/**
	 * All currently live inlays owned by this manager for one editor.
	 */
	@SuppressWarnings("rawtypes")
	private final List<Inlay> activeInlays = new ArrayList<>();

	private final Editor editor;

	public ElasticInlayManager(Editor editor) {
		this.editor = editor;
	}

	/**
	 * Clears all existing inlays and re-creates them from the given groups.
	 * Call this whenever the document changes or the file is first opened.
	 */
	public void refresh(List<AlignmentGroup> groups) {
		clearAll();

		var fm = getFontMetrics();
		if (fm == null)
			return;

		for (var group : groups)
			renderGroup(group, fm);
	}

	/**
	 * Disposes every inlay this manager created. Safe to call multiple times.
	 */
	public void clearAll() {
		for (var inlay : activeInlays)
			if (inlay.isValid())
				Disposer.dispose(inlay);
		activeInlays.clear();
	}

	/**
	 * Computes widths, finds max, places inlays for one group.
	 */
	private void renderGroup(AlignmentGroup group, FontMetrics fm) {
		// Step 1: measure every key.
		int[] widths = new int[group.props.size()];
		int maxWidth = 0;

		for (int i = 0; i < group.props.size(); i++) {
			int w = fm.stringWidth(group.props.get(i).keyText());
			widths[i] = w;
			if (w > maxWidth)
				maxWidth = w;
		}

		// Step 2: for keys narrower than the max, place a spacer inlay.
		var model = editor.getInlayModel();

		for (int i = 0; i < group.props.size(); i++) {
			int gap = maxWidth - widths[i];
			if (gap <= 0)
				continue; // widest key — no inlay needed

			// Inlay goes at colonOffset + 1, i.e., right after ':' and before
			// the existing space character. The existing space + this inlay =
			//  the same total visual width for every property in the group.
			int inlayOffset = group.props.get(i).colonOffset() + 1;

			@SuppressWarnings({"rawtypes"})
			Inlay inlay = model.addInlineElement(
				 inlayOffset,
				 true,  // relatesToPrecedingText
				 new SpacerRenderer(gap)
			);

			if (inlay != null)
				activeInlays.add(inlay);
		}
	}

	/**
	 * Derives FontMetrics from the editor's current plain font.
	 * Returns null only if the editor is disposed of or has no graphics context.
	 */
	private FontMetrics getFontMetrics() {
		if (editor.isDisposed())
			return null;

		var font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
		Component component = editor.getContentComponent();
		var g = component.getGraphics();
		if (g == null)
			return null;

		try {
			return g.getFontMetrics(font);
		} finally {
			g.dispose();
		}
	}


	/**
	 * A fully transparent inlay that occupies exactly `widthPx` pixels.
	 * Nothing is painted — the space is reserved by calcWidthInPixels alone.
	 */
	static class SpacerRenderer implements EditorCustomElementRenderer {

		private final int widthPx;

		SpacerRenderer(int widthPx) {
			this.widthPx = widthPx;
		}

		@Override
		public int calcWidthInPixels(@NotNull Inlay inlay) {
			return Math.max(1, widthPx); // must always be > 0
		}

		@Override
		public void paint(
			 @NotNull Inlay inlay,
			 @NotNull Graphics g,
			 @NotNull Rectangle targetRegion,
			 @NotNull TextAttributes textAttributes
		) {
			// Transparent spacer — intentionally empty.
		}
	}
}

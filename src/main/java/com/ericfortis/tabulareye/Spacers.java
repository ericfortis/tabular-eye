package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentFinder.AlignmentGroup;
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
	private final List<Inlay<Spacer>> activeInlays = new ArrayList<>();

	private final Editor editor;

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


	private void renderGroup(AlignmentGroup group, FontMetrics fm) {
		// measure keys
		int[] widths = new int[group.props.size()];
		int maxWidth = 0;
		for (int i = 0; i < group.props.size(); i++) {
			int w = fm.stringWidth(group.props.get(i).keyText()); // supports proportional fonts 
			widths[i] = w;
			if (w > maxWidth)
				maxWidth = w;
		}

		// insert spacers
		var model = editor.getInlayModel();
		for (int i = 0; i < group.props.size(); i++) {
			int gap = maxWidth - widths[i];
			if (gap > 0) { // ignore longest key
				int inlayOffset = group.props.get(i).colonOffset() + 1;
				var inlay = model.addInlineElement(inlayOffset, true, new Spacer(gap));
				if (inlay != null)
					activeInlays.add(inlay);
			}
		}
	}


	private FontMetrics getFontMetrics() {
		if (editor.isDisposed())
			return null;

		var g = editor.getContentComponent().getGraphics();
		if (g == null)
			return null;

		try {
			return g.getFontMetrics(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
		} finally {
			g.dispose();
		}
	}


	/**
	 * A fully transparent inlay that occupies exactly `widthPx` pixels.
	 * Note: Antialising (user setting) greyscale makes the columns not to align 100% perfect.
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

package com.ericfortis.tabulareye;

import com.ericfortis.tabulareye.finders.AlignmentGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.util.Disposer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class SpacerManager {
	private final List<Inlay<?>> activeInlays = new ArrayList<>();

	private final Editor editor;

	public SpacerManager(Editor editor) {
		this.editor = editor;
	}

	public void refresh(List<AlignmentGroup> groups) {
		clearAll();

		var fm = getFontMetrics();
		if (fm == null)
			return;

		for (var group : groups)
			renderGroup(group, fm);
	}

	public void clearAll() {
		for (var inlay : activeInlays)
			if (inlay.isValid())
				Disposer.dispose(inlay);
		activeInlays.clear();
	}

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

			Inlay<?> inlay = model.addInlineElement(inlayOffset, true, new Spacer(gap));

			if (inlay != null)
				activeInlays.add(inlay);
		}
	}

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
}

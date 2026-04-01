package com.ericfortis.tabulareye;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayModel;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class MiddleASpacerPlugin implements EditorFactoryListener {

	// Keeps one inlay per editor so we can dispose it before recreating.
	// Raw Inlay type matches the interface's type-erased declaration.
	@SuppressWarnings("rawtypes")
	private final Map<Editor, Inlay> inlayMap = new HashMap<>();

	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		Editor editor = event.getEditor();

		// Attach a document listener scoped to this editor's lifetime.
		DocumentListener listener = new DocumentListener() {
			@Override
			public void documentChanged(@NotNull DocumentEvent e) {
				updateInlay(editor);
			}
		};

		editor.getDocument().addDocumentListener(listener);

		// Also update immediately in case the file is already "AAA".
		updateInlay(editor);
	}

	@Override
	public void editorReleased(@NotNull EditorFactoryEvent event) {
		clearInlay(event.getEditor());
	}


	/**
	 * Checks whether the document holds exactly "AAA" (trimmed).
	 * If yes, ensures a spacer inlay sits at offset 1 (before the middle A).
	 * If no,  removes any existing inlay for this editor.
	 */
	private void updateInlay(Editor editor) {
		String text = editor.getDocument().getText();

		if (isExactlyAAA(text)) {
			// Offset 1 = just before the second character ('A').
			// We always re-create so font changes are picked up automatically.
			clearInlay(editor);
			createInlay(editor, 1);
		} else {
			clearInlay(editor);
		}
	}

	/**
	 * Returns true only when the document's non-whitespace content is "AAA".
	 */
	private boolean isExactlyAAA(String text) {
		return text != null && text.trim().equals("AAA");
	}

	/**
	 * Adds a zero-painting inline inlay at the given document offset.
	 * The inlay's width is computed lazily inside SpacerRenderer.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void createInlay(Editor editor, int offset) {
		InlayModel model = editor.getInlayModel();
		SpacerRenderer renderer = new SpacerRenderer(editor);

		Inlay inlay = model.addInlineElement(
			 offset,
			 true, // relatesToPrecedingText — sticks to the char before
			 renderer
		);

		if (inlay != null) {
			inlayMap.put(editor, inlay);
		}
	}

	/**
	 * Disposes and removes the tracked inlay for the given editor.
	 */
	@SuppressWarnings("rawtypes")
	private void clearInlay(Editor editor) {
		Inlay old = inlayMap.remove(editor);
		if (old != null && old.isValid()) {
			Disposer.dispose(old);
		}
	}


	/**
	 * An invisible spacer whose width == half the pixel width of 'A'
	 * in the editor's current plain font.
	 * <p>
	 * calcWidthInPixels is called by the layout engine before paint();
	 * we derive the width there using FontMetrics so it stays correct
	 * even if the user changes the editor font size while the file is open.
	 */
	static class SpacerRenderer implements EditorCustomElementRenderer {

		private final Editor editor;

		SpacerRenderer(Editor editor) {
			this.editor = editor;
		}

		/**
		 * Returns half the width of 'A' in the editor font, rounded up.
		 * The IntelliJ layout engine calls this to reserve horizontal space.
		 * <p>
		 * Note: raw Inlay (no wildcard) is required — the interface declares
		 * calcWidthInPixels(Inlay) without a type parameter at the call site.
		 */
		@Override
		public int calcWidthInPixels(@NotNull Inlay inlay) {
			FontMetrics fm = getFontMetrics();
			if (fm == null) {
				// Fallback: use the editor's logical column width.
				return editor.getLineHeight() / 2;
			}
			int fullWidth = fm.charWidth('A');
			// Integer ceil-divide by 2 so odd widths round up.
			return (fullWidth + 1) / 2;
		}

		/**
		 * Paint nothing — the inlay is a pure transparent spacer.
		 * The space has already been reserved by calcWidthInPixels.
		 * <p>
		 * Note: raw Inlay (no wildcard) matches the interface declaration.
		 */
		@Override
		public void paint(
			 @NotNull Inlay inlay,
			 @NotNull Graphics g,
			 @NotNull Rectangle targetRegion,
			 @NotNull TextAttributes textAttributes
		) {
			// Intentionally empty.
		}

		// -------------------------------------------------------------- //

		/**
		 * Derives FontMetrics from the editor's color scheme font.
		 * Returns null only if the editor is already disposed.
		 */
		private FontMetrics getFontMetrics() {
			if (editor.isDisposed()) return null;

			Font font = editor.getColorsScheme()
				 .getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN);

			// We need a Graphics context to get accurate metrics.
			// The editor component is the safest source for one.
			Component component = editor.getContentComponent();
			Graphics g = component.getGraphics();
			if (g == null) return null;

			try {
				return g.getFontMetrics(font);
			} finally {
				g.dispose();
			}
		}
	}
}
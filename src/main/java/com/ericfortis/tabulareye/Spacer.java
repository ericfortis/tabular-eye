package com.ericfortis.tabulareye;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import org.jetbrains.annotations.NotNull;

/**
 * A fully transparent inlay that occupies exactly `widthPx` pixels.
 * Note: Antialising (user setting) greyscale makes the columns not to align 100% perfect.
 */
class Spacer implements EditorCustomElementRenderer {

	private final int widthPx;

	Spacer(int widthPx) {
		this.widthPx = widthPx;
	}

	@Override
	public int calcWidthInPixels(@NotNull Inlay inlay) {
		return widthPx;
	}

	@Override
	public int calcHeightInPixels(@NotNull Inlay inlay) {
		return 1;
	}
}

package com.cope.meteormcp.gui;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Renders text as masked characters while preserving the underlying textbox value.
 */
public class MaskedTextBoxRenderer implements WTextBox.Renderer {
    @Override
    public void render(GuiRenderer renderer, double x, double y, String text, Color color) {
        if (text == null || text.isEmpty()) return;
        renderer.text("*".repeat(text.length()), x, y, color, false);
    }
}

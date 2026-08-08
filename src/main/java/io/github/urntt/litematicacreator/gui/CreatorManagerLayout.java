package io.github.urntt.litematicacreator.gui;

final class CreatorManagerLayout
{
    private static final int MIN_LEFT_PANE_WIDTH = 160;
    private static final int PREFERRED_LEFT_PANE_WIDTH = 240;
    private static final int MAX_LEFT_PANE_WIDTH = 320;
    private static final int MIN_CONTENT_PANE_WIDTH = 320;
    private static final int MIN_CONTROL_WIDTH = 96;

    private CreatorManagerLayout()
    {
    }

    static int leftPaneWidth(int screenWidth)
    {
        int preferred = Math.max(PREFERRED_LEFT_PANE_WIDTH, screenWidth / 3);
        int available = Math.max(MIN_LEFT_PANE_WIDTH, screenWidth - MIN_CONTENT_PANE_WIDTH);
        return Math.min(Math.min(MAX_LEFT_PANE_WIDTH, preferred), available);
    }

    static int buttonColumns(int availableWidth, int itemCount, int widestButtonWidth, int gap)
    {
        if (itemCount <= 0)
        {
            return 0;
        }

        int requiredWidth = Math.max(1, widestButtonWidth + gap);
        return Math.max(1, Math.min(itemCount, (Math.max(1, availableWidth) + gap) / requiredWidth));
    }

    static int buttonCellWidth(int availableWidth, int columns, int gap)
    {
        if (columns <= 0)
        {
            return 0;
        }

        return Math.max(24, (Math.max(1, availableWidth) - gap * (columns - 1)) / columns);
    }

    static int labelColumnWidth(int contentWidth, int widestLabelWidth)
    {
        int maximum = Math.max(48, Math.min(168, contentWidth - MIN_CONTROL_WIDTH));
        return Math.max(48, Math.min(maximum, widestLabelWidth + 14));
    }
}

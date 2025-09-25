import java.awt.*;
import java.awt.image.BufferedImage;

public class SelectionTool {
    private Rectangle selection;
    private BufferedImage clipboard;

    public void startSelection(int x, int y) {
        selection = new Rectangle(x, y, 0, 0);
    }

    public void updateSelection(int x, int y) {
        if (selection != null) {
            selection.setSize(x - selection.x, y - selection.y);
        }
    }

    public void clearSelection() {
        selection = null;
    }

    public Rectangle getSelection() {
        return selection;
    }

    public void copy(BufferedImage image) {
        if (selection == null || image == null) return;
        clipboard = image.getSubimage(
                Math.max(0, selection.x),
                Math.max(0, selection.y),
                Math.min(selection.width, image.getWidth() - selection.x),
                Math.min(selection.height, image.getHeight() - selection.y)
        );
    }

    public void paste(Graphics2D g2, int x, int y) {
        if (clipboard != null) {
            g2.drawImage(clipboard, x, y, null);
        }
    }
}

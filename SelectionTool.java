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

        int sx = Math.max(0, selection.x);
        int sy = Math.max(0, selection.y);
        int sw = Math.max(0, Math.min(selection.width, image.getWidth() - sx));
        int sh = Math.max(0, Math.min(selection.height, image.getHeight() - sy));
        if (sw <= 0 || sh <= 0) return;

        BufferedImage sub = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = sub.createGraphics();
        g2.drawImage(image, 0, 0, sw, sh, sx, sy, sx + sw, sy + sh, null);
        g2.dispose();
        clipboard = sub;
    }

    public void paste(Graphics2D g2, int x, int y) {
        if (clipboard != null) {
            g2.drawImage(clipboard, x, y, null);
        }
    }
}

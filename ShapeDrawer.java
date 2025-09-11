import java.awt.*;

public class ShapeDrawer {
    public static void drawShape(Graphics2D g2, ShapeType type, int x1, int y1, int x2, int y2) {
        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);
        int startX = Math.min(x1, x2);
        int startY = Math.min(y1, y2);

        switch (type) {
            case RECTANGLE:
                g2.drawRect(startX, startY, w, h);
                break;

            case SQUARE:
                int side = Math.min(w, h);
                g2.drawRect(startX, startY, side, side);
                break;

            case CIRCLE:
                int diameter = Math.min(w, h);
                g2.drawOval(startX, startY, w, h);
                break;

            case ELLIPSE:
                g2.drawOval(startX, startY, w, h);
                break;

            case TRIANGLE:
                int[] xPoints = {x1, x2, (x1 + x2) / 2};
                int[] yPoints = {y2, y2, y1};
                g2.drawPolygon(xPoints, yPoints, 3);
                break;
        }
    }
}

import java.awt.*;
public class AdvancedShapeDrawer {
    public static void drawShape(Graphics2D g2, ShapeType type, int x1, int y1, int x2, int y2, int polygonSides) {
        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);
        int startX = Math.min(x1, x2);
        int startY = Math.min(y1, y2);

        switch (type) {
            case RIGHT_TRIANGLE:
                int[] xR = {x1, x1, x2};
                int[] yR = {y1, y1, y2};
                g2.drawPolygon(xR, yR, 3);
                break;

            case TRIANGLE:
                int[] xI = {x1, x2, (x1 + x2) / 2};
                int[] yI = {y2, y2, y1};
                g2.drawPolygon(xI,yI, 3);
                break;

            case POLYGON:
                if (polygonSides >= 3) {
                    int centerX = (x1 + x2) / 2;
                    int centerY = (y1 + y2) / 2;
                    int radius = Math.min(w, h) / 2;

                    int[] xPoints = new int[polygonSides];
                    int[] yPoints = new int[polygonSides];
                    for (int i = 0; i < polygonSides; i++) {
                        double angle = 2 * Math.PI * i / polygonSides;
                        xPoints[i] = centerX + (int)(radius * Math.cos(angle));
                        yPoints[i] = centerY + (int)(radius * Math.sin(angle));
                    }
                    g2.drawPolygon(xPoints, yPoints, polygonSides);
                }
                break;

            case KITE:
                int centerX = (x1 + x2) / 2;
                int centerY = (y1 + y2) / 2;
                int[] xK = {centerX, x1, centerX, x2};
                int[] yK = {y1, centerY, y2, centerY};
                g2.drawPolygon(xK, yK, 4);
                break;

            case STAR:
                int points = Math.max(4, polygonSides);
                drawStar(g2, x1, y1, x2, y2, points);
                break;

            default:
                ShapeDrawer.drawShape(g2, type, x1, y1, x2, y2);
                break;

        }
    }

    private static void drawStar(Graphics2D g2, int x1, int y1, int x2, int y2, int points) {
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        int outerRadius = Math.min(Math.abs(x2 - x1), Math.abs(y2 - y1)) /2;
        int innerRadius = Math.max(1, outerRadius / 2);

        double angleStep = Math.PI / points;
        int total = points * 2;
        int[] xs = new int [total];
        int[] ys = new int[total];
        double angle = -Math.PI / 2;
        for (int i = 0; i < total; i++) {
            int r = (i % 2 == 0) ? outerRadius : innerRadius;
            xs[i] = centerX + (int) (r * Math.cos(angle));
            ys[i] = centerY + (int) (r * Math.sin(angle));
            angle += angleStep;
        }
        g2.drawPolygon(xs, ys, total);
    }
}

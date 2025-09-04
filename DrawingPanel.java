import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class DrawingPanel extends JPanel {
    private BufferedImage image;
    private int lastX, lastY;
    private boolean drawing;

    private Color brushColor = Color.RED;
    private int brushSize = 3;

    public DrawingPanel() {
        setBackground(Color.WHITE);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ensureImageExists();
                lastX = e.getX();
                lastY = e.getY();
                drawing = true;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if(drawing && image != null) {
                    int x = e.getX();
                    int y = e.getY();
                    drawLineOnImage(lastX, lastY, x, y);
                    lastX = x;
                    lastY = y;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                drawing = false;
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private void ensureImageExists() {
        if (image == null) {
            //creating a blank canvas here
            image = new BufferedImage(1440, 1920, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2.dispose();
        }
    }
    public void setImage(BufferedImage img) {
        this.image = img;
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    private void drawLineOnImage(int x1, int y1, int x2, int y2) {
        Graphics2D g2 = image.createGraphics();
        g2.setColor(brushColor);
        g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        g2.dispose();
    }

    public void setBrushColor(Color color) {
        this.brushColor = color;
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
    }
    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        return new Dimension(1440, 1920);
    }
}

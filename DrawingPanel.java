import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class DrawingPanel extends JPanel {
    private BufferedImage image;
    private int startX, startY, endX, endY;
    private boolean drawing;

    public DrawingPanel() {
        setBackground(Color.WHITE);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (image == null) return;
                startX = e.getX();
                startY = e.getY();
                endX = startX;
                endY = startY;
                drawing = true;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if(drawing) {
                    endX = e.getX();
                    endY = e.getY();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (drawing) {
                    endX = e.getX();
                    endY = e.getY();
                    drawLineOnImage();
                    drawing = false;
                    repaint();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    public void setImage(BufferedImage img) {
        this.image = img;
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    private void drawLineOnImage() {
        if (image != null) {
            Graphics2D g2 = image.createGraphics();
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(startX, startY, endX, endY);
            g2.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
        if(drawing) {
            g.setColor(Color.RED);
            ((Graphics2D)g).setStroke(new BasicStroke(2));
            g.drawLine(startX, startY, endX, endY);
        }
    }
    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        return super.getPreferredSize();
    }
}

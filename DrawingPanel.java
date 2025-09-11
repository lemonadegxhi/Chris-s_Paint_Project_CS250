import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class DrawingPanel extends JPanel {
    private BufferedImage image;
    private int startX, startY, lastX, lastY;
    private boolean drawing;
    private Runnable onChange;

    private DrawingTool currentTool = DrawingTool.PENCIL;
    private ToolOptions toolOptions = new ToolOptions();

    private Color brushColor = Color.BLACK;
    private int brushSize = 3;

    public DrawingPanel(Runnable onChange) {
        this.onChange = onChange;
        setBackground(Color.WHITE);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ensureImageExists();
                startX = e.getX();
                startY = e.getY();

                if (currentTool == DrawingTool.EYEDROPPER && image != null) {
                    int rgb = image.getRGB(startX, startY);
                    toolOptions.setColor(new Color(rgb, true));
                    return;
                }
                drawing = true;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if(drawing && image != null && currentTool == DrawingTool.PENCIL) {
                    int x = e.getX();
                    int y = e.getY();
                    drawLineOnImage(lastX, lastY, x, y);
                    lastX = x;
                    lastY = y;
                    repaint();
                    if (onChange != null) onChange.run();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (drawing && image != null) {
                    if (currentTool == DrawingTool.SHAPE) {
                        Graphics2D g2 = image.createGraphics();
                        g2.setColor(toolOptions.getColor());
                        g2.setStroke(toolOptions.getStroke());

                        ShapeDrawer.drawShape(g2, toolOptions.getShapeType(), startX, startY, e.getX(), e.getY());
                        g2.dispose();
                        repaint();
                        if (onChange != null) onChange.run();
                    }
                }
                drawing = false;
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private void drawLineOnImage(int x1, int y1, int x2, int y2) {
        Graphics2D g2 = image.createGraphics();
        g2.setColor(toolOptions.getColor());
        g2.setStroke(toolOptions.getStroke());
        g2.drawLine(x1, y1, x2, y2);
        g2.dispose();
    }

    public void setTool(DrawingTool tool) {
        this.currentTool = tool;
    }

    public void setShapeType(ShapeType type) {
        this.toolOptions.setShapeType(type);
    }

    public void setDashed(boolean dashed) {
        this.toolOptions.setDashed(dashed);
    }

    private void ensureImageExists() {
        if (image == null) {
            //creating a blank canvas here
            resizeCanvas (1440, 1920);
            if (onChange != null) onChange.run();
        }
    }
    public void setImage(BufferedImage img) {
        this.image = img;
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    /*private void drawLineOnImage(int x1, int y1, int x2, int y2) {
        Graphics2D g2 = image.createGraphics();
        g2.setColor(brushColor);
        g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        g2.dispose();
    }*/

    public void setBrushColor(Color color) {
        this.brushColor = color;
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
    }

    public void resizeCanvas(int width, int height) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        if (image != null) {
            g2.drawImage(image, 0, 0, null);
        }
        g2.dispose();
        image = newImage;
        revalidate();
        repaint();
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

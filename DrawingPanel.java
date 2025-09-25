import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

public class DrawingPanel extends JPanel {
    private BufferedImage image;
    private int startX, startY, lastX, lastY;
    private boolean drawing;
    private Runnable onChange;

    private DrawingTool currentTool = DrawingTool.PENCIL;
    private ToolOptions toolOptions = new ToolOptions();

    private Color brushColor = Color.BLACK;
    private int brushSize = 3;

    // Undo/Redo part
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private final int MAX_HISTORY = 50;

    public ToolOptions getToolOptions() {
        return this.toolOptions;
    }

    public DrawingPanel(Runnable onChange) {
        this.onChange = onChange;
        setBackground(Color.WHITE);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ensureImageExists();
                startX = e.getX();
                startY = e.getY();
                lastX = startX;
                lastY = startY;

                if (currentTool == DrawingTool.EYEDROPPER && image != null) {
                    if (startX >= 0 && startX < image.getWidth() && startY >= 0 && startY < image.getHeight()) {
                        int rgb = image.getRGB(startX, startY);
                        Color picked = new Color(rgb, true);
                        toolOptions.setColor(picked);
                        setBrushColor(picked);
                    }
                    return;
                }
                if (willModifyImage(currentTool)) {
                    pushStateForUndo();
                    redoStack.clear();
                }
                drawing = true;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if(drawing && image != null) {
                    int x = e.getX();
                    int y = e.getY();

                    if (currentTool == DrawingTool.PENCIL || currentTool == DrawingTool.DRAW) {
                        drawLineOnImage(lastX, lastY, x, y, toolOptions.getColor());
                    } else if (currentTool == DrawingTool.ERASER) {
                        drawLineOnImage(lastX, lastY, x, y, Color.WHITE);
                    }
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

                        AdvancedShapeDrawer.drawShape(g2, toolOptions.getShapeType(),
                                startX, startY, e.getX(), e.getY(),
                                toolOptions.getPolygonSides());
                        g2.dispose();
                        repaint();
                        if (onChange != null) onChange.run();
                    } else if (currentTool == DrawingTool.TEXT) {
                        pushStateForUndo();
                        redoStack.clear();

                        Graphics2D g2 = image.createGraphics();
                        g2.setColor(toolOptions.getColor());
                        g2.setFont(new Font("SansSerif", Font.PLAIN, toolOptions.getTextSize()));
                        FontMetrics fm = g2.getFontMetrics();
                        int ascent = fm.getAscent();
                        g2.drawString(toolOptions.getText(), e.getX(), e.getY() + ascent);
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

    private boolean willModifyImage(DrawingTool t) {
        return t == DrawingTool.PENCIL || t == DrawingTool.DRAW || t == DrawingTool.ERASER
                || t == DrawingTool.SHAPE || t == DrawingTool.TEXT;
    }

    private void drawLineOnImage(int x1, int y1, int x2, int y2, Color c) {
        Graphics2D g2 = image.createGraphics();
        g2.setColor(c);
        g2.setStroke(toolOptions.getStroke());
        g2.drawLine(x1, y1, x2, y2);
        g2.dispose();
    }

    public void setBrushSize(int size) {
        this.toolOptions.setSize(size);
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
            resizeCanvas (800, 600);
            if (onChange != null) onChange.run();
        }
    }
    public void setImage(BufferedImage img) {
        if (img == null) return;
        image = copyImage(img);
        undoStack.clear();
        redoStack.clear();
        pushStateForUndo();
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
        this.toolOptions.setColor(color);
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

        pushStateForUndo();
        if (onChange != null) onChange.run();
    }

    public void clearCanvas() {
        if (image == null) {
            resizeCanvas(800, 600);
            return;
        }
        pushStateForUndo();
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.dispose();
        repaint();
        if (onChange != null) onChange.run();
        redoStack.clear();
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

    // UNDO/REDO
    private void pushStateForUndo() {
        if (image == null) return;
        // push a copy
        undoStack.push(copyImage(image));
        //cap history
        while (undoStack.size() > MAX_HISTORY) {
            BufferedImage[] tmp = undoStack.toArray(new BufferedImage[0]);
            undoStack.removeLast();
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) return;
        if (image != null) {
            redoStack.push(copyImage(image));
        }
        BufferedImage prev = undoStack.pop();
        image = copyImage(prev);
        repaint();
        if (onChange != null) onChange.run();
    }

    public void redo() {
        if (!canRedo()) return;
        if (image != null) {
            undoStack.push(copyImage(image));
        }
        BufferedImage next = redoStack.pop();
        image = copyImage(next);
        repaint();
        if (onChange != null) onChange.run();
    }
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private BufferedImage copyImage(BufferedImage src) {
        if (src == null) return null;
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

public class DrawingPanel extends JPanel {
    private BufferedImage image;
    private int startX, startY, lastX, lastY;
    private boolean drawing;
    private Runnable onChange;

    private DrawingTool currentTool = DrawingTool.PENCIL;
    private ToolOptions toolOptions = new ToolOptions();

    private SelectionTool selectionTool = new SelectionTool();
    private boolean selecting = false;
    private boolean pasting = false;

    private Color brushColor = Color.BLACK;
    private int brushSize = 3;

    // Undo/Redo part
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private final int MAX_HISTORY = 50;

    public ToolOptions getToolOptions() {
        return this.toolOptions;
    }



    private void pushStack() {
        BufferedImage snapshot = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = snapshot.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        undoStack.push(snapshot);
        redoStack.clear();
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
                if (currentTool == DrawingTool.SELECT) {
                    selectionTool.startSelection(e.getX(), e.getY());
                    selecting = true;
                    return;
                }
                if (currentTool == DrawingTool.PASTE) {
                    Graphics2D g2 = image.createGraphics();
                    selectionTool.paste(g2, e.getX(), e.getY());
                    g2.dispose();
                    repaint();

                    return;
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
                if (selecting) {
                    selectionTool.updateSelection(e.getX(), e.getY());
                    repaint();
                    return;
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
                if (selecting) {
                    selecting = false;
                    repaint();
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

    public void copySelection() {
        if (image != null) {
            selectionTool.copy(image);
        }
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
        if (selectionTool.getSelection() != null) {
            g.setColor(Color.BLUE);
            ((Graphics2D) g).setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0));
            Rectangle sel = selectionTool.getSelection();
            g.drawRect(sel.x, sel.y, sel.width, sel.height);
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

    public void rotateSelectionOrCanvas(int degrees) {
        if (image == null) return;
        Rectangle sel = selectionTool.getSelection();
        if (sel != null && sel.width > 0 && sel.height > 0) {
            pushStateForUndo();
            redoStack.clear();

            BufferedImage chunk = image.getSubimage(
                    Math.max(0, sel.x),
                    Math.max(0, sel.y),
                    Math.min(sel.width, image.getWidth() - sel.x),
                    Math.min(sel.height, image.getHeight() - sel.y)
            );
            BufferedImage rotated = rotateImage(chunk, degrees);

            //Draw rotated into the selection rectangle, scaling if necessary to fit bounds.
            Graphics2D g2 = image.createGraphics();
            g2.setComposite(AlphaComposite.SrcOver);
            g2.drawImage(rotated, sel.x, sel.y, sel.width, sel.height, null);
            g2.dispose();
            repaint();
            if (onChange != null) onChange.run();
        } else {
            pushStateForUndo();
            redoStack.clear();

            BufferedImage rotated = rotateImage(image, degrees);
            image = rotated;
            revalidate();
            repaint();
            if (onChange != null) onChange.run();
        }
    }

    /**
     * Flip either the selection (if one exists) or the whole canvas.
     * @param horizontal true => horizontal flip (left <-> right), false => vertical flip (top <-> bottom)
     */
    public void flipSelectionOrCanvas(boolean horizontal) {
        if (image == null) return;
        Rectangle sel = selectionTool.getSelection();
        if (sel != null && sel.width > 0 && sel.height > 0) {
            pushStateForUndo();
            redoStack.clear();

            BufferedImage chunk = image.getSubimage(
                    Math.max(0, sel.x),
                    Math.max(0, sel.y),
                    Math.min(sel.width, image.getWidth() - sel.x),
                    Math.min(sel.height, image.getHeight() - sel.y)
            );
            BufferedImage flipped = flipImage(chunk, horizontal);

            Graphics2D g2 = image.createGraphics();
            g2.setComposite(AlphaComposite.SrcOver);
            g2.drawImage(flipped, sel.x, sel.y, sel.width, sel.height, null);
            g2.dispose();
            repaint();
            if (onChange != null) onChange.run();
        } else {
            pushStateForUndo();
            redoStack.clear();

            BufferedImage flipped = flipImage(image, horizontal);
            image = flipped;
            revalidate();
            repaint();
            if (onChange != null) onChange.run();
        }
    }

    private BufferedImage rotateImage(BufferedImage src, int degrees) {
        if (degrees % 360 == 0) return copyImage(src);

        double radians = Math.toRadians(degrees);
        int srcW = src.getWidth();
        int srcH = src.getHeight();

        int destW = srcW;
        int destH = srcH;
        if (degrees % 180 != 0) {
            destW = srcH;
            destH = srcW;
        }

        BufferedImage result = new BufferedImage(destW, destH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();

        AffineTransform at = new AffineTransform();

        //Translate & rotate so that the image is centerede correctly
        if (degrees == 90) {
            at.translate(destW, 0);
            at.rotate(radians);
        } else if (degrees == 180) {
            at.translate(destW, destH);
            at.rotate(radians);
        } else if (degrees == 270) {
            at.translate(0, destH);
            at.rotate(radians);
        }  else {
            at.rotate(radians, srcW / 2.0, srcH / 2.0);
        }

        g2.setTransform(at);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return result;
    }

    private BufferedImage flipImage(BufferedImage src, boolean horizontal) {
        int w = src.getWidth();
        int h = src.getHeight();

        AffineTransform at = new AffineTransform();
        if (horizontal) {
            at.scale(-1.0, 1.0);
            at.translate(-w, 0);
        } else {
            at.scale(1.0, -1.0);
            at.translate(0, -h);
        }
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(src, at, null);
        g2.dispose();
        return result;
    }
}

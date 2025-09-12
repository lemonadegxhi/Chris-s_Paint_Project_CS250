import java.awt.*;

public class ToolOptions {
    private Color color = Color.BLACK;
    private int size = 3;
    private boolean dashed = false;
    private ShapeType shapeType = ShapeType.RECTANGLE;

    public Color getColor() {return color; }
    public void setColor(Color color) {this.color = color; }

    public int getSize() {return size;}
    public void setSize(int size) {this.size = size;}
    public void setDashed(boolean dashed) {this.dashed = dashed; }

    public ShapeType getShapeType() {return shapeType;}
    public void setShapeType(ShapeType shapeType) {this.shapeType = shapeType;}


    public Stroke getStroke() {
        if (dashed) {
            float dashLength = size * 2.5f;
            return new BasicStroke(
                    size,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10,
                    new float[]{dashLength, dashLength},
                    0
            );
        }
        return new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}

import javax.swing.*;
public class PolygonShape {
    public static int askSides() {
        String input = JOptionPane.showInputDialog("Enter number of sides (3+):", "5");
        try {
            int sides = Integer.parseInt(input);
            if (sides >= 3) return sides;
        } catch (Exception ignored) {}
        return 3;
    }
}

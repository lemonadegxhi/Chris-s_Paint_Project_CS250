import javax.swing.*;
public class PolygonShape {
    public static int askSides() {
        return askSides("Enter number of sides (3+):", "5");
    }
    public static int askSides(String message, String defaultValue) {
        String input = JOptionPane.showInputDialog(message, defaultValue);
        try {
            int val = Integer.parseInt(input);
            return Math.max(3, val);
        } catch (Exception ignored) {}
        return 3;
    }
}
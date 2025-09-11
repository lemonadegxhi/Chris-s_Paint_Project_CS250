import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.File;
import javax.imageio.ImageIO;

public class PaintApp {
    private JFrame frame;
    private DrawingPanel drawingPanel;
    private BufferedImage currentImage;
    private File currentFile;
    private boolean isDirty = false; //looking for unsaved changes.

    public PaintApp() {
        frame = new JFrame("Chris' Magical Paint");
        frame.setSize(600, 400);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attemptExit();
            }
        });

        drawingPanel = new DrawingPanel(() -> setDirty(true));
        JScrollPane scrollPane = new JScrollPane(drawingPanel);
        frame.add(scrollPane, BorderLayout.CENTER);

        //MENU RIGHT HEREEEEEEEE
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Image");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        JMenuItem closeItem = new JMenuItem("Close");

    openItem.addActionListener(e -> openImage());
    saveItem.addActionListener(e -> saveImage(false));
    saveAsItem.addActionListener(e -> saveImage(true));
    closeItem.addActionListener(e -> frame.dispose());

        fileMenu.add(openItem);

        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();
        fileMenu.add(closeItem);

        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        //Edit Menu
        JMenu toolMenu = new JMenu("Tools");
        JMenuItem colorItem = new JMenuItem("Line Color");
        //JMenuItem sizeItem = new JMenuItem("Line Width");
        JMenuItem resizeCanvasItem = new JMenuItem("Resize Canvas");

        colorItem.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(frame, "Pick Line", Color.RED);
            if (newColor != null) {
                drawingPanel.setBrushColor(newColor);
            }
        });

        resizeCanvasItem.addActionListener(e -> {
            JTextField heightField = new JTextField("");
            JTextField widthField = new JTextField("");
            JPanel panel = new JPanel(new GridLayout(2, 2));
            panel.add(new JLabel( "Height:"));
            panel.add(heightField);
            panel.add(new JLabel("Width:"));
            panel.add(widthField);

            int result = JOptionPane.showConfirmDialog(frame, panel, "Resize Canvas", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int w = Integer.parseInt(widthField.getText());
                    int h = Integer.parseInt(heightField.getText());
                    drawingPanel.resizeCanvas(w, h);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid size input!");
                }
            }
        });

        JSlider sizeItem = new JSlider(1, 50, 3);
        sizeItem.setMajorTickSpacing(10);
        sizeItem.setMinorTickSpacing(1);
        sizeItem.setPaintTicks(true);
        sizeItem.setPaintLabels(true);

        sizeItem.addChangeListener (e -> {
            int size = sizeItem.getValue();
            drawingPanel.setBrushSize(size);
        });

        JPanel sliderPanel = new JPanel();
        sliderPanel.add(new JLabel("Line Width:"));
        sliderPanel.add(sizeItem);

        JMenuItem sliderItem = new JMenuItem("Adjust Line Width");
        sliderItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame, sliderPanel, "Brush Size", JOptionPane.PLAIN_MESSAGE);
        });

        JMenu shapeMenu = new JMenu("Shapes");
        for (ShapeType type : ShapeType.values()) {
            JMenuItem shapeItem = new JMenuItem(type.name());
            shapeItem.addActionListener(e -> {
                drawingPanel.setTool(DrawingTool.SHAPE);
                drawingPanel.setShapeType(type);
            });
            shapeMenu.add(shapeItem);
        }

        JMenuItem eyedropperItem = new JMenuItem("Eyedropper");
        eyedropperItem.addActionListener(e -> drawingPanel.setTool(DrawingTool.EYEDROPPER));

        JCheckBoxMenuItem dashedItem = new JCheckBoxMenuItem("Dasehd Line");
        dashedItem.addActionListener(e -> drawingPanel.setDashed(dashedItem.isSelected()));


        toolMenu.add(colorItem);
        //editMenu.add(sizeItem);
        toolMenu.add(eyedropperItem);
        toolMenu.add(shapeMenu);
        toolMenu.add(sliderItem);
        toolMenu.add(dashedItem);
        toolMenu.add(resizeCanvasItem);



        //Adding a help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("Help");
        JMenuItem aboutItem = new JMenuItem("About");

        helpItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame,
                    "Chris' Magical Paint:\n\n" +
                    "- Use the mouse to draw on the canvas.\n" +
                    "- File > Open to load an image.\n" +
                    "- File > Save/Save As to save your work. \n" +
                    "- Edit > Line Color to change brush color.\n" +
                    "- Edit > Adjust Line Width to change brush size.\n" +
                    "- Edit > Resize Canvas to change canvas size for larger paintings. \n",
                    "Help", JOptionPane.INFORMATION_MESSAGE);
        });

        aboutItem.addActionListener(e -> showAboutDialog());

        helpMenu.add(helpItem);
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(toolMenu);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);

    }

    private void attemptExit() {
        if (isDirty) {
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    "YOUR ABOUT TO CLOSE WITHOUT SAVEING. SAVE???" +
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION) {
                return;
            } else if (choice == JOptionPane.YES_OPTION) {
                saveImage(false);
            }
        }
        frame.dispose();
    }

    private void setDirty(boolean dirty) {
        isDirty = dirty;
        String title = "Chris' Magical Paint" + (dirty ? " *" : "");
        frame.setTitle(title);
    }

    //THIS NOW MAKEY OPEY AND SAY IT OPEY
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                currentFile = chooser.getSelectedFile();
                currentImage = ImageIO.read(currentFile); //loading it into the memory here

                if (currentImage != null) {
                    drawingPanel.setImage(currentImage);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Failed to Open Image: " + ex.getMessage());
            }
        }
    }

    //THIS ABOUT KILLED ME, GAHHHHHHHHHHHHHh
    private void saveImage(boolean saveAs) {
        if (drawingPanel.getImage() == null) {
            JOptionPane.showMessageDialog(frame, "Nothing to save!");
            return;
        }
        try {
            File file = currentFile;
            String format = "png"; //setting png as default format

            if (saveAs || file == null) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save Image As");
                int result = chooser.showSaveDialog(frame);

                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                file = chooser.getSelectedFile();
                String filename = file.getAbsolutePath().toLowerCase();

                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    format = "jpg";
                } else if (filename.endsWith(".bmp")) {
                    format = "bmp";
                } else if (filename.endsWith(".png")) {
                    format = "png";
                } else { //defaulting to png if nothing saved
                    file = new File(filename + ".png");
                    format = "png";
                }
                currentFile = file;
            }
            ImageIO.write(drawingPanel.getImage(), format, file);
            JOptionPane.showMessageDialog(frame, "Image saved to" + file.getAbsolutePath());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to save image: " + ex.getMessage());
        }
    }

    private void showAboutDialog() {
        JDialog aboutDialog = new JDialog(frame, "About", true);
        aboutDialog.setSize(300, 200);
        aboutDialog.setLayout(new BorderLayout());

        JTextArea textArea = new JTextArea(
                "Chris' Magical Paint\n\n" +
                        "Version: 1.3.1\n" +
                        "Author: Lemonadegxhi\n" +
                        "Recreation of Microsoft Pain.");
        textArea.setEditable(false);
        textArea.setBackground(null);
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        textArea.setMargin(new Insets(10, 10, 10, 10));

        aboutDialog.add(textArea, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> aboutDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);

        aboutDialog.add(buttonPanel, BorderLayout.SOUTH);
        aboutDialog.setLocationRelativeTo(frame);
        aboutDialog.setVisible(true);
    }

    public void showApp() {
        frame.setVisible(true);
    }
}

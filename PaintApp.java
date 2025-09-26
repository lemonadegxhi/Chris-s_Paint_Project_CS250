import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Main Application class
 * <p>
 *     Handles the window, menus, tool options, saving/loading iamges,
 *     and integrates the DrawingPanel for canvas drawing.
 */

public class PaintApp {
    private JFrame frame;

    private JTabbedPane tabbedPane;

    //private DrawingPanel drawingPanel;
    private BufferedImage currentImage;
    private File currentFile;
    private boolean isDirty = false; //looking for unsaved changes.

    private Timer autoSaveTimer;
    private boolean autoSaveEnabled = true;
    private PaintWebServer webServer = new PaintWebServer();

    /**
     * Construcs the PaintApp and initializes the window, menus, and tools.
     */
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

        tabbedPane = new JTabbedPane();
        frame.add(tabbedPane, BorderLayout.CENTER);
        addNewTab();


        //MENU RIGHT HEREEEEEEEE
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newTabItem = new JMenuItem("New Tab");
        newTabItem.addActionListener(e -> addNewTab());

        JMenuItem openItem = new JMenuItem("Open Image");
        JMenuItem selectItem = new JMenuItem("Select");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveImage(false));
        JMenuItem saveAsItem = new JMenuItem("Save As");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveImage(true));
        JMenuItem closeItem = new JMenuItem("Close");

        JCheckBoxMenuItem autoSaveToggle = new JCheckBoxMenuItem("Enable Autosave", true);

        autoSaveToggle.addActionListener(e -> {
            autoSaveEnabled = autoSaveToggle.isSelected();
            if (autoSaveEnabled) {
                autoSaveTimer.start();
            } else {
                autoSaveTimer.stop();
            }
        });

    selectItem.addActionListener(e -> getCurrentPanel().setTool(DrawingTool.SELECT));
    copyItem.addActionListener(e -> {
        getCurrentPanel().copySelection();
    });
    pasteItem.addActionListener(e -> getCurrentPanel().setTool(DrawingTool.PASTE));

    openItem.addActionListener(e -> openImage());
    saveItem.addActionListener(e -> saveImage(false));
    saveAsItem.addActionListener(e -> saveImage(true));
    closeItem.addActionListener(e -> frame.dispose());

        fileMenu.add(newTabItem);
        fileMenu.add(openItem);

        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();
        fileMenu.add(closeItem);

        fileMenu.addSeparator();
        fileMenu.add(selectItem);
        fileMenu.add(copyItem);
        fileMenu.add(pasteItem);

        fileMenu.addSeparator();
        fileMenu.add(autoSaveToggle);

        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        //Edit Menu
        JMenu toolMenu = new JMenu("Tools");
        JMenuItem colorItem = new JMenuItem("Line Color");
        //JMenuItem sizeItem = new JMenuItem("Line Width");
        JMenuItem textItem = new JMenuItem("Text");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem clearCanvasItem = new JMenuItem("Clear Canvas");
        JMenuItem resizeCanvasItem = new JMenuItem("Resize Canvas");

        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));

        undoItem.addActionListener(e -> {
            getCurrentPanel().undo();
        });

        redoItem.addActionListener(e -> {
            getCurrentPanel().redo();
        });

        clearCanvasItem.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to clear the canvas?",
                    "Confirm Clear?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                getCurrentPanel().clearCanvas();
                setDirty(true);
            }
        });

        textItem.addActionListener(e -> {
            JPanel p = new JPanel(new BorderLayout(5, 5));
            JTextField textField = new JTextField(getCurrentPanel() != null ? "" : "");
            JPanel inner = new JPanel(new GridLayout(2, 2, 4, 4));
            inner.add(new JLabel("Text:"));
            inner.add(textField);
            inner.add(new JLabel("Size:"));
            SpinnerNumberModel model = new SpinnerNumberModel(24, 6, 200, 1);
            JSpinner sizeSpinner = new JSpinner(model);
            inner.add(sizeSpinner);

            p.add(inner, BorderLayout.CENTER);

            int res = JOptionPane.showConfirmDialog(frame, p, "Insert Text", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String text = textField.getText();
                int size = (Integer) sizeSpinner.getValue();
                if (text == null || text.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "No text entered.");
                } else {
                    getCurrentPanel().setTool(DrawingTool.TEXT);
                    getCurrentPanel().getToolOptions().setText(text);
                    getCurrentPanel().getToolOptions().setTextSize(size);
                }
            }
        });

        colorItem.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(frame, "Pick Line", Color.RED);
            if (newColor != null) {
                getCurrentPanel().setBrushColor(newColor);
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
                    getCurrentPanel().resizeCanvas(w, h);
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

        sizeItem.addChangeListener(e -> {
            int size = sizeItem.getValue();
            getCurrentPanel().setBrushSize(size);
        });

        getCurrentPanel().setBrushSize(sizeItem.getValue());

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
                getCurrentPanel().setTool(DrawingTool.SHAPE);
                getCurrentPanel().setShapeType(type);
                if (type == ShapeType.POLYGON) {
                    int sides = PolygonShape.askSides();
                    getCurrentPanel().getToolOptions().setPolygonSides(sides);
                }
                getCurrentPanel().setTool(DrawingTool.SHAPE);
                getCurrentPanel().setShapeType(type);
            });
            shapeMenu.add(shapeItem);
        }

        JMenuItem eyedropperItem = new JMenuItem("Eyedropper");
        eyedropperItem.addActionListener(e -> getCurrentPanel().setTool(DrawingTool.EYEDROPPER));

        JCheckBoxMenuItem dashedItem = new JCheckBoxMenuItem("Dashed Line");
        dashedItem.addActionListener(e -> getCurrentPanel().setDashed(dashedItem.isSelected()));

        JMenuItem drawItem = new JMenuItem("Draw");
        drawItem.addActionListener(e -> getCurrentPanel().setTool(DrawingTool.DRAW));

        JMenuItem eraserItem = new JMenuItem("Eraser");
        eraserItem.addActionListener(e -> getCurrentPanel().setTool(DrawingTool.ERASER));


        toolMenu.add(colorItem);
        toolMenu.add(eyedropperItem);
        toolMenu.add(drawItem);
        toolMenu.add(eraserItem);
        toolMenu.add(shapeMenu);
        toolMenu.add(sliderItem);
        toolMenu.add(dashedItem);
        toolMenu.add(resizeCanvasItem);

        toolMenu.addSeparator();
        toolMenu.add(undoItem);
        toolMenu.add(redoItem);
        toolMenu.add(clearCanvasItem);
        toolMenu.addSeparator();
        toolMenu.add(textItem);





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

        menuBar.add(toolMenu);
        menuBar.add(helpMenu);

        //Web Menu

        JMenu webMenu = new JMenu("Web");
        JMenuItem startWebItem = new JMenuItem("Start Web Server");
        JMenuItem stopWebItem = new JMenuItem("Stop Web Server");
        JMenuItem selectTabsItem = new JMenuItem("Select Tabs to Share");

        startWebItem.addActionListener(e -> {
            try {
                webServer.start(8000);
                updateWebShares();
                JOptionPane.showMessageDialog(frame, "Web server started at http://localhost:8000/");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Failed to start web server: " + ex.getMessage());
            }
        });

        stopWebItem.addActionListener(e -> {
            webServer.stop();
            JOptionPane.showMessageDialog(frame, "Web server stopped.");
        });

        selectTabsItem.addActionListener(e -> {
            int count = tabbedPane.getTabCount();
            JCheckBox[] boxes = new JCheckBox[count];
            JPanel panel = new JPanel(new GridLayout(count, 1));
            for (int i = 0; i < count; i++) {
                boxes[i] = new JCheckBox(tabbedPane.getTitleAt(i), true);
                panel.add(boxes[i]);
            }
            int res = JOptionPane.showConfirmDialog(frame, panel, "Select Tabs to Share", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                updateWebShares(Arrays.asList(boxes));
            }
        });

        webMenu.add(startWebItem);
        webMenu.add(stopWebItem);
        webMenu.add(selectTabsItem);
        menuBar.add(webMenu);




        frame.setJMenuBar(menuBar);

        autoSaveTimer = new javax.swing.Timer(120_000, e -> {
            if (autoSaveEnabled) {
                DrawingPanel panel = getCurrentPanel();
                if (panel.getImage() != null) {
                    if (currentFile != null) {
                        saveImage(false);
                    } else {
                        saveImage(true);
                    }
                }
            }
        });
        autoSaveTimer.start();
    }

    private void updateWebShares() {
        Map<String, BufferedImage> map = new HashMap<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            JScrollPane scroll = (JScrollPane) tabbedPane.getComponentAt(i);
            DrawingPanel panel = (DrawingPanel) scroll.getViewport().getView();
            if (panel.getImage() != null) {
                map.put("canvas" + (i + 1), panel.getImage());
            }
        }
        webServer.updateSharedImages(map);
    }
    private void updateWebShares(java.util.List<JCheckBox> boxes) {
        Map<String, BufferedImage> map = new HashMap<>();
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).isSelected()) {
                JScrollPane scroll = (JScrollPane) tabbedPane.getComponentAt(i);
                DrawingPanel panel = (DrawingPanel) scroll.getViewport().getView();
                if (panel.getImage() != null) {
                    map.put("canvas" + (i + 1), panel.getImage());
                }
            }
        }
        webServer.updateSharedImages(map);
    }

    private void addNewTab() {
        DrawingPanel drawingPanel = new DrawingPanel(() -> setDirty(true));
        JScrollPane scrollPane = new JScrollPane(drawingPanel);
        tabbedPane.addTab("Canvas " + (tabbedPane.getTabCount() + 1), scrollPane);
        tabbedPane.setSelectedComponent(scrollPane);
    }

    private DrawingPanel getCurrentPanel() {
        JScrollPane scroll = (JScrollPane) tabbedPane.getSelectedComponent();
        return (DrawingPanel) scroll.getViewport().getView();
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
                    getCurrentPanel().setImage(currentImage);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Failed to Open Image: " + ex.getMessage());
            }
        }
    }

    //THIS ABOUT KILLED ME, GAHHHHHHHHHHHHHh
    private void saveImage(boolean saveAs) {
        DrawingPanel drawingPanel = getCurrentPanel();
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
                        "Version: 1.4.6\n" +
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

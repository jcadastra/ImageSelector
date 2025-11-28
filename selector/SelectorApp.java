package selector;

import static selector.SelectionModel.SelectionState.NO_SELECTION;
import static selector.SelectionModel.SelectionState.PROCESSING;
import static selector.SelectionModel.SelectionState.SELECTED;
import static selector.SelectionModel.SelectionState.SELECTING;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import scissors.ScissorsSelectionModel;
import selector.SelectionModel.SelectionState;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by
     * `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;

    // New in A6
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;

    private JComboBox comboBox;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on
     * the Swing Event Dispatch Thread.  Does not show the application window (call `start()` to
     * do that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add status bar
        statusLabel = new JLabel();
        // TODO 1A: Add `statusLabel` to the bottom of our window.  Stylistic alteration of the
        //  label (i.e., custom fonts and colors) is allowed.
        //  See the BorderLayout tutorial [1] for example code that you can adapt.
        //  [1]: https://docs.oracle.com/javase/tutorial/uiswing/layout/border.html
        frame.add(statusLabel, BorderLayout.PAGE_END);

        // New in A6: Add progress bar
        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);

        // Add image component with scrollbars
        imgPanel = new ImagePanel();
        // TODO 1B: Replace the following line with code to put scroll bars around `imgPanel` while
        //  otherwise keeping it in the center of our window.  The scroll pane should also be given
        //  a moderately large preferred size (e.g., between 400 and 700 pixels wide and tall).
        //  The Swing Tutorial has lots of info on scrolling [1], but for this task you only need
        //  the basics from lecture.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/scrollpane.html

        JScrollPane scrollPane = new JScrollPane(imgPanel);
        scrollPane.setPreferredSize(new Dimension(500, 500));
        frame.add(scrollPane, BorderLayout.CENTER);

        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        // TODO 3E: Call `makeControlPanel()`, then add the result to the window next to the image.
        JPanel control = makeControlPanel();
        frame.add(control, BorderLayout.LINE_END);

        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach
     * listeners. Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);

        // TODO (embellishment): Assign keyboard shortcuts to menu items [1].  (1 point)
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html#mnemonic

        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
        // TODO 3D: Create and return a panel containing the Cancel, Undo, Reset, and Finish
        //  buttons (remember that these buttons are fields).  Activating the buttons should call
        //  `cancelProcessing()`, `undo()`, `reset()`, and `finishSelection()` on the selection
        //  model, respectively.  You may arrange and style the buttons however you like (so long as
        //  they are usable); a vertical grid [2] is a good place to start.  See `makeMenuBar()`
        //  above for inspiration.
        //  The JPanel tutorial [1] shows how to set a layout manager and add components to a panel.
        //  You are welcome to add borders, labels, and subpanels to improve its appearance.
        //  The Visual Guide to Layout Managers [3] might give you other ideas for how to arrange
        //  the buttons.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/panel.html
        //  [2] https://docs.oracle.com/javase/tutorial/uiswing/layout/grid.html
        //  [3] https://docs.oracle.com/javase/tutorial/uiswing/layout/visual.html

        // TODO A6.0a: Add a widget to your control panel allowing the user to choose which
        //  selection model to use.  We recommend using a `JComboBox` [1].  To start with, the user
        //  should be able to choose between the following options:
        //  1. Point-to-point (`PointToPointSelectionModel`).
        //  2. Intelligent scissors: gray (`ScissorsSelectionModel` with a "CrossGradMono" weight
        //     name).  You will need to `import scissors.ScissorsSelectionModel` to use this class.
        //  When an item is selected, you should construct a new `SelectionModel` of the appropriate
        //  class, passing the previous `model` object to the constructor so that any existing
        //  selection is preserved.  Then you should call `setSelectionModel()` with your new model
        //  object.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/combobox.html

        JPanel p = new JPanel(new BorderLayout());
        GridLayout layout = new GridLayout(5, 1);
        p.setLayout(layout);
        String[] options = {"Point-to-point", "Intelligent scissors:gray",
                "Intelligent scissors:color"};
        comboBox = new JComboBox(options);
        comboBox.setSelectedIndex(0);
        p.add(comboBox);
        cancelButton = new JButton("Cancel");
        p.add(cancelButton);
        undoButton = new JButton("Undo");
        p.add(undoButton);
        resetButton = new JButton("Reset");
        p.add(resetButton);
        finishButton = new JButton("Finish");
        p.add(finishButton);


        comboBox.addActionListener(e -> handleSelectionChange());

        cancelButton.addActionListener(e -> model.cancelProcessing());
        undoButton.addActionListener(e -> model.undo());
        resetButton.addActionListener(e -> model.reset());
        finishButton.addActionListener(e -> model.finishSelection());

        return p;
    }

    private void handleSelectionChange(){
        int index = comboBox.getSelectedIndex();

        if (index == 0){
            setSelectionModel(new PointToPointSelectionModel(model));
        } else if (index == 1){
            setSelectionModel(new ScissorsSelectionModel("CrossGradMono", model));
        } else if (index == 2){
            setSelectionModel(new ScissorsSelectionModel("ColorBand", model));
        }
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include:
     * * "state": Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // TODO A6.0b: Update the progress bar [1] as follows:
        //  * When the model transitions into the PROCESSING state, set the progress bar to
        //    "indeterminate" mode.  That way, the user sees that something is happening even before
        //    the model has an estimate of its progress.
        //  * When the model transitions to any other state, ensure that the progress bar's value is
        //    0 and that it is not in "indeterminate" mode.  The progress bar should look inert if
        //    the model isn't currently processing.
        //  * Upon receiving a "progress" property change, set the progress bar's value to the new
        //    progress value (which will be an integer in [0..100]) and ensure it is not in
        //    "indeterminate" mode.  You need to use the event object to get the new value.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html
        if ("state".equals(evt.getPropertyName())) {
                reflectSelectionState(model.state());
            if(model.state() == SelectionState.PROCESSING){
                processingProgress.setIndeterminate(true);
            } else {
                processingProgress.setIndeterminate(false);
                processingProgress.setValue(0);
            }
        } else if("progress".equals(evt.getPropertyName())){
            processingProgress.setIndeterminate(false);
            processingProgress.setValue((Integer) evt.getNewValue());
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());

        // TODO 3F: Enable/disable components (both buttons and menu items) as follows:
        //  * Cancel is only allowed when the selection is processing
        //  * Undo and Reset are not allowed when there is no selection (pending or complete)
        //  * Finish is only allowed when selecting
        //  * Saving is only allowed when the selection is complete
        //  The JButton tutorial [1] shows an example of enabling buttons in an event handler.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/button.html

        cancelButton.setEnabled(true);
        undoButton.setEnabled(true);
        resetButton.setEnabled(true);
        finishButton.setEnabled(true);
        saveItem.setEnabled(true);

        if (model.state() != PROCESSING) {
            cancelButton.setEnabled(false);
        }
        if (model.state() == NO_SELECTION){
            undoButton.setEnabled(false);
            resetButton.setEnabled(false);
        }
        if (model.state() != SELECTING){
            cancelButton.setEnabled(false);
            finishButton.setEnabled(false);
        }
        if(model.state() != SELECTED){
            saveItem.setEnabled(false);
        }

    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());

        // New in A6: Listen for "progress" events
        model.addPropertyChangeListener("progress", this);
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));

        // TODO 1C: Complete this method as specified by performing the following tasks:
        //  * Show an "open file" dialog using the above chooser [1].
        //  * If the user selects a file, read it into a BufferedImage [2], then set that as the
        //    current image (by calling `this.setImage()`).
        //  * If a problem occurs when reading the file (either an exception is thrown or null is
        //    returned), show an error dialog with a descriptive title and message [3].
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
        //  [2] https://docs.oracle.com/javase/tutorial/2d/images/loadimage.html
        //  [3] https://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html
        // TODO (embellishment): After a problem, re-show the open dialog.  By reusing the same
        //  chooser, the dialog will show the same directory as before the problem. (1 point)

        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null){
                    JOptionPane.showMessageDialog(frame, "Could not read the image at "
                            + file.getPath(),"Unsupported image format", JOptionPane.ERROR_MESSAGE);
                    openImage();
                } else {
                    imgPanel.setImage(img);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Could not read the image at "
                        + file.getPath(),"Unsupported image format", JOptionPane.ERROR_MESSAGE);
                openImage();
            }
        }
    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog.
     * Show an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

        // TODO 3G: Complete this method as specified by performing the following tasks:
        //  * Show a "save file" dialog using the above chooser [1].
        //  * If the user selects a file, write an image containing the selected pixels to the file.
        //  * If a problem occurs when opening or writing to the file, show an error dialog with the
        //    class of the exception as its title and the exception's message as its text [2].
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
        //  [2] https://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html
        // TODO (embellishment):
        //  * If the selected filename does not end in ".png", append that extension. (1 point)
        //  * Prompt with a yes/no/cancel dialog before overwriting a file. (1 point)
        //  * After an IOException, or after user selects "No" (instead of "Cancel") when prompted,
        //    re-show the save dialog.  By reusing the same chooser, the dialog will show the same
        //    directory as before the problem. (1 point)
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // Ensure the file has a ".png" extension
            if (!file.getName().toLowerCase().endsWith(".png")) {
                // Append ".png" extension if it's missing
                file = new File(file.getAbsolutePath() + ".png");
            }

            // Check if the file already exists and prompt the user before overwriting
            if (file.exists()) {
                int overwriteConfirmation = JOptionPane.showOptionDialog(frame,
                        "File already exists. Do you want to overwrite it?",
                        "File Exists",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"Yes", "No", "Cancel"},
                        "Yes");
                if (overwriteConfirmation == JOptionPane.CANCEL_OPTION) {
                    // User canceled
                    return; // Exit without saving
                } else if (overwriteConfirmation == JOptionPane.NO_OPTION) {
                    // User chose not to overwrite, so re-show the save dialog
                    saveSelection();
                    return;
                }
            }

            try {
                // Get the current image from the ImagePanel
                OutputStream out = new FileOutputStream(file);
                // Write the current image to the file
                model.saveSelection(out);
                out.close();
            } catch (IOException ex) {
                // Show an error dialog if an IOException occurs
                JOptionPane.showMessageDialog(frame, "Destination selected is not supported",
                        ex.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);

                // Re-show the save dialog if IOException occurs
                saveSelection();
            }
        }
    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}

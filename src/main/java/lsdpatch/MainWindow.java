package lsdpatch;

import kitEditor.KitEditor;
import kitEditor.KitPlayer;
import net.miginfocom.swing.MigLayout;
import sun.applet.Main;
import utils.JFileChooserFactory;
import utils.RomUtilities;

import javax.swing.*;
import java.io.File;
import java.io.RandomAccessFile;

public class MainWindow extends JFrame {
    private final JButton openRomButton = new JButton("Open ROM", UIManager.getIcon("FileView.directoryIcon"));
    private JButton saveRomButton = new JButton("Save ROM", UIManager.getIcon("FileView.floppyDriveIcon"));

    private JButton kitEditorButton = new JButton("Kit Editor");
    private JButton paletteEditorButton = new JButton("Palette Editor");
    private JButton fontEditorButton = new JButton("Font Editor");

    private JButton kitPlayerButton = new JButton("Kit Player");


    private JLabel romStatus = new JLabel();
    private byte[] romImage = null;

    public MainWindow() {
        setTitle("LSDPatcher " + LSDPatcher.getVersion());
        setLayout(new MigLayout("", "[grow][grow][grow]", "[][][grow][grow][]"));

        romStatus.setBorder(BorderFactory.createLoweredSoftBevelBorder());

        add(openRomButton, "sg icon,split 2, span,grow x");
        add(saveRomButton, "sg icon, wrap, grow x");
        add(new JSeparator(), "span,grow x,wrap");

        add(kitEditorButton, "center");
        add(paletteEditorButton, "center");
        add(fontEditorButton, "center,wrap");
        add(kitPlayerButton, "center");

        add(romStatus, "south");
        setFileStatus(null);
        setActionListeners();
        enableAllActions(false);
        pack();

        setMinimumSize(getPreferredSize());
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void setActionListeners() {
        openRomButton.addActionListener(e -> openRom());
        kitPlayerButton.addActionListener(e -> openKitPlayer());
    }

    private void openKitPlayer() {
        KitPlayer player = new KitPlayer(romImage);
        player.setVisible(true);
    }

    private boolean loadRom(File gbFile) {
        try {
            byte[] tempRomImage = new byte[RomUtilities.BANK_SIZE * RomUtilities.BANK_COUNT];
            RandomAccessFile romFile = new RandomAccessFile(gbFile, "r");
            romFile.readFully(tempRomImage);
            romFile.close();
            romImage = tempRomImage;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void enableAllActions(boolean enabled) {
        saveRomButton.setEnabled(enabled);
        kitEditorButton.setEnabled(enabled);
        fontEditorButton.setEnabled(enabled);
        paletteEditorButton.setEnabled(enabled);
        kitPlayerButton.setEnabled(enabled);
    }

    private void setFileStatus(File loadedFile) {
        if (loadedFile == null) {
            romStatus.setText("No ROM loaded.");
        } else {
            romStatus.setText("ROM loaded : " + loadedFile.getAbsolutePath());
        }
    }

    private void openRom() {
        JFileChooser chooser = JFileChooserFactory.createChooser("Load ROM Image", JFileChooserFactory.FileType.Gb, JFileChooserFactory.FileOperation.Load);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (f != null) {
                JFileChooserFactory.recordNewBaseFolder(f.getParent());
                if (loadRom(f)) {
                    enableAllActions(true);
                    setFileStatus(f);
                }
            }
        }
    }
}

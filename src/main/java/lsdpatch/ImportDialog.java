package lsdpatch;

import net.miginfocom.swing.MigLayout;
import utils.JFileChooserFactory;
import utils.RomUtilities;

import javax.swing.*;
import java.io.File;
import java.io.RandomAccessFile;

class ImportDialog extends JFrame {

    private final JLabel filePathLabel = new JLabel("No ROM selected.");
    private final JLabel processLabel = new JLabel("Please import a ROM.");
    private final JButton openRomButton = new JButton ("Select");

    private final JCheckBox importKits = new JCheckBox("kits");
    private final JCheckBox importPalettes = new JCheckBox("palettes");
    private final JCheckBox importFonts = new JCheckBox("fonts");
    private final JButton importButton = new JButton("Import!");

    private byte[] targetRomFile;
    private byte[] romImage;

    private String targetRomPath;
    private String targetRomFilename;

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

    private void openRom() {
        JFileChooser chooser = JFileChooserFactory.createChooser("Select ROM to import from", JFileChooserFactory.FileType.Gb, JFileChooserFactory.FileOperation.Load);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (f != null) {
                JFileChooserFactory.recordNewBaseFolder(f.getParent());
                if (loadRom(f)) {
                    filePathLabel.setText(f.getAbsolutePath());
                    StringBuilder processBuilder = new StringBuilder();
                    processBuilder.append("<html>");

                    if (f.getName().equalsIgnoreCase(targetRomFilename)) {
                        processBuilder.append(f.getAbsolutePath());
                        processBuilder.append(" ⇨ ");
                        processBuilder.append(targetRomPath);
                    }
                    else {
                        processBuilder.append(f.getName());
                        processBuilder.append(" ⇨ ");
                        processBuilder.append(targetRomFilename);
                    }
                    processBuilder.append("</html>");
                    processLabel.setText(processBuilder.toString());
                    processLabel.setVisible(true);
                    enableImportActions(true);
                }
            }
        }
    }

    private void enableImportActions(boolean enabled) {
        importFonts.setEnabled(enabled);
        importKits.setEnabled(enabled);
        importPalettes.setEnabled(enabled);
        importButton.setEnabled(enabled);
    }

    ImportDialog(byte[] targetRomFile, String gbFilePath) {

        this.targetRomFile = targetRomFile;
        targetRomPath = gbFilePath;
        targetRomFilename = new File(gbFilePath).getName();

        openRomButton.addActionListener(e -> openRom());
        processLabel.setVisible(false);

        JPanel comboButton = new JPanel();
        comboButton.setLayout(new BoxLayout(comboButton, BoxLayout.X_AXIS));
        comboButton.add(openRomButton);
        comboButton.add(Box.createHorizontalStrut(3));
        comboButton.add(filePathLabel);


        setLayout(new MigLayout("fill"));

        add(comboButton, "growx, wrap");
        add(importFonts, "growx, wrap");
        add(importKits, "growx, wrap");
        add(importPalettes, "growx, wrap");
        add(processLabel, "growx, wrap");
        add(importButton, "growx, wrap");

        enableImportActions(false);
        pack();
        setMinimumSize(getPreferredSize());
    }
}

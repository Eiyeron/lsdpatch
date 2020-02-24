package lsdpatch;

import kitEditor.KitEditor;
import kitEditor.KitPlayer;
import net.miginfocom.swing.MigLayout;
import utils.GlobalHolder;
import utils.JFileChooserFactory;
import utils.RomUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.prefs.Preferences;

class MainWindow extends JFrame {
    private final JButton openRomButton = new JButton("Open ROM", UIManager.getIcon("FileView.directoryIcon"));
    private final JButton saveRomButton = new JButton("Save ROM", UIManager.getIcon("FileView.floppyDriveIcon"));
    private final JButton importFromButton = new JButton("Import...", UIManager.getIcon("FileChooser.upFolderIcon"));

    private final JButton kitEditorButton = new JButton("Kit Editor");
    private final JButton paletteEditorButton = new JButton("Palette Editor");
    private final JButton fontEditorButton = new JButton("Font Editor");

    private final JButton kitPlayerButton = new JButton("Kit Player");

    private final JLabel romStatus = new JLabel();

    private final JPanel recentFiles = new JPanel();
    private String loadedFilePath = null;
    private byte[] romImage = null;

    private KitEditor kitEditor = null;
    private KitPlayer kitPlayer = null;
    private ImportDialog importDialog = null;

    private class Hyperlink extends JLabel {
        Hyperlink(String text) {
            setText("<html><a href=\"#\">"+text+"</a></html>");
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    private void attachOpenActionToHyperlink(Hyperlink link, String path) {
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRomAction(new File(path));
            }
        });

    }

    private void resetFilePanel() {
        recentFiles.removeAll();
        String recentFilesLoaded = GlobalHolder.get(Preferences.class).get("recentFiles", "");
        String[] recentFileList = recentFilesLoaded.split("@@");
        if (recentFileList.length > 1 || !recentFileList[0].equals("")) {
            for (String path : recentFileList) {

                // Skipping all missing paths.
                File testFile = new File(path);
                if (!testFile.exists()) {
                    continue;
                }

                Hyperlink romShortcut = new Hyperlink(path);
                attachOpenActionToHyperlink(romShortcut, path);
                recentFiles.add(romShortcut, "span");
            }
        }
        else {
            JLabel noRecentFilesLabel = new JLabel("No recent files.");
            Font italicFont = new Font("Sans", Font.ITALIC, 12);
            noRecentFilesLabel.setFont(italicFont);
            recentFiles.add(noRecentFilesLabel, "center, grow y");
        }

    }

    private void setupRecentFilesPanel() {

        add(new JLabel("Recent Files"), "span, wrap");

        recentFiles.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        recentFiles.setLayout(new MigLayout("", "[grow]", ""));

        add(recentFiles, "grow, span");
        resetFilePanel();
    }

    MainWindow() {
        setTitle("LSDPatcher " + LSDPatcher.getVersion());
        setLayout(new MigLayout("", "[grow][grow][grow]", "[][][grow][grow][][grow][]"));

        romStatus.setBorder(BorderFactory.createLoweredSoftBevelBorder());

        add(openRomButton, "sg icon,split 3, span,grow x");
        add(saveRomButton, "sg icon, grow x");
        add(importFromButton, "sg icon, wrap, grow x");
        add(new JSeparator(), "span,grow x,wrap");

        add(kitEditorButton, "center");
        add(paletteEditorButton, "center");
        add(fontEditorButton, "center,wrap");

        add(kitPlayerButton, "center,wrap");

        setupRecentFilesPanel();

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
        saveRomButton.addActionListener(e -> saveRom());
        importFromButton.addActionListener(e -> importDialog());
        kitEditorButton.addActionListener(e -> openKitEditor());
        kitPlayerButton.addActionListener(e -> openKitPlayer());
    }

    private void importDialog() {
        if (importDialog != null) {
            importDialog.dispose();
        }
        importDialog = new ImportDialog(romImage, loadedFilePath);
        importDialog.setVisible(true);
    }

    private void openKitEditor() {
        if (kitEditor != null) {
            kitEditor.dispose();
        }
        kitEditor = new KitEditor(romImage);
        kitEditor.setVisible(true);
    }

    private void openKitPlayer() {
        if (kitPlayer != null) {
            kitPlayer.dispose();
        }

        kitPlayer = new KitPlayer(romImage);
        kitPlayer.setVisible(true);
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
        importFromButton.setEnabled(enabled);
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
            loadedFilePath = loadedFile.getAbsolutePath();
        }
    }


    private void updateRecentFiles(String newPath) {
        String absoluteNewPath = new File(newPath).getAbsolutePath();

        String recentFilesLoaded = GlobalHolder.get(Preferences.class).get("recentFiles", "");
        ArrayList<String> recentFileList = new ArrayList<>(Arrays.asList(recentFilesLoaded.split("@@")));
        if (recentFileList.size() == 1 && recentFileList.get(0).equals("")) {
            GlobalHolder.get(Preferences.class).put("recentFiles", absoluteNewPath);
        }

        // Trim first files not found
        recentFileList.removeIf(f -> !new File(f).exists());

        Optional<String> path = recentFileList.stream().filter(f -> f.equals(absoluteNewPath)).findFirst();
        recentFileList.add(0, absoluteNewPath);


        ArrayList<String> filtered = new ArrayList<>();
        filtered.add(recentFileList.get(0));
        for (int i = 1; i < recentFileList.size(); ++i) {
            String currentPath = recentFileList.get(i);

            boolean isFoundInFiltered = false;
            for (String filteredPath : filtered) {
                if (filteredPath.equals(currentPath)) {
                    isFoundInFiltered = true;
                    break;
                }
            }
            if (!isFoundInFiltered) {
                filtered.add(currentPath);
            }
        }

        StringBuilder result = new StringBuilder(filtered.get(0));
        for (int i = 1; i < filtered.size(); ++i) {
            result.append("@@").append(filtered.get(i));
        }

        GlobalHolder.get(Preferences.class).put("recentFiles", result.toString());


    }

    private void openRomAction(File f){
        if (f != null) {
            JFileChooserFactory.recordNewBaseFolder(f.getParent());
            if (loadRom(f)) {
                updateRecentFiles(f.getAbsolutePath());
                resetFilePanel();
                enableAllActions(true);
                setFileStatus(f);

                // TODO : close or reload child windows on load.
                if (kitEditor != null) {
                    openKitEditor();
                }
                if (kitPlayer != null) {
                    openKitPlayer();
                }
            }
        }
    }

    private void openRom() {
        JFileChooser chooser = JFileChooserFactory.createChooser("Load ROM Image", JFileChooserFactory.FileType.Gb, JFileChooserFactory.FileOperation.Load);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            openRomAction(f);
        }
    }

    private void saveRom() {
        JFileChooser chooser = JFileChooserFactory.createChooser("Save ROM image", JFileChooserFactory.FileType.Gb, JFileChooserFactory.FileOperation.Save);

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File f = chooser.getSelectedFile();
                JFileChooserFactory.recordNewBaseFolder(f.getParent());

                RomUtilities.fixChecksum(romImage);
                RandomAccessFile romFile = new RandomAccessFile(f, "rw");
                romFile.write(romImage);
                romFile.close();
                setTitle(f.getAbsoluteFile().toString() + " - lsdpatch.LSDPatcher Redux v" + LSDPatcher.getVersion());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

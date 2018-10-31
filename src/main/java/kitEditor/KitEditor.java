package kitEditor;/* Copyright (C) 2001-2011 by Johan Kotlinski

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE. */

import fontEditor.FontEditor;
import lsdpatch.LSDPatcher;
import net.miginfocom.swing.MigLayout;
import paletteEditor.PaletteEditor;
import structures.LSDJFont;
import utils.FileDrop;
import utils.JFileChooserFactory;
import utils.JFileChooserFactory.FileOperation;
import utils.JFileChooserFactory.FileType;
import utils.RomUtilities;
import utils.SampleCanvas;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class KitEditor extends JFrame {
    private static final long serialVersionUID = -3993608561466542956L;
    private JPanel contentPane;
    private final JPanel jPanel1 = new JPanel();
    //JLabel fileNameLabel = new JLabel();
    private int prevBankBoxIndex = -1;
    private final JComboBox<String> bankBox = new JComboBox<>();
    private final JList<String> instrList = new JList<>();
    private final PaletteEditor paletteEditor = new PaletteEditor();
    private final FontEditor fontEditor = new FontEditor();

    private static final int MAX_SAMPLES = 15;

    private final java.awt.event.ActionListener bankBoxListener =
            e -> bankBox_actionPerformed();

    private int totSampleSize = 0;

    private byte[] romImage = null;

    private Sample[] samples = new Sample[MAX_SAMPLES];

    private JMenuItem importKitsItem;
    private final JButton loadKitButton = new JButton();
    private final JButton exportKitButton = new JButton();
    private final JButton exportSampleButton = new JButton();
    private final JButton renameKitButton = new JButton();
    private final JTextArea kitTextArea = new JTextArea();
    private final JButton addSampleButton = new JButton();
    private final JButton dropSampleButton = new JButton();
    private final JButton saveROMButton = new JButton();
    private final JLabel kitSizeLabel = new JLabel();
    private final SampleCanvas sampleView = new SampleCanvas();

    private final JToggleButton playSpeedToggle = new JToggleButton();

    private final JMenuBar menuBar = new JMenuBar();

    private final Sound soundPlayer = new Sound();

    class KitFileFilter implements java.io.FilenameFilter {
        public boolean accept(java.io.File dir, String name) {
            return name.toLowerCase().endsWith(".kit");
        }
    }

    class WavFileFilter implements java.io.FilenameFilter {
        public boolean accept(java.io.File dir, String name) {
            return name.toLowerCase().endsWith(".wav");
        }
    }

    private void emptyInstrList() {
        String listData[] = {"1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.",
                "12.", "13.", "14.", "15."};
        instrList.setListData(listData);
    }

    public KitEditor( byte[] romImage) {
        this.romImage = romImage;

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateRomView();
    }

    private void buildMenus() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        fileMenu.addSeparator();

        importKitsItem = new JMenuItem("Import kits from ROM...", KeyEvent.VK_I);
        importKitsItem.addActionListener(e3 -> importKits_actionPerformed());

        fileMenu.add(importKitsItem);

        // -----

        JMenu paletteMenu = new JMenu("Palette");
        paletteMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(paletteMenu);
        JMenuItem editPaletteItem = new JMenuItem("Edit Palettes...", KeyEvent.VK_P);
        editPaletteItem.addActionListener(e -> paletteEditor.setVisible(true));
        paletteMenu.add(editPaletteItem);

        JMenu fontMenu = new JMenu("Font");
        fontMenu.setMnemonic(KeyEvent.VK_O);
        menuBar.add(fontMenu);
        JMenuItem editFontItem = new JMenuItem("Edit Fonts...", KeyEvent.VK_F);
        editFontItem.addActionListener(e -> fontEditor.setVisible(true));
        fontMenu.add(editFontItem);

        setJMenuBar(menuBar);
    }

    private void setListeners() {
        bankBox.addActionListener(bankBoxListener);

        instrList.addMouseListener(new MouseAdapter() {
            public void mousePressed (MouseEvent e) {
                if (romImage != null) {
                    int index = instrList.locationToIndex(e.getPoint());

                    boolean hasIndex = (index > -1);
                    if (hasIndex) {
                        playSample(index);
                    }
                    dropSampleButton.setEnabled(hasIndex);
                    exportSampleButton.setEnabled(hasIndex);
                }
            }
        });

        loadKitButton.addActionListener(e -> loadKitButton_actionPerformed());
        exportKitButton.addActionListener(e -> exportKitButton_actionPerformed());
        renameKitButton.addActionListener(e1 -> renameKitButton_actionPerformed());
        exportSampleButton.addActionListener(e -> exportSample());

        addSampleButton.addActionListener(e -> selectSampleToAdd());
        dropSampleButton.addActionListener(e -> dropSample());
    }

    /**
     * Component initialization
     */
    private void jbInit() {
        //setIconImage(Toolkit.getDefaultToolkit().createImage(Frame1.class.getResource("[Your Icon]")));
        setTitle("Kit Editor");
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new MigLayout("",
                "[150:60%:,grow][200:40%:,fill,grow]",
                ""));

        createFileDrop();


        instrList.setBorder(BorderFactory.createEtchedBorder());

        JPanel kitContainer = new JPanel();
        TitledBorder kitContainerBorder = new TitledBorder(BorderFactory.createEtchedBorder(), "ROM Image");
        kitContainer.setBorder(kitContainerBorder);
        kitContainer.setLayout(new MigLayout("", "[grow,fill]", ""));
        kitContainer.add(bankBox, "grow,wrap");
        kitContainer.add(instrList, "grow,wrap");
        kitContainer.add(kitSizeLabel, "grow,wrap");
        kitContainer.setMinimumSize(kitContainer.getPreferredSize());

        loadKitButton.setText("Load Kit");
        exportKitButton.setText("Export Kit");
        renameKitButton.setText("Rename Kit");

        kitTextArea.setBorder(BorderFactory.createEtchedBorder());

        exportSampleButton.setText("Export Sample");
        addSampleButton.setText("Add sample");
        dropSampleButton.setText("Drop sample");

        saveROMButton.setText("Save ROM");


        contentPane.add(kitContainer, "grow, cell 0 0, spany");
        contentPane.add(loadKitButton, "wrap");
        contentPane.add(exportKitButton, "wrap");
        contentPane.add(kitTextArea, "grow,split 2");
        contentPane.add(renameKitButton, "wrap 10");

        contentPane.add(exportSampleButton, "wrap");
        contentPane.add(addSampleButton, "span 2,wrap");
        contentPane.add(dropSampleButton, "span 2,wrap 10");
        contentPane.add(saveROMButton, "span 2,wrap push");
        contentPane.add(playSpeedToggle, "growy,split 2");
        contentPane.add(new JLabel("Play half speed"), "grow, wrap");
        contentPane.add(sampleView, "grow, span 2,wmin 10, hmin 64");

        setMinimumSize(getPreferredSize());
        pack();

        setListeners();
        buildMenus();
    }

    private void createFileDrop() {
        new FileDrop(contentPane, files -> {
            for (File file : files) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".wav")) {
                    if (romImage == null) {
                        JOptionPane.showMessageDialog(contentPane,
                                "Open .gb file before adding samples.",
                                "Can't add sample!",
                                JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    addSample(file);
                } else if (fileName.endsWith(".kit")) {
                    if (romImage == null) {
                        JOptionPane.showMessageDialog(contentPane,
                                "Open .gb file before adding samples.",
                                "Can't add sample!",
                                JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    loadKit(file);
                } else {
                    JOptionPane.showMessageDialog(contentPane,
                            "Unknown file type!",
                            "File error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });
    }

    private byte[] get4BitSamples(int index, boolean halfSpeed) {
        int offset = getSelectedROMBank() * RomUtilities.BANK_SIZE + index * 2;
        int start = (0xff & romImage[offset]) | ((0xff & romImage[offset + 1]) << 8);
        int stop = (0xff & romImage[offset + 2]) | ((0xff & romImage[offset + 3]) << 8);
        if (stop <= start) {
            return null;
        }
        byte[] arr;
        if (halfSpeed) {
            arr = new byte[(stop - start) * 2];
            for (int i = start; i < stop; ++i) {
                arr[(i - start) * 2] = romImage[getSelectedROMBank() * RomUtilities.BANK_SIZE - RomUtilities.BANK_SIZE + i];
                arr[(i - start) * 2 + 1] = romImage[getSelectedROMBank() * RomUtilities.BANK_SIZE - RomUtilities.BANK_SIZE + i];
            }

        } else {
            arr = new byte[stop - start];
            for (int i = start; i < stop; ++i) {
                arr[i - start] = romImage[getSelectedROMBank() * RomUtilities.BANK_SIZE - RomUtilities.BANK_SIZE + i];
            }
        }
        return arr;
    }

    private void playSample(int index) {

        byte[] nibblesForRepaint;
        byte[] nibblesForPlayback;
        if (playSpeedToggle.isSelected()) {
            nibblesForPlayback = get4BitSamples(index, true);
            nibblesForRepaint = get4BitSamples(index, false);
        } else {
            nibblesForPlayback = get4BitSamples(index, false);
            nibblesForRepaint = nibblesForPlayback;

        }
        if (nibblesForPlayback == null) {
            return;
        }
        try {
            soundPlayer.play(nibblesForPlayback);
            sampleView.setBufferContent(nibblesForRepaint);
            sampleView.repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Audio error",
                    JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
        }
    }

    private boolean isKitBank(int a_bank) {
        int l_offset = (a_bank) * RomUtilities.BANK_SIZE;
        byte l_char_1 = romImage[l_offset++];
        byte l_char_2 = romImage[l_offset];
        return (l_char_1 == 0x60 && l_char_2 == 0x40);
    }

    private boolean isEmptyKitBank(int a_bank) {
        int l_offset = (a_bank) * RomUtilities.BANK_SIZE;
        byte l_char_1 = romImage[l_offset++];
        byte l_char_2 = romImage[l_offset];
        return (l_char_1 == -1 && l_char_2 == -1);
    }

    private String getKitName(int a_bank) {
        if (isEmptyKitBank(a_bank)) {
            return "Empty";
        }

        byte buf[] = new byte[6];
        int offset = (a_bank) * RomUtilities.BANK_SIZE + 0x52;
        for (int i = 0; i < 6; i++) {
            buf[i] = romImage[offset++];
        }
        return new String(buf);
    }

    private void updateRomView() {
        int tmp = bankBox.getSelectedIndex();
        bankBox.removeActionListener(bankBoxListener);
        bankBox.removeAllItems();

        int l_ui_index = 0;
        for (int bankNo = 0; bankNo < RomUtilities.BANK_COUNT; bankNo++) {
            if (isKitBank(bankNo) || isEmptyKitBank(bankNo)) {
                bankBox.addItem(Integer.toHexString(++l_ui_index).toUpperCase() + ". " + getKitName(bankNo));
            }
        }
        bankBox.setSelectedIndex(tmp == -1 ? 0 : tmp);
        bankBox.addActionListener(bankBoxListener);
        updateBankView();
    }

    private int m_selected = -1;

    private int getSelectedUiBank() {
        if (bankBox.getSelectedIndex() > -1) {
            m_selected = bankBox.getSelectedIndex();
        }
        return m_selected;
    }

    private int getSelectedROMBank() {
        int l_rom_bank = 0;
        int l_ui_bank = 0;

        for (; ; ) {
            if (isKitBank(l_rom_bank) || isEmptyKitBank(l_rom_bank)) {
                if (getSelectedUiBank() == l_ui_bank) {
                    return l_rom_bank;
                }
                l_ui_bank++;
            }
            l_rom_bank++;
        }

    }

    private int getROMOffsetForSelectedBank() {
        return getSelectedROMBank() * RomUtilities.BANK_SIZE;
    }

    private void updateBankView() {
        if (isEmptyKitBank(getSelectedROMBank())) {
            emptyInstrList();
            return;
        }

        byte buf[] = new byte[3];
        String s[] = new String[15];

        totSampleSize = 0;

        int bankOffset = getROMOffsetForSelectedBank();
        instrList.removeAll();
        //do banks

        //update names
        int offset = bankOffset + 0x22;
        for (int instrNo = 0; instrNo < MAX_SAMPLES; instrNo++) {
            boolean isNull = false;
            for (int i = 0; i < 3; i++) {
                buf[i] = romImage[offset++];
                if (isNull) {
                    buf[i] = '-';
                } else {
                    if (buf[i] == 0) {
                        buf[i] = '-';
                        isNull = true;
                    }
                }
            }
            s[instrNo] = (instrNo + 1) + ". " + new String(buf);
            Sample sample = samples[instrNo];
            if (sample != null) {
                int sampleLength = (sample.length() / 2 - sample.length() / 2 % 0x10);
                totSampleSize += sampleLength;
                s[instrNo] += " (" + Integer.toHexString(sampleLength) + ")";
            }
        }
        instrList.setListData(s);

        updateKitSizeLabel();
        addSampleButton.setEnabled(firstFreeSampleSlot() != -1);
    }

    private void updateKitSizeLabel() {
        int sampleSize = totSampleSize;
        kitSizeLabel.setText(Integer.toHexString(sampleSize) + "/3fa0 bytes used");
        boolean tooFull = sampleSize > 0x3fa0;

        Color c = tooFull ? Color.red : Color.black;
        kitSizeLabel.setForeground(c);
        instrList.setForeground(c);
    }

    private void bankBox_actionPerformed() {
        int index = bankBox.getSelectedIndex();
        if (prevBankBoxIndex == index) {
            return;
        }
        // Switched bank.
        prevBankBoxIndex = index;
        flushWavFiles();
        createSamplesFromRom();
        updateBankView();
    }

    private String getRomSampleName(int index) {
        int offset = getROMOffsetForSelectedBank() + 0x22 + index * 3;
        String name = "";
        name += (char) romImage[offset++];
        name += (char) romImage[offset++];
        name += (char) romImage[offset];
        return name;
    }

    private void createSamplesFromRom() {
        for (int sampleIt = 0; sampleIt < MAX_SAMPLES; ++sampleIt) {
            if (samples[sampleIt] != null) {
                continue;
            }
            byte[] nibbles = get4BitSamples(sampleIt, false);

            if (nibbles != null) {
                String name = getRomSampleName(sampleIt);
                samples[sampleIt] = Sample.createFromNibbles(nibbles, name);
            } else {
                samples[sampleIt] = null;
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private int importKits(File f) {
        try {
            int outBank = 0;
            int copiedBankCount = 0;
            FileInputStream in = new FileInputStream(f.getAbsolutePath());
            while (in.available() > 0) {
                byte[] inBuf = new byte[RomUtilities.BANK_SIZE];
                in.read(inBuf);
                if (inBuf[0] == 0x60 && inBuf[1] == 0x40) {
                    //is kit bank
                    outBank++;
                    while (!isKitBank(outBank) && !isEmptyKitBank(outBank)) {
                        outBank++;
                    }
                    int outPtr = outBank * RomUtilities.BANK_SIZE;
                    for (int i = 0; i < RomUtilities.BANK_SIZE; i++) {
                        romImage[outPtr++] = inBuf[i];
                    }
                    copiedBankCount++;
                }
            }
            updateRomView();
            in.close();
            return copiedBankCount;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return 0;
    }


    private File importRomSelect() {
        JFileChooser chooser = JFileChooserFactory.createChooser("Select ROM to import from", FileType.Gb, FileOperation.Load);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {

            File f = chooser.getSelectedFile();
            JFileChooserFactory.recordNewBaseFolder(f.getParent());
            return f;
        }
        return null;
    }

    private void importKits_actionPerformed() {
        File f = importRomSelect();
        if (f != null) {
            int amountOfCopiedKits = importKits(f);
            JOptionPane.showMessageDialog(this,
                    "Imported " + amountOfCopiedKits + " kits!",
                    "Kit import result.", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exportKitButton_actionPerformed() {
        JFileChooser chooser = JFileChooserFactory.createChooser("Export kit", FileType.Kit, FileOperation.Save);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {

            try {
                File f = chooser.getSelectedFile();
                JFileChooserFactory.recordNewBaseFolder(f.getParent());
                byte buf[] = new byte[RomUtilities.BANK_SIZE];
                int offset = getROMOffsetForSelectedBank();
                RandomAccessFile bankFile = new RandomAccessFile(f, "rw");

                for (int i = 0; i < buf.length; i++) {
                    buf[i] = romImage[offset++];
                }
                bankFile.write(buf);
                bankFile.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
                        JOptionPane.ERROR_MESSAGE);
            }
            updateRomView();
        }

    }

    private void loadKit(File kitFile) {
        try {
            byte buf[] = new byte[RomUtilities.BANK_SIZE];
            int offset = getROMOffsetForSelectedBank();
            RandomAccessFile bankFile = new RandomAccessFile(kitFile, "r");
            bankFile.readFully(buf);

            for (byte aBuf : buf) {
                romImage[offset++] = aBuf;
            }
            bankFile.close();
            flushWavFiles();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
                    JOptionPane.ERROR_MESSAGE);
        }
        updateRomView();
    }

    private void loadKitButton_actionPerformed() {
        FileDialog dialog = new FileDialog(this, "Load kit", FileDialog.LOAD);
        dialog.setFilenameFilter(new KitFileFilter());
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            loadKit(new File(dialog.getDirectory(), dialog.getFile()));
        }
    }

    private String dropExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(0, i);
        }
        return ext;
    }

    private void createKit() {
        //clear all bank
        int offset = getROMOffsetForSelectedBank() + 2;
        int max_offset = getROMOffsetForSelectedBank() + RomUtilities.BANK_SIZE;
        while (offset < max_offset) {
            romImage[offset++] = 0;
        }

        //clear kit name
        offset = getROMOffsetForSelectedBank() + 0x52;
        for (int i = 0; i < 6; i++) {
            romImage[offset++] = ' ';
        }

        //clear instrument names
        offset = getROMOffsetForSelectedBank() + 0x22;
        for (int i = 0; i < 15; i++) {
            romImage[offset++] = 0;
            romImage[offset++] = '-';
            romImage[offset++] = '-';
        }

        flushWavFiles();

        updateRomView();
    }

    private void flushWavFiles() {
        samples = new Sample[MAX_SAMPLES];
    }

    private void renameKitButton_actionPerformed() {
        int offset = getROMOffsetForSelectedBank() + 0x52;
        String s = kitTextArea.getText().toUpperCase();
        for (int i = 0; i < 6; i++) {
            if (i < s.length()) {
                romImage[offset++] = (byte) s.charAt(i);
            } else {
                romImage[offset++] = ' ';
            }
        }
        compileKit();
        updateRomView();
    }

    private int firstFreeSampleSlot() {
        for (int sampleIt = 0; sampleIt < MAX_SAMPLES; ++sampleIt) {
            if (samples[sampleIt] == null) {
                return sampleIt;
            }
        }
        return -1;
    }

    private void addSample(File wavFile) {
        if (isEmptyKitBank(getSelectedROMBank())) {
            createKit();
        }
        if (firstFreeSampleSlot() == -1) {
            JOptionPane.showMessageDialog(contentPane,
                    "Can't add sample, kit is full!",
                    "Kit full",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        int offset = getROMOffsetForSelectedBank() + 0x22 +
                firstFreeSampleSlot() * 3;
        String s = dropExtension(wavFile).toUpperCase();

        for (int i = 0; i < 3; ++i) {
            if (s.length() > 0) {
                romImage[offset] = (byte) s.charAt(i);
            } else {
                romImage[offset] = '-';
            }

            offset++;
        }

        Sample sample = Sample.createFromWav(wavFile);
        if (sample == null) {
            return;
        }
        samples[firstFreeSampleSlot()] = sample;

        compileKit();
        updateRomView();
    }

    private void selectSampleToAdd() {
        FileDialog dialog = new FileDialog(this, "Load sample",
                FileDialog.LOAD);
        dialog.setFilenameFilter(new WavFileFilter());
        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            addSample(new File(dialog.getDirectory(), dialog.getFile()));
        }
    }

    private void compileKit() {
        updateBankView();
        if (totSampleSize > 0x3fa0) {
            kitSizeLabel.setText(Integer.toHexString(totSampleSize) + "/3fa0 bytes used");
            return;
        }
        kitSizeLabel.setText(Integer.toHexString(totSampleSize) + " bytes written");
        // TODO : reimplement dither.
        sbc.DITHER_VAL = 0;

        byte newSamples[] = new byte[RomUtilities.BANK_SIZE];
        int lengths[] = new int[15];
        sbc.handle(newSamples, samples, lengths);

        // Checks if at least one sample has been added.
        boolean hasAnySample = false;
        for (int i = 0; i < 15; ++i) {
            if (lengths[i] > 0) {
                hasAnySample = true;
                break;
            }
        }
        if (!hasAnySample) {
            // Not allowed to create kits without samples!
            return;
        }

        //copy sampledata to ROM image
        int offset = getROMOffsetForSelectedBank() + 0x60;
        int offset2 = 0x60;
        do {
            romImage[offset++] = newSamples[offset2++];
        } while (offset2 != RomUtilities.BANK_SIZE);

        //update samplelength info in rom image
        int bankOffset = 0x4060;
        offset = getROMOffsetForSelectedBank();
        romImage[offset++] = 0x60;
        romImage[offset++] = 0x40;

        for (int i = 0; i < 15; i++) {
            bankOffset += lengths[i];
            if (lengths[i] != 0) {
                romImage[offset++] = (byte) (bankOffset & 0xff);
                romImage[offset++] = (byte) (bankOffset >> 8);
            } else {
                romImage[offset++] = 0;
                romImage[offset++] = 0;
            }
        }

        // Resets forced loop data.
        romImage[getROMOffsetForSelectedBank() + 0x5c] = 0;
        romImage[getROMOffsetForSelectedBank() + 0x5d] = 0;
    }

    private void dropSample() {
        int[] indices = instrList.getSelectedIndices();

        for (int indexIt = 0; indexIt < indices.length; ++indexIt) {
            // Assumes that indices are sorted...
            int index = indices[indexIt];

            // Moves up samples.
            if (14 - index >= 0) System.arraycopy(samples, index + 1, samples, index, 14 - index);
            samples[14] = null;

            // Moves up instr names.
            int offset = getROMOffsetForSelectedBank() + 0x22 + index * 3;
            int i;
            for (i = offset; i < getROMOffsetForSelectedBank() + 0x22 + 14 * 3; i += 3) {
                romImage[i] = romImage[i + 3];
                romImage[i + 1] = romImage[i + 4];
                romImage[i + 2] = romImage[i + 5];
            }
            romImage[i] = 0;
            romImage[i + 1] = '-';
            romImage[i + 2] = '-';

            // Adjusts indices.
            for (int indexIt2 = indexIt + 1; indexIt2 < indices.length; ++indexIt2) {
                --indices[indexIt2];
            }
        }

        compileKit();
        updateBankView();
    }

    private void exportSample() {
        Sample s = samples[instrList.getSelectedIndex()];
        if (s == null) {
            return;
        }
        FileDialog dialog = new FileDialog(this, "Save sample as .wav",
                FileDialog.SAVE);
        dialog.setFilenameFilter(new WavFileFilter());
        dialog.setFile(s.getName() + ".wav");
        dialog.setVisible(true);
        if (dialog.getFile() == null) {
            return;
        }
        File f = new File(dialog.getDirectory(), dialog.getFile());
        if (!f.toString().toUpperCase().endsWith(".WAV")) {
            f = new File(f.getAbsoluteFile().toString() + ".wav");
        }
        s.writeToWav(f);
    }
}

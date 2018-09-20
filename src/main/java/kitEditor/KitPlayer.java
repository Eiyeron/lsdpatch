package kitEditor;

import net.miginfocom.swing.MigLayout;
import structures.LSDJKit;
import utils.RomUtilities;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class KitPlayer extends JFrame implements KeyListener {
    private byte[] romImage;
    private final JComboBox<String> comboList = new JComboBox<>();
    private JButton[] keypad = new JButton[16];
    private ArrayList<LSDJKit> kits = new ArrayList<>();
    private LSDJKit currentKit;

    private void playSample(int index) {
        Sound sound = new Sound();
        try {
            sound.play(currentKit.get4BitSamples(index, false));
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void updateButtons() {
        for (int i = 0; i < 16; i++) {
            Sample currentSample = currentKit.getSamples(i);
            if (currentSample != null && currentSample.getName() != null) {
                keypad[i].setText(currentSample.getName());
                keypad[i].setEnabled(true);
            } else {
                keypad[i].setText("---");
                keypad[i].setEnabled(false);

            }
        }
    }

    public KitPlayer(byte[] romImage) {
        setLayout(new MigLayout());
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; ++x) {
                JButton currentButton = new JButton("---");
                if (x == 3)
                    add(currentButton, "sg keypad,wrap");
                else
                    add(currentButton, "sg keypad");

                final int indexToPlay = y * 4 + x;
                currentButton.addActionListener(e -> playSample(indexToPlay));
                keypad[y * 4 + x] = currentButton;
            }
        }

        this.romImage = romImage;
        for (int i = 0; i < romImage.length / RomUtilities.BANK_SIZE; ++i) {
            if (RomUtilities.isPageKit(romImage, i))
                kits.add(new LSDJKit(romImage, i));
        }

        if (kits.size() > 0) {
            currentKit = kits.get(0);
            updateButtons();
        }

        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        pack();
    }

    @Override
    public void keyTyped(KeyEvent key) {

    }

    @Override
    public void keyPressed(KeyEvent key) {
        switch (key.getKeyChar()) {
            case '0':
                playSample(0);
                break;
            case '1':
                playSample(1);
                break;
            case '2':
                playSample(2);
                break;
            case '3':
                playSample(3);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }
}

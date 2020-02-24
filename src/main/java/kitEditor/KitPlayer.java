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
    private final JComboBox<String> kitList = new JComboBox<>();
    private JToggleButton halfSpeedButton = new JToggleButton("Play at half-speed");
    private JButton[] keypad = new JButton[16];
    private final JSlider volumeSlider = new JSlider();

    private ArrayList<LSDJKit> kits = new ArrayList<>();
    private LSDJKit currentKit;

    private void playSample(int index) {
        Sound sound = new Sound();
        try {
            sound.play(currentKit.get4BitSamples(index, halfSpeedButton.isSelected()), volumeSlider.getValue()/100.f);
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

    private void populateKitSelector() {
        kitList.removeAllItems();

        for (LSDJKit kit : kits) {
            kitList.addItem(kit.getName());
        }
        kitList.setSelectedIndex(0);
    }

    private void selectNewKit(int newIndex) {
        currentKit = kits.get(newIndex);
        updateButtons();
    }

    public KitPlayer(byte[] romImage) {
        setLayout(new MigLayout());
        setTitle("Kit Player");

        halfSpeedButton.setFocusable(false);
        add(halfSpeedButton, "split 2");
        add(kitList, "grow, span, wrap");

        JPanel keypadPanel = new JPanel();
        keypadPanel.setLayout(new MigLayout());
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; ++x) {
                JButton currentButton = new JButton("---");
                if (x == 3)
                    keypadPanel.add(currentButton, "sg keypad,wrap");
                else
                    keypadPanel.add(currentButton, "sg keypad");

                final int indexToPlay = y * 4 + x;
                currentButton.addActionListener(e -> playSample(indexToPlay));
                currentButton.setFocusable(false);
                keypad[y * 4 + x] = currentButton;
            }
        }
        add(keypadPanel, "grow, span, wrap");
        add(new JLabel("Volume"), "split 2");
        add(volumeSlider, "grow, span");

        this.romImage = romImage;
        for (int i = 0; i < romImage.length / RomUtilities.BANK_SIZE; ++i) {
            if (RomUtilities.isPageKit(romImage, i)) {
                LSDJKit kit = new LSDJKit(romImage, i);
                kits.add(kit);
            }
        }

        if (kits.size() > 0) {
            currentKit = kits.get(0);
            updateButtons();
            populateKitSelector();
        }

        kitList.addItemListener(e -> selectNewKit(kitList.getSelectedIndex()));
        kitList.setFocusable(false);

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
        char keyChar = key.getKeyChar();
        if (keyChar >= '0' && keyChar <= '9') {
            int index = keyChar - '0';
            if (keypad[index].isEnabled())
                playSample(keyChar - '0');
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
    }
}

package emu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

import chip.Chip;

public class ChipFrame extends JFrame implements KeyListener {

    private static final long serialVersionUId = 1L;

    private ChipPanel panel;

    private int[] keyBuffer;
    private int[] keyIdToKey;

    public ChipFrame(Chip c) {
        setPreferredSize(new Dimension(640, 320));
        pack();
        setPreferredSize(
                new Dimension(640 + getInsets().left + getInsets().right,
                        320 + getInsets().top + getInsets().bottom));
        panel = new ChipPanel(c);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Chip 8 Emulator");
        pack();
        setVisible(true);
        addKeyListener(this);
        
        keyBuffer = new int[16];
        keyIdToKey = new int[256];

        fillKeyIds();
    }

    /**
     * 键盘布局
     * 1 2 3 C
     * 4 5 6 D
     * 7 8 9 E
     * A 0 B F
     */
    private void fillKeyIds() {
        for (int i = 0; i < keyIdToKey.length; i++) {
            keyIdToKey[i] = -1;
        }

        keyIdToKey['1'] = 1;
        keyIdToKey['2'] = 2;
        keyIdToKey['3'] = 3;

        keyIdToKey['Q'] = 4;
        keyIdToKey['W'] = 5;
        keyIdToKey['E'] = 6;

        keyIdToKey['A'] = 7;
        keyIdToKey['S'] = 8;
        keyIdToKey['D'] = 9;

        keyIdToKey['Z'] = 0xA;
        keyIdToKey['X'] = 0;
        keyIdToKey['C'] = 0xB;

        keyIdToKey['4'] = 0xC;
        keyIdToKey['R'] = 0xD;
        keyIdToKey['F'] = 0xE;
        keyIdToKey['V'] = 0xF;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = keyIdToKey[e.getKeyCode()];
        if (key != -1) {
            keyBuffer[key] = 1;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = keyIdToKey[e.getKeyCode()];
        if (key != -1) {
            keyBuffer[key] = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }
    
    public int[] getKeyBuffer() {
        return keyBuffer;
    }
}

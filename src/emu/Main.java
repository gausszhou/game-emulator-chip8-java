package emu;

import chip.Chip;

public class Main extends Thread {

    private Chip chip8;
    private ChipFrame frame;

    public Main(){
        chip8 = new Chip();
        chip8.init();
        chip8.loadProgram("./programs/pong2.c8");
        frame = new ChipFrame(chip8);
    }

    public void run() {
        while (true) {
            chip8.run();
            if (chip8.isNeedRedraw()) {
                frame.repaint();
                chip8.removeDrawFlag();
            }
            try {
                Thread.sleep(16);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("\r");
        Main main = new Main();
        main.start();

    }
}

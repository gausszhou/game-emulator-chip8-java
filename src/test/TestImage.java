package test;

public class TestImage {

    public static void main(String[] args) {
        int[] IMAGE = { 0xF0, 0x90, 0x90, 0x90, 0xF0 };
        System.out.println("\r");
        for (int i = 0; i < IMAGE.length; i++) {
            int row = IMAGE[i];
            for (int j = 0; j < 8; j++) {
                // 10000000
                // 01000000
                // ...
                int pixel = row & (0x80 >> j);
                if (pixel == 0) {
                    System.out.print("0");
                } else {
                    System.out.print("1");
                }
            }
            System.out.print(" ");
            for (int j = 0; j < 8; j++) {
                // 10000000
                // 01000000
                // ...
                int pixel = row & (0x80 >> j);
                if (pixel == 0) {
                    System.out.print(" ");
                } else {
                    System.out.print("X");
                }
            }
            System.out.println();
        }
    }

}

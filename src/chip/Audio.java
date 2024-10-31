package chip;

import java.awt.Toolkit;

public class Audio {
  public static void playSound() {
    // 发出蜂鸣声
    Toolkit.getDefaultToolkit().beep();

    // 可选择添加延迟，便于识别蜂鸣声
    // try {
    // Thread.sleep(1000); // 延迟1秒，以便听到蜂鸣
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }
  }
}

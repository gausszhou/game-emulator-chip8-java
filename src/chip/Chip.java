package chip;

public class Chip {

	// 内存
	private char[] memory;
	// 寄存器
	private char[] V;
	private char I;
	private char pc;

	// 堆栈
	private char stack[];
	private int stackPointer;

	// 定时器
	private int delay_timer;
	private int sound_timer;

	// 键盘输入
	private byte[] keys;
	// 显示输出
	private byte[] display;

	public void init() {
		memory = new char[4096];
		V = new char[16];
		I = 0x0;

		stack = new char[16];
		stackPointer = 0;

		delay_timer = 0;
		sound_timer = 0;

		keys = new byte[16];
		display = new byte[64 * 32];
	}

	public void run() {
		// fetch opcode
		char opcode = (char) (memory[pc] << 8 | memory[pc + 1]);
		// decode opcode
		System.out.println(Integer.toHexString(opcode) + ": ");

		switch (opcode & 0xF000) { // get head 1 type
			case 0x8000:
				switch (opcode & 0x000F) {
					case 0x0000: // set VX value to VY

						break;

					default:
						System.err.println("Unsupported Opcode!");
						System.exit(0);
						break;
				}

				break;
			default:
				System.err.println("Unsupported Opcode!");
				break;
		}
		// execute opcode
	}
}

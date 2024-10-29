package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

	private boolean needRedraw;

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
			// 1NNN 跳转到地址 NNN
			case 0x1000:
				pc = (char) (opcode & 0x0FFF);
				break;
			// 2NNN 在 NNN 处调用子例程。
			case 0x2000:
				stack[stackPointer] = pc;
				stackPointer += 1;
				pc = (char) (opcode & 0x0FFF);
				break;
			// 3XNN 如果 VX 等于 NN，则跳过下一条指令
			case 0x3000:
				int x3 = (opcode & 0x0F00) >> 8;
				// TODO
				break;
			// 4XNN 如果 VX 不等于 NN，则跳过下一条指令
			case 0x4000:
				int x4 = (opcode & 0x0F00) >> 8;
				// TODO
				break;
			// 5XY0 如果 VX 等于 VY，则跳过下一条指令
			case 0x5000:
				int x5 = (opcode & 0x0F00) >> 8;
				int y5 = (opcode & 0x00F0) >> 4;
				// TODO
				break;
			// 6XNN 将 VX 设置为 NN
			case 0x6000:
				int x6 = (opcode & 0x0F00) >> 8;
				int nn6 = (opcode & 0x00FF);
				V[x6] = (char) nn6;
				pc += 2;
				break;
			// 7XNN 将 NN 添加到 VX
			case 0x7000:
				int x7 = (opcode & 0x0F00) >> 8;
				int nn7 = (opcode & 0x00FF);
				V[x7] = (char) ((V[x7] + nn7) & 0xFF);
				pc += 2;
				break;
			// 8XNN
			case 0x8000:
				switch (opcode & 0x000F) {
					// 8XY0 将 VX 设置为 VY 的值
					case 0x0000:

						break;
					// 8XY1 将 VX 设置为 VX 位或 VY Vx |= Vy
					// 8XY2 将 VX 设置为 VX 位与 VY Vx &= Vy
					// 8XY3 将 VX 设置为 VX 位异或 VY Vx ^= Vy
					// 8XY4 将 VY 添加到 VX 中

					default:
						System.err.println("Unsupported Opcode! 0x8000");
						System.exit(0);
						break;
				}

				break;
			// ANNN 将 I 设置为地址 NNN => I = NNN
			case 0xA000:
				I = (char) (opcode & 0x0FFF);
				pc += 2;
				break;
			// BNNN 跳转到地址 NNN 加 V0 => PC = V0 + NNN
			case 0xB000:
				stack[stackPointer] = pc;
				stackPointer += 1;
				pc = (char) (V[0] + (opcode & 0x00FFF));

				break;
			case 0xD000:
				// TODO
				pc += 2;
				break;
			default:
				System.err.println("Unsupported Opcode!" + opcode);
				break;
		}
		// execute opcode
	}

	public byte[] getDisplay() {
		return display;
	}

	public boolean isNeedRedraw() {
		return needRedraw;
	}

	public void removeDrawFlag() {
		needRedraw = false;
	}

	public void loadProgram(String file) {

		try (DataInputStream input = new DataInputStream(new FileInputStream(new File(file)));) {
			int offset = 0;
			while (input.available() > 0) {
				//
				memory[0x200 + offset] = (char) (input.readByte() & 0xFF);
				offset++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

	}

}

package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Chip {

	// 内存 4096 2 的 12 次方
	private char[] memory;
	// 数据寄存器 16 个 8 bit
	private char[] V;
	// 地址寄存器 12 位
	private char I;
	// 程序指针寄存器
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
		pc = 0x200;

		stack = new char[16];
		stackPointer = 0;

		delay_timer = 0;
		sound_timer = 0;

		keys = new byte[16];
		display = new byte[64 * 32];

	}

	public void run() {
		// fetch opcode 16 bit
		char opcode = (char) (memory[pc] << 8 | memory[pc + 1]);
		// decode opcode
		System.out.print(Integer.toHexString(opcode).toUpperCase() + ": ");

		switch (opcode & 0xF000) { // get head 1 type
			// 0NNN
			case 0x0000: {
				switch (opcode & 0x0FFF) {
					// 00E0 清除屏幕
					case 0x00E0: {
						System.err.println("Unsupported Opcode!");
						System.exit(0);
						break;
					}
					// 00EE 从子例程返回
					case 0x00EE: {
						stackPointer--;
						pc = (char) (stack[stackPointer]);
						pc += 2;
						System.out.println("Returning to " + Integer.toHexString(pc).toUpperCase());
						break;
					}
					// 0NNN 在地址 NNN 处调用机器代码例程
					default: {
						System.err.println("Unsupported Opcode!");
						System.exit(0);
					}
				}
				break;

			}
			// 1NNN 跳转到地址 NNN
			case 0x1000: {
				pc = (char) (opcode & 0x0FFF);
				System.out.println(" Jump " + Integer.toHexString(pc).toUpperCase());
				break;
			}
			// 2NNN 在 NNN 处调用子例程。
			case 0x2000:
				stack[stackPointer] = pc;
				stackPointer += 1;
				pc = (char) (opcode & 0x0FFF);
				System.out.println("Calling " + Integer.toHexString(pc).toUpperCase());
				break;
			// 3XNN 如果 VX 等于 NN，则跳过下一条指令
			case 0x3000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				if (V[x] == nn) {
					pc += 4;
					System.out.println("Skipping next instruction (V[" + x + "] == " + nn + ")");
				} else {
					pc += 2;
				}
				break;
			}

			// 4XNN 如果 VX 不等于 NN，则跳过下一条指令
			case 0x4000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				if (V[x] != nn) {
					pc += 4;
					System.out.println("Skipping next instruction (V[" + x + "] != " + nn + ")");
				} else {
					pc += 2;
				}
				break;
			}
			// 5XY0 如果 VX 等于 VY，则跳过下一条指令
			case 0x5000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if (V[x] == V[y]) {
					pc += 4;
					System.out.println("Skipping next instruction (V[" + x + "] == V[" + y + "])");
				} else {
					pc += 2;
				}
				break;
			}
			// 6XNN 将 VX 设置为 NN
			case 0x6000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				V[x] = (char) nn;
				pc += 2;
				System.out.println("Setting V[" + x + "] to " + nn);
				break;
			}
			// 7XNN 将 NN 添加到 VX
			case 0x7000: {
				int x7 = (opcode & 0x0F00) >> 8;
				int nn7 = (opcode & 0x00FF);
				V[x7] = (char) ((V[x7] + nn7) & 0xFF);
				pc += 2;
				System.out.println("Adding " + nn7 + " to V[" + x7 + "] = " + (int) V[x7]);
				break;
			}

			// 8XNN
			case 0x8000:
				switch (opcode & 0x000F) {
					// 8XY0 将 VX 设置为 VY 的值
					case 0x0000:
						System.err.println("Unsupported Opcode! 0x8000");
						System.exit(0);
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
			case 0xA000: {
				I = (char) (opcode & 0x0FFF);
				pc += 2;
				System.out.println("Set I to " + Integer.toHexString(I).toUpperCase());
				break;
			}
			// BNNN 跳转到地址 NNN 加 V0 => PC = V0 + NNN
			case 0xB000: {
				stack[stackPointer] = pc;
				stackPointer += 1;
				pc = (char) (V[0] + (opcode & 0x00FFF));
				break;
			}

			// DXYN 在坐标 （VX， VY） 处绘制一个 sprite，该 sprite 的宽度为 8 像素，高度为 N 像素
			case 0xD000: {
				int xd = (opcode & 0x0F00) >> 8;
				int vxd = V[xd];
				int yd = (opcode & 0x00F0) >> 4;
				int vyd = V[yd];
				int height = (opcode & 0x000F);
				for (int _y = 0; _y < height; _y++) {
					int line = memory[I + _y];
					for (int _x = 0; _x < 8; _x++) {
						int pixel = line & (0x80 >> _x);
						if (pixel != 0) {
							int totalX = vxd + _x;
							int totalY = vyd + _y;
							int index = totalY * 64 + totalX;
							if (display[index] == 1) {
								V[0xF] = 1;
							}
							display[index] ^= 1;
						}
					}
				}
				pc += 2;
				needRedraw = true;
				System.out.println("Draws a sprite at coordinate (" + vxd + "," + vyd + ")");
				break;
			}
			// FNNN
			case 0xF000: {
				switch (opcode & 0x00FF) {

					// FX07 将 VX 设置为延迟计时器的值
					case 0x0007: {
						int x = (opcode & 0x0F00) >> 8;
						V[x] = (char)delay_timer;
						pc += 2;
						System.out.println("Sets VX "+ V[x] +" to the value of the delay timer.");
						break;
					}
					// FX15 将延迟计时器设置为 VX
					case 0x0015: {
						int x = (opcode & 0x0F00) >> 8;
						delay_timer = (int)V[x];
						pc += 2;
						System.out.println("Sets the delay timer to VX." + V[x]);
						break;
					}
					// FX18 将声音计时器设置为 VX
					case 0x0018: {
						int x = (opcode & 0x0F00) >> 8;
						sound_timer = V[x];
						pc += 2;
						System.out.println("Sets the sound timer to VX." + V[x]);
						break;
					}
					// FX33 存储 VX 的二进制编码十进制表示形式，其中内存中的百位数字位于 I 位置，十位数字位于位置 I+1，个位数位于位置 I+2。
					case 0x0033: {
						int x = (opcode & 0x0F00) >> 8;
						int vx = V[x];
						int hundreds = (vx - (vx % 100)) / 100;
						vx -= hundreds * 100;
						int tens = (vx - (vx % 10)) / 10;
						vx -= tens * 10;
						int ones = vx;
						memory[I] = (char) hundreds;
						memory[I + 1] = (char) tens;
						memory[I + 2] = (char) ones;
						pc += 2;
						System.out
								.println("Stores the binary-coded decimal representation of V[" + x + "]: " + hundreds + tens + ones);
						break;
					}
					// FX29 将 I 设置为角色在 VX 中的 sprite 位置。字符 0-F（十六进制）由 4x5 字体表示。
					case 0x0029: {
						int x = (opcode & 0x0F00) >> 8;
						int character = V[x];
						I = (char)(0x050 + (character * 5));
						pc += 2;
						System.out.println("Sets I to the location of the sprite for the character in V[" + x +"]");
						break;
					}
					// FX65 使用内存中的值从 V0 填充到 VX（包括 VX），从地址 I 开始。对于每个读取的值，与 I 的偏移量增加 1，但 I 本身保持不变。
					case 0x0065: {
						int x = (opcode & 0x0F00) >> 8;
						for (int i = 0; i < x; i++) {
							V[i] = memory[I + i];
						}
						pc += 2;
						System.out.println("Fills from V[0] to V[" + x + "] with values from memory[0x"
								+ Integer.toHexString((I & 0xFF)).toUpperCase() + "]");
						break;
					}
					default: {
						System.err.println("Unsupported Opcode!" + opcode);
						System.exit(0);
						break;
					}
				}
				break;
			}

			default:
				System.err.println("Unsupported Opcode!" + Integer.toHexString(opcode).toUpperCase());
				System.exit(0);
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

	public void loadFontset() {
		for (int i = 0; i < ChipData.fontset.length; i++) {
			memory[0x50 + i] = (char) (ChipData.fontset[i] & 0xFF);
		}
	}

}

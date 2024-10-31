package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class Chip {

	// 内存 4 KB 4096 2 的 12 次方
	private char[] memory;
	// 数据寄存器 16 个 8 bit
	private char[] V;
	// 地址寄存器 12 位 用于指向内存地址（基址）
	private char I;
	// 程序指针寄存器
	private char pc;

	// 堆栈
	private char stack[];
	// 栈顶指针
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

		needRedraw = false;
		loadFontset();
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
						for (int i = 0; i < display.length; i++) {
							display[i] = 0;
						}
						needRedraw = true;
						pc += 2;
						System.out.println("00E0 Clears the screen");
						break;
					}
					// 00EE 从子例程返回
					case 0x00EE: {
						stackPointer--;
						pc = (char) (stack[stackPointer] + 2);
						System.out.println("00EE Returning to " + Integer.toHexString(pc).toUpperCase());
						break;
					}
					// 0NNN 在地址 NNN 处调用机器代码例程
					default: {
						System.err.println("0NNN Unsupported Opcode!");
						System.exit(0);
					}
				}
				break;

			}
			// 1NNN 跳转到地址 NNN
			case 0x1000: {
				int nnn = (opcode & 0x0FFF);
				pc = (char) nnn;
				System.out.println("1NNN Jumps to address " + Integer.toHexString(pc).toUpperCase());
				break;
			}
			// 2NNN 在 NNN 处调用子例程。
			case 0x2000: {
				stack[stackPointer] = pc;
				stackPointer += 1;
				pc = (char) (opcode & 0x0FFF);
				System.out.println("2NNN Calls subroutine at " + Integer.toHexString(pc).toUpperCase());
				break;
			}

			// 3XNN 如果 VX 等于 NN，则跳过下一条指令
			case 0x3000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				if (V[x] == nn) {
					pc += 4;
					System.out.println("3XNN Skipping next instruction if (V[" + x + "] == " + nn + ")");
				} else {
					pc += 2;
					System.out.println("3XNN Not skipping next instruction if (V[" + x + "] != " + nn + ")");
				}
				break;
			}

			// 4XNN 如果 VX 不等于 NN，则跳过下一条指令
			case 0x4000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				if (V[x] != nn) {
					pc += 4;
					System.out.println("4XNN Skipping next instruction if (V[" + x + "] != " + nn + ")");
				} else {
					pc += 2;
					System.out.println("4XNN Not Skipping next instruction if (V[" + x + "] == " + nn + ")");
				}
				break;
			}
			// 5XY0 如果 VX 等于 VY，则跳过下一条指令
			case 0x5000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if (V[x] == V[y]) {
					pc += 4;
					System.out.println("5XY0 Skipping next instruction (V[" + x + "] == V[" + y + "])");
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
				System.out.println("6XNN Sets V[" + x + "] to NN = " + nn);
				break;
			}
			// 7XNN 将 NN 添加到 VX
			case 0x7000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				V[x] = (char) ((V[x] + nn) & 0xFF);
				pc += 2;
				System.out.println("7XNN Adding " + nn + " to V[" + x + "] = " + (int) V[x]);
				break;
			}

			// 8XYN
			case 0x8000: {
				switch (opcode & 0x000F) {
					// 8XY0 将 VX 设置为 VY 的值
					// Vx = Vy
					case 0x0000: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						V[x] = V[y];
						pc += 2;
						System.out.println("8XY0 Set V[" + x + "] to the value of V[" + y + "] = " + (int) V[x]);
						break;
					}
					// 8XY1 将 VX 设置为 VX 位或
					// VY Vx |= Vy
					case 0x0001: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						V[x] = (char) (V[x] | V[y]);
						pc += 2;
						System.out.println("8XY1 Set V[" + x + "] to V[" + x + "] | V[" + y + "]= " + (int) V[x]);
						break;
					}
					// 8XY2 将 VX 设置为 VX 位与 VY
					// Vx &= Vy
					case 0x0002: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						V[x] = (char) (V[x] & V[y]);
						pc += 2;
						System.out.println("8XY2 Set V[" + x + "] to V[" + x + "] & V[" + y + "]= " + (int) V[x]);
						break;
					}
					// 8XY3 将 VX 设置为 VX 位异或 VY
					// Vx ^= Vy
					case 0x0003: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						V[x] = (char) (V[x] ^ V[y]);
						pc += 2;
						System.out.println("8XY3 Set V[" + x + "] to V[" + x + "] ^ V[" + y + "]= " + (int) V[x]);
						break;
					}
					// 8XY4 将 VY 添加到 VX 中
					// Vx += Vy
					case 0x0004: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						if (V[y] > 0xFF - V[x]) {
							V[0xF] = 1;
							System.out.println("8XY4 Carry!");
						} else {
							V[0xF] = 0;
							System.out.println("8XY4 No Carry");
						}
						V[x] = (char) ((V[x] + V[y]) & 0xFF);
						pc += 2;
						System.out.println("8XY4 Set V[" + x + "] to V[" + x + "] + V[" + y + "]= " + (int) V[x]);
						break;
					}
					// 8XY5 从 VX 中减去 VY
					// Vx -= Vy
					case 0x0005: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						if (V[x] > V[y]) {
							V[0xF] = 1;
							System.out.println("8XY5 No Borrow");
						} else {
							V[0xF] = 0;
							System.out.println("8XY5 Borrow");
						}
						V[x] = (char) ((V[x] - V[y]) & 0xFF);
						pc += 2;
						System.out.println("8XY5 Set V[" + x + "] to V[" + x + "] - V[" + y + "]= " + (int) V[x]);
						break;
					}
					// 8XY6 将 VX 向右移动 1，然后在切换到 VF 之前存储 VX 的最低有效位
					case 0x0006: {
						int x = (opcode & 0x0F00) >> 8;
						V[0xF] = (char) (V[x] & 0x1);
						V[x] = (char) (V[x] >> 1);
						pc += 2;
						System.out.println("8XY6 Shifts V[" + x + "] to the right by 1");
						break;
					}
					// 8XY7 将 VX 设置为 VY 减去 VX
					case 0x0007: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						if (V[y] > V[x]) {
							V[0xF] = 1;
						} else {
							V[0xF] = 0;
						}
						V[x] = (char) (V[y] - V[x]);
						pc += 2;
						System.out.println("8XY7 Sets V[" + x + "] to V[" + y + "] - V[" + x + "] = " + (int) V[x]);
						break;
					}
					// 8XYE TODO

					default:
						System.err.println("Unsupported Opcode! 0x8000");
						System.exit(0);
						break;
				}
				break;
			}
			// 9NNN
			case 0x9000: {
				switch (opcode & 0x000F) {
					// 9XY0 如果 VX 不等于 VY，则跳过下一条指令
					case 0x0000: {
						int x = (opcode & 0x0F00) >> 8;
						int y = (opcode & 0x00F0) >> 4;
						if(V[x] != V[y]) {
							pc += 4;
							
						} else {
							pc += 2;
						}
						System.out.println("9XY0 Skips the next instruction if VX does not equal VY. ");
						break;
					}
					default: {
						System.err.println("Unsupported Opcode! 0x8000");
						System.exit(0);
						break;
					}
				}
				break;
			}
			// ANNN 将 I 设置为地址 NNN => I = NNN
			case 0xA000: {
				I = (char) (opcode & 0x0FFF);
				pc += 2;
				System.out.println("ANNN Set I to " + Integer.toHexString(I).toUpperCase());
				break;
			}
			// BNNN 跳转到地址 NNN 加 V0 => PC = V0 + NNN
			case 0xB000: {
				stack[stackPointer] = pc;
				stackPointer += 1;
				int nnn = (opcode & 0x00FFF);
				pc = (char) (V[0] + nnn);
				System.err.println("BNNN Jumps to the address NNN plus V[0] = " + pc);
				break;
			}
			// CXNN 将 VX 设置为随机数（通常为 0 到 255）和 NN 的按位运算的结果
			case 0xC000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				int randomNumber = new Random().nextInt(255) & nn;
				V[x] = (char) randomNumber;
				pc += 2;
				System.out.println("CXNN Sets V[" + x + "] to the result of a bitwise and operation on a random number");
				break;
			}
			// DXYN 在坐标 （VX， VY） 处绘制一个 sprite，该 sprite 的宽度为 8 像素，高度为 N 像素
			case 0xD000: {
				int xd = (opcode & 0x0F00) >> 8;
				int vxd = V[xd];
				int yd = (opcode & 0x00F0) >> 4;
				int vyd = V[yd];
				int height = (opcode & 0x000F);
				V[0xF] = 0;
				for (int _y = 0; _y < height; _y++) {
					int line = memory[I + _y];
					for (int _x = 0; _x < 8; _x++) {
						int pixel = line & (0x80 >> _x);
						if (pixel != 0) {
							int totalX = vxd + _x;
							int totalY = vyd + _y;
							totalX = totalX % 64;
							totalY = totalY % 32;
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
				System.out.println("DXYN Draws a sprite at coordinate (" + vxd + "," + vyd + ")");
				break;
			}
			// ENNN 处理键盘操作
			case 0xE000: {
				switch (opcode & 0x00FF) {
					// EX9E 如果按下 VX 中存储的键，则跳过下一条指令
					case 0x009E: {
						int x = (opcode & 0x0F00) >> 8;
						int key = V[x];
						if (keys[key] == 1) {
							pc += 4;
							System.out.println("ENNN Skips the next instruction if the key stored in V[" + x + "] is not pressed");
						} else {
							pc += 2;
						}
						break;
					}

					// EXA1 如果未按下存储在 VX 中的键，则跳过下一条指令
					case 0x00A1: {
						int x = (opcode & 0x0F00) >> 8;
						int key = V[x];
						if (keys[key] == 0) {
							pc += 4;
							System.out.println("EXA1 Skips the next instruction if the key stored in V[" + x + "] is not pressed");
						} else {
							pc += 2;
						}
						break;
					}

					default:
						System.err.println("Unexisting Opcode!");
						System.exit(0);
						break;
				}
				break;
			}
			// FNNN
			case 0xF000: {
				switch (opcode & 0x00FF) {

					// FX07 将 VX 设置为延迟计时器的值
					case 0x0007: {
						int x = (opcode & 0x0F00) >> 8;
						V[x] = (char) (delay_timer & 0x0FFF);
						pc += 2;
						System.out.println("FX07 Sets V[" + x + "] " + (int) V[x] + " to the value of the delay timer.");
						break;
					}
					// FX0A 等待按键，然后存储在 VX 中 （阻塞操作，所有指令停止，直到下一个按键事件）
					case 0x000A: {
						int x = (opcode & 0x0F00) >> 8;
						for (int i = 0; i < keys.length; i++) {
							if (keys[i] == 1) {
								V[x] = (char) i;
								pc += 2;
								break;
							}
						}
						System.out.println("FX0A Awaiting a key pressed store to V["+ x +"]");
					}
					// FX15 将延迟计时器设置为 VX
					case 0x0015: {
						int x = (opcode & 0x0F00) >> 8;
						delay_timer = V[x] & 0x0FFF;
						pc += 2;
						System.out.println("FX15 Sets the delay timer to V[" + x + "] = " + (int) V[x]);
						break;
					}
					// FX18 将声音计时器设置为 VX
					case 0x0018: {
						int x = (opcode & 0x0F00) >> 8;
						// sound_timer = V[x];
						sound_timer = 1; // 调整为只发一次声音
						pc += 2;
						System.out.println("FX18 Sets the sound timer to VX." + V[x]);
						break;
					}
					// FX29 将 I 设置为角色在 VX 中的 sprite 位置。字符 0-F（十六进制）由 4x5 字体表示。
					case 0x0029: {
						int x = (opcode & 0x0F00) >> 8;
						int character = V[x];
						I = (char) (0x050 + (character * 5));
						pc += 2;
						System.out.println("FX29 Setting I to Character V[" + x + "] = " + (int) V[x] + " Offset to 0x"
								+ Integer.toHexString(I).toUpperCase());
						break;
					}
					// FX1E
					case 0x001E: {
						int x = (opcode & 0x0F00) >> 8;
						I = (char) (I + V[x]);
						pc += 2;
						break;
					}
					// FX33 存储 VX 的二进制编码十进制表示形式，其中内存中的百位数字位于 I 位置，十位数字位于位置 I+1，个位数位于位置 I+2。
					case 0x0033: {
						int x = (opcode & 0x0F00) >> 8;
						int value = V[x];
						int hundreds = (value - (value % 100)) / 100;
						value -= hundreds * 100;
						int tens = (value - (value % 10)) / 10;
						value -= tens * 10;
						int ones = value;
						memory[I] = (char) hundreds;
						memory[I + 1] = (char) tens;
						memory[I + 2] = (char) ones;
						pc += 2;
						System.out
								.println(
										"FX33 Stores the binary-coded decimal representation of V[" + x + "]: " + hundreds + tens + ones);
						break;
					}
					// FX55 将 V0 到 VX（包括 VX）存储在内存中，从地址 I 开始。每写入一个值，与 I 的偏移量就会增加 1，但 I 本身保持不变。
					case 0x0055: {
						int x = (opcode & 0x0F00) >> 8;
						for (int i = 0; i < x; i++) {
							memory[I + i] = V[i];
						}
						pc += 2;
						System.out.println("FX55 Stores from V0 to VX (including VX) in memory, starting at address I.");
						break;
					}

					// FX65 使用内存中的值从 V0 填充到 VX（包括 VX），从地址 I 开始。对于每个读取的值，与 I 的偏移量增加 1，但 I 本身保持不变。
					case 0x0065: {
						int x = (opcode & 0x0F00) >> 8;
						for (int i = 0; i < x; i++) {
							V[i] = memory[I + i];
						}
						I = (char) (I + x + 1);
						pc += 2;
						System.out.println("FX65 Fills from V[0] to V[" + x + "] with values from memory[0x"
								+ Integer.toHexString((I & 0xFF)).toUpperCase() + "]");
						break;
					}
					default: {
						System.err.println("Unsupported Opcode!");
						System.exit(0);
						break;
					}
				}
				break;
			}

			default: {
				System.err.println("Unsupported Opcode!");
				System.exit(0);
				break;
			}

		}
		if (sound_timer > 0) {
			sound_timer--;
			Audio.playSound();
		}
		if (delay_timer > 0) {
			delay_timer--;
		}
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

	public void setKeyBuffer(int[] keyBuffer) {
		for (int i = 0; i < keys.length; i++) {
			keys[i] = (byte) keyBuffer[i];
		}
	}

}

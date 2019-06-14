package com.vtsman.gbemu;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's RAM, which has 8 banks for the gameboy color and 1
//for the normal gameboy
public class RAM implements IAddressable {

	private byte[][] ram = new byte[8][0x1000];
	private int ind = 1;

	@Override
	public byte read(int addr) {
		//Correct address for echo RAM
		if (addr >= 0xE000) {
			addr -= (0x2000);
		}
		if (addr >= 0xC000 && addr < 0xD000) {
			return ram[0][addr - 0xC000];
		}
		return ram[ind][addr - 0xD000];
	}

	@Override
	public void write(int addr, byte value) {
		if (addr >= 0xE000) {
			addr -= (0x2000);
		}
		if (addr >= 0xC000 && addr < 0xD000) {
			ram[0][addr - 0xC000] = value;
		} else {
			ram[ind][addr - 0xD000] = value;
		}
	}

	@Override
	public boolean isAddressInRange(int addr) {
		return addr < 0xFE00 && addr >= 0xC000;
	}

	public void setBank(int bank) {
		this.ind = bank;
	}

}

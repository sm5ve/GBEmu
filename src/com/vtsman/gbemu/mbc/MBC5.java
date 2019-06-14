package com.vtsman.gbemu.mbc;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's fifth generation MBC
public class MBC5 implements MBC {
	int bank = 0;

	int ramBank = 0;

	//The ROM bank is now 9 bits long!
	@Override
	public void romWrite(int addr, byte value) {
		if (addr < 0x3000 && addr >= 0x2000) {
			bank = (bank & ~0xff) | (value & 0xff);
		}
		if (addr < 0x4000 && addr >= 0x3000) {
			bank = (bank & 0xff) | ((value & 1) << 8);
		}
		if (addr < 0x6000 && addr >= 0x4000) {
			ramBank = value & 0xff;
		}
	}

	@Override
	public int getRomBank() {
		return bank;
	}

	@Override
	public int getRamBank() {
		return ramBank;
	}

	@Override
	public boolean ramEnabled() {
		return true;
	}

}

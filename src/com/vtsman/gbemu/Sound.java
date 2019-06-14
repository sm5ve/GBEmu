package com.vtsman.gbemu;

//Spencer Martin
//5/20/16
//This class was supposed to simulate the gameboy's sound system, but sound is hard.
public class Sound implements IAddressable {

	byte[] temp = new byte[0x100];

	@Override
	public byte read(int addr) {
		// TODO Auto-generated method stub
		//System.out.printf("Sound read from 0x%04x\n", addr);
		return temp[addr - 0xff00];
	}

	@Override
	public void write(int addr, byte value) {
		// TODO Auto-generated method stub
		//System.out.printf("Sound wrote 0x%02x to 0x%04x\n", value, addr);
		temp[addr - 0xff00] = value;
	}

	@Override
	public boolean isAddressInRange(int addr) {
		if (addr >= 0xff10 && addr <= 0xff26) {
			return true;
		}
		if (addr >= 0xff30 && addr <= 0xff3f) {
			return true;
		}
		return false;
	}

}

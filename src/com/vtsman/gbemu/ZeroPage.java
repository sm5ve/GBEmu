package com.vtsman.gbemu;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's zero page. I have no idea why there's a zero page in IO.
//Is this even used?!?!?
public class ZeroPage implements IAddressable {

	private byte[] page = new byte[127];

	@Override
	public byte read(int addr) {
		return this.page[addr - 0xff80];
	}

	@Override
	public void write(int addr, byte value) {
		this.page[addr - 0xff80] = value;
	}

	@Override
	public boolean isAddressInRange(int addr) {
		return addr >= 0xff80 && addr < 0xffff;
	}

}

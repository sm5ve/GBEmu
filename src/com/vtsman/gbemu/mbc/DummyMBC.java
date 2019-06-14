package com.vtsman.gbemu.mbc;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's MBC - it is the bare minimum implementation
//And used as a fall back.
public class DummyMBC implements MBC {

	@Override
	public void romWrite(int addr, byte value) {

	}

	@Override
	public int getRomBank() {
		return 1;
	}

	@Override
	public int getRamBank() {
		return 0;
	}

	@Override
	public boolean ramEnabled() {
		return true;
	}

}

package com.vtsman.gbemu.mbc;

//Spencer Martin
//5/20/16
//This interface sets a standard interface for all memory bank controllers
public interface MBC {
	public void romWrite(int addr, byte value);

	public int getRomBank();

	public int getRamBank();

	public boolean ramEnabled();
}

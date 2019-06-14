package com.vtsman.gbemu;

//Spencer Martin
//5/20/16
//This interface specifies how all memory devices should expose themselves
public interface IAddressable {
	public byte read(int addr);

	public void write(int addr, byte value);

	//Used by MMU to determine which memory device to access
	public boolean isAddressInRange(int addr);

	//Quick way to write a short to memory
	public default void writeShort(int addr, short value) {
		this.write(addr, (byte) (value & 0xff));
		this.write(addr + 1, (byte) (value >> 8));
	}
}

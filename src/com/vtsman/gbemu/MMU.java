package com.vtsman.gbemu;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's memory management unit. It routes the correct
//read/write request to the correct memory device
public class MMU implements IAddressable {

	private IAddressable[] devs;

	//Zero page is some upper RAM - very fast
	byte[] zeroPage = new byte[127];

	public MMU(IAddressable... devices) {
		this.devs = devices;
	}

	private IAddressable findDevice(int addr) {
		for (int i = 0; i < devs.length; i++) { //Enhanced for loop degrades performance by allocating an iterator
			if (devs[i].isAddressInRange(addr)) {
				return devs[i];
			}
		}
		System.err.printf("Error: tried to access unmapped memory address: 0x%04x\n", addr);
		for (;;)
			;
		//return null;
	}

	@Override
	public byte read(int addr) {
		addr &= 0xffff;
		IAddressable dev = findDevice(addr);
		return dev.read(addr);
	}

	@Override
	public void write(int addr, byte value) {
		addr &= 0xffff;
		IAddressable dev = findDevice(addr);
		dev.write(addr, value);
	}

	@Override
	public boolean isAddressInRange(int addr) {
		return true;
	}

}

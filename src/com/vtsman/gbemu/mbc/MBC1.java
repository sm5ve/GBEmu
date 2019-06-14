package com.vtsman.gbemu.mbc;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's first generation MBC
public class MBC1 implements MBC {

	private int bank = 0;
	private int upperBank = 0;
	private boolean upperBankRam = false;
	private boolean ramEnable = false;
	private boolean ram, battery;

	public MBC1(boolean ram, boolean battery){
		this.ram = ram;
		this.battery = battery;
	}

	@Override
	public void romWrite(int addr, byte value) {
		//Quirky stuff with the banks
		if(addr < 0x2000){
			if(value == 0){
				this.ramEnable = false;
			}
			if(value == 0xa){
				this.ramEnable = true;
			}
		}
		if (addr < 0x4000 && addr >= 0x2000) {
			bank = value & 0xff;
		}
		if(addr >= 0x4000 && addr < 0x6000){
			this.upperBank = value & 3;
		}
		if(addr >= 0x6000 && addr < 0x8000){
			if(value == 0){
				this.upperBankRam = false;
			}
			if(value == 1){
				this.upperBankRam = true;
			}
		}
		if (bank == 0 || bank == 0x20 || bank == 0x40 || bank == 0x60) {
			bank++;
		}
	}

	@Override
	public int getRomBank() {
		if(upperBankRam)
			return bank;
		else
			return bank + (upperBank << 8);
	}

	@Override
	public int getRamBank() {
		if(upperBankRam)
			return this.upperBank;
		else
			return 0;
	}

	@Override
	public boolean ramEnabled() {
		return this.ramEnable;
	}

}

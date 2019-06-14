package com.vtsman.gbemu.mbc;

import java.util.Calendar;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's third generation MBC, which has an RTC!
public class MBC3 implements MBC, MBCTimer {
	private int bank = 0;

	private int ramBank = 0;

	private boolean ramEnabled = true;

	private int mode = 0;
	private boolean inClock = false;

	private byte[] regs = new byte[5];
	private byte[] temp = new byte[5];

	public MBC3() {
		this.regs = updateTimeRegs();
	}

	//Set registers
	@Override
	public void romWrite(int addr, byte value) {
		if (addr < 0x3000 && addr >= 0x2000) {
			bank = (bank & ~0xff) | (value & 0xff);
		} else if (addr < 0x6000 && addr >= 0x4000) {
			if (value >= 0x08) {
				mode = value - 0x08;
			} else {
				ramBank = value & 0xff;
			}
		} else if (0 <= addr && addr < 0x2000) {
			if (value == 0x0a) {
				this.ramEnabled = true;
			}
			if (value == 0x0) {
				this.ramEnabled = false;
			}
		} else if (0x6000 <= addr && addr < 0x8000) {
			if (value == 0) {
				this.temp = this.updateTimeRegs();
			}
			if (value == 1) {
				this.regs = this.temp;
			}
		}
		//System.out.printf("-> 0x%04x\n", this.bank);
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
		return this.ramEnabled;
	}

	@Override
	public boolean inTimer() {
		return this.inClock;
	}

	//Hacked together RTC
	@SuppressWarnings("deprecation")
	private byte[] updateTimeRegs() {
		byte[] r = new byte[5];
		r[0] = (byte) Calendar.getInstance().getTime().getSeconds();
		r[1] = (byte) Calendar.getInstance().getTime().getMinutes();
		r[2] = (byte) Calendar.getInstance().getTime().getHours();
		r[3] = (byte) ((Calendar.getInstance().getTime().getTime() / timeToDays) & 0xff);
		r[4] = (byte) (((Calendar.getInstance().getTime().getTime() / timeToDays) >> 8) & 0x1);
		return r;
	}

	private static final long timeToDays = 60 * 60 * 24 * 1000;

	@Override
	public byte getTimerValue() {
		return this.regs[this.mode];
	}
}

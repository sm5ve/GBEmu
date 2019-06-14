package com.vtsman.gbemu.mbc;

//Spencer Martin
//5/20/16
//This interface specifies which functions need to be defined in MBCs which offer
//real time clock functionality
public interface MBCTimer {
	public boolean inTimer();

	public byte getTimerValue();
}

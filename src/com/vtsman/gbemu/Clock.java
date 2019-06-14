package com.vtsman.gbemu;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's clock, which is used to time GPU functions and
//triggers an interrupt periodically
public class Clock implements IAddressable {

	private short divider = (short)0xABCC;

	private int timaCounterCritical = 16;
	private byte tima = 0;
	private byte tma = 0;

	private boolean started = true;

	private long totalTicks = 0;


	//Necessary to trigger the interrupt
	InterruptController c;

	public Clock(InterruptController ic) {
		this.c = ic;
	}

	public void reset() {
		/*ticks = 0;*/
	}

	//Called every time the CPU executes an instruction. Triggers interrupt if necessary.
	public void inc(int amount) {
		for(int i = 0; i < amount; i++){
			totalTicks++;
			divider++;
			if((divider & (timaCounterCritical - 1)) == timaCounterCritical >> 1){
				if(this.started) {
					tima++;
					if (tima == 0) {
						tima = tma;
						this.c.timerInterrupt();
					}
				}
			}
		}
	}

	public long getTicks() {
		return this.totalTicks;
	}

	@Override
	public byte read(int addr) {
		//System.out.println(addr);
		if (addr == 0xff04) {
			return (byte)(this.divider >> 8);
		}
		if (addr == 0xff05) {
			System.out.println("read tima as " + this.tima);
			return this.tima;
		}
		if (addr == 0xff06) {
			return this.tma;
		}
		if (addr == 0xff07) {
			int b3 = this.started ? 4 : 0;
			switch (this.timaCounterCritical){
				case 1024: return (byte)(b3 | 0);
				case 16: return (byte)(b3 | 0);
				case 64: return (byte)(b3 | 0);
				case 256: return (byte)(b3 | 0);
			}
		}
		return 0;
	}

	//Various reads / writes to internal registers
	@Override
	public void write(int addr, byte value) {
		if (addr == 0xff04) {
			this.divider = 0;
		}
		if (addr == 0xff05) {
			this.tima = value;
			System.out.println("wrote tima as " + this.tima);
		}
		if (addr == 0xff06) {
			this.tma = value;
		}
		if (addr == 0xff07) {
			switch (value & 3){
				case 0: this.timaCounterCritical = 1024; break;
				case 1: this.timaCounterCritical = 16; break;
				case 2: this.timaCounterCritical = 64; break;
				case 3: this.timaCounterCritical = 256; break;
			}
			if(((value & 4) == 0) && this.started){
				if((this.divider & this.timaCounterCritical >> 1) != 0){
					this.tima++;
				}
			}
			this.started = ((value & 4) != 0);
		}
	}

	@Override
	public boolean isAddressInRange(int addr) {
		return addr <= 0xff07 && addr >= 0xff04;
	}
}

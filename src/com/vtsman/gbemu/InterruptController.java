package com.vtsman.gbemu;

import java.util.concurrent.CopyOnWriteArrayList;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's interrupt controller, which temporarily halts
//normal CPU operation to call special functions whenever a hardware interrupt is
//triggered
public class InterruptController implements IAddressable {
	//TODO code in interrupt delays

	//Various interrupt registers
	private int mask = 0;
	private volatile int flags = 0;

	//The CPU needs to be known, otherwise it can't call the interrupt
	private CPU c;

	private void request(InterruptType type) {
		//flags |= (type.mask & this.mask);
		flags |= type.mask;
		//System.out.println("Requested interrupt " + type + " with mask " + type.mask);
		if(this.c != null && (type.mask & this.mask) != 0){
			this.c.exitHalt();
		}
	}

	//IAddressable functions
	@Override
	public byte read(int addr) {
		if (addr == 0xffff) {
			return (byte) this.mask;
		}
		if (addr == 0xff0f) {
			return (byte) (224 | this.flags);
		}
		return 0;
	}

	@Override
	public void write(int addr, byte value) {
		if (addr == 0xff0f) {
			this.flags = value;
		}

		if (addr == 0xffff) {
			this.mask = value;
			//System.out.println(this.mask);
		}
	}

	@Override
	public boolean isAddressInRange(int addr) {
		if (addr == 0xffff) {
			return true;
		}
		if (addr == 0xff0f) {
			return true;
		}
		return false;
	}

	//Functions to call specific interrupts
	public void keyPressInterrupt(int button, boolean pressed) {
		this.request(InterruptType.JOYPAD);
	}

	public void vblankInterrupt() {
		this.request(InterruptType.VBLANK);
	}

	public void timerInterrupt() {
		this.request(InterruptType.TIMER);
	}

	public void lcdStatInterrupt() {
		this.request(InterruptType.LCDSTAT);
	}

	//Called each clock cycle - will run an interrupt if needed
	private final InterruptType[] order = {InterruptType.VBLANK, InterruptType.LCDSTAT, InterruptType.TIMER, InterruptType.SERIAL, InterruptType.JOYPAD};

	public void updateInterrupts() {
		if (this.c.areIntsEnabled()) {
			for(int i = 0; i < order.length; i++){
				if(tryInterrupt(order[i])){
					return;
				}
			}
		}
	}

	public void requestBP(){
		this.c.reqBP();
	}

	//Decodes enum into address, jumps to it
	private boolean tryInterrupt(InterruptType type) {
		if ((this.flags & (this.mask & type.mask)) != 0) {
			this.flags &= ~(type.mask);
			switch (type) {
			case VBLANK:
				this.c.gotoInterrupt(0x40);
				return true;
			case LCDSTAT:
				this.c.gotoInterrupt(0x48);
				return true;
			case TIMER:
				this.c.gotoInterrupt(0x50);
				return true;
			case SERIAL:
				this.c.gotoInterrupt(0x58);
				return true;
			case JOYPAD:
				this.c.gotoInterrupt(0x60);
				return true;
			}
		}
		return false;
	}

	public void setCPU(CPU c) {
		this.c = c;
	}

	public enum InterruptType {
		VBLANK(0), LCDSTAT(1), TIMER(2), SERIAL(3), JOYPAD(4);

		public final int mask;

		private InterruptType(int mask) {
			this.mask = (1 << mask);
		}
	}

}

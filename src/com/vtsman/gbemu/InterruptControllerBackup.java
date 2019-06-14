package com.vtsman.gbemu;

import java.util.concurrent.CopyOnWriteArrayList;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's interrupt controller, which temporarily halts
//normal CPU operation to call special functions whenever a hardware interrupt is
//triggered
public class InterruptControllerBackup implements IAddressable {
	//Various interrupt registers
	private int mask = 0;
	private int flags = 0;

	//A thread-safe arraylist which allows me to call interrupts from other threads
	//This is important because otherwise, pushing buttons would be very unreliable
	private CopyOnWriteArrayList<InterruptAction> queue = new CopyOnWriteArrayList<InterruptAction>();

	//The CPU needs to be known, otherwise it can't call the interrupt
	private CPU c;

	//Helper functions
	public void setMask(int value) {
		this.mask = value;
	}

	public void setIntVBlank(boolean state) {
		this.setInt(state, 1);
	}

	private void setInt(boolean state, int i) {
		if (this.c.areIntsEnabled()) {
			if ((mask & i) > 0) {
				if (state) {
					flags |= i;
				} else {
					flags &= ~i;
				}
			}
		}
	}

	public int getFlags() {
		return flags;
	}

	//IAddressable functions
	@Override
	public byte read(int addr) {
		if (addr == 0xffff) {
			return (byte) this.mask;
		}
		if (addr == 0xff0f) {
			return (byte) this.flags;
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
		this.addInterrupt(new ButtonAction(button, pressed), 16);
		this.flags |= InterruptType.JOYPAD.mask;
	}

	public void vblankInterrupt() {
		this.addInterrupt(new InterruptAction() {

			@Override
			public void execute() {
				triggerInterrupt(InterruptType.VBLANK);
			}

			@Override
			public InterruptType getType() {
				return InterruptType.VBLANK;
			}

		}, 1);
		this.flags |= InterruptType.VBLANK.mask;
	}

	public void timerInterrupt() {
		this.addInterrupt(new InterruptAction() {

			@Override
			public void execute() {
				triggerInterrupt(InterruptType.TIMER);
			}

			@Override
			public InterruptType getType() {
				return InterruptType.TIMER;
			}

		}, 4);
		this.flags |= InterruptType.TIMER.mask;
	}

	public void lcdStatInterrupt() {
		this.addInterrupt(new InterruptAction() {

			@Override
			public void execute() {
				triggerInterrupt(InterruptType.LCDSTAT);
			}

			@Override
			public InterruptType getType() {
				return InterruptType.LCDSTAT;
			}

		}, 4);
		this.flags |= InterruptType.LCDSTAT.mask;
	}

	private void addInterrupt(InterruptAction a, int flag) {
		if (this.c == null) {
			return;
		}
		if (this.c.areIntsEnabled() && ((this.flags & flag) > 0)) {
			this.queue.add(a);
		}
	}

	//Called each clock cycle - will run an interrupt if needed
	public void updateInterrupts() {
		if (this.c.areIntsEnabled()) {
			if (this.queue.size() > 0) {
				//System.out.println("Triggering interrupt " + this.queue.get(0).getType());
				this.queue.remove(0).execute();
			}
		}
	}

	//Decodes enum into address, jumps to it
	private void triggerInterrupt(InterruptType type) {
		if (this.c.areIntsEnabled() && ((this.mask & type.mask) != 0)) {
			this.flags &= ~(type.mask);
			switch (type) {
			case VBLANK:
				this.c.gotoInterrupt(0x40);
				break;
			case LCDSTAT:
				this.c.gotoInterrupt(0x48);
				break;
			case TIMER:
				this.c.gotoInterrupt(0x50);
				break;
			case SERIAL:
				this.c.gotoInterrupt(0x58);
				break;
			case JOYPAD:
				this.c.gotoInterrupt(0x60);
				break;
			}
		}
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

	//Actions that are added to interrupt queue
	private interface InterruptAction {
		public void execute();

		public InterruptType getType();
	}

	private class ButtonAction implements InterruptAction {

		public ButtonAction(int button, boolean pressed) {
		}

		@Override
		public void execute() {
			triggerInterrupt(InterruptType.JOYPAD);
		}

		@Override
		public InterruptType getType() {
			return InterruptType.JOYPAD;
		}

	}

}

package com.vtsman.gbemu;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.vtsman.gbemu.mbc.DummyMBC;
import com.vtsman.gbemu.mbc.MBC;
import com.vtsman.gbemu.mbc.MBC1;
import com.vtsman.gbemu.mbc.MBC3;
import com.vtsman.gbemu.mbc.MBC5;
import com.vtsman.gbemu.mbc.MBCTimer;

//Spencer Martin
//5/20/16
//This class simulates a gameboy cartridge, which contains a ROM chip, but may also contain
//a memory bank controller, extra RAM, a real time clock, or some other peripheral
public class ROM implements IAddressable {

	private byte[] data;
	private MBC mbc;
	private byte[] cram;

	public ROM(InputStream source) throws IOException {
		//Load rom data into array
		ArrayList<Byte> buffer = new ArrayList<Byte>();
		while (source.available() > 0) {
			byte[] b = new byte[source.available()];
			source.read(b);
			for (byte by : b) {
				buffer.add(by);
			}
		}
		data = new byte[buffer.size()];
		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.get(i);
		}
		//Parse out info from cartridge header, set MBC accordingly
		switch (this.getType()) {
			case 0x01:
				this.mbc = new MBC1(false, false);
				break;
			case 0x02:
				this.mbc = new MBC1(true, false);
				this.cram = new byte[131072];
				break;
			case 0x03:
				this.mbc = new MBC1(true, true);
				this.cram = new byte[131072];
				break;
		case 0x10:
		case 0x12:
		case 0x13:
			this.cram = new byte[131072];
		case 0x0f:
		case 0x11:
			this.mbc = new MBC3();
			break;
		case 0x1A:
		case 0x1B:
			this.cram = new byte[131072];
		case 0x19:
		case 0x1C:
		case 0x1D:
		case 0x1E:
			this.mbc = new MBC5();
			break;
		default:
			System.err.printf("WARNING: Dummy MBC in use. MBC for cart type 0x%x unimplemented\n", this.getType());
			this.mbc = new DummyMBC();
			break;
		}
		//System.out.println(Integer.toHexString(data.length));
	}

	@Override
	public byte read(int addr) {
		//reads from 0th ROM page
		if (addr >= 0 && addr < 0x4000) {
			return data[addr];
		}
		//Reads from cartridge RAM
		if (addr >= 0xa000 && addr < 0xc000) {
			if (cram != null && this.mbc.ramEnabled()) {
				if (this.mbc instanceof MBCTimer) {
					MBCTimer time = (MBCTimer) this.mbc;
					if (time.inTimer()) {
						return time.getTimerValue();
					}
				}
				return cram[(addr - 0xa000) + (0x2000 * this.mbc.getRamBank())];
			} else {
				return 0;
				//System.out.printf("Error: attempted to read from cartridge ram on incompatible cart @ 0x%04x\n", addr);
				//System.exit(-1);
			}
		}
		if (addr - 0x4000 + (0x4000 * this.mbc.getRomBank()) > data.length) {
			return 0;
		}
		//Reads data from ROM at correct ROM page
		return data[addr - 0x4000 + (0x4000 * this.mbc.getRomBank())];
	}

	@Override
	public void write(int addr, byte value) {
		//Writing to CRAM makes sense
		if (addr >= 0xa000 && addr < 0xc000) {
			if (cram != null && this.mbc.ramEnabled()) {
				cram[(addr - 0xa000) + (0x2000 * this.mbc.getRamBank())] = value;
			} else {
				//System.out.printf("Error: attempted to write to cartridge ram on incompatible cart @ 0x%04x\n", addr);
				//System.exit(-1);
			}
		} else {
			//Writing to ROM does not, unless you recognize that you're actually
			//writing to the MBC
			this.mbc.romWrite(addr, value);
		}
	}

	@Override
	public boolean isAddressInRange(int addr) {
		return (addr >= 0 && addr < 0x8000) || (addr >= 0xa000 && addr < 0xc000);
	}

	//Parse out info from cart header
	public String getTitle() {
		byte[] str = new byte[17];
		for (int i = 0; i < 16; i++) {
			str[i] = data[0x134 + i];
		}
		return new String(str);
	}

	public int getType() {
		return data[0x147];
	}

	public boolean isGBC() {
		return (data[0x143] & 0xff) == 0x80 || (data[0x143] & 0xff) == 0xC0;
	}

	@Override
	public String toString() {
		return this.getTitle() + " cart type: " + Integer.toHexString(this.getType());
	}

	public MBC getMBC() {
		return this.mbc;
	}
}

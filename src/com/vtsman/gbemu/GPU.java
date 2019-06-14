package com.vtsman.gbemu;

import jdk.internal.org.objectweb.asm.tree.ParameterNode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's GPU, which cycles through a few states and draws the
//Tile map and sprites
public class GPU implements IAddressable {

	//GPU resolution
	private final int RESX = 160;
	private final int RESY = 144;

	private final int OAM_DMA_LEN = 0xa0;

	private final int HBLANK_DURATION = 204;
	private final int VBLANK_DURATION = 4560;
	private final int OAM_DURATION = 80;
	private final int VRAM_DURATION = 172;

	private final int INT_HBLANK = 1 << 3;
	private final int INT_VBLANK = 1 << 4;
	private final int INT_OAM = 1 << 5;
	private final int INT_COINC = 1 << 6;

	//Final image to be displayed
	public final BufferedImage image = new BufferedImage(RESX, RESY, BufferedImage.TYPE_INT_RGB);
	//Current scan line
	private int scan = 0, scanComp = 0;

	//The state of the GPU, there are four modes that the GPU cycles through
	private State st;
	//Calculate delta time for CPU
	private long ticks = 0;
	private long cpuLastTicks = 0;

	private boolean enableIntCoinc = true, enableIntVBlank = true, enableIntHBlank = true, enableIntOAM = true;
	//VRAM bank, used in GBC only
	private int bank = 0;

	//Direct memory access registers, GBC feature only
	//Used to quickly copy large chunks of memory into the VRAM
	private int dmaSource, dmaDest, dmaLength;
	private boolean dmaHblank;

	private boolean inDMA = false;
	private int dmaProgress = 0, dmaIntermediateCountdown = 0;

	//CPU interface stuff
	private Clock c;
	private InterruptController ic;

	//'Character RAM' - stores pixles for each tile
	private byte[][] charRam = new byte[2][0x1800];

	//'Background RAM' - store the order of the tiles
	private byte[][] bg1 = new byte[2][0x400];
	private byte[][] bg2 = new byte[2][0x400];

	//Color pallet for background
	private byte[] bgPallet = { (byte) 0xFF, 0x7F, (byte) 0xBF, 0x03, 0x1F, 0x00, 0x00, 0x00, (byte) 0xFF, 0x7F,
			(byte) 0x80, 0x69, 0x1F, 0x00, 0x00, 0x00, (byte) 0xFF, 0x7F, (byte) 0xF7, 0x63, 0x1F, 0x00, 0x00, 0x00,
			(byte) 0xFF, 0x7F, (byte) 0xBF, 0x03, (byte) 0x80, 0x69, 0x00, 0x00, (byte) 0xFF, 0x7F, (byte) 0x94, 0x7E,
			(byte) 0x80, 0x69, 0x00, 0x00, (byte) 0xFF, 0x7F, (byte) 0xF7, 0x63, (byte) 0x80, 0x69, 0x00, 0x00,
			(byte) 0xC0, 0x00, (byte) 0xC0, 0x00, (byte) 0xC0, 0x00, (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00 };
	//Registers for setting color pallet values
	private boolean shouldIncBg = false;
	private int bgIndex = 0;

	//Regular gameboy 'color pallet' register - not real color pallets
	private int bwBgPalletNum = 0;
	private byte bwBgPallet0 = (byte) 0xE4;
	private byte bwBgPallet1 = (byte) 0xE4;

	//Sprite pallet - stores colors for sprites
	private byte[] spritePallet = new byte[0x40];
	private boolean shouldIncSprite = false;
	private int spriteIndex = 0;

	//Sprite memory - each sprite is 4 bytes long
	private byte[] sprites = new byte[0x100];

	//Foreground map - used to block out sprites
	private boolean[][] fgMap = new boolean[RESX][RESY];

	//The screen window, used to update the graphics
	private Screen s;

	//Various scroll registers - specify where background and windows are rendered
	private int scrollX = 0, scrollY = 0;
	private int winX = 7, winY = 0;

	private Graphics2D g2d;

	private int control = 0x91;

	private byte lcdstat = 0x0;

	//Debug screen for debugging
	private GPUDebugScreen debug;

	//Tile debugging buffers
	public final BufferedImage map1 = new BufferedImage(8 * 16, 8 * 16, BufferedImage.TYPE_INT_RGB);
	public final BufferedImage map2 = new BufferedImage(8 * 16, 8 * 16, BufferedImage.TYPE_INT_RGB);

	//MMU for DMA operations
	private MMU m;

	//Whether or not this is a gameboy color
	private boolean color;

	//frame skip stuff to make it run fast
	private int frameSkip = 0;
	private int frames = 0;

	public GPU(Clock clock, InterruptController ic, Screen sc, IO io, boolean gbc) {
		this(clock, ic, sc, io, gbc, null);
	}

	public GPU(Clock clock, InterruptController ic, Screen sc, IO io, boolean gbc, GPUDebugScreen s) {
		this.c = clock;
		this.ic = ic;
		this.st = State.HBLANK;
		this.s = sc;
		this.g2d = this.image.createGraphics();
		this.debug = s;
		this.color = gbc;
	}

	public void setFrameSkip(int i) {
		this.frameSkip = i;
	}

	//Called every clock cycle
	public void update() {
		int mult = 1;
		if (this.m != null && (this.m.read(0xff4d) & 0x80) == 0x80) {
			mult = 2;
		}
		if(this.inDMA && !this.dmaHblank){
		//while(this.inDMA){
			this.dmaIntermediateCountdown++;
			if(this.dmaIntermediateCountdown / mult >= (160 * 4 * OAM_DMA_LEN / this.dmaLength)){
				this.dmaIntermediateCountdown = 0;
				this.m.write(this.dmaDest + this.dmaProgress, this.m.read(this.dmaSource + this.dmaProgress));
				this.dmaProgress++;
			}
			if(this.dmaProgress == this.dmaLength){
				this.inDMA = false;
			}
		}
			//Get the number of CPU ticks since last instruction
		this.ticks += this.c.getTicks() - this.cpuLastTicks;
		this.cpuLastTicks = this.c.getTicks();
		//Switch behavior based on current state
		switch (st) {
		case HBLANK:
			if (this.ticks >= HBLANK_DURATION * mult) {
				//Increment scan line
				this.scan++;
				if(this.scan == this.scanComp && this.enableIntCoinc){
					triggerInterrupt(INT_COINC);
				}
				/*if(this.enableIntHBlank){
					triggerInterrupt(INT_HBLANK);
				}*/
				//If the scan line at the very end, update the screen
				if (this.scan == RESY - 1) {
					this.frames++;
					//this.ic.setIntVBlank(true);
					//If the screen is on and it's not a frame skip, the render
					if (this.s != null && (this.frames % (this.frameSkip + 1)) == 0) {

						if ((this.control & 2) != 0 && (this.control & (1 << 7)) != 0) {
							this.drawSprites();
							//System.out.println("Drawing sprites");
						}
						this.s.update(this.image);
					}
					//If the debug screen exists, draw all of the debugging info
					if (this.debug != null) {
						this.updateDebugScreen();
						this.debug.map1.update(this.map1);
						this.debug.map2.update(this.map2);

						Color[][] bgp = new Color[8][8];
						for (int i = 0; i < 8; i++) {
							for (int j = 0; j < 4; j++) {
								int n = (i << 3) | (j << 1);
								int data = ((this.bgPallet[n | 1] & 0x7f) << 8) | (this.bgPallet[n] & 0xff);
								int r = ((data) & 0x1f);
								int g = ((data >> 5) & 0x1f);
								int b = ((data >> 10) & 0x1f);

								bgp[i][j] = new Color(r * CONV, g * CONV, b * CONV);

								data = ((this.spritePallet[n | 1] & 0x7f) << 8) | (this.spritePallet[n] & 0xff);
								r = ((data) & 0x1f);
								g = ((data >> 5) & 0x1f);
								b = ((data >> 10) & 0x1f);

								bgp[i][j + 4] = new Color(r * CONV, g * CONV, b * CONV);
							}
						}

						this.debug.updateBgPallet(bgp);
					}
					//Trigger the VBLANK interrupt
					this.ic.vblankInterrupt();
					this.st = State.VBLANK;
					//If there's a DMA operation scheduled to run at the beginning of HBLANK, do it
					//Trigger LCDSTAT interrupt
					if(this.enableIntVBlank){
						triggerInterrupt(INT_VBLANK);
					}

					//Blank the screen
					this.g2d.setColor(Color.black);
					this.g2d.drawRect(0, 0, RESX, RESY);
				} else {
					//OAM state - in the real gameboy, memory is copied into internal VRAM
					this.st = State.OAM;
					if(this.enableIntOAM){
						triggerInterrupt(INT_OAM);
					}
				}
				this.ticks -= HBLANK_DURATION * mult;
				for(int temp = 0; temp < 0x10 && this.inDMA && this.dmaHblank; temp++){
					//while(this.inDMA){
					this.dmaIntermediateCountdown++;
					if(this.dmaIntermediateCountdown / mult >= (160 * 4 * OAM_DMA_LEN / this.dmaLength)){
						this.dmaIntermediateCountdown = 0;
						this.m.write(this.dmaDest + this.dmaProgress, this.m.read(this.dmaSource + this.dmaProgress));
						this.dmaProgress++;
					}
					if(this.dmaProgress == this.dmaLength){
						this.inDMA = false;
					}
				}
			}
			break;
		case VBLANK:
			//More GPU state emulation
			if (this.ticks >= VBLANK_DURATION * mult) {
				this.scan++;
				if (this.scan > 153) {
					this.scan = 0;
					this.st = State.OAM;
					if(this.enableIntOAM){
						triggerInterrupt(INT_OAM);
					}
				}
				if(this.scan == this.scanComp && this.enableIntCoinc){
					triggerInterrupt(INT_COINC);
				}
				this.ticks -= VBLANK_DURATION * mult;
			}
			break;
		case OAM:
			//How boring
			if (this.ticks >= OAM_DURATION * mult) {
				this.st = State.VRAM;
				/*if(this.enableIntOAM){
					triggerInterrupt(INT_HBLANK);
				}*/
				this.ticks -= OAM_DURATION * mult;
			}
			break;
		case VRAM:
			//Finally, we can draw a scan line
			if (this.ticks >= VRAM_DURATION * mult) {
				this.st = State.HBLANK;
				this.drawScanLine();
				this.ticks -= VRAM_DURATION * mult;
				if(this.enableIntHBlank){
					triggerInterrupt(INT_HBLANK);
				}
			}
		}
	}

	//Does an LCD stat interrupt
	private void triggerInterrupt(int mask) {
		if ((this.control & (1 << 7)) != 0) {
			lcdstat = (byte)mask;
			this.ic.lcdStatInterrupt();
		}
		//this.ic.
	}

	//Draws necessary parts of screen depending on flags
	private void drawScanLine() {
		if (this.scan >= RESY || (this.frames % (this.frameSkip + 1)) != 0) {
			return;
		}
		for (int i = 0; i < RESX; i++) {
			if ((this.control & (1 << 7)) != 0) {
				if ((this.control & 1) != 0) {
					//Background first
					drawBGPixel(i, scan);
				}
				if ((this.control & (1 << 5)) != 0) {
					//Then window
					drawWindowPixel(i, scan);
				}
			}
		}
	}

	private int getTileIndex(int x, int y) {
		//Converts (x,y) coords into an index
		return (((x / 8) & 0x1f) + 32 * ((y / 8) & 0x1f));
	}

	private byte[] getTS1(int tileNum) {
		//Gets tile from tile set 1
		byte[] out = new byte[16];
		for (int i = 0; i < 16; i++) {
			out[i] = this.charRam[this.bank][tileNum * 16 + i];
		}
		return out;
	}

	/*private byte getAttribute(int x, int y, boolean table){
		int index = x + y * 32;
		if(table){

		}
	}*/

	private byte[] getTS(int tileNum) {
		//gets tile from tile set 2
		byte[] out = new byte[16];
		int base = 0x1000 + tileNum * 16;
		if ((byte) tileNum < 0) {
			base = 0x800 + 16 * (128 + (byte) tileNum); // I forgot parens here... screwed up everything
		} //Order of operations man... so hard.
		for (int i = 0; i < 16; i++) {
			out[i] = this.charRam[this.bank][base + i];
		}
		return out;
	}

	//Draw debugging info
	private void updateDebugScreen() {
		Graphics2D g1 = this.map1.createGraphics();
		Graphics2D g2 = this.map2.createGraphics();
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				byte[] tile1 = getTS(i + j * 16);
				byte[] tile2 = getTS1(i + j * 16);
				for (int x = 0; x < 8; x++) {
					for (int y = 0; y < 8; y++) {
						int i1 = this.getPixelFromTile(tile1, x, y);
						int i2 = this.getPixelFromTile(tile2, x, y);
						g1.setColor(new Color(i1 * (255 / 3), i1 * (255 / 3), i1 * (255 / 3)));
						g2.setColor(new Color(i2 * (255 / 3), i2 * (255 / 3), i2 * (255 / 3)));

						g1.drawRect(x + i * 8, y + j * 8, 1, 1);
						g2.drawRect(x + i * 8, y + j * 8, 1, 1);
					}
				}
			}
		}
	}

	//Gets the tile from a background map
	private int getTile(int index, int bank) {
		return this.bg2[bank][index] & 0xff;
	}

	private int getTile1(int index, int bank) {
		return this.bg1[bank][index] & 0xff;
	}

	//Reads a specific pixel from a tile
	private int getPixelFromTile(byte[] tile, int x, int y) {
		x = 7 - x;
		//Pixel data is stored across 2 bytes
		byte bit1 = tile[2 * (y & 7)];
		byte bit2 = tile[1 + 2 * (y & 7)];
		return (((bit1 >> (x & 7)) & 0x1) | (((bit2 >> (x & 7)) & 0x1) << 1));
	}

	private void drawBGPixel(int x, int y) {
		//Get pixel data, then draw it
		int rX = x + scrollX;
		int rY = y + scrollY;
		rX &= 0xff;
		rY &= 0xff;
		byte[] tile;
		int index = 0;
		if ((control & (1 << 3)) != 0) {
			index = getTile(getTileIndex(rX, rY), 0);
		} else {
			index = getTile1(getTileIndex(rX, rY), 0);
		}

		if ((control & (1 << 4)) == 0) {
			tile = getTS(index);
		} else {
			tile = getTS1(index);
		}
		int tX = rX % 8;
		int tY = rY % 8;

		int color = getPixelFromTile(tile, tX, tY);
		this.fgMap[x][y] = color != 0;

		//this.g2d.setColor(new Color(color * (0xff / 3), color * (0xff / 3), color * (0xff / 3)));
		this.g2d.setColor(this.getTileColor(index, rX, rY, color));
		this.g2d.drawRect(x, y, 1, 1);
	}

	private void drawWindowPixel(int x, int y) {
		//Get pixel data, then draw it
		x += winX - 7;
		y -= winY;
		byte[] tile;
		int index = getTile(getTileIndex(x - winX + 7, y - winY), 0);

		if ((control & (1 << 6)) != 0) {
			tile = getTS(index);
		} else {
			tile = getTS1(index);
		}
		int tX = x % 8;
		int tY = y % 8;

		int color = getPixelFromTile(tile, tX, tY);
		if ((color != 0 || ((this.control) & (1 << 1)) != 1) && (x < RESX && y < RESY && x > 0 && y > 0)) {
			//this.g2d.setColor(new Color(color * (0xff / 3), color * (0xff / 3), color * (0xff / 3)));
			this.g2d.setColor(this.getTileColor(index, x, y, color));
			this.g2d.drawRect(x, y, 1, 1);
		}
	}

	//Various register read/writes
	@Override
	public byte read(int addr) {
		if (addr == 0xff40) {
			return (byte) this.control;
		}
		if (addr == 0xff44) {
			return (byte) this.scan;
		}
		if (addr == 0xff41) {
			return (byte) (this.st.ordinal() | this.lcdstat | (this.scan == this.scanComp ? 4 : 0));
		}
		if (addr == 0xff42) {
			return (byte) this.scrollY;
		}
		if (addr == 0xff43) {
			return (byte) this.scrollX;
		}
		if (addr == 0xff47) {
			return (byte) this.bwBgPalletNum;
		}
		if (addr == 0xff48) {
			return this.bwBgPallet0;
		}
		if (addr == 0xff49) {
			return this.bwBgPallet1;
		}
		if (addr == 0xff4a) {
			return (byte) this.winY;
		}
		if (addr == 0xff4b) {
			return (byte) this.winX;
		}
		if (addr == 0xff4f) {
			return (byte) this.bank;
		}
		if (addr == 0xff51) {
			return (byte) (this.dmaSource >> 8);
		}
		if (addr == 0xff52) {
			return (byte) (this.dmaSource & 0xff);
		}
		if (addr == 0xff53) {
			return (byte) (this.dmaDest >> 8);
		}
		if (addr == 0xff54) {
			return (byte) (this.dmaDest & 0xff);
		}
		if (addr == 0xff55) { //DMA for pallets
			byte out = 0;
			if (this.dmaHblank) {
				out |= 1 << 7;
			}
			out |= (this.dmaLength / 0x10) - 1;
			return out; //TODO fix?
		}
		if (addr == 0xff68) {
			byte out = (byte) this.bgIndex;
			if (this.shouldIncBg) {
				out |= (1 << 7);
			}
			return out;
		}
		if (addr == 0xff69) {
			return this.bgPallet[this.bgIndex];
		}
		if (addr == 0xff6a) {
			byte out = (byte) this.spriteIndex;
			if (this.shouldIncSprite) {
				out |= (1 << 7);
			}
			return out;
		}
		if (addr == 0xff6b) {
			return this.spritePallet[this.spriteIndex];
		}
		if (addr >= 0x8000 && addr < 0x9800) {
			return this.charRam[this.bank][addr - 0x8000];
		}
		if (addr >= 0x9800 && addr < 0x9C00) {
			return this.bg1[this.bank][addr - 0x9800];
		}
		if (addr >= 0x9C00 && addr < 0xa000) {
			return this.bg2[this.bank][addr - 0x9C00];
		}
		if (addr >= 0xfe00 && addr < 0xff00) {
			return this.sprites[addr - 0xfe00];
		}
		return 0;
	}

	@Override
	public void write(int addr, byte value) {
		if (addr == 0xff40) {
			this.control = value & 0xff;
		}
		if(addr == 0xff41){
			System.out.println("Write to display flags: " + value);
			this.enableIntHBlank = (value & INT_HBLANK) == INT_HBLANK;
			this.enableIntVBlank = (value & INT_VBLANK) == INT_VBLANK;
			this.enableIntCoinc = (value & INT_COINC) == INT_COINC;
			this.enableIntOAM = (value & INT_OAM) == INT_OAM;

			System.out.println("HBlank: " + enableIntHBlank);
			System.out.println("VBlank: " + enableIntVBlank);
			System.out.println("OAM: " + enableIntOAM);
			System.out.println("COINC: " + enableIntCoinc);
		}
		if (addr == 0xff42) {
			this.scrollY = value;
		}
		if (addr == 0xff43) {
			this.scrollX = value;
		}
		if (addr == 0xff45){
			this.scanComp = value & 0xff;
			System.out.println("LYC = " + this.scanComp);
		}
		if (addr == 0xff46) {
			if(this.inDMA){
				return;
			}
			//System.out.println("OAM DMA");
			this.dmaSource = 0x100 * (value & 0xff);
			this.dmaDest = 0xfe00;
			this.dmaLength = OAM_DMA_LEN;

			this.startDMA(false);
		}
		if (addr == 0xff47) {
			//TODO do black and white palette reordering
			this.bwBgPalletNum = value & 1;
		}
		if (addr == 0xff48) {
			this.bwBgPallet0 = value;
		}
		if (addr == 0xff49) {
			this.bwBgPallet1 = value;
		}
		if (addr == 0xff4a) {
			this.winY = value;
		}
		if (addr == 0xff4b) {
			this.winX = value;
		}
		if (addr == 0xff4f) {
			this.bank = value & 1;
		}
		if (addr == 0xff68) {
			this.bgIndex = value & 0x3f;
			this.shouldIncBg = (value & (1 << 7)) != 0;
		}
		if (addr == 0xff69) {
			this.bgPallet[this.bgIndex] = value;
			//System.out.printf("<0x%02x>\n", value);
			if (this.shouldIncBg) {
				this.bgIndex = ((this.bgIndex + 1) & 0x3f);
			}
		}
		if (addr == 0xff6a) {
			this.spriteIndex = value & 0x3f;
			this.shouldIncSprite = (value & (1 << 7)) != 0;
		}
		if (addr == 0xff6b) {
			this.spritePallet[this.spriteIndex] = value;
			if (this.shouldIncSprite) {
				this.spriteIndex = (this.spriteIndex + 1) & 0x3f;
			}
		}
		if (addr == 0xff51) {
			if(this.inDMA){
				return;
			}
			this.dmaSource |= (value & 0xff) << 8;
		}
		if (addr == 0xff52) {
			if(this.inDMA){
				return;
			}
			this.dmaSource |= (value & 0xf0);
		}
		if (addr == 0xff53) {
			if(this.inDMA){
				return;
			}
			this.dmaDest |= (value & 0x1f) << 8;
		}
		if (addr == 0xff54) {
			if(this.inDMA){
				return;
			}
			this.dmaDest |= (value & 0xf0);
		}
		if (addr == 0xff55) {
			if(this.inDMA){
				return;
			}
			System.out.println("GBC DMA");
			System.out.println("From: " + this.dmaSource);
			System.out.println("To: " + this.dmaDest);
			boolean hblank = (value & (1 << 7)) != 0;
			System.out.println("hblank: " + hblank);
			this.dmaLength = 0x10 * ((value & 0x7f) + 1);
			/*if (!this.dmaHblank) {
				for (int i = 0; i < dmaLength; i++) {
					if (this.m != null) {
						this.m.write(this.dmaDest + i, this.m.read(this.dmaSource + i));
					}
				}
			}*/
			this.startDMA(hblank);
		}
		if (addr >= 0x8000 && addr < 0x9800) {
			this.charRam[this.bank][addr - 0x8000] = value;
		}
		if (addr >= 0x9800 && addr < 0x9C00) {
			this.bg1[this.bank][addr - 0x9800] = value;
		}
		if (addr >= 0x9C00 && addr < 0xA000) {
			this.bg2[this.bank][addr - 0x9C00] = value;
		}
		if (addr >= 0xfe00 && addr < 0xff00) {
			this.sprites[addr - 0xfe00] = value;
		}
	}

	private static final int CONV = (0xff / 0x1f);

	//Gets the color from sprite pallet data
	private Color getFromSpritePallet(int index, int color) {
		if (!this.color) {
			color = 3 - color;
			return new Color(color * 255 / 3, color * 255 / 3, color * 255 / 3);
		}
		int i = (index << 3) | (color << 1);
		int data = ((this.spritePallet[i | 1] & 0x7f) << 8) | (this.spritePallet[i] & 0xff);
		int r = (data) & 0x1f;
		int g = (data >> 5) & 0x1f;
		int b = (data >> 10) & 0x1f;

		return new Color((r * 255) / 31, (g * 255) / 31, (b * 255) / 31);
	}

	//Gets the tile color
	private Color getTileColor(int tileNum, int x, int y, int color) {
		color = 3 - color;
		return new Color(color * 255 / 3, color * 255 / 3, color * 255 / 3);
	}

	/*private Color getTileColor(int tileNum, int x, int y, int color) {
		if (!this.color) {
			color = 3 - color;
			return new Color(color * 255 / 3, color * 255 / 3, color * 255 / 3);
		}
		int attr;
		if ((control & (1 << 3)) != 0) {
			attr = getTile(getTileIndex(x, y), 1);
		} else {
			attr = getTile1(getTileIndex(x, y), 1);
		}
		attr = getTile1(getTileIndex(x, y), 1);
		int cbank = attr & 7;

		int i = (cbank << 3) | (color << 1);
		int data = ((this.bgPallet[i | 1] & 0x7f) << 8) | (this.bgPallet[i] & 0xff);
		//int data = ((this.spritePallet[i | 1] & 0x7f) << 8) | (this.spritePallet[i] & 0xff);
		int r = ((data) & 0x1f);
		int g = ((data >> 5) & 0x1f);
		int b = ((data >> 10) & 0x1f);

		return new Color(r * CONV, g * CONV, b * CONV);
	}*/

	@Override
	public boolean isAddressInRange(int addr) {
		if (addr == 0xff44 || addr == 0xff41 || addr == 0xff42 || addr == 0xff43 || addr == 0xff46 || addr == 0xff40 || addr == 0xff4a
				|| addr == 0xff4b || addr == 0xff47 || addr == 0xff4f || addr == 0xff48 || addr == 0xff49 || addr == 0xff45) {
			return true;
		}
		if (addr >= 0xff51 && addr <= 0xff55) {
			return true;
		}
		if (addr >= 0xff68 && addr <= 0xff6b) {
			return true;
		}
		if (addr >= 0x8000 && addr < 0xA000) {
			return true;
		}
		if (addr >= 0xfe00 && addr < 0xff00) {
			return true;
		}
		return false;
	}

	public void setMMU(MMU m) {
		this.m = m;
	}

	//Iterate over all sprites, get data, draw
	private void drawSprites() {
		boolean isDoubleHeight = ((this.control & 4) != 0);
		for (int i = 0; i < 40; i++) {
			byte[] sprite = new byte[4];
			for (int n = 0; n < 4; n++) {
				sprite[n] = this.sprites[i * 4 + n];
			}
			//x and y are stored with offsets
			int y = (sprite[0] & 0xff) - 16;
			int x = (sprite[1] & 0xff) - 8;

			//this specifies which tile in character ram to render
			int tileNum = sprite[2] & 0xff;
			boolean fg = (sprite[3] & (1 << 7)) == 0;
			boolean yflip = (sprite[3] & (1 << 6)) != 0;
			boolean xflip = (sprite[3] & (1 << 5)) != 0;
			boolean tileSet = (sprite[3] & (1 << 4)) != 0;
			int pallet = (sprite[3] & 7);

			if(isDoubleHeight){
				if(yflip){
					drawSpriteTile(tileSet, tileNum, xflip, yflip, x, y + 8, fg, pallet);
					drawSpriteTile(tileSet, tileNum + 1, xflip, yflip, x, y, fg, pallet);
				}
				else{
					drawSpriteTile(tileSet, tileNum, xflip, yflip, x, y, fg, pallet);
					drawSpriteTile(tileSet, tileNum + 1, xflip, yflip, x, y + 8, fg, pallet);
				}
			}
			else{
				drawSpriteTile(tileSet, tileNum, xflip, yflip, x, y, fg, pallet);
			}
		}
	}

	private void drawSpriteTile(boolean tileSet, int tileNum, boolean xflip, boolean yflip, int x, int y, boolean fg, int pallet){
		byte[] tile;
		if (tileSet) {
			tile = getTS(tileNum);
		} else {
			tile = getTS1(tileNum);
		}

		//Draw each pixel
		for (int xOff = 0; xOff < 8; xOff++) {
			for (int yOff = 0; yOff < 8; yOff++) {
				int rx = xflip ? x + 7 - xOff : x + xOff;
				int ry = yflip ? y + 7 - yOff : y + yOff;
				if (rx < RESX && ry < RESY && rx > 0 && ry > 0) {
					if (!(this.fgMap[rx][ry] && !fg)) {
						int color = getPixelFromTile(tile, xOff, yOff);
						if (color != 0) {
							this.g2d.setColor(getFromSpritePallet(pallet, color));
							this.g2d.drawRect(rx, ry, 1, 1);
						}
					}
				}
			}
		}
	}

	private void startDMA(boolean hblank){
		this.inDMA = true;
		this.dmaProgress = 0;
		this.dmaIntermediateCountdown = 0;
		this.dmaHblank = hblank;
	}

	private enum State {
		HBLANK, VBLANK, OAM, VRAM
	}
}

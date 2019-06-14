package com.vtsman.gbemu;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

//Spencer Martin
//5/20/16
//This class loads a configuration file and opens the rom selection window
public class Main {

	public static void main(String[] args) {
		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "GBEmu");
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			MainWindow mw = new MainWindow();

			if (romDirExists()) {
				mw.setROMs(enumerateROMs(getROMDir()));
			}

			//listLoadedClasses(Main.class.getClassLoader());



		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void listLoadedClasses(ClassLoader byClassLoader) {
		Class clKlass = byClassLoader.getClass();
		System.out.println("Classloader: " + clKlass.getCanonicalName());
		while (clKlass != java.lang.ClassLoader.class) {
			clKlass = clKlass.getSuperclass();
		}
		try {
			java.lang.reflect.Field fldClasses = clKlass
					.getDeclaredField("classes");
			fldClasses.setAccessible(true);
			Vector classes = (Vector) fldClasses.get(byClassLoader);
			for (Iterator iter = classes.iterator(); iter.hasNext();) {
				Class c = (Class) iter.next();
				System.out.println("   Loaded " + c);
				for(Field f : c.getFields()){
					System.out.println(f);
				}
				for(Method m : c.getMethods()){
					System.out.println(m);
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	//Helper function to quickly get the default ROMs
	public static ArrayList<File> getROMs() throws IOException {
		return enumerateROMs(getROMDir());
	}

	//Changes the config file
	public static void setROMDir(File f) throws FileNotFoundException {
		File cfg = new File("./.emu");
		PrintWriter pw = new PrintWriter(new FileOutputStream(cfg), false);
		pw.print(f.getPath().toString());
		pw.close();
	}

	//Checks if the rom directory exists
	private static boolean romDirExists() {
		return new File("./.emu").exists();
	}

	//Reads the config file, returns a directory
	private static File getROMDir() throws IOException {
		File f = new File("./.emu");
		if (!f.exists()) {
			f.createNewFile();
		}
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			Process p;
			try {
				p = Runtime.getRuntime().exec("attrib +h " + f.getPath());
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		String path = "";
		for (String s : Files.readAllLines(f.toPath())) {
			path += s;
		}
		return new File(path);
	}

	//Recursively scans the rom directory to find all gameboy roms
	private static ArrayList<File> enumerateROMs(File dir) {
		ArrayList<File> out = new ArrayList<File>();
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				out.addAll(enumerateROMs(f));
			} else {
				if (f.getName().endsWith(".gb") || f.getName().endsWith(".gbc")) {
					out.add(f);
				}
			}
		}
		return out;
	}

	//Runs openEmu in a new thread
	public static void openNewEmu(File f) {
		new Thread(() -> openEmu(f, Thread.currentThread())).start();
	}

	//Creates and configures all of the hardware, opens the emulator
	private static void openEmu(File f, final Thread t) {
		try {
			ROM r = new ROM(new FileInputStream(f));
			InterruptController ic = new InterruptController();
			Clock clock = new Clock(ic);
			System.out.printf("Loaded cart of type 0x%02x\n", r.getType());
			IO io = new IO();
			GPU g = new GPU(clock, ic, new Screen(ic, r.getTitle(), io, t), io, r.isGBC(), new GPUDebugScreen());
			RAM ram = new RAM();
			MMU mmu = new MMU(r, g, ic, clock, new ZeroPage(), new Sound(), ram, io);
			CPU c = new CPU(mmu, clock, false);
			g.setMMU(mmu);
			ic.setCPU(c);
			io.setGPU(g);
			io.setMMU(mmu);
			io.setRAM(ram);
			//c.addBreakPoint(0xaef);
			//c.addBreakPoint(0xc36f);
			//c.addBreakPoint(0xc2c5);
			//c.addBreakPoint(0xc08b);
			//c.addBreakPoint(0x2a2);
			//c.addBreakPoint(0x284);

			executeNormal(c, g, ic);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Loop runs a CPU instruction, the updates GPU state and interrupts
	private static void executeNormal(CPU c, GPU g, InterruptController ic) {
		while (true) {
			//2^22Hz
			c.execute();
			//c.debugExecute();
			g.update();
			ic.updateInterrupts();
		}
	}
}

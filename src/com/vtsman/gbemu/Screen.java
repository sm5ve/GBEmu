package com.vtsman.gbemu;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

//Spencer Martin
//5/20/16
//This class simulates the Gameboy's screen, which takes the buffered image from the GPU
//and exposes it to the user
public class Screen extends JFrame {

	private static final long serialVersionUID = 7640331520500116820L;

	private BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB); //temp image
	private Graphics2D g2d;
	private String t;
	private int lastInstruction = 0;
	private Thread thread;

	public Screen(InterruptController c, String title, IO io, final Thread t) {
		this.g2d = this.image.createGraphics();
		//Handle controls
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				if(t != null)
				t.stop();
			}
		});
		this.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					io.setStart(true);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
					io.setSelect(true);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_Z) {
					io.setA(true);
					c.keyPressInterrupt(0, true);
				}

				if (e.getKeyCode() == KeyEvent.VK_X) {
					io.setB(true);
					c.keyPressInterrupt(0, true);
				}

				if (e.getKeyCode() == KeyEvent.VK_UP) {
					io.setUp(true);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					io.setDown(true);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					io.setLeft(true);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					io.setRight(true);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					c.requestBP();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					io.setStart(false);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
					io.setSelect(false);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_Z) {
					io.setA(false);
					c.keyPressInterrupt(0, true);
				}

				if (e.getKeyCode() == KeyEvent.VK_X) {
					io.setB(false);
					c.keyPressInterrupt(0, true);
				}

				if (e.getKeyCode() == KeyEvent.VK_UP) {
					io.setUp(false);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					io.setDown(false);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					io.setLeft(false);
					c.keyPressInterrupt(0, true);
				}
				if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					io.setRight(false);
					c.keyPressInterrupt(0, true);
				}
			}

		});
		this.setSize(new Dimension(image.getWidth() * 2, image.getHeight() * 2));
		this.setLocationRelativeTo(null);
		this.setTitle(title);
		this.t = title;
		this.setVisible(true);
	}

	//Paint the buffer onto the screen
	@Override
	public void paint(Graphics g) {
		g.drawImage(image, 0, 0, image.getWidth() * 2, image.getHeight() * 2, null);
	}

	public void update(BufferedImage frame) {
		this.g2d.drawImage(frame, 0, 0, frame.getWidth(), frame.getHeight(), null);
		this.repaint();
	}
}

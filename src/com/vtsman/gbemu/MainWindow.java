package com.vtsman.gbemu;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

//Spencer Martin
//5/20/16
//This class allows the user to select a rom to run
public class MainWindow extends JFrame {
	private static final long serialVersionUID = -5262634893407811377L;

	//Stuff for JList
	private DefaultListModel<String> model = new DefaultListModel<String>();
	private JFrame tis;
	//ROMs
	private ArrayList<File> fs;

	//Set up window
	public MainWindow() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mnFile.add(mntmOpen);
		//Allows one to open up a rom directly
		mntmOpen.setAction(new Action() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				//Open file chooser
				chooser.setCurrentDirectory(new File("."));
				chooser.setDialogTitle("Choose a ROM");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						return (!f.isDirectory() && (f.getName().endsWith(".gb") || f.getName().endsWith(".gbc")));
					}

					@Override
					public String getDescription() {
						return null;
					}

				});
				chooser.setAcceptAllFileFilterUsed(false);
				if (chooser.showOpenDialog(tis) == JFileChooser.APPROVE_OPTION) {
					//Open the emulator!
					Main.openNewEmu(chooser.getSelectedFile());
				}
			}

			@Override
			public Object getValue(String key) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void putValue(String key, Object value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setEnabled(boolean b) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isEnabled() {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public void addPropertyChangeListener(PropertyChangeListener listener) {
				// TODO Auto-generated method stub

			}

			@Override
			public void removePropertyChangeListener(PropertyChangeListener listener) {
				// TODO Auto-generated method stub

			}

		});

		JMenuItem mntmSetRomDirectory = new JMenuItem("Set ROM directory");
		mnFile.add(mntmSetRomDirectory);
		mntmSetRomDirectory.setAction(new Action() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				//Another file chooser!
				chooser.setCurrentDirectory(new File("."));
				chooser.setDialogTitle("Choose a ROM directory");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setAcceptAllFileFilterUsed(false);
				if (chooser.showOpenDialog(tis) == JFileChooser.APPROVE_OPTION) {
					try {
						//Sets the rom directory to the selected one
						Main.setROMDir(chooser.getSelectedFile());
						//Updates rom list
						setROMs(Main.getROMs());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}

			@Override
			public Object getValue(String key) {
				return null;
			}

			@Override
			public void putValue(String key, Object value) {
			}

			@Override
			public void setEnabled(boolean b) {
			}

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public void addPropertyChangeListener(PropertyChangeListener listener) {
			}

			@Override
			public void removePropertyChangeListener(PropertyChangeListener listener) {
			}

		});

		mntmOpen.setText("Open ROM");
		mntmSetRomDirectory.setText("Set ROM directory");
		getContentPane().setLayout(new GridLayout(0, 1));

		JList<String> list = new JList<String>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getContentPane().add(new JScrollPane(list));

		//Checks for double click on rom
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				@SuppressWarnings("unchecked")
				JList<String> list = (JList<String>) evt.getSource();
				if (evt.getClickCount() == 2) {
					int index = list.locationToIndex(evt.getPoint());
					Main.openNewEmu(fs.get(index));
				}
			}
		});

		this.setLocationRelativeTo(null);
		this.setVisible(true);
		tis = this;
	}

	//Update stored roms
	public void setROMs(ArrayList<File> f) {
		this.model.clear();
		for (File fi : f) {
			this.model.addElement(fi.getName());
		}
		this.fs = f;
	}

}

package main;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;


import maps.BuildRoadMap;


public class ViewMap  {
	private JFrame jf;
	BuildRoadMap map;
	boolean stop;

	private void dispose() {
		if (jf != null) {
			jf.dispose();
			jf.setVisible(false);
		}
		if (map != null)
			map.dispose();
	}

	public ViewMap() {
		stop = false;
		jf = new JFrame();
		String fileName = "ExpansionMap.geojson";
		String path= "E:/PlayWithMaps";
		map = new BuildRoadMap(fileName,path);

		// tests
		jf.setSize(800, 600);
		jf.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		jf.setAlwaysOnTop(true);
		jf.add(map);
		jf.setVisible(true);

		// must be intialized in javafx thread, only initialize once
		jf.addWindowListener(new WindowListener() {
			@Override
			public void windowClosed(WindowEvent e) {
				jf.dispose();
				stop=true;
			}

			@Override
			public void windowOpened(WindowEvent e) {

			}

			@Override
			public void windowClosing(WindowEvent e) {
				jf.dispose();
				stop=true;
//				while (map.isLoading()) {
//					try {
//						Thread.sleep(100);
//					} catch (InterruptedException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//				}
//				// must call in javafx thread
//				Platform.runLater(() -> {
//					map.cancelChanges(null);
//				});

			}

			@Override
			public void windowIconified(WindowEvent e) {

			}

			@Override
			public void windowDeiconified(WindowEvent e) {

			}

			@Override
			public void windowActivated(WindowEvent e) {

			}

			@Override
			public void windowDeactivated(WindowEvent e) {

			}

		});
		while (!stop) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
	}
}

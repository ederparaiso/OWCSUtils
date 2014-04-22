package org.eder.owcs.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.eder.owcs.DependencyHelper;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class DependencyView {

	private JFrame frame;
	private JList<String> lstWcselements;
	private JList<String> lstElemUses;
	private JList<String> lstElemUsedBy;
	private JTabbedPane tabDependencies;
	private File file;
	private boolean isFile;
	private DirectedGraph<String, DefaultEdge> dependencyGraph;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DependencyView window = new DependencyView();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public DependencyView() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 650, 500);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu menuFile = new JMenu("File");
		menuBar.add(menuFile);
		
		JMenuItem mitemOpen = new JMenuItem("Open...");
		mitemOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JFileChooser fc = new JFileChooser();
		        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); 
		        int returnVal = fc.showOpenDialog(frame);
		        
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                file = fc.getSelectedFile();
	                isFile = file.isFile();
	                //TODO Colocar status bar informando o arquivo carregado
	            } 
			}
		});
		menuFile.add(mitemOpen);
		
		JMenu menuDependency = new JMenu("Dependencies");
		menuBar.add(menuDependency);
		
		JMenuItem mitemGenGraph = new JMenuItem("Generate Graph");
		mitemGenGraph.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (file == null)
					JOptionPane.showMessageDialog(frame, "No file loaded.");
				else{
					String path = file.getAbsolutePath();
					dependencyGraph = isFile ? DependencyHelper.calculateDependenciesFromSingleElement(path)
							: DependencyHelper.calculateDependenciesFromElementsFolder(path);
					loadElements(dependencyGraph.vertexSet());
				}
			}
		});
		menuDependency.add(mitemGenGraph);
		
		lstWcselements = new JList<String>();
		lstWcselements.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lstWcselements.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (!event.getValueIsAdjusting()) {
					loadDependeciesByElement(lstWcselements.getSelectedValue());
					if(tabDependencies != null)
						tabDependencies.setSelectedIndex(0);
				}
			}
		});
		
		JScrollPane spWcselements = new JScrollPane();
		spWcselements.setPreferredSize(new Dimension(frame.getWidth()/3, frame.getHeight()));
		spWcselements.setViewportView(lstWcselements);
		frame.getContentPane().add(spWcselements, BorderLayout.WEST);
		
		tabDependencies = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabDependencies, BorderLayout.CENTER);
		
		lstElemUses = new JList<String>();
		lstElemUses.setEnabled(false);
		
		JScrollPane spElemUses = new JScrollPane();
		spElemUses.setViewportView(lstElemUses);
		tabDependencies.addTab("Uses", null, spElemUses, null);
		tabDependencies.setEnabledAt(0, true);

		lstElemUsedBy = new JList<String>();
		lstElemUsedBy.setEnabled(false);
		
		JScrollPane spElemUsedBy = new JScrollPane();
		spElemUsedBy.setViewportView(lstElemUsedBy);
		tabDependencies.addTab("Used by", null, spElemUsedBy, null);
		tabDependencies.setEnabledAt(1, true);
		
	}
	
	private void loadElements(Set<String> elements) {
		if (lstWcselements != null) {
			lstWcselements.removeAll();
			String[] elementsArr = new String[elements.size()];
			lstWcselements.setListData(elements.toArray(elementsArr));
		}
	}
	
	private void loadDependeciesByElement(String element) {
		if (lstElemUses != null) {
			lstElemUses.removeAll();
			List<String> usedElems = DependencyHelper.getElementsUsedBy(element, dependencyGraph);
			String[] elementsArr = new String[usedElems.size()];
			lstElemUses.setListData(usedElems.toArray(elementsArr));
		}
		if (lstElemUsedBy != null) {
			lstElemUsedBy.removeAll();
			List<String> dependents = DependencyHelper.getDependentsOf(element, dependencyGraph);
			String[] elementsArr = new String[dependents.size()];
			lstElemUsedBy.setListData(dependents.toArray(elementsArr));
		}
	}
}

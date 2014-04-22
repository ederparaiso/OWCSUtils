package org.eder.owcs.gui;

import java.awt.geom.*;
import java.util.Iterator;
import java.util.Random;

import javax.swing.*;

import org.eder.owcs.DependencyHelper;
import org.jgraph.*;
import org.jgraph.graph.*;
import org.jgrapht.*;
import org.jgrapht.ext.*;
import org.jgrapht.graph.*;
import org.jgrapht.graph.DefaultEdge;


public class DependencyGraphView {
	JGraph jgraph;
	
	public static void main(String[] args) {
		new DependencyGraphView(); 
	}
	
	public DependencyGraphView() {
		DirectedGraph<String, DefaultEdge> dgraph = DependencyHelper.calculateDependenciesFromSingleElement("C:\\temp\\RDLaHome.jsp");
		JGraphModelAdapter<String, DefaultEdge> adapter = new JGraphModelAdapter<String, DefaultEdge>(dgraph);
		jgraph = new JGraph(adapter);
		//jgraph.setEnabled(false);
		JScrollPane scroller = new JScrollPane(jgraph);
		JFrame frame = new JFrame("The Body");
		frame.setSize(600,600);
		frame.add(scroller);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Iterator<String> it = dgraph.vertexSet().iterator();
		while(it.hasNext()){
			positionVertexAt(it.next(), adapter);
		}
	}
	
	private void positionVertexAt(Object vertex, JGraphModelAdapter<String, DefaultEdge> adapter)
    {
		AttributeMap eatrr = adapter. getDefaultEdgeAttributes();
		Random r = new Random();
        DefaultGraphCell cell = adapter.getVertexCell(vertex);
        AttributeMap attr = cell.getAttributes();
        Rectangle2D bounds = GraphConstants.getBounds(attr);

        Rectangle2D newBounds =
            new Rectangle2D.Double(r.nextDouble() * 400, r.nextDouble() * 400,
                bounds.getWidth(),bounds.getHeight());

        GraphConstants.setBounds(attr, newBounds);

        // TODO: Clean up generics once JGraph goes generic
        AttributeMap cellAttr = new AttributeMap();
        cellAttr.put(cell, attr);
        adapter.edit(cellAttr, null, null, null);
        
    }
	
	/**
	 * a listenable directed multigraph that allows loops and parallel edges.
	 */
	private static class ListenableDirectedMultigraph<V, E>
	    extends DefaultListenableGraph<V, E>
	    implements DirectedGraph<V, E>
	{
	    private static final long serialVersionUID = 1L;

	    ListenableDirectedMultigraph(Class<E> edgeClass)
	    {
	        super(new DirectedMultigraph<V, E>(edgeClass));
	    }
	}
}


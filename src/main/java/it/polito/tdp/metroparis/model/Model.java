package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import it.polito.tdp.metroparis.db.MetroDAO;

public class Model {
	private Graph<Fermata, DefaultEdge> graph;
	
	private List<Fermata> fermate;
	private List<Linea> linee;
	private List<Connessione> connessioni;
	
	private Map<Integer, Fermata> fermateIdMap;
	private Map<Integer, Linea> lineeIdMap;
	
	public Model() {
		this.graph = new SimpleDirectedGraph<>(DefaultEdge.class);
		
		Long start = System.currentTimeMillis();

		MetroDAO dao = new MetroDAO();

		this.fermate = dao.getAllFermate();
		this.linee = dao.getAllLinee();
		// Map che mappa Id fermata su oggetto fermata
		this.fermateIdMap = new HashMap<>();
		
		for (Fermata f : this.fermate)
			fermateIdMap.put(f.getIdFermata(), f);
		
		this.lineeIdMap = new HashMap<>();

		for (Linea l : this.linee)
			lineeIdMap.put(l.getIdLinea(), l);
		
		Graphs.addAllVertices(this.graph, this.fermate);
		
		connessioni = dao.getConnessioni(fermateIdMap, lineeIdMap);
		
		for(Connessione c : connessioni)
			graph.addEdge(c.getStazP(), c.getStazA());
		
		Long end = System.currentTimeMillis();

		System.out.format("Grafo con %d vertici e %d archi\n", graph.vertexSet().size(), graph.edgeSet().size());
		System.out.format("Tempo di esecuzione: %d ms", (end-start));
	}
	
	
	/**
	 * Visita l'intero grafo con laa strategia Breadth Firsst 
	 * e ritorno l'insieme dei vertici incontrati.
	 * @param source vertice di partenza per l'iterazione
	 * @return l'insieme dei vertici incontrati
	 */
	
	public List<Fermata> visitaAmpiezza(Fermata source) {
		GraphIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source);
		List<Fermata> visita  = new ArrayList<>();
		while(bfv.hasNext()) {
			visita.add(bfv.next());
		}
		
		return visita;
	}
	
	public List<Fermata> visitaProfondita(Fermata source) {
		GraphIterator<Fermata, DefaultEdge> dfv = new DepthFirstIterator<>(graph, source);
		List<Fermata> visita  = new ArrayList<>();
		while(dfv.hasNext()) {
			visita.add(dfv.next());
		}
		
		return visita;
	}
	
	public Map<Fermata, Fermata> alberoVisita(Fermata source) {
		Map<Fermata, Fermata> tree = new HashMap<Fermata, Fermata>();
		tree.put(source, null);
		
		GraphIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source);
		
		bfv.addTraversalListener(new TraversalListener<Fermata, DefaultEdge>() {
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			}
			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			}
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> e) {
					// la visita sta considerando un nuovo arco
					// l'arco ha scoperto un vertice? se si, da dove?
				DefaultEdge edge = e.getEdge();
				Fermata a = graph.getEdgeSource(edge);
				Fermata b = graph.getEdgeTarget(edge);
				
				if (tree.containsKey(a) && !tree.containsKey(b)) 
					tree.put(b, a);
				else
					tree.put(a, b);
				
			}
			@Override
			public void vertexTraversed(VertexTraversalEvent<Fermata> e) {
			}
			@Override
			public void vertexFinished(VertexTraversalEvent<Fermata> e) {
			}
			
		});
		
		while(bfv.hasNext()) {
			bfv.next();  // utile per scatenare creazione nuovi key in albero
		}
		
		return tree;
	}
	
	public List<Fermata> camminiMinimi(Fermata partenza, Fermata arrivo) {
		DijkstraShortestPath<Fermata, DefaultEdge> dj = new DijkstraShortestPath<>(graph);
		
		GraphPath<Fermata,DefaultEdge> cammino = dj.getPath(partenza, arrivo);
		
		return cammino.getVertexList();
	}
	
	public static void main(String args[]) {
		Model m = new Model();
		List<Fermata> visita = m.visitaAmpiezza(m.fermate.get(0));
		List<Fermata> visita2 = m.visitaProfondita(m.fermate.get(0));

		System.out.println("\n"+visita);
		
		System.out.println("\n"+visita2);
		Map<Fermata, Fermata> tree = m.alberoVisita(m.fermate.get(0));
		
		for (Fermata f: tree.keySet())
			System.out.format("%s -> %s\n", f, tree.get(f));
	
		List<Fermata> cammino = m.camminiMinimi(m.fermate.get(0), m.fermate.get(1));
		System.out.println(cammino);
	}
		
		
	
	// TODO: Soluzioni alternative in base alla densità
	// Creazione di archi -> metodo 1 (coppie di vertici)
			// Facile ma estremamente lento, bisogna trovare una implementazione più veloce
			// Complessità O(n2)
			/**
				for (Fermata fp : this.fermate) {
				 
					for (Fermata fa : this.fermate)
						if (dao.getConnessione())
						this.graph.addEdge(fp, fa);
				}
				System.out.format("Grafo con %d vertici e %d archi", graph.vertexSet().size(), graph.edgeSet().size());
			 */
			
			//Con questo metodo complessità pari a O(n) -> grado medio dei vertici è basso
			// rispetto al numero dei vertici
			
			/**
			for (Fermata fp : this.fermate) {
				List<Fermata> connesse = dao.getFermateConnesse(fermateIdMap, fp);
				for (Fermata fa : connesse)
					this.graph.addEdge(fp, fa);
			}
			System.out.format("Grafo con %d vertici e %d archi", graph.vertexSet().size(), graph.edgeSet().size());
			*/
			
			// Terzo metodo: creazione degli archi -> chiedo al database direttamente l'elenco degli archi
			// SE densità molto bassa -> soluzione migliore
	
}



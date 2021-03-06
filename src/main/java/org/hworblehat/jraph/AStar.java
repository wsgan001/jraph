package org.hworblehat.jraph;

import org.hworblehat.jraph.util.MinPriorityQueue;
import org.hworblehat.jraph.util.PairingHeap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;
import java.util.stream.Stream;

/**
 * A* route-finding algorithm.
 */
public class AStar {

	public static <V,E> Optional<Route<V,E,Double>> findDoubleRoute(V start, V end, DirectedGraph<V, E, Double> graph,
			ToDoubleBiFunction<V, V> costEstimator) {
		return findRoute(start, end, graph, costEstimator::applyAsDouble, 0.0, Double::sum);
	}

	public static <V,E> Optional<Route<V,E,Long>> findLongRoute(V start, V end, DirectedGraph<V, E, Long> graph,
			ToLongBiFunction<V, V> costEstimator) {
		return findRoute(start, end, graph, costEstimator::applyAsLong, 0L, Long::sum);
	}

	public static <V,E> Optional<Route<V,E,Integer>> findIntRoute(V start, V end, DirectedGraph<V, E, Integer> graph,
			ToIntBiFunction<V, V> costEstimator) {
		return findRoute(start, end, graph, costEstimator::applyAsInt, 0, Integer::sum);
	}

	public static <V, E, C extends Comparable<C>> Optional<Route<V, E, C>> findRoute(V start, V end,
			DirectedGraph<V, E, C> graph, BiFunction<V, V, C> costEstimator, C zeroCost, BinaryOperator<C> add) {
		return findRoute(start, end, graph, costEstimator, zeroCost, add, Comparator.naturalOrder());
	}

	public static <V, E, C> Optional<Route<V, E, C>> findRoute(V start, V end, DirectedGraph<V, E, C> graph,
			BiFunction<V, V, C> costEstimator, C zeroCost, BinaryOperator<C> add, Comparator<C> costComparator) {

		Set<V> visited = new HashSet<>();
		MinPriorityQueue<V, C> discovered = new PairingHeap<>(costComparator);
		discovered.offer(start, costEstimator.apply(start, end));

		Map<V, E> cameFrom = new HashMap<>();

		Map<V, C> gScores = new HashMap<>();
		gScores.put(start, zeroCost);

		for (Optional<V> c = discovered.poll(); c.isPresent(); c = discovered.poll()) {
			V current = c.get();

			if (Objects.equals(current, end)) {
				return Optional.of(constructRoute(start, end, graph, cameFrom, gScores.get(end)));
			}

			visited.add(current);

			try(Stream<E> outgoing = graph.getOutgoingEdges(current)) {
				outgoing.forEach(edge -> {
					V neighbour = graph.getEndVertex(edge);

					if (!visited.contains(neighbour)) {
						C gScoreCandidate = add.apply(gScores.get(current), graph.getCost(edge));

						if (discovered.offer(neighbour, add.apply(gScoreCandidate, costEstimator.apply(neighbour, end)))) {
							cameFrom.put(neighbour, edge);
							gScores.put(neighbour, gScoreCandidate);
						}
					}
				});
			}
		}

		return Optional.empty();
	}

	private static <V, E, C> Route<V, E, C> constructRoute(V start, V end, DirectedGraph<V, E, C> graph, Map<V, E> cameFrom,
			C totalCost) {

		List<V> vertices = new ArrayList<>();
		List<E> edges = new ArrayList<>();

		E edge;
		for (V vertex = end; !Objects.equals(vertex, start); vertex = graph.getStartVertex(edge)) {
			edge = cameFrom.get(vertex);
			vertices.add(vertex);
			edges.add(edge);
		}
		vertices.add(start);

		Collections.reverse(vertices);
		Collections.reverse(edges);

		return new ImmutableRoute<>(vertices, edges, totalCost);
	}

}

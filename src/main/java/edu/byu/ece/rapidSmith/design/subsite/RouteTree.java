/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.*;

/**
 *
 */
public final class RouteTree<data_t> implements Iterable<RouteTree> {
	private RouteTree<data_t> sourceTree; // Do I want bidirectional checks?
	private final Wire wire;
	private Connection connection;
	private data_t data;
	private final Collection<RouteTree<data_t>> sinkTrees = new ArrayList<>(1);

	public RouteTree(Wire wire) {
		this.wire = wire;
	}

	RouteTree(Wire wire, Connection c) {
		this.wire = wire;
		this.connection = c;
	}

	public Wire getWire() {
		return wire;
	}

	public data_t getData() {
		return data;
	}

	public void setData(data_t data) {
		this.data = data;
	}

	public Connection getConnection() {
		return connection;
	}

	private void setConnection(Connection connection) {
		this.connection = connection;
	}

	public RouteTree<data_t> getSourceTree() {
		return sourceTree;
	}

	public RouteTree<data_t> getFirstSource() {
		RouteTree<data_t> parent = this;
		while (parent.isSourced())
			parent = parent.getSourceTree();
		return parent;
	}

	public boolean isSourced() {
		return sourceTree != null;
	}

	private void setSourceTree(RouteTree<data_t> sourceTree) {
		this.sourceTree = sourceTree;
	}

	public Collection<RouteTree<data_t>> getSinkTrees() {
		return sinkTrees;
	}

	private void addSinkTree(RouteTree<data_t> sinkTree) {
		sinkTrees.add(sinkTree);
	}
	
	/**
	 * Returns true if the RouteTree object is a leaf (i.e. it has no children). 
	 * For a fully routed net, a leaf tree should connect to either a SitePin
	 * or BelPin.
	 */
	public boolean isLeaf() {
		return sinkTrees.size() == 0;
	}
	
	/**
	 * Returns the SitePin connected to the wire of the RouteTree. If no SitePin
	 * object is connected, null is returned.
	 */
	public SitePin getConnectingSitePin() {
		Collection<Connection> pinConnections = wire.getPinConnections();
		return (pinConnections.isEmpty()) ? null : pinConnections.iterator().next().getSitePin(); 
	}
	
	/**
	 * Returns the BelPin connected to the wire of the RouteTree. If no BelPin
	 * object is connected, null is returned.
	 */
	public BelPin getConnectingBelPin() {
		Collection<Connection> terminalConnections = wire.getTerminals();
		return terminalConnections.isEmpty() ? null : terminalConnections.iterator().next().getBelPin();
	}

	public RouteTree<data_t> addConnection(Connection c) {
		RouteTree<data_t> endTree = new RouteTree<data_t>(c.getSinkWire(), c);
		endTree.setSourceTree(this);
		sinkTrees.add(endTree);
		return endTree;
	}

	public RouteTree<data_t> addConnection(Connection c, RouteTree<data_t> sink) {
		if (sink.getSourceTree() != null)
			throw new Exceptions.DesignAssemblyException("Sink tree already sourced");
		if (!c.getSinkWire().equals(sink.getWire()))
			throw new Exceptions.DesignAssemblyException("Connection does not match sink tree");

		sinkTrees.add(sink);
		sink.setSourceTree(this);
		sink.setConnection(c);
		return sink;
	}

	public void removeConnection(Connection c) {
		for (Iterator<RouteTree<data_t>> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTree<data_t> sink = it.next();
			if (sink.getConnection().equals(c)) {
				sink.setSourceTree(null);
				it.remove();
			}
		}
	}

	public List<PIP> getAllPips() {
		return getFirstSource().getAllPips(new ArrayList<>());
	}

	private List<PIP> getAllPips(List<PIP> pips) {
		for (RouteTree<data_t> rt : sinkTrees) {
			if (rt.getConnection().isPip())
				pips.add(rt.getConnection().getPip());
			rt.getAllPips(pips);
		}
		return pips;
	}

	public RouteTree<data_t> deepCopy() {
		RouteTree<data_t> copy = new RouteTree<data_t>(wire, connection);
		sinkTrees.forEach(rt ->{
			RouteTree<data_t> tree = rt.deepCopy();
			tree.setSourceTree(copy);
			copy.addSinkTree(tree);
		});
		return copy;
	}

	public boolean prune(RouteTree<data_t> terminal) {
		Set<RouteTree<data_t>> toPrune = new HashSet<>();
		toPrune.add(terminal);
		return prune(toPrune);
	}
	
	public boolean prune(Set<RouteTree<data_t>> terminals) {
		return pruneChildren(terminals);
	}

	private boolean pruneChildren(Set<RouteTree<data_t>> terminals) {
		sinkTrees.removeIf(rt -> !rt.pruneChildren(terminals));
		return !sinkTrees.isEmpty() || terminals.contains(this);
	}
	
	@Override
	public Iterator<RouteTree> iterator() {
		return preorderIterator();
	}

	public Iterator<RouteTree> preorderIterator() {
		return new PreorderIterator();
	}

	private class PreorderIterator implements Iterator<RouteTree> {
		private final Stack<RouteTree> stack;

		PreorderIterator() {
			this.stack = new Stack<>();
			this.stack.push(RouteTree.this);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public RouteTree next() {
			if (!hasNext())
				throw new NoSuchElementException();
			RouteTree tree = stack.pop();
			stack.addAll(tree.getSinkTrees());
			return tree;
		}
	}

	// Uses identity equals

	@Override
	public int hashCode() {
		return Objects.hash(connection);
	}
}

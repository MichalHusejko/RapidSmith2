package edu.byu.ece.rapidSmith.examples;

import java.util.Collection;
import java.util.Map;

import edu.byu.ece.rapidSmith.device.*;

import edu.byu.ece.rapidSmith.RSEnvironment;

public class DeviceAnalyzer {

	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("Usage: DeviceAnalyzer deviceName");
			System.exit(1);
		}

		msg("Starting DeviceAnalyzer...\n");

		// Load the device file
		Device device = RSEnvironment.defaultEnv().getDevice(args[0]);

		System.out.println("Loaded the device");
		System.out.println("Family: " + device.getFamily().name());
		System.out.println(device.getColumns() + " columns");
		System.out.println(device.getRows() + " rows");

		Collection<Tile> tiles = device.getTiles();
		System.out.println(tiles.size() + " tiles");

		Map<String, Site> sites = device.getSites();
		System.out.println(sites.size() + " sites");

		// TODO: Create a getTilesOfType method in Device.java
		// Print a CLB tile's wires
		printTileWires(device.getTile("CLBLM_L_X26Y68"));

		// Print an INT tile's wires
		printTileWires(device.getTile("INT_L_X26Y68"));
	}
	
	/**
	 * Prints out the wires and their connections within a tile
	 * @param t A handle to the tile of interest.
	 */
	private static void printTileWires(Tile t) {

		msg("\n===========================\nSelected tile " + t.toString());
		msg("Its row and column numbers are: [" + t.getRow() + ", " + t.getColumn() + "]");
		
		
		// Build each wire and print its statistics
		Collection<Wire> wires = t.getWires();
		msg("There are " + wires.size() + " wires in this tile...");
		for (Wire tw : wires) {
			printWire(tw);
		}
		
		msg("Done...");
	}
	
	private static void printWire(Wire w) {
		Tile t = w.getTile();
		msg("Wire " + w.getFullName() + " has " + w.getWireConnections().size() + " connections.");

		/*
		 * A wire has a number of connections to other wires. 
		 * 
		 * These are essentially of two types. The first is a programmable 
		 * connection, also known as a PIP. The other is a non-programmable
		 * connection and essentially is the name of the other end of the
		 * wire (that is, each end of a wire typically has a different
		 * name).
		 * 
		 * For many wires, the other end of the connection is in the same
		 * tile. For others, it is in a different tile.
		 * 
		 * The following code will print out the various wire connections,
		 * marking whether they are PIPs or not. Additionally, if the other
		 * end of the connection is in a different tile, it will print the
		 * offset as well.
		 *  
		 */ 
		for (Connection c : w.getWireConnections()) {	 
			String s;
			if (c.getSinkWire().getTile() != t) {	 
				int xoff = c.getSinkWire().getTile().getColumn() - t.getColumn() ;	 
				int yoff = c.getSinkWire().getTile().getRow() - t.getRow() ;	 
				s = c.getSinkWire().getTile().toString() + "/" + c.getSinkWire().getName() + " [" + yoff + "," + xoff + "]";
			}	
			else	 
				s = c.getSinkWire().getName();
			if (c.isPip())	
				msg("  [PIP] " + s);	 
			else
				msg("  [nonPIP] " + s);	
		}
	}	
	
	private static void msg(String s) {
		System.out.println(s);
	}

}

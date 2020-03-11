package org.liris.smartgov.lez.core.environment.lez;

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

import java.util.Hashtable;
import org.liris.smartgov.lez.core.agent.driver.vehicle.Vehicle;
import org.liris.smartgov.lez.core.copert.fields.Pollutant;
import org.liris.smartgov.lez.core.environment.lez.criteria.AllAllowedCriteria;
import org.liris.smartgov.lez.core.environment.lez.criteria.LezCosts;
import org.liris.smartgov.lez.core.environment.lez.criteria.LezCriteria;
import org.liris.smartgov.lez.core.environment.pollution.Pollution;
import org.liris.smartgov.simulator.core.environment.graph.astar.Costs;
import org.liris.smartgov.simulator.urban.geo.environment.graph.DistanceCosts;
import org.liris.smartgov.simulator.urban.geo.utils.LatLon;
import org.liris.smartgov.simulator.urban.geo.utils.lonLat.LonLat;
import org.liris.smartgov.simulator.urban.osm.environment.graph.OsmNode;

/**
 * A Low Emission Zone representation.
 *
 */
public class Lez {
	
	private LatLon[] perimeter;
	private PointOnGeometryLocator locator;
	private LezCriteria lezCriteria;
	private int id;
	private Pollution pollution;
	
	/**
	 * Lez constructor.
	 * 
	 * @param perimeter polygon that describes the perimeter of the LEZ.
	 * If the polygon is not closed, it will be completed automatically.
	 * @param lezCriteria criteria associated to this lez, that determines which
	 * vehicles are allowed or not
	 */
	public Lez(LatLon[] perimeter, LezCriteria lezCriteria, int id) {
		this.id = id;
		this.perimeter = perimeter;
		this.lezCriteria = lezCriteria;
		pollution = new Pollution();
		
		GeometryFactory factory = new GeometryFactory();
		
		Coordinate[] coordinates;
		boolean closed;
		if(perimeter[0].equals(perimeter[perimeter.length - 1])) {
			coordinates = new Coordinate[perimeter.length];
			closed = true;
		}
		else {
			coordinates = new Coordinate[perimeter.length + 1];
			closed = false;
		}
		
		LonLat projector = new LonLat();
		for(int i = 0; i < perimeter.length; i++) {
			coordinates[i] = projector.project(perimeter[i]);
		}
		
		if(!closed) {
			coordinates[coordinates.length - 1] = projector.project(perimeter[0]);
		}
		
		locator = new IndexedPointInAreaLocator(
				new Polygon(
					new LinearRing(
						new CoordinateArraySequence(coordinates),
						factory
						),
					new LinearRing[] {},
					factory
					));
	}
	
	public LatLon[] getPerimeter() {
		return perimeter;
	}
	
	/**
	 * Return the LEZ criteria associated to this LEZ, that determines
	 * which vehicles are allowed or not.
	 * 
	 * @return lez criteria
	 */
	public LezCriteria getLezCriteria() {
		return lezCriteria;
	}
	
	public int getId() {
		return id;
	}
	
	public void increasePollution(Pollutant pollutant, double increment) {
		pollution.get(pollutant).increasePollution(increment);
	}
	
	public Pollution getPollution() {
		return pollution;
	}
	
	/**
	 * Returns the cost function associated to this vehicle, depending
	 * on its permission to enter the LEZ or not.
	 * <ul>
	 * <li> If the vehicle is allowed, a normal DistanceCosts is returned, as if the
	 * lez did not exist for this vehicle.</li>
	 * <li> Else, {@link org.liris.smartgov.lez.core.environment.lez.criteria.LezCosts} are used.</li>
	 * </ul>
	 * 
	 * @param deliveryVehicle vehicle
	 * @return cost function associated to the specified vehicle in the current urban area
	 */
	public Costs costs(Vehicle deliveryVehicle) {
		if(lezCriteria.isAllowed(deliveryVehicle))
			return new DistanceCosts();
		return new LezCosts();
	}
	
	public void resetPollution() {
		pollution = new Pollution();
	}

	/*
	 * Used by NoLez class below
	 */
	private Lez() {
		this.lezCriteria = new AllAllowedCriteria();
	}
	
	/**
	 * Determines if the specified osm node is contained in the lez,
	 * thanks to a <a href="https://locationtech.github.io/jts/javadoc/org/locationtech/jts/algorithm/locate/package-summary.html">
	 * JTS Point-In-Polygon algorithm</a>.
	 * 
	 * @param node osm node
	 * @return true if and only if the node is strictly contained in this LEZ
	 */
	public boolean contains(OsmNode node) {
		if(locator.locate(
					new LonLat().project(node.getPosition())
				) == Location.INTERIOR)
			return true;
		return false;
	}
	
	/**
	 * Returns a special LEZ instance with no perimeter and all vehicles allowed,
	 * that can be smartly used by scenarios with a lez parameter to compare results
	 * with or without LEZ.
	 * 
	 * @return a lez with absolutely no restriction
	 */
	public static Lez none() {
		return new NoLez();
	}
	
	public Hashtable<Integer, LatLon> createTable(LatLon[] coordinates) {
		
		Hashtable<Integer, LatLon> perimeter = new Hashtable<Integer, LatLon>();		
		int id = 0;
		for (LatLon coordinate : coordinates) {
			perimeter.put(id, coordinate);
			id++;
		}
		return perimeter;
	}
	
	private static class NoLez extends Lez {
		
		/*
		 * Overrides the contains method so that no node is contained
		 * in the lez, without using the JTS algorithm.
		 */
		@Override
		public boolean contains(OsmNode node) {
			return false;
		}
		

	}

}

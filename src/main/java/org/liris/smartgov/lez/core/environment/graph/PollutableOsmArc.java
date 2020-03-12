package org.liris.smartgov.lez.core.environment.graph;

import java.util.ArrayList;
import java.util.Collection;

import org.liris.smartgov.lez.core.agent.driver.vehicle.Vehicle;
import org.liris.smartgov.lez.core.copert.fields.Pollutant;
import org.liris.smartgov.lez.core.environment.lez.Neighborhood;
import org.liris.smartgov.lez.core.environment.lez.criteria.CritAir;
import org.liris.smartgov.lez.core.environment.pollution.Pollution;
import org.liris.smartgov.simulator.core.events.EventHandler;
import org.liris.smartgov.simulator.urban.osm.environment.graph.OsmArc;
import org.liris.smartgov.simulator.urban.osm.environment.graph.OsmNode;
import org.liris.smartgov.simulator.urban.osm.environment.graph.Road;

/**
 * An OsmArc that can be polluted with some particles.
 * 
 * @author pbreugnot
 *
 */
public class PollutableOsmArc extends OsmArc {

	private Pollution pollution;
	private Neighborhood neighborhood;
	
	private Collection<EventHandler<PollutionIncreasedEvent>> pollutionIncreasedListeners;
	
	/**
	 * PollutableOsmArc constructor.
	 * 
	 * @param id arc id
	 * @param startNode start node
	 * @param targetNode target node
	 * @param road osm road
	 * @param roadDirection BACKWARD or FORWARD
	 * @param inLez true if this arc is contained in a LEZ
	 */
	public PollutableOsmArc(
			String id,
			OsmNode startNode,
			OsmNode targetNode,
			Road road,
			RoadDirection roadDirection,
			Neighborhood neighborhood) {
		super(id, startNode, targetNode, road, roadDirection);
		this.neighborhood = neighborhood;
		pollution = new Pollution();
		pollutionIncreasedListeners = new ArrayList<>();
	}
	
	/**
	 * Increases the pollution amount recorded on this arc for the given pollutant
	 *  by the specified amount, in g.
	 * 
	 * @param pollutant pollutant
	 * @param increment emission in g
	 */
	public void increasePollution(Pollutant pollutant, double increment) {
		neighborhood.increasePollution(pollutant, increment);
		pollution.get(pollutant).increasePollution(increment);
	}
	
	/**
	 * Returns the pollution amounts registered on this arc.
	 * Values are given in g.
	 * 
	 * @return arc pollution
	 */
	public Pollution getPollution() {
		return pollution;
	}
	
	public void resetPollution() {
		pollution = new Pollution();
	}
	
	public String getNeighborhoodId() {
		return neighborhood.getId();
	}
	
	public boolean isAllowed (Vehicle vehicle) {
		return neighborhood.isAllowed(vehicle);
	}
	
	/**
	 * Adds a new pollution increased event handler, called each time the arc is
	 * polluted by a {@link org.liris.smartgov.lez.core.agent.driver.mover.PollutantCarMover}.
	 * 
	 * @param pollutionIncreasedListener pollution increased event handler
	 */
	public void addPollutionIncreasedListener(EventHandler<PollutionIncreasedEvent> pollutionIncreasedListener) {
		this.pollutionIncreasedListeners.add(pollutionIncreasedListener);
	}
	
	/**
	 * Used by the {@link org.liris.smartgov.lez.core.agent.driver.mover.PollutantCarMover} to
	 * trigger pollution increased listeners
	 * 
	 * @param event pollution increased event
	 */
	public void _triggerPollutionIncreasedListeners(PollutionIncreasedEvent event) {
		for(EventHandler<PollutionIncreasedEvent> listener : pollutionIncreasedListeners) {
			listener.handle(event);
		}
	}
}

package org.liris.smartgov.lez.core.agent.driver.mover;

import java.util.ArrayList;

import org.liris.smartgov.lez.core.agent.driver.DriverBody;
import org.liris.smartgov.lez.core.copert.fields.Pollutant;
import org.liris.smartgov.lez.core.environment.graph.PollutableOsmArc;
import org.liris.smartgov.lez.core.environment.graph.PollutionIncreasedEvent;
import org.liris.smartgov.simulator.core.agent.moving.MovingAgentBody;
import org.liris.smartgov.simulator.urban.geo.agent.GeoAgentBody;
import org.liris.smartgov.simulator.urban.geo.agent.mover.BasicGeoMover;
import org.liris.smartgov.simulator.urban.osm.agent.mover.CarMover;

/**
 * This class implements the same behavior has the {@link org.liris.smartgov.simulator.urban.osm.agent.mover.CarMover CarMover},
 * but with utilities to compute pollution emissions. To do so, it records the traveled distance and propagate the
 * pollution on crossed arcs each time a distance threshold has been reached.
 * 
 * 
 * @author pbreugnot
 *
 */
public class PollutantCarMover extends CarMover {
	
	public static double maximumAcceleration = 4.;
	public static double maximumBraking = -6.;
	public static double maximumSpeed = 15.; // m/s
	public static double vehicleSize = 6.;
	
	public static final double pollutionDistanceTreshold = 1000;
	
	// Record of the distance traveled since the last pollution emission.
	private double traveledDistance; // m
	private double time; // s
	private double currentSpeed; // m/s
	
	private ArrayList<PollutableOsmArc> arcsCrossed;

	
	public PollutantCarMover() {
		super(
			maximumAcceleration,
			maximumBraking,
			maximumSpeed,
			vehicleSize
			);
		arcsCrossed = new ArrayList<>();
	}
	
	/*
	 * Set up necessary listeners to compute arc pollution while the agent
	 * is moving.
	 */
	private void setUpPollutionListeners() {
		((BasicGeoMover) agentBody.getMover()).addGeoMoveEventListener((event) -> {
				if (currentSpeed > 0) {
					traveledDistance += event.getDistanceCrossed();
					time += event.getDistanceCrossed() / currentSpeed; // Speed remains constant between each move event.
				}
				currentSpeed = agentBody.getSpeed();
			}
			);
		
		((MovingAgentBody) agentBody).addOnArcLeftListener((event) -> {
				arcsCrossed.add((PollutableOsmArc) event.getArc());
				if (traveledDistance >= pollutionDistanceTreshold || ((MovingAgentBody) agentBody).getPlan().isComplete()) {
					polluteArcs();
					traveledDistance = 0;
					time = 0;
					arcsCrossed.clear();
				}
			}
			);
	}
	
	@Override
	public void setAgentBody(GeoAgentBody agentBody) {
		super.setAgentBody(agentBody);
		setUpPollutionListeners();
		currentSpeed = agentBody.getSpeed();
		time = 0;
	}
	
	/*
	 * Propagates pollution on the crossed arcs, when the distance threshold has been
	 * reached (or when the agent plan has been completed).
	 */
	private void polluteArcs() {
		for(Pollutant pollutant : Pollutant.values()) {
			double emissions = 
					((DriverBody) agentBody)
					.getVehicle()
					.getEmissions(pollutant, traveledDistance / time, traveledDistance);
			for (PollutableOsmArc arc : arcsCrossed) {
				arc.increasePollution(pollutant, emissions * arc.getLength() / traveledDistance);
			}
		}
		for (PollutableOsmArc arc : arcsCrossed) {
			arc._triggerPollutionIncreasedListeners(new PollutionIncreasedEvent(arc));
		}
	}

}

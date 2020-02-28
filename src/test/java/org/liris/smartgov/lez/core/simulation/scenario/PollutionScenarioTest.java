package org.liris.smartgov.lez.core.simulation.scenario;

import org.liris.smartgov.lez.core.agent.driver.DriverBody;
import org.liris.smartgov.lez.core.environment.LezContext;
import org.liris.smartgov.lez.core.environment.graph.PollutableOsmArc;
import org.liris.smartgov.lez.core.simulation.scenario.PollutionScenario;
import org.liris.smartgov.simulator.SmartGov;
import org.liris.smartgov.simulator.core.agent.core.Agent;
import org.liris.smartgov.simulator.core.environment.graph.Arc;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import org.junit.Test;

public class PollutionScenarioTest {
	
	private SmartGov loadSmartGov() {
		return new SmartGov(new LezContext(this.getClass().getResource("pollution_scenario.properties").getFile()));
	}
	
	@Test
	public void testLoadPollutionScenario() {
		SmartGov smartGov = loadSmartGov();
		assertThat(
				smartGov.getContext().getScenario() instanceof PollutionScenario,
				equalTo(true)
				);
	}
	
	@Test
	public void testAgentBodiesType() {
		SmartGov smartGov = loadSmartGov();
		for(Agent<?> agent : smartGov.getContext().agents.values()) {
			assertThat(
					agent.getBody() instanceof DriverBody,
					equalTo(true)
					);
		}
	}
	
	@Test
	public void testArcsType() {
		SmartGov smartGov = loadSmartGov();
		for(Arc arc : smartGov.getContext().arcs.values()) {
			assertThat(
					arc instanceof PollutableOsmArc,
					equalTo(true)
					);
		}
	}
}

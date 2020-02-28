package org.liris.smartgov.lez.core.environment.lez.criteria;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.liris.smartgov.lez.core.agent.driver.vehicle.Vehicle;
import org.liris.smartgov.lez.core.environment.lez.criteria.CritAir;
import org.liris.smartgov.lez.core.environment.lez.criteria.CritAirCriteria;

public class CritAirCriteriaTest {

	@Test
	public void testCritAir() {
		List<CritAir> allowed = Arrays.asList(
				CritAir.CRITAIR_1,
				CritAir.CRITAIR_2,
				CritAir.CRITAIR_3
				);
		
		CritAirCriteria criteria = new CritAirCriteria(
				allowed
				);
		
		for (CritAir critair : CritAir.values()) {
			Vehicle vehicle = mock(Vehicle.class);
			when(vehicle.getCritAir()).thenReturn(critair);
			
			if(allowed.contains(critair)) {
				assertThat(
						criteria.isAllowed(vehicle),
						is(true)
						);
			}
			else {
				assertThat(
						criteria.isAllowed(vehicle),
						is(false)
						);
			}
		}
	}
}

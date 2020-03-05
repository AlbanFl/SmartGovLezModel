package org.liris.smartgov.lez.core.agent.establishment.preprocess;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.liris.smartgov.lez.core.agent.driver.vehicle.Vehicle;
import org.liris.smartgov.lez.core.agent.establishment.Establishment;
import org.liris.smartgov.lez.core.agent.establishment.Round;
import org.liris.smartgov.lez.core.agent.establishment.preprocess.LezPreprocessor;
import org.liris.smartgov.lez.core.copert.fields.CopertFieldsTest;
import org.liris.smartgov.lez.core.copert.fields.EuroNorm;
import org.liris.smartgov.lez.core.copert.fields.Fuel;
import org.liris.smartgov.lez.core.copert.fields.HeavyDutyTrucksSegment;
import org.liris.smartgov.lez.core.copert.fields.Technology;
import org.liris.smartgov.lez.core.copert.fields.VehicleCategory;
import org.liris.smartgov.lez.core.copert.tableParser.CopertParser;
import org.liris.smartgov.lez.core.environment.lez.Environment;
import org.liris.smartgov.lez.core.environment.lez.criteria.NothingAllowedCriteria;
import org.liris.smartgov.simulator.urban.osm.environment.graph.OsmNode;

public class LezPreprocessorTest {

	private static CopertParser loadCopertParser() {
		URL url = CopertFieldsTest.class.getResource("complete_test_table.csv"); // Complete Copert table with Light Commercial Vehicles and Heavy Duty Trucks
		return new CopertParser(new File(url.getFile()), new Random(1907190831l));
	}
	
	/*
	 * An example where anything is contained in the lez, so does the origin
	 * establishment, so all the vehicles must be replaced.
	 */
	@Test
	public void testBasicPreprocess() {
		Environment environment = mock(Environment.class);
		when(environment.getNeighborhood(any()).getLezCriteria()).thenReturn(new NothingAllowedCriteria());
		when(environment.getNeighborhood(any()).contains(any())).thenReturn(true);
		
		Map<String, Vehicle> testFleet = new HashMap<>();
		
		testFleet.put(
			"0",
			new Vehicle(
				"0",
				VehicleCategory.HEAVY_DUTY_TRUCK,
				Fuel.DIESEL,
				HeavyDutyTrucksSegment.ARTICULATED_14_20_T,
				EuroNorm.EURO2,
				Technology.DPF_SCR,
				null
				)
			);
		
		testFleet.put(
				"1",
				new Vehicle(
					"1",
					VehicleCategory.HEAVY_DUTY_TRUCK,
					Fuel.DIESEL,
					HeavyDutyTrucksSegment.ARTICULATED_14_20_T,
					EuroNorm.EURO1,
					Technology.DPF_SCR,
					null
					)
				);
		
		Establishment establishment = mock(Establishment.class);
		when(establishment.getFleet()).thenReturn(testFleet);
		
		LezPreprocessor preprocessor = new LezPreprocessor(environment, loadCopertParser());
		preprocessor.preprocess(establishment);
		
		Vehicle expectedCharacteristics = new Vehicle(
				null,
				VehicleCategory.HEAVY_DUTY_TRUCK,
				Fuel.DIESEL,
				HeavyDutyTrucksSegment.ARTICULATED_14_20_T,
				EuroNorm.EURO6,
				null,
				null
				);
		
		assertThat(
				testFleet.values(),
				hasSize(2)
				);
		
		for (Vehicle vehicle : testFleet.values()) {
			assertThat(
					vehicle.getCategory().equals(expectedCharacteristics.getCategory()),
					is(true)
					);
			assertThat(
					vehicle.getSegment().equals(expectedCharacteristics.getSegment()),
					is(true)
					);
			assertThat(
					vehicle.getFuel().equals(expectedCharacteristics.getFuel()),
					is(true)
					);
			assertThat(
					vehicle.getEuroNorm().equals(expectedCharacteristics.getEuroNorm()),
					is(true)
					);
		}
	}
	
	/*
	 * An example with an establishment not located in the lez, but
	 * with a round that has an establishment in the lez. The associated
	 * vehicle must be replaced.
	 */
	@Test
	public void testRoundPreprocess() {
		OsmNode originFakeNode = mock(OsmNode.class);
		when(originFakeNode.getId()).thenReturn("0");
		OsmNode destinationFakeNode = mock(OsmNode.class);
		when(destinationFakeNode.getId()).thenReturn("1");
		
		Establishment origin = mock(Establishment.class);
		when(origin.getClosestOsmNode()).thenReturn(originFakeNode);
		
		Map<String, Vehicle> originFleet = new HashMap<>();
		originFleet.put("0", new Vehicle(
				"0",
				VehicleCategory.HEAVY_DUTY_TRUCK,
				Fuel.DIESEL,
				HeavyDutyTrucksSegment.ARTICULATED_14_20_T,
				EuroNorm.EURO2,
				Technology.DPF_SCR,
				null
				)
				);
		when(origin.getFleet()).thenReturn(originFleet);
		
		Establishment destination = mock(Establishment.class);
		when(destination.getClosestOsmNode()).thenReturn(destinationFakeNode);
		
		Round round = mock(Round.class);
		List<Establishment> establishments = Arrays.asList(destination);
		when(round.getEstablishments()).thenReturn(establishments);
		
		Map<String, Round> rounds = new HashMap<>();
		rounds.put("0", round);
		
		when(origin.getRounds()).thenReturn(rounds);
		
		Environment environment = mock(Environment.class);
		when(environment.getNeighborhood(any()).getLezCriteria()).thenReturn(new NothingAllowedCriteria());
		
		when(environment.getNeighborhood(any()).contains(originFakeNode)).thenReturn(false);
		when(environment.getNeighborhood(any()).contains(destinationFakeNode)).thenReturn(true);
		
		assertThat(
				environment.getNeighborhood(any()).contains(originFakeNode),
				is(false)
				);
		
		assertThat(
				environment.getNeighborhood(any()).contains(destinationFakeNode),
				is(true)
				);
		
		LezPreprocessor preprocessor = new LezPreprocessor(environment, loadCopertParser());
		preprocessor.preprocess(origin);
		
		Vehicle expectedCharacteristics = new Vehicle(
				null,
				VehicleCategory.HEAVY_DUTY_TRUCK,
				Fuel.DIESEL,
				HeavyDutyTrucksSegment.ARTICULATED_14_20_T,
				EuroNorm.EURO6,
				Technology.DPF_SCR,
				null
				);
		
		Vehicle vehicle = origin.getFleet().get("0");
		assertThat(
				vehicle.getCategory().equals(expectedCharacteristics.getCategory()),
				is(true)
				);
		assertThat(
				vehicle.getSegment().equals(expectedCharacteristics.getSegment()),
				is(true)
				);
		assertThat(
				vehicle.getFuel().equals(expectedCharacteristics.getFuel()),
				is(true)
				);
		assertThat(
				vehicle.getEuroNorm().equals(expectedCharacteristics.getEuroNorm()),
				is(true)
				);
	}
}

package org.liris.smartgov.lez.core.simulation.scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.liris.smartgov.lez.cli.tools.Run;
import org.liris.smartgov.lez.core.agent.driver.DeliveryDriverAgent;
import org.liris.smartgov.lez.core.agent.driver.DriverBody;
import org.liris.smartgov.lez.core.agent.driver.behavior.DeliveryDriverBehavior;
import org.liris.smartgov.lez.core.agent.driver.vehicle.Vehicle;
import org.liris.smartgov.lez.core.agent.establishment.Establishment;
import org.liris.smartgov.lez.core.agent.establishment.ST8;
import org.liris.smartgov.lez.core.agent.establishment.preprocess.LezPreprocessor;
import org.liris.smartgov.lez.core.copert.fields.EuroNorm;
import org.liris.smartgov.lez.core.copert.tableParser.CopertParser;
import org.liris.smartgov.lez.core.environment.LezContext;
import org.liris.smartgov.lez.core.environment.graph.PollutableOsmArcFactory;
import org.liris.smartgov.lez.core.environment.lez.Lez;
import org.liris.smartgov.lez.input.establishment.EstablishmentLoader;
import org.liris.smartgov.simulator.SmartGov;
import org.liris.smartgov.simulator.core.agent.core.Agent;
import org.liris.smartgov.simulator.core.environment.SmartGovContext;
import org.liris.smartgov.simulator.core.environment.graph.Node;
import org.liris.smartgov.simulator.urban.geo.environment.graph.GeoStrTree;
import org.liris.smartgov.simulator.urban.geo.utils.lonLat.LonLat;
import org.liris.smartgov.simulator.urban.osm.agent.OsmAgent;
import org.liris.smartgov.simulator.urban.osm.environment.graph.OsmNode;
import org.liris.smartgov.simulator.urban.osm.environment.graph.Road;
import org.liris.smartgov.simulator.urban.osm.environment.graph.tags.Highway;
import org.liris.smartgov.simulator.urban.osm.utils.OsmArcsBuilder;

/**
 * Scenario used to model deliveries between establishments with
 * pollutant emissions.
 *
 */
public class DeliveriesScenario extends PollutionScenario {


	/**
	 * LezDeliveries
	 */
	public static final String name = "LezDeliveries";
	
	/**
	 * Establishments won't be delivered in those highways, even
	 * if they can be used in trajectories.
	 * 
	 * <ul>
	 * <li> {@link Highway#MOTORWAY} </li>
	 * <li> {@link Highway#MOTORWAY_LINK} </li>
	 * <li> {@link Highway#TRUNK} </li>
	 * <li> {@link Highway#TRUNK_LINK} </li>
	 * <li> {@link Highway#LIVING_STREET} </li>
	 * <li> {@link Highway#SERVICE} </li>
	 * </ul>
	 * 
	 * <p>
	 * Living streets and service ways are not used, because their
	 * usage to often bring situations with dead ends.
	 * </p>
	 */
	public static final Highway[] forbiddenClosestNodeHighways = {
			Highway.MOTORWAY,
			Highway.MOTORWAY_LINK,
			Highway.TRUNK,
			Highway.TRUNK_LINK,
			Highway.LIVING_STREET,
			Highway.SERVICE
	};
	
	/**
	 * DeliveriesScenario constructor.
	 * 
	 * @param lez LEZ used in this scenario
	 */
	public DeliveriesScenario(Lez lez) {
		super(lez);
	}
	
	@Override
	public void reloadWorld(SmartGovContext context) {
		for (Agent<?> agent : rebuildAgents(context)) {
			context.agents.put(agent.getId(), agent);
		}
	}
	
	public Collection<? extends Agent<?>> rebuildAgents(SmartGovContext context) {
		CopertParser parser = loadParser(context);
		LezPreprocessor preprocessor = new LezPreprocessor(getLez(), parser);

		int establishmentsInLez = 0;
		int totalVehiclesReplaced = 0;
		for( Establishment establishment : ((LezContext) context).getEstablishments().values() ) {
			if(getLez().contains(establishment.getClosestOsmNode())) {
				//Run.logger.info("[LEZ] " + establishment.getId() + " - " + establishment.getName());
				int replacedVehiclesCount = preprocessor.preprocess(establishment);
				totalVehiclesReplaced += replacedVehiclesCount;
				//Run.logger.info("[LEZ] Number of vehicles replaced : " + replacedVehiclesCount);
				establishmentsInLez++;
			}
		}
		Run.logger.info("[LEZ] Number of establishments in lez : " + establishmentsInLez);
		Run.logger.info("[LEZ] Total number of vehicles replaced : " + totalVehiclesReplaced);
		
		int agentId = 0;
		Collection<OsmAgent> agents = new ArrayList<>();
		Collection<BuildAgentThread> threads = new ArrayList<>();
		
		
		
		for ( Establishment establishment : ((LezContext) context).getEstablishments().values() ) {
			for(String vehicleId : establishment.getRounds().keySet()) {
				BuildAgentThread thread = new BuildAgentThread(agentId++, vehicleId, establishment, (LezContext) context);
				threads.add(thread);
				thread.start();
			}
		}
		for(BuildAgentThread thread : threads) {
			try {
				thread.join();
				agents.add(thread.getBuiltAgent());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		for( Establishment establishment : ((LezContext) context).getEstablishments().values() ) {
			if(!establishment.getFleet().isEmpty()) {
				List<EuroNorm> euroNorms = new ArrayList<>();
				for(Vehicle vehicle : establishment.getFleet().values()) {
					euroNorms.add(vehicle.getEuroNorm());
				}
				Run.logger.info(
						"[" + establishment.getId() + "] " + establishment.getName()
						+ " - fleet norms : " + euroNorms
						);
			}
		}
		
		return agents;
	}
	
	@Override
	public Collection<? extends Agent<?>> buildAgents(SmartGovContext context) {
		int deadEnds = 0;
		for(Node node : context.nodes.values()) {
			if(node.getOutgoingArcs().isEmpty() || node.getIncomingArcs().isEmpty()) {
				deadEnds++;
				Road road = ((OsmNode) node).getRoad();
				Run.logger.debug("Dead end found on node " + node.getId() + ", road " + road.getId());
			}
		}
		Run.logger.info(deadEnds + " dead ends found.");
		
		OsmArcsBuilder.fixDeadEnds((LezContext) context, new PollutableOsmArcFactory(getLez()));

		// All the vehicles will belong to the loaded copert table
		CopertParser parser = loadParser(context);
		
		Map<String, Establishment> establishments = null;
		try {
			establishments = 
					EstablishmentLoader.loadEstablishments(
							context.getFileLoader().load("establishments"),
							context.getFileLoader().load("fleet_profiles"),
							parser,
							PollutionScenario.random
							);
		} catch (IOException e) {
			e.printStackTrace();
		}
		((LezContext) context).setEstablishments(establishments);
		
		Map<String, OsmNode> geoNodes = new HashMap<>();
		for (String id : context.nodes.keySet()) {
			OsmNode node = (OsmNode) context.nodes.get(id);
			if(!Arrays.asList(forbiddenClosestNodeHighways).contains(node.getRoad().getHighway()))
					geoNodes.put(id, node);
		}
		GeoStrTree kdTree = new GeoStrTree(geoNodes);
		
		for (Establishment establishment : establishments.values()) {
			establishment.setClosestOsmNode((OsmNode) kdTree.getNearestNodeFrom(
					new LonLat().project(establishment.getLocation())
					)
				);
		}
		
		Run.logger.info("Applying lez...");
		LezPreprocessor preprocessor = new LezPreprocessor(getLez(), parser);

		int establishmentsInLez = 0;
		int totalVehiclesReplaced = 0;
		for(Establishment establishment : establishments.values()) {
			if(getLez().contains(establishment.getClosestOsmNode())) {
				//Run.logger.info("[LEZ] " + establishment.getId() + " - " + establishment.getName());
				int replacedVehiclesCount = preprocessor.preprocess(establishment);
				totalVehiclesReplaced += replacedVehiclesCount;
				//Run.logger.info("[LEZ] Number of vehicles replaced : " + replacedVehiclesCount);
				establishmentsInLez++;
			}
		}
		Run.logger.info("[LEZ] Number of establishments in lez : " + establishmentsInLez);
		Run.logger.info("[LEZ] Total number of vehicles replaced : " + totalVehiclesReplaced);
		
		int agentId = 0;
		Collection<OsmAgent> agents = new ArrayList<>();
		Collection<BuildAgentThread> threads = new ArrayList<>();
		
		for (Establishment establishment : establishments.values()) {
			if ( establishment.getActivity() == ST8.PRIVATE_HABITATION ) {
				//if it's a passenger car TODO

			} 
			else {
				for(String vehicleId : establishment.getRounds().keySet()) {
					BuildAgentThread thread = new BuildAgentThread(agentId++, vehicleId, establishment, (LezContext) context);
					threads.add(thread);
					thread.start();
				}
			}
		}
		for(BuildAgentThread thread : threads) {
			try {
				thread.join();
				agents.add(thread.getBuiltAgent());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		for(Establishment establishment : establishments.values()) {
			if(!establishment.getFleet().isEmpty()) {
				List<EuroNorm> euroNorms = new ArrayList<>();
				for(Vehicle vehicle : establishment.getFleet().values()) {
					euroNorms.add(vehicle.getEuroNorm());
				}
				/*Run.logger.info(
						"[" + establishment.getId() + "] " + establishment.getName()
						+ " - fleet norms : " + euroNorms
						);*/
			}
		}
		
		return agents;
	}
	
	private static class BuildAgentThread extends Thread {
		
		private int agentId;
		private String vehicleId;
		private Establishment establishment;
		private LezContext context;
		
		private DeliveryDriverAgent builtAgent;
		private DeliveryDriverBehavior builtBehavior;
		
		public BuildAgentThread(int agentId, String vehicleId, Establishment establishment, LezContext context) {
			super();
			this.agentId = agentId;
			this.vehicleId = vehicleId;
			this.establishment = establishment;
			this.context = context;
		}

		public void run() {
			DriverBody driver = new DriverBody(establishment.getFleet().get(vehicleId));
			builtBehavior
				= new DeliveryDriverBehavior(
						driver,
						establishment.getRounds().get(vehicleId),
						context
						);
			
			builtAgent = new DeliveryDriverAgent(String.valueOf(agentId), driver, builtBehavior);

			builtBehavior.addRoundDepartureListener((event) -> {
				Run.logger.info(
				"[" + SmartGov.getRuntime().getClock().getHour()
				+ ":" + SmartGov.getRuntime().getClock().getMinutes() + "]"
				+ "Agent " + builtAgent.getId()
				+ " begins round for [" + establishment.getId() + "] "
				+ establishment.getName());
			});
				
			builtBehavior.addRoundEndListener((event) -> {
				Run.logger.info(
					"[" + SmartGov.getRuntime().getClock().getHour()
					+ ":" + SmartGov.getRuntime().getClock().getMinutes() + "]"
					+ "Agent " + builtAgent.getId()
					+ " ended round for [" + establishment.getId() + "] "
					+ establishment.getName()
					);
				context.ongoingRounds.remove(builtAgent.getId());
				Run.logger.info("Rounds still ongoing : " + context.ongoingRounds.size());
				if(context.ongoingRounds.isEmpty()) {
					SmartGov.getRuntime().stop();
				}
			});
		}
		
		/*
		 * Listeners are initialized there from the main thread to avoid
		 * concurrent modifications errors.
		 * Rounds are also added to the context there, for the same reasons.
		 */
		public DeliveryDriverAgent getBuiltAgent() {
			builtBehavior.setUpListeners();
			context.ongoingRounds.put(builtAgent.getId(), builtBehavior.getRound());
			return builtAgent;
		}
		
	}
	
	/**
	 * Special scenario used to model deliveries without LEZ.
	 *
	 */
	public static class NoLezDeliveries extends DeliveriesScenario {
		
		/**
		 * NoLezDeliveries
		 */
		public static final String name = "NoLezDeliveries";

		/**
		 * NoLezDeliveries constructor.
		 * 
		 * {@link Lez#none()} is used as the LEZ for this scenario.
		 */
		public NoLezDeliveries() {
			super(Lez.none());
		}
		
	}



}

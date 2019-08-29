package org.liris.smartgov.lez.input.osm;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.liris.smartgov.lez.cli.tools.Run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.smartgov.osmparser.Osm;
import com.smartgov.osmparser.OsmParser;
import com.smartgov.osmparser.examples.roads.WayNodesFilter;
import com.smartgov.osmparser.filters.elements.TagFilter;
import com.smartgov.osmparser.filters.tags.BaseTagMatcher;
import com.smartgov.osmparser.filters.tags.NoneTagMatcher;
import com.smartgov.osmparser.filters.tags.TagMatcher;

/**
 * A pre-process used to extract json nodes and roads file from an input osm
 * file.
 */
public class OsmRoadParser {
	
	/**
	 * Highways types extracted from the osm file.
	 * 
	 * Check the <a href="https://wiki.openstreetmap.org/wiki/Key:highway">osm documentation</a>
	 * for more information.
	 * 
	 * <ul>
	 * <li> motorway </li>
	 * <li> trunk </li>
	 * <li> primary </li>
	 * <li> secondary </li>
	 * <li> tertiary </li>
	 * <li> unclassified </li>
	 * <li> residential </li>
	 * <li> motorway_link </li>
	 * <li> trunk_link </li>
	 * <li> primary_link </li>
	 * <li> secondary_link </li>
	 * <li> tertiary_link </li>
	 * <li> living_street </li>
	 * </ul>
	 */
	public static final String[] highways = {
			"motorway",
			"trunk",
			"primary",
			"secondary",
			"tertiary",
			"unclassified",
			"residential",
			"motorway_link",
			"trunk_link",
			"primary_link",
			"secondary_link",
			"tertiary_link",
			"living_street"
	};
	
	/**
	 * Parses the input osm file, and write results as json nodes and ways
	 * files.
	 *
	 * Extracts the {@link #highways} road types and associated nodes.
	 *
	 * @param args <i>osm_input_file nodes_output_file ways_output_file</i>
	 * <p>
	 * The osm input file must be in the OSM XML format.
	 * </p>
	 * @throws JAXBException if a case of a problem parsing the input osm file
	 * @throws IOException is case of a problem reading or writing an input / output file
	 * @throws ParseException in case of bad command line options
	 */
	public static void main(String[] args) throws JAXBException, IOException, ParseException {
		Options helpOpts = new Options();
		Option help = new Option("h", "help", false, "Displays this help message");
		helpOpts.addOption(help);
		
		
		Options fullOpts = new Options();
		fullOpts.addOption(help);
		Option osmFile = new Option("f", "osm-file", true, "Input OSM file");
		osmFile.setArgName("file");
		osmFile.setRequired(true);
		Option nodeOutput = new Option("n", "nodes-file", true, "JSON nodes output file");
		nodeOutput.setArgName("file");
		nodeOutput.setRequired(true);
		Option waysOutput = new Option("w", "ways-file", true, "JSON ways output file");
		waysOutput.setArgName("file");
		waysOutput.setRequired(true);
		fullOpts.addOption(osmFile);
		fullOpts.addOption(nodeOutput);
		fullOpts.addOption(waysOutput);
		
		if(args.length == 0) {
			printHelp(fullOpts);
			return;
		}

		CommandLineParser cmdParser = new DefaultParser();
		CommandLine helpCmd = cmdParser.parse(helpOpts, args, true);
		
		if(helpCmd.hasOption("h")) {
			printHelp(fullOpts);
			return;
		}
		
		cmdParser = new DefaultParser();
		CommandLine mainCmd = cmdParser.parse(fullOpts, args);
		
		long beginTime = System.currentTimeMillis();
		OsmParser parser = new OsmParser();
		
		Run.logger.info("Parsing osm data from : " + new File(mainCmd.getOptionValue("f")));
	    // Parse the test osm file
	    Osm osm = (Osm) parser.parse(new File(mainCmd.getOptionValue("f")));
	    
	    Run.logger.info("Nodes found : " + osm.getNodes().size());
	    Run.logger.info("Ways found : " + osm.getWays().size());
	    
	    Run.logger.info("Applying filters...");
	    
	    // Start from a tag matche that doesn't match anything
	    TagMatcher highwaysTagMatcher = new NoneTagMatcher();
	    
	    // Increment the matcher with required highway types
	    // Everything that is not an highway won't be kept
	    for(String highway : highways) {
	    	highwaysTagMatcher = highwaysTagMatcher.or(new BaseTagMatcher("highway", highway));
	    }
	    
	    
	    // Filter only highways
        parser.setWayFilter(
        		// Considering only highways
        		new TagFilter(
        				highwaysTagMatcher
        		)
        		.or(
        			// Also service highways
        			new TagFilter(new BaseTagMatcher("highway", "service"))
        			.and(
        				// Only service highways with a "service" tag
        				new TagFilter(new BaseTagMatcher("service", ".*"))
        				.and(new TagFilter(
	        					// That is equal to "alley", "parking_aisle" or "driveway"
	        					new BaseTagMatcher("service", "alley")
	        					.or("service", "parking_aisle")
	        					.or("service", "driveway")
	        					)
	        				))
        				)
        		);
        
        // Keep highway, service, name and ref tags
        parser.setWayTagMatcher(
        		new BaseTagMatcher("highway", ".*")
        		.or("service", ".*")
        		.or("name", ".*")
        		.or("ref", ".*")
        		.or("oneway", ".*")
        		);

        Run.logger.info("Filtering ways...");
        long filterBeginTime = System.currentTimeMillis();
        // Filter the ways and their tags
        parser.filterWays();
        Run.logger.info("Ways filtered in " + (System.currentTimeMillis() - filterBeginTime) + "ms");
        // Keep only nodes that belong to ways
        parser.setNodeFilter(new WayNodesFilter(osm.getWays()));
        
        // Does not keep any tag for nodes
        parser.setNodeTagMatcher(new NoneTagMatcher());
        
        Run.logger.info("Filtering nodes...");
        filterBeginTime = System.currentTimeMillis();
        // Filter nodes
        parser.filterNodes();
        Run.logger.info("Nodes filtered in " + (System.currentTimeMillis() - filterBeginTime) + "ms");
        
        Run.logger.info("Number of filtered roads : " + osm.getWays().size());
        Run.logger.info("Number of filtered nodes : " + osm.getNodes().size());
        
        // Custom object mapper to indent output
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        Run.logger.info("Writing filtered roads to " + new File(mainCmd.getOptionValue("w")));
        parser.writeWays(new File(mainCmd.getOptionValue("w")), mapper);
        
        Run.logger.info("Writing filtered nodes to " + new File(mainCmd.getOptionValue("n")));
        parser.writeNodes(new File(mainCmd.getOptionValue("n")), mapper);
        
        Run.logger.info("Parsing end. Total process time : " + (System.currentTimeMillis() - beginTime) + "ms");

	}
	
	private static void printHelp(Options opts) {
		String header = "Build JSON nodes and ways input file from the specified osm node.";
		String footer =""
				+ "\t - Loads OSM data from preprocessed nodes and ways files, specified as <nodes> and "
				+ "<roads> fields in the specified configuration.\n"
				+ "\t - Builds establishments, delivery drivers and fleets, and compute the shortest path of the "
				+ "first step of each round.\n"
				+ "\t - Writes initial nodes and arcs to <output>/init folder.\n"
				+ "\nNotice that this step is just a convenient way to check the initial configuration, "
				+ "but is not required before launching the \"run\" task, that will perform the initialization "
				+ "step anyway.";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("smartgovlez roads", header, opts, footer, true);
	}
}
package com.smartgov.lez.core.copert.fields;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.smartgov.lez.core.copert.fields.CopertField;
import com.smartgov.lez.core.copert.tableParser.CopertHeader;
import com.smartgov.lez.core.copert.tableParser.CopertParser;
import com.smartgov.lez.core.copert.tableParser.SubTable;

/**
 * Class used to test the integrity of the implemented Copert fields and headers.
 * It will check that for each implemented column (fields) and headers, the actual
 * String value is well mapped to an implemented Enum value.
 * 
 * @author pbreugnot
 *
 */
public class CopertFieldsTest {
	
	
	private SubTable loadTable() {
		URL url = this.getClass().getResource("copert_table.csv"); // Complete Copert table with Light Commercial Vehicles and Heavy Duty Trucks
		CopertParser copertParser = new CopertParser(url.getFile());
		return copertParser.getCopertTree().getSubTable();
	}
	
	@Test
	public void TestHeaders() {
		
		SubTable table = loadTable();
		for (String stringHeader : table.keySet()) {
			CopertHeader matchingHeader = CopertHeader.getValue(stringHeader);
			// Assert that the CopertHeader enum value exists
			assertThat(
					matchingHeader,
					notNullValue()
					);
			
			// Assert that the enum value corresponds to the actual column name
			assertThat(
					matchingHeader.columnName(),
					equalTo(stringHeader)
					);
			
		}
	}
	
	@Test
	public void testFields() {
		
		SubTable table = loadTable();
		List<String> selectorsHeaders = Arrays.asList("Category", "Fuel", "Segment", "Euro Standard");
		for (String stringHeader : selectorsHeaders) {
			Set<String> headers = table.keySet();
			Set<String> fields = new HashSet<String>(table.get(stringHeader));
			for(String field : fields) {
				CopertField matchingField = CopertField.getValue(
						CopertHeader.getValue(stringHeader),
						field);
				assertThat(
						matchingField,
						notNullValue()
						);
				
				assertThat(
						Pattern.matches(matchingField.matcher(), field),
						equalTo(true)
						);
			}
		}
	}

}
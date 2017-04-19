package org.simplity.kernel.util.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.test.Customer;

public class SheetUtilTest {

	@Test
	public final void columnToArray(){
		MultiRowsSheet dataSheet = getSheet();
		Object[] colarray = dataSheet.columnAsArray("customerNumber");
		assertNotNull(colarray);
		Object[] expectedResult = new Integer[]{103,112,114,119,121};
		assertEquals(Arrays.asList(expectedResult).toString(), Arrays.asList(colarray).toString());
	}
	
	@Test
	public final void columnToList(){
		MultiRowsSheet dataSheet = getSheet();
		List<Object> list= dataSheet.columnAsList("customerName");
		List<String> expectedList = new ArrayList<String>();
		expectedList.add("Atelier graphique");
		expectedList.add("Signal Gift Stores");
		expectedList.add("Australian Collectors");
		expectedList.add("La Rochelle Gifts");
		expectedList.add("Baane Mini Imports");
		assertEquals(expectedList,list);
	}
	
	@Test
	public final void columnToSet(){
		MultiRowsSheet dataSheet = getSheet();
		Set<Object> set= dataSheet.columnAsSet("city");
		Set<String> expectedSet = new HashSet<String>();
		expectedSet.add("Nantes");
		expectedSet.add("Las Vegas");
		expectedSet.add("Melbourne");
		expectedSet.add("Nantes");
		expectedSet.add("San Rafael");
		System.out.println(set);
		assertEquals(expectedSet,set);
	}
	
	@Test
	public final void columnsToMap(){
		MultiRowsSheet dataSheet = getSheet();
		Map<String,Object> map= dataSheet.columnsAsMap("city","country");
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("Nantes", "France");
		expectedResult.put("Las Vegas", "USA");
		expectedResult.put("Melbourne", "Australia");
		expectedResult.put("Nantes", "France");
		expectedResult.put("San Rafael", "USA");
		assertEquals(expectedResult,map);
	}
	
	@Test
	public final void sheetToList(){
		MultiRowsSheet dataSheet = getSheet();
		List<Object> actualResult = dataSheet.toList("org.simplity.test.Customer");
		Customer cust = (Customer)actualResult.get(0);
		assertEquals(cust.getCustomerName(), "Atelier graphique");
	}
	
	@Test
	public final void sheetToSet(){
		MultiRowsSheet dataSheet = getSheet();
		Set<Object> actualResult = dataSheet.toSet("org.simplity.test.Customer");
		Customer cust = (Customer)actualResult.iterator().next();
		assertEquals(cust.getCustomerNumber(), 112);
	}
	
	
	public final MultiRowsSheet getSheet(){
		String[] columnNames = {"customerNumber","customerName","address","city","state","country","postalCode"};
		ValueType[] columnValueTypes = {ValueType.INTEGER,ValueType.TEXT,ValueType.TEXT,ValueType.TEXT,ValueType.TEXT,ValueType.TEXT,ValueType.INTEGER};
		MultiRowsSheet sheet = new MultiRowsSheet(columnNames, columnValueTypes);

		Value[] row1 = {Value.newIntegerValue(103),Value.newTextValue("Atelier graphique"),Value.newTextValue("54, rue Royale"),Value.newTextValue("Nantes"),Value.newTextValue("NULL"),Value.newTextValue("France"),Value.newIntegerValue(44000)};
		Value[] row2 = {Value.newIntegerValue(112),Value.newTextValue("Signal Gift Stores"),Value.newTextValue("8489 Strong St."),Value.newTextValue("Las Vegas"),Value.newTextValue("NV"),Value.newTextValue("USA"),Value.newIntegerValue(83030)};
		Value[] row3 = {Value.newIntegerValue(114),Value.newTextValue("Australian Collectors"),Value.newTextValue("636 St Kilda Road"),Value.newTextValue("Melbourne"),Value.newTextValue("Victoria"),Value.newTextValue("Australia"),Value.newIntegerValue(3004)};
		Value[] row4 = {Value.newIntegerValue(119),Value.newTextValue("La Rochelle Gifts"),Value.newTextValue("67, rue des Cinquante Otages"),Value.newTextValue("Nantes"),Value.newTextValue("NULL"),Value.newTextValue("France"),Value.newIntegerValue(4110)};
		Value[] row5 = {Value.newIntegerValue(121),Value.newTextValue("Baane Mini Imports"),Value.newTextValue("Erling Skakkes gate 78"),Value.newTextValue("San Rafael"),Value.newTextValue("CA"),Value.newTextValue("USA"),Value.newIntegerValue(97562)};
	
		sheet.addRow(row1);
		sheet.addRow(row2);
		sheet.addRow(row3);
		sheet.addRow(row4);
		sheet.addRow(row5);
		
		return sheet;
	}
}

import java.util.ArrayList;

public class Join {
	public static Table joinNormal(Table table1, Table table2, String name1, String name2) {
		Table table = new Table(table1.database_name, table1.schema_name, table1.name + " join " + table2.name);
		ArrayList<Attribute> attributes = new ArrayList<>();
		for (Attribute attribute: table1.attributes) {
			attributes.add(new Attribute(attribute.name, attribute.type, attribute.maxLen, attribute.unique,
					attribute.nullable, attribute.primaryKey, attribute.default_val));
		}
		for (Attribute attribute: table2.attributes) {
			attributes.add(new Attribute(attribute.name, attribute.type, attribute.maxLen, attribute.unique,
					attribute.nullable, attribute.primaryKey, attribute.default_val));
		}
		table.attributes = attributes;
		int attributeId = 0, ruleColumn1 = 0, ruleColumn2 = 0;
		for (Attribute attribute: table1.attributes) {
			if (attribute.name.equals(name1)) {
				ruleColumn1 = attributeId;
				break;
			}
			++attributeId;
		}
		attributeId = 0;
		for (Attribute attribute: table2.attributes) {
			if (attribute.name.equals(name2)) {
				ruleColumn2 = attributeId;
				break;
			}
			++attributeId;
		}
		// enumerate all elements in the Cartesian product of the sets of rows in each table
		for (ArrayList<String> record1: table1.array) {
			for (ArrayList<String> record2: table2.array) {
				if (record1.get(ruleColumn1).equals(record2.get(ruleColumn2))) {
					ArrayList<String> record = new ArrayList<>();
					record.addAll(record1);
					record.addAll(record2);
					table.array.add(record);
				}
			}
		}
		return table;
	}

	public static Table joinUnique(Table table1, Table table2, String name1, String name2, int uniqueTable) {
		Table table = new Table(table1.database_name, table1.schema_name, table1.name + " join " + table2.name);
		ArrayList<Attribute> attributes = new ArrayList<>();
		for (Attribute attribute : table1.attributes) {
			attributes.add(new Attribute(attribute.name, attribute.type, attribute.maxLen, attribute.unique,
					attribute.nullable, attribute.primaryKey, attribute.default_val));
		}
		for (Attribute attribute : table2.attributes) {
			attributes.add(new Attribute(attribute.name, attribute.type, attribute.maxLen, attribute.unique,
					attribute.nullable, attribute.primaryKey, attribute.default_val));
		}
		table.attributes = attributes;
		int attributeId = 0, ruleColumn1 = 0, ruleColumn2 = 0;
		for (Attribute attribute : table1.attributes) {
			if (attribute.name.equals(name1)) {
				ruleColumn1 = attributeId;
				break;
			}
			++attributeId;
		}
		attributeId = 0;
		for (Attribute attribute : table2.attributes) {
			if (attribute.name.equals(name2)) {
				ruleColumn2 = attributeId;
				break;
			}
			++attributeId;
		}
		// sort table1.array by ruleColumn1
		int finalRuleColumn1 = ruleColumn1;
		table1.array.sort((ArrayList<String> record1, ArrayList<String> record2) -> {
			if (record1.get(finalRuleColumn1) == null) {
				if (record2.get(finalRuleColumn1) == null) {
					return 0;
				}
				return -1;
			}
			if (record2.get(finalRuleColumn1) == null) {
				return 1;
			}
			return record1.get(finalRuleColumn1).compareTo(record2.get(finalRuleColumn1));
		});
		// sort table2.array by ruleColumn2
		int finalRuleColumn2 = ruleColumn2;
		table2.array.sort((ArrayList<String> record1, ArrayList<String> record2) -> {
			if (record1.get(finalRuleColumn2) == null) {
				if (record2.get(finalRuleColumn2) == null) {
					return 0;
				}
				return -1;
			}
			if (record2.get(finalRuleColumn2) == null) {
				return 1;
			}
			return record1.get(finalRuleColumn2).compareTo(record2.get(finalRuleColumn2));
		});
		// merge table1.array and table2.array
		int pnt1 = 0, pnt2 = 0;
		while (pnt1 < table1.array.size() && pnt2 < table2.array.size()) {
			ArrayList<String> record1 = table1.array.get(pnt1), record2 = table2.array.get(pnt2);
			if (record1.get(ruleColumn1).equals(record2.get(ruleColumn2))) {
				ArrayList<String> record = new ArrayList<>();
				record.addAll(record1);
				record.addAll(record2);
				table.array.add(record);
				if (uniqueTable == 1) {
					++pnt2;
				}
				else {
					++pnt1;
				}
			}
			else if (record1.get(ruleColumn1).compareTo(record2.get(ruleColumn2)) < 0) {
				++pnt1;
			}
			else {
				++pnt2;
			}
		}
		return table;
	}
}

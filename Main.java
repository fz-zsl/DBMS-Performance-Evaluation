import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
	public static ArrayList<Table> tables = new ArrayList<>();
	public static String database_name, schema_name;
	public static int selectCnt = 0;

    public static void main(String[] args) {
	    Scanner sc = new Scanner(System.in);
	    System.out.print("Please select database: ");
		database_name = sc.nextLine();
		System.out.print("Please select schema: ");
		schema_name = sc.nextLine();
		File database_folder = new File("./data/" + database_name);
		if (!database_folder.exists()) {
			System.out.println("[#]CREATE DATABASE " + database_name + ";");
			database_folder.mkdirs();
			System.out.println("[.]Done.");
		}
		File schema_folder = new File("./data/" + database_name + "/" + schema_name);
		if (!schema_folder.exists()) {
			System.out.println("[#]CREATE SCHEMA " + schema_name + ";");
			schema_folder.mkdirs();
			System.out.println("[.]Done.");
		}
		File sql;
		String sql_file;
		while (true) {
			System.out.print("Please select SQL file (under SQL_src folder): ");
			sql_file = sc.nextLine();
			sql = new File("./SQL_src/" + sql_file);
			if (!sql.exists()) {
				System.out.println("\u001B[31m[!]\u001B[0mFile not found.");
			}
			else {
				break;
			}
		}
		readSql(sql);
		Instant startOfOutput = Instant.now();
		for (Table table: tables) {
			String output = "name:," + table.name + "\n";
			if (!table.comments.isEmpty()) {
				for (String comment: table.comments) {
					output = output.concat("Comment:," + comment + "\n");
				}
			}
			boolean showAttributeComment = false;
			for (Attribute attribute: table.attributes) {
				output = output.concat(attribute.name + ",");
				showAttributeComment |= !attribute.comments.isEmpty();
			}
			output = output.concat("\n");
			if (showAttributeComment) {
				for (Attribute attribute: table.attributes) {
					if (!attribute.comments.isEmpty()) {
						for (String comment: attribute.comments) {
							output = output.concat(comment.replace(",", "，"));
							if (!comment.equals(attribute.comments.get(attribute.comments.size() - 1))) {
								output = output.concat(" | ");
							}
						}
					}
					output = output.concat(",");
				}
				output = output.concat("\n");
			}
			for (ArrayList<String> record: table.array) {
				for (String value: record) {
					output = output.concat(Utils.lastCheck(value) + ",");
				}
				output = output.concat("\n");
			}
			try {
				String dbFolderName = "./data/" + database_name;
				File dbFolder = new File(dbFolderName);
				if (!dbFolder.exists()) {
					dbFolder.mkdirs();
				}
				String schemaFolderName = "./data/" + database_name + "/" + schema_name;
				File schemaFolder = new File(schemaFolderName);
				if (!schemaFolder.exists()) {
					schemaFolder.mkdirs();
				}
				String fileName = "./data/" + database_name + "/" + schema_name + "/"
						+ sql_file.substring(0, sql_file.length() - 4) + "_" + table.name + ".csv";
				File file = new File(fileName);
				if (file.exists()) {
					file.delete();
				}
				BufferedWriter bufferedWriter = new BufferedWriter(
						new OutputStreamWriter(
								new FileOutputStream(fileName, true), StandardCharsets.UTF_8
						)
				);
				bufferedWriter.write(output);
				bufferedWriter.close();
				try {
					Desktop.getDesktop().open(file);
				}
				catch (Exception exception) {
					System.out.println("\u001B[31m[!]Error:\u001B[0m " + exception.getMessage());
				}
			}
			catch (Exception exception) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m " + exception.getMessage());
			}
		}
		Instant endOfOutput = Instant.now();
		Duration durationOfOutput = Duration.between(startOfOutput, endOfOutput);
		System.out.println("[.]Output time: " + durationOfOutput.toMillis() + "ms.");
    }

	public static void readSql(File sql) {
		try {
			Scanner sc = new Scanner(sql);
			String prefix = "";
			System.out.println("[.]Timer: Start.");
			ArrayList<Instant> timestamps = new ArrayList<>();
			timestamps.add(Instant.now());
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				line = line.trim();
				if (line.isEmpty()) {
					System.out.println("[.]Timer: Record.");
					timestamps.add(Instant.now());
					continue;
				}
				if (line.startsWith("--")) {
					continue;
				}
				prefix = prefix.concat(" " + line);
				if (prefix.endsWith(";")) {
					readLine(Utils.toLowerCase2(prefix));
					prefix = "";
				}
			}
			if (!prefix.isEmpty()) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m The SQL file is incomplete.");
			}
			else {
				System.out.println("[.]Timer: End.");
				timestamps.add(Instant.now());
				System.out.println("[.]Timer: Generating report.");
				genReport(timestamps, sql);
				System.out.println("[.]Generator: Done.");
			}
		}
		catch (Exception exception) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m " + exception.getMessage());
		}
	}

	public static void readLine(String line) {
		line = Utils.cleanString(line);
		ArrayList<String> words = Utils.divString(line);
		if (words.isEmpty()) {
			return;
		}
		if (words.get(0).equals("create")) {
			commandCreate(words, line);
		}
		else if (words.get(0).equals("alter") && words.get(1).equals("table")) {
			commandAlter(words, line, words.get(3));
		}
		else if (words.get(0).equals("drop") && words.get(1).equals("table")) {
			commandDrop(words);
		}
		else if (words.get(0).equals("insert")) {
			commandInsert(words, line);
		}
		else if (words.get(0).equals("select")) {
			commandSelect(words, line);
		}
		else if (words.get(0).equals("update")) {
			commandUpdate(words, line);
		}
		else if (words.get(0).equals("delete")) {
			commandDelete(words, line);
		}
		else if (words.get(0).equals("comment")) {
			commandComment(words, line);
		}
		else {
			System.out.println("\u001B[31m[!]\u001B[0m Unsupported command: " + words.get(0));
		}
	}

	public static void commandCreate(ArrayList<String> words, String line) {
		if (words.get(1).equals("database")) {
			database_name = words.get(2).substring(0, words.get(2).length() - 1);
			return;
		}
		if (words.get(1).equals("schema")) {
			schema_name = words.get(2).substring(0, words.get(2).length() - 1);
			return;
		}
		if (!words.get(1).equals("table")) {
			System.out.println("\u001B[31m[!]\u001B[0m Unsupported command: " + words.get(0) + ' ' + words.get(1));
			return;
		}
		if (!tables.isEmpty()) {
			for (Table prev_table: tables) {
				if (prev_table.name.equals(words.get(2))) {
					System.out.println("\u001B[31m[!]Error:\u001B[0m Table " + words.get(2) + " already exists.");
					return;
				}
			}
		}
		Table table = new Table(database_name, schema_name, words.get(2));
		tables.add(table);
		String[] attributes_array = line.substring(line.indexOf('(') + 2, line.length() - 4).split(",");
		ArrayList<String> attributes = new ArrayList<>();
		String prefix = "";
		int parenCnt = 0;
		for (String str: attributes_array) {
			System.out.println(str);
			if (!prefix.isEmpty()) {
				prefix = prefix.concat(",");
			}
			prefix = prefix.concat(str.trim());
			for (int i = 0; i < str.length(); ++i) {
				if (str.charAt(i) == '(') {
					++parenCnt;
				}
				else if (str.charAt(i) == ')') {
					--parenCnt;
				}
			}
			if (parenCnt == 0) {
				attributes.add(prefix);
				System.out.println("Prefix:" + prefix);
				prefix = "";
			}
		}
		if (!prefix.isEmpty()) {
			attributes.add("," + prefix);
		}
		for (String attribute: attributes) {
			if (attribute.endsWith(",")) {
				table.addAttribute(attribute.substring(0, attribute.length() - 1).trim());
			}
			else {
				table.addAttribute(attribute.trim());
			}
		}
	}

	public static void commandAlter(ArrayList<String> words, String line, String operation) {
		String tableName = words.get(2);
		Table targetTable = null;
		for (Table table: tables) {
			if (table.name.equals(tableName)) {
				targetTable = table;
				break;
			}
		}
		if (targetTable == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Table " + tableName + " does not exist.");
			return;
		}
		if (operation.equalsIgnoreCase("set") && words.get(4).equals("schema")) {
			targetTable.schema_name = words.get(5).substring(0, words.get(5).length() - 1);
			return;
		}
		if (operation.equalsIgnoreCase("add")) {
			if (words.get(4).equals("column")) {
				targetTable.addAttribute(line.substring(line.indexOf("column") + 7, line.length() - 1).trim());
				return;
			}
			if (words.get(4).equals("constraint")) {
				String constraintName = words.get(5);
				String constraintFormula = line.substring(line.indexOf("(") + 2, line.length() - 3).trim();
				targetTable.constraints.add(new Table.Tuple<>(constraintName, constraintFormula));
				if (targetTable.where(constraintFormula).size() != targetTable.array.size()) {
					System.out.println("\u001B[31m[!]Error:\u001B[0m Constraint " + constraintName + " is not satisfied.");
					targetTable.constraints.remove(targetTable.constraints.size() - 1);
				}
				return;
			}
		}
		if (operation.equalsIgnoreCase("drop") && words.get(4).equals("constraint")) {
			String constraintName = words.get(5);
			for (Table.Tuple<String, String> constraint: targetTable.constraints) {
				if (constraint.first.equals(constraintName)) {
					targetTable.constraints.remove(constraint);
					System.out.println("[.]Constraint " + constraintName + " dropped.");
					return;
				}
			}
			System.out.println("\u001B[31m[!]Error:\u001B[0m Constraint " + constraintName + " does not exist.");
			return;
		}
		if (operation.equalsIgnoreCase("owner") && words.get(4).equals("to")) {
			targetTable.owner = words.get(5).substring(0, words.get(5).length() - 1);
			return;
		}
		String attributeName = words.get(5);
		if (words.size() == 6 && attributeName.endsWith(";")) {
			attributeName = attributeName.substring(0, attributeName.length() - 1);
		}
		Attribute targetAttribute = null;
		int targetCol = 0;
		for (Attribute attribute: targetTable.attributes) {
			if (attribute.name.equals(attributeName)) {
				targetAttribute = attribute;
				break;
			}
			++targetCol;
		}
		if (targetAttribute == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + attributeName + " does not exist.");
			return;
		}
		if (operation.equalsIgnoreCase("drop") && words.get(4).equals("column")) {
			targetTable.attributes.remove(targetAttribute);
			for (ArrayList<String> record: targetTable.array) {
				record.remove(targetCol);
			}
		}
		else if (operation.equalsIgnoreCase("rename") && words.get(4).equals("column")) {
			targetAttribute.name = words.get(7).substring(0, words.get(7).length() - 1);
		}
	}

	public static void commandDrop(ArrayList<String> words) {
		String tableName = words.get(2).substring(0, words.get(2).length() - 1);
		Table targetTable = null;
		for (Table table: tables) {
			if (table.name.equals(tableName)) {
				targetTable = table;
			}
		}
		if (targetTable == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Table not specified.");
			return;
		}
		tables.remove(targetTable);
	}

	public static void commandInsert(ArrayList<String> words, String line) {
		String table_name = null;
		for (String word: words) {
			if (word.equals("into")) {
				table_name = words.get(words.indexOf(word) + 1);
				break;
			}
		}
		if (table_name == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Table not specified.");
			return;
		}
		Table targetTable = null;
		for (Table table: tables) {
			if (table.name.equals(table_name)) {
				targetTable = table;
				break;
			}
		}
		if (targetTable == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Table " + table_name + " does not exist.");
			return;
		}
		String attributes_string = null;
		if (words.get(2).equals("(")) {
			attributes_string = words.get(2).substring(words.get(2).indexOf('(') + 2, words.get(2).lastIndexOf(')') - 1);
		}
		ArrayList<String> attribute_names = new ArrayList<>();
		if (attributes_string != null) {
			String[] attribute_names_array = attributes_string.split(",");
			for (String attribute_name: attribute_names_array) {
				attribute_names.add(attribute_name.trim());
			}
		}
		String[] values_array = line.substring(line.lastIndexOf("values") + 9, line.length() - 4).split(",");
		ArrayList<String> values = new ArrayList<>();
		for (String attribute_name: values_array) {
			if (attribute_name.startsWith("\"") && attribute_name.endsWith("\"")) {
				if (attribute_name.startsWith("\"\"") && attribute_name.endsWith("\"\"")) {
					values.add("");
				}
				else {
					values.add(attribute_name.substring(1, attribute_name.length() - 1));
				}
			}
			else {
				values.add(attribute_name.trim());
			}
		}
		if (attributes_string != null) {
			targetTable.addRecord(attribute_names, values);
		}
		else {
			targetTable.addRecord(values);
		}
	}

	public static void commandSelect(ArrayList<String> words, String line) {
		++selectCnt;
		Table targetTable = null;
		for (String word: words) {
			if (word.equals("from")) {
				for (Table table: tables) {
					if (table.name.equals(words.get(words.indexOf(word) + 1))) {
						targetTable = table;
						break;
					}
				}
				if (targetTable == null) {
					System.out.println("\u001B[31m[!]Error:\u001B[0m Table " + words.get(words.indexOf(word) + 1) + " does not exist.");
					return;
				}
				break;
			}
		}
		if (targetTable == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Table not specified.");
			return;
		}
		ArrayList<Integer> selectAttributes = new ArrayList<>();
		if (words.get(1).equals("*")) {
			//all attributes
			for (Attribute attribute: targetTable.attributes) {
				selectAttributes.add(attribute.id);
			}
		}
		else {
			//specified attributes
			String[] attribute_names = line.substring(line.indexOf('(') + 2, line.indexOf(')') - 1).split(",");
			for (String attribute_name: attribute_names) {
				boolean found = false;
				for (Attribute attribute: targetTable.attributes) {
					if (attribute.name.equals(attribute_name.trim())) {
						selectAttributes.add(attribute.id);
						found = true;
						break;
					}
				}
				if (!found) {
					System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + attribute_name + " does not exist.");
					return;
				}
			}
		}
		ArrayList<Integer> selectedRecords = targetTable.where(
				line.substring(line.indexOf("where") + 6, line.length() - 1)
		);
		String output = line + "\n";
		for (Integer attribute_id: selectAttributes) {
			output = output.concat(targetTable.attributes.get(attribute_id).name + ",");
		}
		output = output.concat("\n");
		for (Integer selectedId: selectedRecords) {
			for (Integer attribute_id: selectAttributes) {
				output = output.concat(Utils.lastCheck(
						targetTable.array.get(selectedId).get(attribute_id)
				) + ",");
			}
			output = output.concat("\n");
		}
		output = output.concat("Total: " + selectedRecords.size() + " record(s).\n");
		try {
			String fileName = "./select_results/" + targetTable.name + selectCnt + ".csv";
			File file = new File(fileName);
			if (file.exists()) {
				file.delete();
			}
			BufferedWriter bufferedWriter = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream(fileName, true), StandardCharsets.UTF_8
					)
			);
			bufferedWriter.write(output);
			bufferedWriter.close();
			try {
				Desktop.getDesktop().open(file);
			}
			catch (Exception exception) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m " + exception.getMessage());
			}
		}
		catch (Exception exception) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m " + exception.getMessage());
		}
	}

	public static void commandUpdate(ArrayList<String> words, String line) {
		String tableName = words.get(1);
		Table targetTable = null;
		for (Table table: tables) {
			if (table.name.equals(tableName)) {
				targetTable = table;
				break;
			}
		}
		if (targetTable == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Table " + tableName + " does not exist.");
			return;
		}
		String formula;
		String targetAttributeString = line.substring(line.indexOf("set") + 4).trim().split(" ")[0];
		Attribute targetAttribute = null;
		int targetCol = 0;
		for (Attribute attribute: targetTable.attributes) {
			if (attribute.name.equals(targetAttributeString)) {
				targetAttribute = attribute;
				break;
			}
			++targetCol;
		}
		if (targetAttribute == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + targetAttributeString + " does not exist.");
			return;
		}
		ArrayList<Integer> selectedRecords = new ArrayList<>();
		if (line.contains("where")) {
			formula = line.substring(line.indexOf("=") + 2, line.indexOf("where")).trim();
			selectedRecords = targetTable.where(
					line.substring(line.indexOf("where") + 6, line.length() - 1)
			);
		}
		else {
			formula = line.substring(line.indexOf("=") + 2).trim();
			for (int rowId = 0; rowId < targetTable.array.size(); ++rowId) {
				selectedRecords.add(rowId);
			}
		}
		for (Integer selectedRecord: selectedRecords) {
			for (UniqueValueSet uniqueValueSet: targetTable.uniqueValueSets) {
				uniqueValueSet.deleteValue(
						targetTable.array.get(selectedRecord)
				);
			}
		}
		if (words.get(5).equals("replace")) {
			String[] replaceInfo = line.substring(line.indexOf("replace") + 10, line.length() - 4).split(",");
			String srcAttributeString = replaceInfo[0].trim();
			int srcCol = 0;
			for (Attribute attribute: targetTable.attributes) {
				if (attribute.name.equals(srcAttributeString)) {
					break;
				}
				++srcCol;
			}
			String srcString = replaceInfo[1].trim();
			srcString = srcString.substring(1, srcString.length() - 1);
			String dstString = replaceInfo[2].trim();
			dstString = dstString.substring(1, dstString.length() - 1);
			for (Integer selectedId: selectedRecords) {
				if (targetTable.attributes.get(targetCol).verify(targetTable.array.get(selectedId).get(targetCol)) != 0) {
					System.out.println("\u001B[31m[!]\u001B[0mSkip: Invalid value for attribute " + targetTable.attributes.get(targetCol).name + ".");
				}
				else {
					targetTable.array.get(selectedId).set(targetCol,
							targetTable.array.get(selectedId).get(srcCol).replace(srcString, dstString)
					);
				}
			}
		}
		else if (words.get(5).equals("upper") || words.get(5).equals("lower")) {
			int parenCnt = 0;
			String srcAttributeString = null;
			for (int pos = line.indexOf("er") + 3; pos < line.length(); ++pos) {
				if (line.charAt(pos) == '(') {
					++parenCnt;
				}
				else if (line.charAt(pos) == ')') {
					--parenCnt;
				}
				if (parenCnt == 0) {
					srcAttributeString = line.substring(line.indexOf("er") + 4, pos - 1).trim();
					break;
				}
			}
			int srcCol = 0;
			for (Attribute attribute: targetTable.attributes) {
				if (attribute.name.equals(srcAttributeString)) {
					break;
				}
				++srcCol;
			}
			if (words.get(5).charAt(0) == 'u') {
				for (int selectedId: selectedRecords) {
					targetTable.array.get(selectedId).set(targetCol,
							targetTable.array.get(selectedId).get(srcCol).toUpperCase()
					);
				}
			}
			else {
				for (int selectedId: selectedRecords) {
					targetTable.array.get(selectedId).set(targetCol,
							targetTable.array.get(selectedId).get(srcCol).toLowerCase()
					);
				}
			}
		}
		else {
			ArrayList<Object> PN = targetTable.getPN(formula);
			for (Integer selectedId: selectedRecords) {
				if (!targetTable.attributes.get(targetCol).type.equals("String")) {
					targetTable.array.get(selectedId).set(targetCol,
							((Double) targetTable.calcArithmetic(PN, targetTable.array.get(selectedId))).toString());
				}
				else {
					System.out.println("\u001B[31m[!]\u001B[0mSkip: Invalid value for attribute " + targetTable.attributes.get(targetCol).name + ".");
				}
			}
		}
		for (Integer selectedId: selectedRecords) {
			for (UniqueValueSet uniqueValueSet: targetTable.uniqueValueSets) {
				if (!uniqueValueSet.checkAndAdd(targetTable.array.get(selectedId))) {
					System.out.println(selectedId);
					System.out.println("\u001B[31m[!]Error:\u001B[0m Unique value constraint violated during update.");
					return;
				}
			}
		}
		for (Table.Tuple<String, String> constraint: targetTable.constraints) {
			if (targetTable.where(constraint.second).size() != targetTable.array.size()) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Constraint " + constraint.first + " does not hold.");
				return;
			}
		}
	}

	public static void commandDelete(ArrayList<String> words, String line) {
		String tableName = words.get(2);
		Table targetTable = null;
		for (Table table: tables) {
			if (table.name.equals(tableName)) {
				targetTable = table;
			}
		}
		if (targetTable == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Table not specified.");
			return;
		}
		ArrayList<Integer> selectedRecords = targetTable.where(
				line.substring(line.indexOf(words.get(4)), line.length() - 1)
		);
		System.out.println("[.]" + selectedRecords.size() + " record(s) deleted.");
		for (int i = 0; i < selectedRecords.size(); ++i) {
			for (UniqueValueSet uniqueValueSet: targetTable.uniqueValueSets) {
				uniqueValueSet.deleteValue(
						targetTable.array.get(selectedRecords.get(i) - i)
				);
			}
			targetTable.array.remove(selectedRecords.get(i) - i);
		}
	}

	public static void commandComment(ArrayList<String> words, String line) {
		Table targetTable = null;
		if (words.get(2).equals("column")) {
			String tableName = words.get(3).split("\\.")[0];
			for (Table table: tables) {
				if (table.name.equalsIgnoreCase(tableName)) {
					targetTable = table;
					break;
				}
			}
			if (targetTable == null) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Table " + tableName + " does not exist.");
				return;
			}
			String attributeName = words.get(3).split("\\.")[1];
			Attribute targetAttribute = null;
			for (Attribute attribute: targetTable.attributes) {
				if (attribute.name.equalsIgnoreCase(attributeName)) {
					targetAttribute = attribute;
					break;
				}
			}
			if (targetAttribute == null) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + attributeName + " does not exist.");
				return;
			}
			targetAttribute.comments.add(line.substring(line.indexOf(words.get(4)), line.length() - 2).trim());
		}
		else if (words.get(2).equals("table")) {
			String tableName = words.get(3);
			for (Table table: tables) {
				if (table.name.equals(tableName)) {
					targetTable = table;
					break;
				}
			}
			if (targetTable == null) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Table " + tableName + " does not exist.");
				return;
			}
			targetTable.comments.add(line.substring(line.indexOf(words.get(4)), line.length() - 2).trim());
		}
		else {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Unsupported comment type: " + words.get(2));
		}
	}

	public static void genReport(ArrayList<Instant> timestamps, File sql) {
		StringBuilder data = new StringBuilder();
		data.append("Event,Time (UTC),Duration from start (ms),Duration from last (ms)\n");
		data.append(String.format("0, %s, --, --\n", timestamps.get(0).toString()));
		for (int i = 1; i < timestamps.size(); ++i) {
			data.append(String.format("%d, %s, %d, %d\n", i, timestamps.get(i).toString(),
					Duration.between(timestamps.get(0), timestamps.get(i)).toMillis(),
					Duration.between(timestamps.get(i - 1), timestamps.get(i)).toMillis()));
		}
		try {
			String fileName = "./reports/" + sql.toString().substring(10, sql.toString().length() - 4) + ".csv";
			FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(data.toString());
			bufferedWriter.close();
			try {
				Desktop.getDesktop().open(new File(fileName));
			}
			catch (Exception exception) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m " + exception.getMessage());
			}
		}
		catch (Exception exception) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m " + exception.getMessage());
		}
	}
}
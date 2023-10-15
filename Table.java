import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Table {
	String database_name;
	String schema_name;
	String name;
	String owner = "postgres";
	ArrayList<Attribute> attributes;
	ArrayList<ArrayList<String> > array;
	ArrayList<UniqueValueSet> uniqueValueSets;
	ArrayList<String> comments;
	ArrayList<Tuple<String, String> > constraints;

	static class Tuple<Type1, Type2> {
		public Type1 first; // name
		public Type2 second; // content

		public Tuple(Type1 first, Type2 second) {
			this.first = first;
			this.second = second;
		}
	}

	public Table(String database_name, String schema_name, String name) {
		this.database_name = database_name;
		this.schema_name = schema_name;
		this.name = name;
		attributes = new ArrayList<>();
		array = new ArrayList<>();
		uniqueValueSets = new ArrayList<>();
		comments = new ArrayList<>();
		constraints = new ArrayList<>();
	}

	public void addAttribute(String info) {
		ArrayList<String> infos = Utils.divString(info);
		String name = infos.get(0);
		if (name.equals("unique") || name.equals("primary")) {
			String uniqueAttributeString = info.substring(
					info.indexOf('(') + 2, info.indexOf(')') - 1
			);
			String[] uniqueAttributes = uniqueAttributeString.split(",");
			ArrayList<Integer> uniqueAttributeIds = new ArrayList<>();
			for (String attributeName: uniqueAttributes) {
				attributeName = attributeName.trim();
				boolean found = false;
				int colId = 0;
				for (Attribute attribute: attributes) {
					if (attribute.name.equals(attributeName)) {
						found = true;
						uniqueAttributeIds.add(colId);
						break;
					}
					++colId;
				}
				if (!found) {
					System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + attributeName + " does not exist.");
					return;
				}
			}
			uniqueValueSets.add(new UniqueValueSet(uniqueAttributeIds));
			return;
		}
		if (name.equals("check")) {
			constraints.add(new Tuple<>(infos.get(1), info.substring(info.indexOf('(') + 2, info.indexOf(')') - 1)));
			return;
		}
		for (Attribute prev_attribute: attributes) {
			if (prev_attribute.name.equals(name)) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + name + " already exists.");
				return;
			}
		}
		String type = null;
		int maxLen = 0x3f3f3f3f;
		boolean unique = false;
		boolean nullable = true;
		boolean primaryKey = false;
		String default_val = null;
		for (int pos = 1; pos < infos.size(); ++pos) {
			String current = infos.get(pos);
			if (current.equals("int") || current.equals("integer")
				|| current.equals("smallint") || current.equals("bigint")
				|| current.equals("smallserial") || current.equals("serial")
				|| current.equals("bigserial")) {
				type = "int";
			}
			else if (current.equals("float4") || current.equals("float8")
				|| current.equals("decimal") || current.equals("numeric")
				|| current.equals("real") || current.equals("double")) {
				type = "double";
				if (infos.get(pos + 1).equals("precision")) {
					++pos;
				}
			}
			else if (current.equals("varchar") || current.startsWith("char")
				|| current.equals("text")) {
				type = "String";
				if (infos.get(pos + 1).equals("(") && infos.get(pos + 3).equals(")")) {
					maxLen = Integer.parseInt(infos.get(pos + 2));
					pos += 3;
				}
			}
			else if (current.equals("unique")) {
				unique = true;
			}
			else if (current.equals("not") && infos.get(pos + 1).equals("null")) {
				nullable = false;
				++pos;
			}
			else if (current.equals("null")) {
				nullable = true;
			}
			else if (current.equals("primary") && infos.get(pos + 1).equals("key")) {
				primaryKey = true;
				unique = true;
				nullable = false;
				++pos;
			}
			else if (current.equals("default")) {
				default_val = infos.get(pos + 1);
				if (default_val.startsWith("\"") && default_val.endsWith("\"")) {
					default_val = default_val.substring(1, default_val.length() - 1);
				}
				++pos;
			}
			else if (current.equals("check")) {
				int leftParen = info.indexOf('(', info.indexOf(infos.get(pos + 1)));
				int rightParen = leftParen, parenCnt = 0;
				for (; ; ++rightParen) {
					if (info.charAt(rightParen) == '(') {
						++parenCnt;
					}
					else if (info.charAt(rightParen) == ')') {
						--parenCnt;
					}
					if (parenCnt == 0) {
						break;
					}
				}
				constraints.add(new Tuple<>(infos.get(pos + 1),
						info.substring( leftParen + 2, rightParen - 1)
				));
				pos = rightParen;
			}
			if (unique) {
				UniqueValueSet uniqueValueSet = new UniqueValueSet();
				uniqueValueSet.uniqueColumns.add(attributes.size());
				uniqueValueSets.add(uniqueValueSet);
			}
		}
		Attribute newAttribute = new Attribute(name, type, maxLen, unique, nullable, primaryKey, default_val);
		if (newAttribute.verify(default_val) > 0) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Default value " + default_val + " does not match attribute " + name + ".");
			return;
		}
		attributes.add(newAttribute);
		for (ArrayList<String> record: array) {
			record.add(default_val);
		}
	}

	public void addRecord(ArrayList<String> values) {
		ArrayList<Integer> attribute_ids = new ArrayList<>();
		for (Attribute attribute: attributes) {
			attribute_ids.add(attribute.id);
		}
		addRecord0(attribute_ids, values);
	}

	public void addRecord(ArrayList<String> attribute_names, ArrayList<String> values) {
		if (attribute_names.size() != values.size()) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m The number of attributes and values does not match.");
			return;
		}
		ArrayList<Integer> attribute_ids = new ArrayList<>();
		for (String name: attribute_names) {
			boolean found = false;
			for (Attribute attribute: attributes) {
				if (attribute.name.equals(name)) {
					attribute_ids.add(attribute.id);
					found = true;
					break;
				}
			}
			if (!found) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + name + " does not exist.");
				return;
			}
		}
		System.out.println(attribute_ids);
		addRecord0(attribute_ids, values);
	}

	public void addRecord0(ArrayList<Integer> attribute_ids, ArrayList<String> values) {
		ArrayList<String> record = new ArrayList<>(attributes.size());
		for (Attribute attribute: attributes) {
			record.add(attribute.default_val);
		}
		for (int pos = 0; pos < attribute_ids.size(); ++pos) {
			if (Attribute.allAttributes.get(attribute_ids.get(pos)).verify(values.get(pos)) != 0) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Value " + values.get(pos) + " does not match attribute " + Attribute.allAttributes.get(attribute_ids.get(pos)).name + ".");
				return;
			}
			record.set(attribute_ids.get(pos), values.get(pos));
		}
		for (UniqueValueSet uniqueValueSet: uniqueValueSets) {
			if (!uniqueValueSet.checkAndAdd(record)) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Unique value constraint violated during insertion.");
				return;
			}
		}
		Table tmpTable = new Table("tmp", "tmp", "tmp");
		tmpTable.attributes.addAll(attributes);
		tmpTable.array.add(record);
		for (Tuple<String, String> constraint: constraints) {
			if (tmpTable.where(constraint.second).isEmpty()) {
				System.out.println("\u001B[31m[!]Error:\u001B[0m Constraint " + constraint.first + " violated.");
				return;
			}
		}
		array.add(record);
	}

	public ArrayList<Integer> where(String conditions) {
		// 去除末尾分号
		if (conditions.endsWith(";")) {
			conditions = conditions.substring(0, conditions.length() - 1).trim();
		}
		// 如果头尾括号匹配，则一起删除
		Stack<Integer> stack = new Stack<>();
		boolean insideParen = false;
		if (conditions.contains("between")) {
			String varName = conditions.substring(0, conditions.indexOf("between") - 1);
			varName = varName.substring(varName.lastIndexOf(' ') + 1);
			int andPos = conditions.indexOf("and") + 2;  // pos of 'd'
			conditions = conditions.substring(0, andPos + 1)
					+ " " + varName + " <="
					+ conditions.substring(andPos + 1);
			conditions = conditions.replace("between", ">=");
		}
		for (int pos = 0; pos < conditions.length(); ++pos) {
			char ch = conditions.charAt(pos);
			if (ch == '(') {
				stack.push(pos);
			}
			if (pos == conditions.length() - 1 && ch == ')') {
				insideParen = true;
			}
			if (ch == ')') {
				stack.pop();
			}
		}
		if (insideParen) {
			return where(conditions.substring(2, conditions.length() - 2));
		}
		// 否则先看有没有括号外的or，有的话以括号外的or为分解拆分
		ArrayList<Integer> selectedRecords = null;
		int lastPos = 0;
		for (int pos = 0; pos < conditions.length(); ++pos) {
			char ch = conditions.charAt(pos);
			if (ch == '(') {
				stack.push(pos);
			}
			if (ch == ')') {
				stack.pop();
			}
			if (pos > 0 && conditions.startsWith(" or ", pos - 1) && stack.isEmpty()) {
				if (selectedRecords == null) {
					selectedRecords = where(conditions.substring(lastPos, pos - 1));
				}
				else {
					selectedRecords = Utils.mergeOr(selectedRecords, where(conditions.substring(lastPos, pos - 1)));
				}
				lastPos = pos + 3;
			}
		}
		if (selectedRecords != null) {
			selectedRecords = Utils.mergeOr(selectedRecords, where(conditions.substring(lastPos)));
			return selectedRecords;
		}
		// 再看有没有括号外的and，有的话继续拆分
		for (int pos = 0; pos < conditions.length(); ++pos) {
			char ch = conditions.charAt(pos);
			if (ch == '(') {
				stack.push(pos);
			}
			if (ch == ')') {
				stack.pop();
			}
			if (pos > 0 && conditions.startsWith(" and ", pos - 1) && stack.isEmpty()) {
				if (selectedRecords == null) {
					selectedRecords = where(conditions.substring(lastPos, pos - 1));
				}
				else {
					selectedRecords = Utils.mergeAnd(selectedRecords, where(conditions.substring(lastPos, pos - 1)));
				}
				lastPos = pos + 4;
			}
		}
		if (selectedRecords != null) {
			selectedRecords = Utils.mergeAnd(selectedRecords, where(conditions.substring(lastPos)));
			return selectedRecords;
		}
		// 如果这个也没有了，那说明已经变成原子了
		// 先处理带单词的情况：like, not like, between ... and ...
		String[] words = conditions.split(" ");
		String attributeName = words[0];
		Attribute attribute = null;
		selectedRecords = new ArrayList<>();
		for (Attribute tryAttribute: attributes) {
			if (tryAttribute.name.equals(attributeName)) {
				attribute = tryAttribute;
				break;
			}
		}
		if (attribute == null) {
			System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + attributeName + " does not exist.");
			return new ArrayList<>();
		}
		if (words[1].equals("like")) {
			if (words[2].charAt(0) == '"') {
				words[2] = words[2].substring(1);
			}
			if (words[2].charAt(words[2].length() - 1) == '"') {
				words[2] = words[2].substring(0, words[2].length() - 1);
			}
			String regex = words[2].replace("%", ".*");
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher;
			for (ArrayList<String> record: array) {
				matcher = pattern.matcher(record.get(attribute.id));
				if (matcher.matches()) {
					selectedRecords.add(array.indexOf(record));
				}
			}
			return selectedRecords;
		}
		if (words[1].equals("not") && words[2].equals("like")) {
			if (words[3].charAt(0) == '"') {
				words[3] = words[3].substring(1);
			}
			if (words[3].charAt(words[3].length() - 1) == '"') {
				words[3] = words[3].substring(0, words[3].length() - 1);
			}
			String regex = words[3].replace("%", ".*");
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher;
			for (ArrayList<String> record: array) {
				matcher = pattern.matcher(record.get(attribute.id));
				if (!matcher.matches()) {
					selectedRecords.add(array.indexOf(record));
				}
			}
			return selectedRecords;
		}
		if (words[1].equals("null")) {
			for (ArrayList<String> record: array) {
				if (record.get(attribute.id) == null || record.get(attribute.id).isEmpty()
					|| record.get(attribute.id).equalsIgnoreCase("null")) {
					selectedRecords.add(array.indexOf(record));
				}
			}
			return selectedRecords;
		}
		if (words[1].equals("not") && words[2].equals("null")) {
			for (ArrayList<String> record: array) {
				if (record.get(attribute.id) != null && !record.get(attribute.id).isEmpty()
					&& !record.get(attribute.id).equalsIgnoreCase("null")) {
					selectedRecords.add(array.indexOf(record));
				}
			}
			return selectedRecords;
		}
		if (conditions.contains(">") || conditions.contains("=") || conditions.contains("<")) {
			String operator = "";
			if (conditions.contains(">=")) {
				operator = ">=";
			}
			else if (conditions.contains("<=")) {
				operator = "<=";
			}
			else if (conditions.contains(">")) {
				operator = ">";
			}
			else if (conditions.contains("=")) {
				operator = "=";
			}
			else if (conditions.contains("<")) {
				operator = "<";
			}
			String leftValueString = conditions.substring(0, conditions.indexOf(operator) - 1).trim();
			String rightValueString = conditions.substring(conditions.indexOf(operator) + operator.length() + 1).trim();
			String leftValue, rightValue;
			if (operator.equals("=") && (leftValueString.startsWith("upper") || leftValueString.startsWith("lower")
					|| rightValueString.startsWith("upper") || rightValueString.startsWith("lower"))) {
				Attribute leftAttribute = null;
				Attribute rightAttribute = null;
				String leftAttributeName, rightAttributeName;
				if (leftValueString.startsWith("upper") || leftValueString.startsWith("lower")) {
					leftAttributeName = leftValueString.substring(8, leftValueString.length() - 2);
				}
				else {
					leftAttributeName = leftValueString;
				}
				for (Attribute tryAttribute: attributes) {
					if (tryAttribute.name.equals(leftAttributeName)) {
						leftAttribute = tryAttribute;
						break;
					}
				}
				if (rightValueString.startsWith("upper") || rightValueString.startsWith("lower")) {
					rightAttributeName = rightValueString.substring(8, rightValueString.length() - 2);
				}
				else {
					rightAttributeName = rightValueString;
				}
				for (Attribute tryAttribute: attributes) {
					if (tryAttribute.name.equals(rightAttributeName)) {
						rightAttribute = tryAttribute;
						break;
					}
				}
				if (leftAttribute == null || rightAttribute == null) {
					System.out.println("\u001B[31m[!]Error:\u001B[0m Attribute " + leftAttributeName + " or " + rightAttributeName + " does not exist.");
					return new ArrayList<>();
				}
				for (ArrayList<String> record: array) {
					if (leftValueString.startsWith("upper")) {
						leftValue = record.get(leftAttribute.id).toUpperCase();
					}
					else if (leftValueString.startsWith("lower")) {
						leftValue = record.get(leftAttribute.id).toLowerCase();
					}
					else {
						leftValue = record.get(leftAttribute.id);
					}
					if (rightValueString.startsWith("upper")) {
						rightValue = record.get(rightAttribute.id).toUpperCase();
					}
					else if (rightValueString.startsWith("lower")) {
						rightValue = record.get(rightAttribute.id).toLowerCase();
					}
					else {
						rightValue = record.get(rightAttribute.id);
					}
					if (leftValue.equals(rightValue) && !leftValue.equalsIgnoreCase("null")) {
						selectedRecords.add(array.indexOf(record));
					}
				}
				return selectedRecords;
			}
			ArrayList<Object> leftPN = getPN(leftValueString);
			ArrayList<Object> rightPN = getPN(rightValueString);
			for (ArrayList<String> record: array) {
				double leftResult = calcArithmetic(leftPN, record);
				double rightResult = calcArithmetic(rightPN, record);
				if (operator.equals(">=") && leftResult >= rightResult) {
					selectedRecords.add(array.indexOf(record));
				}
				else if (operator.equals("<=") && leftResult <= rightResult) {
					selectedRecords.add(array.indexOf(record));
				}
				else if (operator.equals(">") && leftResult > rightResult) {
					selectedRecords.add(array.indexOf(record));
				}
				else if (operator.equals("=") && leftResult == rightResult) {
					selectedRecords.add(array.indexOf(record));
				}
				else if (operator.equals("<") && leftResult < rightResult) {
					selectedRecords.add(array.indexOf(record));
				}
			}
			return selectedRecords;
		}
		return new ArrayList<>();
	}

	public ArrayList<Object> getPN(String formula) {
		formula = formula.trim();
		// 如果头尾括号匹配，则一起删除
		Stack<Integer> stack = new Stack<>();
		boolean insideParen = false;
		for (int pos = 0; pos < formula.length(); ++pos) {
			char ch = formula.charAt(pos);
			if (ch == '(') {
				stack.push(pos);
			}
			if (pos == formula.length() - 1 && ch == ')') {
				insideParen = true;
			}
			if (ch == ')') {
				stack.pop();
			}
		}
		if (insideParen) {
			return getPN(formula.substring(2, formula.length() - 2));
		}
		// 如果没有嵌套在最外层的括号，看看有没有乘和除，靠左边的优先级高
		// 每次迭代只会处理一个符号
		for (int pos = 0; pos < formula.length(); ++pos) {
			char ch = formula.charAt(pos);
			if (ch == '*' || ch == '/') {
				ArrayList<Object> leftPN = getPN(formula.substring(0, pos - 1));
				ArrayList<Object> rightPN = getPN(formula.substring(pos + 2));
				ArrayList<Object> newPN = new ArrayList<>();
				newPN.add(((Character) ch).toString());
				newPN.addAll(leftPN);
				newPN.addAll(rightPN);
				return newPN;
			}
		}
		// 如果没有乘除，再看看有没有加减
		for (int pos = 0; pos < formula.length(); ++pos) {
			char ch = formula.charAt(pos);
			if (ch == '+' || ch == '-') {
				ArrayList<Object> leftPN = getPN(formula.substring(0, pos - 1));
				ArrayList<Object> rightPN = getPN(formula.substring(pos + 2));
				ArrayList<Object> newPN = new ArrayList<>();
				newPN.add(((Character) ch).toString());
				newPN.addAll(leftPN);
				newPN.addAll(rightPN);
				return newPN;
			}
		}
		// 没有符号了，把字段的编号返回
		ArrayList<Object> newPN = new ArrayList<>();
		for (Attribute attribute: attributes) {
			if (attribute.name.equals(formula)) {
				newPN.add(attribute.id);
				return newPN;
			}
		}
		// 如果不是字段，那就是常数了
		newPN.add(Double.parseDouble(formula));
		return newPN;
	}

	public double calcArithmetic(ArrayList<Object> PN, ArrayList<String> record) {
		Stack<Object> buc = new Stack<>();
		for (Object element: PN) {
			if (element instanceof Integer || element instanceof Double) {
				double curValue;
				if (element instanceof Integer) {
					curValue = Double.parseDouble(record.get((int)element));
				}
				else {
					curValue = (double)element;
				}
				while (!buc.empty() && buc.peek() instanceof Double) {
					double lastValue = (double)buc.pop();
					String arithmeticOperator = (String)buc.pop();
					curValue = switch (arithmeticOperator.charAt(0)) {
						case '+' -> lastValue + curValue;
						case '-' -> lastValue - curValue;
						case '*' -> lastValue * curValue;
						default -> lastValue / curValue;
					};
				}
				buc.add(curValue);
			}
			else {
				buc.add(element);
			}
		}
		return (double)buc.pop();
	}
}

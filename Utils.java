import java.util.ArrayList;

public class Utils {
	public static String setCharAt(String str, int pos, char ch) {
		return str.substring(0, pos) + ch + str.substring(pos + 1);
	}

	public static String toLowerCase2(String str) {
		boolean inQuote = false;
		for (int i = 0; i < str.length(); ++i) {
			if (str.charAt(i) == '"' || str.charAt(i) == '\'') {
				inQuote = !inQuote;
			}
			else if (!inQuote) {
				char ch = str.charAt(i);
				if (ch >= 'A' && ch <= 'Z') {
					str = str.substring(0, i) + (char)(ch - 'A' + 'a') + str.substring(i + 1);
				}
			}
		}
		return str;
	}

	public static String lastCheck(String str) {
		if (str == null) {
			return "";
		}
		boolean inQuote = false;
		for (int i = 0; i < str.length(); ++i) {
			if (str.charAt(i) == '"' || str.charAt(i) == '\'') {
				if (i == 0 || str.charAt(i - 1) != '\\') {
					inQuote = !inQuote;
				}
			}
		}
		str = str.replace("\\'", "'");
		str = str.replace("\\\"", "\"");
		str = str.replace("\\\\", "\\");
		if (str.equals("null")) {
			return "";
		}
		return str;
	}

	public static String cleanString(String info) {
		info = info.trim().replace(" is ", " ").replace(" varying ", " ");
		info = info.replace(",'',", ",\"\",").replace(", '',", ", \"\",");
		info = info.replace(",'' ,", ",\"\" ,").replace(", '' ,", ", \"\" ,");
		//change all '' to \'
		for (int pos = 0; pos + 1 < info.length(); ++pos) {
			if (info.charAt(pos) == '\'' && info.charAt(pos + 1) == '\'') {
				info = setCharAt(info, pos, '\\');
				++pos;
			}
		}
		//change all '...' to "..."
		for (int pos = 0; pos < info.length(); ++pos) {
			if ((pos == 0 || info.charAt(pos - 1) != '\\') && info.charAt(pos) == '\'') {
				info = setCharAt(info, pos, '"');
			}
		}
		// separate operands with operators
		info = info.replace("<=", " <= ").replace(">=", " >= ");
		for (int i = 2; i < info.length() - 1; ++i) {
			if (info.charAt(i - 1) == '<' && info.charAt(i) != '=' && (info.charAt(i - 2) != ' ' || info.charAt(i) != ' ')) { // <
				info = info.substring(0, i - 1) + " < " + info.substring(i);
			}
			else if (info.charAt(i - 1) == '>' && info.charAt(i) != '=' && (info.charAt(i - 2) != ' ' || info.charAt(i) != ' ')) { // >
				info = info.substring(0, i - 1) + " > " + info.substring(i);
			}
			else if (info.charAt(i - 1) != '<' && info.charAt(i - 1) != '>' && info.charAt(i) == '='
					&& (info.charAt(i - 1) != ' ' || info.charAt(i + 1) != ' ')) { // =
				info = info.substring(0, i) + " = " + info.substring(i + 1);
			}
		}
		// separate words with brackets
		boolean inQuote = false;
		for (int i = 1; i < info.length(); ++i) {
			if (info.charAt(i) == '"') {
				inQuote = !inQuote;
			}
			if (inQuote) {
				continue;
			}
			if (info.charAt(i) == '(') {
				info = info.substring(0, i) + " ( " + info.substring(i + 1);
				i += 2;
			}
			else if (info.charAt(i) == ')') {
				info = info.substring(0, i) + " ) " + info.substring(i + 1);
				i += 2;
			}
			else if ((info.charAt(i) == '+' || info.charAt(i) == '-'
					|| info.charAt(i) == '*' || info.charAt(i) == '/')
					&& (info.charAt(i - 1) != ' ' || info.charAt(i + 1) != ' ')) {
				info = info.substring(0, i) + " " + info.charAt(i) + " " + info.substring(i + 1);
				i += 2;
			}
		}
		char lastChar = ',';
		for (int i = 0; i < info.length(); ++i) {
			if ((info.charAt(i) == '+' || info.charAt(i) == '-')
				&& (lastChar == ',' || lastChar == '(' || lastChar == '=')
				&& info.charAt(i + 2) >='0' && info.charAt(i + 2) <= '9') {
				info = info.substring(0, i + 1) + info.substring(i + 2);
			}
			if (info.charAt(i) != ' ') {
				lastChar = info.charAt(i);
			}
		}
		return info;
	}

	public static ArrayList<String> divString(String info) {
		info = cleanString(info);
		//split the string into words
		ArrayList<String> words = new ArrayList<>();
		String[] infos = info.split(" ");
		String prefix = "";
		for (String word: infos) {
			if (word.isEmpty()) {
				continue;
			}
			if (word.startsWith("--")) {
				break;
			}
			// ["...] ... ..."
			if (word.startsWith("\"") && !word.endsWith("\"") && !word.endsWith(";")) {
				prefix = word.substring(1) + " ";
				continue;
			}
			// "... [...] ..."
			if (!prefix.isEmpty() && !word.endsWith("\"") && !word.endsWith(";")) {
				prefix = prefix.concat(word) + " ";
				continue;
			}
			// ["..."]
			if (word.startsWith("\"") && word.endsWith("\"")) {
				prefix = word.substring(1, word.length() - 1);
			}
			// ["...";]
			else if (word.startsWith("\"") && word.endsWith("\";")) {
				prefix = word.substring(1, word.length() - 2);
			}
			// "... ... [..."]
			else if (word.endsWith("\"")) {
				prefix = prefix.concat(word.substring(0, word.length() - 1));
			}
			else if (word.endsWith("\";")) {
				prefix = prefix.concat(word.substring(0, word.length() - 2));
			}
			else {
				prefix = word;
			}
			words.add(prefix);
			prefix = "";
		}
		return words;
	}

	public static ArrayList<Integer> mergeOr(ArrayList<Integer> array1, ArrayList<Integer> array2) {
		int size1 = array1.size(), size2 = array2.size();
		int pnt1 = 0, pnt2 = 0;
		ArrayList<Integer> result = new ArrayList<>();
		while (pnt1 < size1 || pnt2 < size2) {
			if (pnt1 >= size1) {
				result.add(array2.get(pnt2));
				++pnt2;
				continue;
			}
			if (pnt2 >= size2) {
				result.add(array1.get(pnt1));
				++pnt1;
				continue;
			}
			int val1 = array1.get(pnt1), val2 = array2.get(pnt2);
			if (val1 == val2) {
				result.add(val1);
				++pnt1;
				++pnt2;
			}
			else if (val1 < val2) {
				result.add(val1);
				++pnt1;
			}
			else {
				result.add(val2);
				++pnt2;
			}
		}
		return result;
	}

	public static ArrayList<Integer> mergeAnd(ArrayList<Integer> array1, ArrayList<Integer> array2) {
		int size1 = array1.size(), size2 = array2.size();
		int pnt1 = 0, pnt2 = 0;
		ArrayList<Integer> result = new ArrayList<>();
		while (pnt1 < size1 || pnt2 < size2) {
			if (pnt1 >= size1) {
				++pnt2;
				continue;
			}
			else if (pnt2 >= size2) {
				++pnt1;
				continue;
			}
			int val1 = array1.get(pnt1), val2 = array2.get(pnt2);
			if (val1 == val2) {
				result.add(array1.get(pnt1));
				++pnt1;
				++pnt2;
			}
			else if (val1 < val2) {
				++pnt1;
			}
			else {
				++pnt2;
			}
		}
		return result;
	}
}

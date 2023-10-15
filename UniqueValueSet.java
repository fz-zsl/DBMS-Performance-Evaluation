import java.util.ArrayList;
import java.util.HashSet;

public class UniqueValueSet {
	public ArrayList<Integer> uniqueColumns;
	public HashSet<String> uniqueValues;

	public UniqueValueSet() {
		this.uniqueColumns = new ArrayList<>();
		this.uniqueValues = new HashSet<>();
	}

	public UniqueValueSet(ArrayList<Integer> uniqueColumns) {
		this.uniqueColumns = uniqueColumns;
		this.uniqueValues = new HashSet<>();
	}

	public boolean checkAndAdd(ArrayList<String> record) {
		String info = "";
		for (int id: uniqueColumns) {
			if (record.get(id) == null) {
				info = info.concat("null@#^**");
			}
			else {
				info = info.concat(record.get(id) + "@#^**");
			}
		}
		if (uniqueValues.contains(info)) {
			return false;
		}
		uniqueValues.add(info);
		return true;
	}

	public void deleteValue(ArrayList<String> record) {
		String info = "";
		for (int id: uniqueColumns) {
			if (record.get(id) == null) {
				info = info.concat("null@#^**");
			}
			else {
				info = info.concat(record.get(id) + "@#^**");
			}
		}
		uniqueValues.remove(info);
	}
}

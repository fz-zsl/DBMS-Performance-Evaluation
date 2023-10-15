import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
	public static ArrayList<ArrayList<String> > table = new ArrayList<>();

	public static void main(String[] args) throws Exception {
		Instant start = Instant.now();
		BufferedReader br = new BufferedReader(new FileReader("./data/people.csv"));
		while (true) {
			String line = br.readLine();
			if (line == null) break;
			ArrayList<String> record = new ArrayList<>(List.of(line.split(",")));
			if (record.size() < 6) {
				record.set(5, record.get(4));
				record.set(4, "");
			}
			table.add(record);
		}
		br.close();
		Instant end = Instant.now();
		System.out.print((end.toEpochMilli() - start.toEpochMilli()) + ",");
		start = end;
		ArrayList<Integer> selectResult = new ArrayList<>();
		BufferedWriter bw = new BufferedWriter(new FileWriter("./data/select_result.csv"));
		String output = "select  *  from people where first_name like \"%ly%\" or surname like \"%ly%\";\n";
		output = output.concat("id,first_name,surname,born,died,gender,\n");
		for (ArrayList<String> record: table) {
			if (record.get(1).contains("ly") || record.get(2).contains("ly")) {
				output = output.concat(String.join(",", record));
				output = output.concat("\n");
				selectResult.add(Integer.parseInt(record.get(0)));
			}
		}
		end = Instant.now();
		System.out.print((end.toEpochMilli() - start.toEpochMilli()) + ",");
		start = end;
		bw.write(output);
		bw.close();
		for (ArrayList<String> record: table) {
			record.add("");
		}
		end = Instant.now();
		System.out.print((end.toEpochMilli() - start.toEpochMilli()) + ",");
		start = end;
		for (ArrayList<String> record: table) {
			record.set(1, record.get(1).replace("To", "TTOO"));
			record.set(2, record.get(2).replace("To", "TTOO"));
			if (record.get(4).isEmpty() || record.get(4).equalsIgnoreCase("NULL")) {
				record.set(6, 2023 - Integer.parseInt(record.get(3)) + "");
			}
			else {
				record.set(6, Integer.parseInt(record.get(4)) - Integer.parseInt(record.get(3)) + "");
			}
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter("./data/new_table.csv"));
		String output = "id,first_name,surname,born,died,gender,\n";
		for (ArrayList<String> record: table) {
			output = output.concat(String.join(",", record));
			output = output.concat("\n");
		}
		end = Instant.now();
		System.out.println((end.toEpochMilli() - start.toEpochMilli()));
		bw.write(output);
		bw.close();
	}
}
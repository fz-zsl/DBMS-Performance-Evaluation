import java.util.ArrayList;

public class Attribute {
	public static ArrayList<Attribute> allAttributes = new ArrayList<>();
    public int id;
    public String name;
    public String type;
    public int maxLen;
    public boolean unique;
    public boolean nullable;
    public boolean primaryKey;
	public String default_val;
    public ArrayList<String> comments;

    public Attribute(String name, String type, int maxLen, boolean unique, boolean nullable, boolean primaryKey, String default_val) {
		this.id = allAttributes.size();
        this.name = name;
        this.type = type;
        this.maxLen = maxLen;
        this.unique = unique;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
		this.default_val = default_val;
        this.comments = new ArrayList<>();
		allAttributes.add(this);
    }

	@Override
	public String toString() {
		return "Attribute(name = " + this.name + ", type = " + this.type + ") ";
	}

    public int verify(String value) {
		if (value == null || value.isEmpty()) {
			return 0;
		}
        if (value.equalsIgnoreCase("null")) {
            if (!nullable) {
				return 1;
            }
			return 0;
        }
        if (this.type.equals("String")) {
            if (value.length() > maxLen) {
                return 2;
            }
            return 0;
        }
        if (this.type.equals("int")) {
            try {
                Integer.parseInt(value);
            }
            catch (Exception exception) {
                return 3;
            }
            return 0;
        }
        if (this.type.equals("float") || this.type.equals("double")) {
            try {
                Double.parseDouble(value);
            }
            catch (Exception exception) {
                return 3;
            }
            return 0;
        }
        return 0;
    }
}

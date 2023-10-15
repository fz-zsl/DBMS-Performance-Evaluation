#include<iostream>
#include<cstdio>
#include<time.h>
#include<vector>
using namespace std;

vector<vector<string> > table;
vector<int> selectResult;

inline bool check(string word, string token) {
	int len = word.size();
	for (int i = 1; i < len; ++i) {
		if (word[i - 1] == 'l' && word[i] == 'y') {
			return true;
		}
	}
	return false;
}

inline int toNumber(string str) {
	int number = 0;
	for (int i = str.size() - 1; ~i; --i) {
		number = number * 10 + str[i] - '0';
	}
	return number;
}

inline string toString(int number) {
	string str = "";
	while (number > 0) {
		str = str + (char)(number % 10 + '0');
		number /= 10;
	}
	return str;
}

inline string update(string src, string old_str, string new_str) {
	int src_len = src.size();
	int old_len = old_str.size();
	for (int i = 0; i + old_len - 1 < src_len; ++i) {
		if (src.substr(i, old_len)==old_str) {
			src = src.substr(0, i) + new_str
				+ src.substr(i + old_len, src_len - i - old_len);
		}
	}
	return src;
}

int main() {
	ios::sync_with_stdio(false);
	cin.tie(0);
	cout.tie(0);
	int rowCnt = 0;
	int colCnt = 0;
	string line;
	freopen("people.csv", "r", stdin);
	clock_t start = clock();
	for (;; ++rowCnt) {
		getline(cin, line);
		if (line.empty()) {
			break;
		}
		colCnt = 0;
		int lastPos = 0;
		int len = line.size();
		vector<string> tmp;
		table.push_back(tmp);
		for (int pos = 0; pos < len; ++pos) {
			if (line[pos] == ',') {
				if (lastPos == pos) {
					table[rowCnt].push_back("");
				}
				else {
					table[rowCnt].push_back(line.substr(lastPos, pos - lastPos));
				}
				++colCnt;
				lastPos = pos + 1;
			}
		}
		table[rowCnt].push_back(line.substr(lastPos, len - lastPos));
	}
	++colCnt;
	clock_t end = clock();
	fprintf(stderr, "%.0f,", ((double) (end - start)) / CLOCKS_PER_SEC * 1000);
	start = end;
	puts("select  *  from people where first_name like \"%ly%\" or surname like \"%ly%\";");
	puts("id,first_name,surname,born,died,gender,");
	for (int i = 0; i < rowCnt; ++i) {
		if (check(table[i][1], "ly") || check(table[i][2], "ly")) {
			selectResult.push_back(i);
		}
	}
	end = clock();
	fprintf(stderr, "%.0f,", ((double) (end - start)) / CLOCKS_PER_SEC * 1000);
	start = end;
	for (int i = 0; i < rowCnt; ++i) {
		table[i].push_back("");
	}
	end = clock();
	fprintf(stderr, "%.0f,", ((double) (end - start)) / CLOCKS_PER_SEC * 1000);
	start = end;
	for (int i = 0; i < rowCnt; ++i) {
		table[i][1] = update(table[i][1], "To", "TTOO");
		table[i][2] = update(table[i][2], "To", "TTOO");
		if (table[i][4].empty()) {
			table[i][6] = toString(2023 - toNumber(table[i][3]));
		}
		else {
			table[i][6] = toString(toNumber(table[i][4]) - toNumber(table[i][3]));
		}
	}
	end = clock();
	fprintf(stderr, "%.0f", ((double) (end - start)) / CLOCKS_PER_SEC * 1000);
	freopen("new_table.csv", "w", stdout);
	puts("id,first_name,surname,born,died,gender,");
	for (int i = 0; i < rowCnt; ++i) {
		for (int j = 0; j < 7; ++j) {
			printf("%s,", table[i][j].c_str());
		}
		puts("");
	}
	fclose(stdin);
	fclose(stdout);
	return 0;
}

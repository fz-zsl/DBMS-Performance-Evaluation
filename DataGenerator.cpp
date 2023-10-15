#include<bits/stdc++.h>
using namespace std;

inline string genStr() {
	int len = rand() % 10 + 5;
	char str[len+1] = {};
	str[0] = rand() % 26 + 'A';
	for (int i = 1; i < len; ++i) {
		str[i] = rand() % 26 + 'a';
	}
	return (string)(str);
}

inline int genYear(int lowerbound) {
	return rand() % 100 + lowerbound;
}

int main() {
	freopen("insert.sql", "w", stdout);
	srand(time(NULL));
	for (int id = 1; id < 2000001; ++id) {
		string str1 = genStr();
		string str2 = genStr();
		int born = genYear(1850);
		printf("INSERT INTO people VALUES(%d,'%s','%s',%d,", id, str1.c_str(), str2.c_str(), born);
		int alive = rand() % 2;
		if (alive && born > 1930) {
			printf("null,");
		}
		else {
			printf("%d,", genYear(born));
		}
		int male = rand() % 2;
		if (male) {
			puts("'M');");
		}
		else {
			puts("'F');");
		}
	}
	return 0;
}

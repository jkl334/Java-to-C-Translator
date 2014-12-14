
#include "test_input/Test003.hpp"
using namespace std;

string fld;
string A::getFld() {
	return fld;
}

string fld;
int main(int argc, const char* argv[]) {
	A a = (A) {.fld = "A"};
	cout << a.getFld() << "\n";
}


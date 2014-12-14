
#include "test_input/Test006.hpp"
using namespace std;

string fld;
void A::setFld(string f) {
	fld = f;
}

void A::almostSetFld(string f) {
	string fld;
	fld = f;
}

string A::getFld() {
	return fld;
}

string fld;
int main(int argc, const char* argv[]) {
	A a = (A) { };
		a.almostSetFld("B");
	cout << a.getFld() << "\n";
		a.setFld("B");
	cout << a.getFld() << "\n";
}


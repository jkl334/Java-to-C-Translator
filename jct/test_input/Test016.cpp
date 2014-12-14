
#include "Test016.hpp"
using namespace std;

void A::printOther( other) {
	cout << std::to_string(other) << "\n";
}

B some;
void B::printOther( other) {
	cout << std::to_string(other) << "\n";
}

string B::toString() {
	return some;
}

B some;
int main(int argc, const char* argv[]) {
	A a = (A) { };
	B other = (B) { };
	othera;
		a.printOther(other);
}


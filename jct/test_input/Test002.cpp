
#include "test_input/Test002.hpp"
using namespace std;

string A::toString() {
	return "A";
}

int main(int argc, const char* argv[]) {
	A a = (A) { };
	Object o = a;
	cout << std::to_string(o) << "\n";
}


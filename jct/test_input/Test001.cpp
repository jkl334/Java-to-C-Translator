
#include "test_input/Test001.hpp"
using namespace std;

string A::toString() {
	return "A";
}

int main(int argc, const char* argv[]) {
	A a = (A) { };
	cout << std::to_string(a) << "\n";
}


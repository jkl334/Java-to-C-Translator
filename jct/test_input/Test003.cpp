/**
 * Team: SegFault
 */

#include <sstream> 
#include <iostream>
#include <string>

#include "test_input/Test003.hpp"

using namespace std;

string fld;
string A::getFld() {
	return fld;
}

void Test003::main(string args) {
	A a = (A) {.fld = "A"};
	cout << a.getFld() << endl;
}


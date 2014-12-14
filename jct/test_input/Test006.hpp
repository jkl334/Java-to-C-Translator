#include <sstream>
#include <iostream>
#include <string>
using namespace std;
struct A
{
	static void setFld(string f);
	static void almostSetFld(string f);
	static string getFld();
};
struct A_VT
{
	void(*setFld)(string);
	void(*almostSetFld)(string);
	string(*getFld)();
	A_VT():
		setFld(&A::setFld),
		almostSetFld(&A::almostSetFld),
		getFld(&A::getFld){}
};
struct Test006
{
};

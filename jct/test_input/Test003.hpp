#include <sstream>
#include <iostream>
#include <string>
using namespace std;
struct A
{
	static string getFld();
};
struct A_VT
{
	string(*getFld)(
	A_VT():
		getFld(&A::getFld){}
};
struct Test003
{
};

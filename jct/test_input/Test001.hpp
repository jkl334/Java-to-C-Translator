#include java_lang.h
#include <sstream>
#include <iostream>
#include <string>
using namespace std;
struct A
{
	static string toString();
};
struct A_VT
{
	string(*toString)(
	A_VT():
		toString(&A::toString){}
};
struct Test001
{
};

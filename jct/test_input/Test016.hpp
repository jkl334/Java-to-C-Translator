#include <sstream>
#include <iostream>
#include <string>
using namespace std;
struct A
{
	static void printOther( other);
};
struct A_VT
{
	void(*printOther)();
	A_VT():
		printOther(&A::printOther){}
};
struct B
{
	static void printOther( other);
	static string toString();
};
struct B_VT
{
	void(*printOther)();
	string(*toString)();
	B_VT():
		printOther(&B::printOther),
		toString(&B::toString){}
};
struct Test016
{
};

using namespace std;
#include <string>

struct A {
	A(string f)
	{
		fld=f;
	}
	public:
		string getFld();

	private:
		string fld;
};

struct Test003 {
};


class A {
	public int getInt() { return 1; }
}

class B extends A {
	public String getString() { return "sting beans";  }
}

class C extends B {
	public boolean getFacts() { return true; }
}


public class IClass {
	public static void main(String[] args) {
		C c = new C();
		c.toString();
	}
}

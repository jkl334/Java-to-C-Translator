import java.util.ArrayList;


class A {
  public void toString() {
    return "A";
  }
}
class B extends A{

}
public class Test001 { 
  public static void main(String[] args) {
    A a = new A();
    System.out.println(a.toString());
  }
}

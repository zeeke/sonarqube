package lcom4;

public class Simple {

  private String foo;
  private int bar;

  public Simple(String foo, int bar) {
    this.foo = foo;
    this.bar = bar;
  }

  public void printFoo() {
    System.out.println(foo);
  }

  public void printBar() {
    System.out.println(bar);
  }

  public void screamBar() {
    System.out.println(bar + "!!!!!!!!!!!");
  }

  public void hello() {
    System.out.println("hello");
    helloDependant();
  }
  
  public void helloDependant() {}
  
  public void increaseLcom4To2() {
    System.out.println("Hey guess what? LCOM4 is now to 2!");
    increaseLcom4To2Dependant();
  }
  
  public void increaseLcom4To2Dependant() {}
  
  public void increaseLcom4To3() {
    System.out.println("Hey guess what? LCOM4 is now to 3, and this is *really* bad!");
    increaseLcom4To3Dependant();
  }
  
  public void increaseLcom4To3Dependant() {}
  
}
package lcom4;

public class WithInnerClass {

  private static class InnerClass {
    private String foo;
    private int bar;

    public InnerClass(String foo, int bar) {
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
    }
  }
}
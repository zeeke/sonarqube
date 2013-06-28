package lcom4;

public class Accessors {

  private String foo;
  private String bar;

  //
  // accesors
  //

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }

  public String getBar() {
    return bar;
  }

  public void setBar(String bar) {
    this.bar = bar;
  }


  //
  // methods
  //
  public void sayFoo() {
    System.out.println(getFoo());
  }
}
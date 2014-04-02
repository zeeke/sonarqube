package sample;

public class Sample {
  private Integer i;

  public Sample(Integer i) {
    this.i = i;
  }

  private String myMethod() {
    return "hello";
  }

  public Integer getI() {
    Integer j = i;
    return j;
  }

  public String toString() {
    return i.toString();
  }
}

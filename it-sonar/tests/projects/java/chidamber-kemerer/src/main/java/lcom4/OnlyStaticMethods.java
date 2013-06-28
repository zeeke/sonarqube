package lcom4;

public final class OnlyStaticMethods {
  public static final double PI = 3.14;

  private OnlyStaticMethods() {
    // private because only static methods
  }

  public static double getPI() {
    return PI;
  }

  public static int sum(int a, int b) {
    return a + b;
  }
}
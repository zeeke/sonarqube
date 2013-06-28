package foo;

public class EncodedViolationMessage {

  // unused private method
  private void bar() {
    buz("<select>"); // html characters must be replaced in UI
    buz("<select>");
    buz("<select>");
    buz("<select>");
    buz("<select>");
    buz("<select>");
    buz("<select>");
    buz("<select>");
    buz("<select>");
  }

}

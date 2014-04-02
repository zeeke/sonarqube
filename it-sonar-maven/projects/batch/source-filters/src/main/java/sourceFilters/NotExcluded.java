package sourceFilters;

public class NotExcluded {

  private String field = null;

  public NotExcluded(String s) {
    this.field = s;
  }

  public String getField() {
    return field;
  }

  public void setField(String s) {
    this.field = s;
  }

  public void sayHello() {
    for (int i = 0; i < 5; i++) {
      if (field != null) {
        System.out.println(field);
      }
    }
  }
}

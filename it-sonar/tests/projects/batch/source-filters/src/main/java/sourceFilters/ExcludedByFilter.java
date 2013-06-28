package sourceFilters;

// this class is excluded by the resource filter defined in resource-filter-plugin
public class ExcludedByFilter {

  public void say() {
    int i=0;
    if(i>5) {
      System.out.println("say something");
    }
  }

  public void cry() {
    int i=0;
    if(i<5) {
      System.out.println("cry");
    }
  }
}

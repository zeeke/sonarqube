package example;

public class Greeting
{
    public void coveredByUnitTest()
    {
       boolean unitTest=true;
       if (unitTest)
           System.out.println("Covered by unit test");
    }

    public void coveredByIntegrationTest()
    {
       boolean integrationTest=true;
       if (integrationTest) {
         System.out.println("Covered by IT");
        }
    }
}

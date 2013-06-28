import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

public class FakeDashboardTemplate extends DashboardTemplate {

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create()
      .setLayout(DashboardLayout.TWO_COLUMNS_30_70)
      .setDescription("Fake dashboard for integration tests");
    dashboard.addWidget("lcom4", 1);
    dashboard.addWidget("description", 1);
    dashboard.addWidget("comments_duplications", 2);
    dashboard.addWidget("complexity", 3); // should be ignored because the layout is 2 columns
    return dashboard;
  }

  @Override
  public String getName() {
    return "Fake";
  }
}

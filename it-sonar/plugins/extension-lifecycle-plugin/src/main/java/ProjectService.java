import org.sonar.api.BatchExtension;

/**
 * As many instances as projects (maven modules)
 */
public class ProjectService implements BatchExtension {

  private BatchService batchService;

  public ProjectService(BatchService batchService) {
    this.batchService = batchService;
  }

  public void start() {
    System.out.println("Start ProjectService");

    if (!batchService.isStarted()) {
      throw new IllegalStateException("ProjectService must be started after BatchService");
    }
    batchService.incrementProjectService();
  }

  public void stop() {
    System.out.println("Stop ProjectService");
    if (!batchService.isStarted()) {
      System.out.println("ProjectService must be stopped before BatchService");
      System.exit(1);
    }
  }
}

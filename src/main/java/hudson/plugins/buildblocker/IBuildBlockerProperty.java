package hudson.plugins.buildblocker;

/**
 * Common interface for job and folder properties
 */
public interface IBuildBlockerProperty {

    BuildBlockerProperty.BlockLevel getBlockLevel();

    BuildBlockerProperty.QueueScanScope getScanQueueFor();

    boolean isUseBuildBlocker();

    String getBlockingJobs();

}

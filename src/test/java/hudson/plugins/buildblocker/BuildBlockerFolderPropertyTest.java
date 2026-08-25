package hudson.plugins.buildblocker;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class BuildBlockerFolderPropertyTest {

    private JenkinsRule j;
    @Mock
    private BlockingJobsMonitor monitor;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testJobInFolderIsBlocked() throws Exception {
        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject job = folder.createProject(FreeStyleProject.class, "job");
        folder.getProperties().add(folderProperty(true, "blocker"));
        FreeStyleProject blocker = j.createFreeStyleProject("blocker");
        when(monitor.checkAllNodesForRunningBuilds()).thenReturn(blocker);

        CauseOfBlockage cause = dispatcher().canRun(itemFor(job));

        assertNotNull(cause);
        verify(monitor).checkAllNodesForRunningBuilds();
    }

    @Test
    void testNearestFolderPropertyWinsWithoutFallingThrough() throws Exception {
        Folder parent = j.jenkins.createProject(Folder.class, "parent");
        Folder child = parent.createProject(Folder.class, "child");
        FreeStyleProject job = child.createProject(FreeStyleProject.class, "job");
        parent.getProperties().add(folderProperty(true, "blocker"));
        child.getProperties().add(folderProperty(false, "blocker"));

        CauseOfBlockage cause = dispatcher().canRun(itemFor(job));

        assertNull(cause);
        verifyNoInteractions(monitor);
    }

    @Test
    void testJobPropertyTakesPrecedenceOverFolderProperty() throws Exception {
        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject job = folder.createProject(FreeStyleProject.class, "job");
        folder.getProperties().add(folderProperty(true, "folder-blocker"));
        job.addProperty(new BuildBlockerPropertyBuilder()
                .setUseBuildBlocker()
                .setBlockOnGlobalLevel()
                .setBlockingJobs("job-blocker")
                .createBuildBlockerProperty());
        FreeStyleProject blocker = j.createFreeStyleProject("blocker");
        when(monitor.checkAllNodesForRunningBuilds()).thenReturn(blocker);

        RecordingMonitorFactory factory = new RecordingMonitorFactory(monitor);
        CauseOfBlockage cause = dispatcher(factory).canRun(itemFor(job));

        assertNotNull(cause);
        assertEquals("job-blocker", factory.blockingJobs);
        verify(monitor).checkAllNodesForRunningBuilds();
    }

    @Test
    void testMatrixConfigurationOwnerInheritsFolderProperty() throws Exception {
        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        MatrixProject matrix = folder.createProject(MatrixProject.class, "matrix");
        matrix.setAxes(new hudson.matrix.AxisList(new hudson.matrix.TextAxis("axis", "one")));
        MatrixConfiguration configuration = matrix.getItem("axis=one");
        folder.getProperties().add(folderProperty(true, "blocker"));
        assertNotNull(configuration);
        FreeStyleProject blocker = j.createFreeStyleProject("blocker");
        when(monitor.checkAllNodesForRunningBuilds()).thenReturn(blocker);

        Queue.Task task = mock(Queue.Task.class);
        when(task.getOwnerTask()).thenReturn(configuration);
        Queue.BuildableItem item = itemFor(task);
        CauseOfBlockage cause = dispatcher().canTake(null, item);

        assertNotNull(cause);
        verify(monitor).checkAllNodesForRunningBuilds();
    }

    private BuildBlockerQueueTaskDispatcher dispatcher() {
        return dispatcher(new RecordingMonitorFactory(monitor));
    }

    private BuildBlockerQueueTaskDispatcher dispatcher(MonitorFactory factory) {
        return new BuildBlockerQueueTaskDispatcher(factory);
    }

    private static BuildBlockerFolderProperty folderProperty(boolean useBuildBlocker, String blockingJobs) {
        BuildBlockerFolderProperty property = new BuildBlockerFolderProperty();
        property.setUseBuildBlocker(useBuildBlocker);
        property.setBlockLevel("global");
        property.setBlockingJobs(blockingJobs);
        return property;
    }

    private static Queue.BuildableItem itemFor(Queue.Task task) throws Exception {
        return new Queue.BuildableItem(new Queue.WaitingItem(Calendar.getInstance(), task, new ArrayList<>()));
    }

    private static class RecordingMonitorFactory implements MonitorFactory {

        private final BlockingJobsMonitor monitor;
        private String blockingJobs;

        RecordingMonitorFactory(BlockingJobsMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public BlockingJobsMonitor build(String blockingJobs) {
            this.blockingJobs = blockingJobs;
            return monitor;
        }
    }
}

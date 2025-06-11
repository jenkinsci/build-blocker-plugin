package hudson.plugins.buildblocker;

import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildBlockerQueueTaskDispatcherUnitTest {

    @Mock
    private BlockingJobsMonitor monitor;
    @Mock
    private Node node;

    private AbstractProject project;
    private Queue.BuildableItem item;

    private BuildBlockerQueueTaskDispatcher dispatcher;

    @BeforeEach
    void setUp() throws Exception {
        dispatcher = new BuildBlockerQueueTaskDispatcher(new FieldReturningMonitorFactory(monitor));

        project = mock(AbstractProject.class);
        item = mock(Queue.BuildableItem.class);

        Field task = Queue.Item.class.getField("task");
        task.setAccessible(true);
        task.set(item, project);
    }

    @Test
    void testCanRunWithBuildBlockerDisabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanRunWithBuildBlockerEnabledAndNullBlockingJobsDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledAndCheckBuildableEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForBuildableQueueEntries(eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setScanAllQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForQueueEntries(eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledAndCheckBuildableEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setScanAllQueueItemStates()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForQueueEntries(eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanRunWithNodeEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanRunWithNodeEnabledAndCheckBuildableEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanRunWithNodeEnabledAndCheckAllEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setScanAllQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanRunWithNodeEnabledAndCheckAllEnabledAndCheckBuildableEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setScanAllQueueItemStates()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledAndNodeEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setBlockOnNodeLevel()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledAndNodeEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setBlockOnNodeLevel()
                                .setScanAllQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForQueueEntries(eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledAndNodeEnabledAndCheckBuildableEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setBlockOnNodeLevel()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForBuildableQueueEntries(eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanRunWithGlobalEnabledAndNodeEnabledAndCheckAllEnabledAndCheckBuildableEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setBlockOnNodeLevel()
                                .setScanAllQueueItemStates()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForQueueEntries(eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanTakeWithBuildBlockerDisabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithBuildBlockerEnabledAndNullBlockingJobsDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledAndCheckAllEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setScanAllQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledAndCheckBuildableEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledAndCheckBuildableEnabledAndCheckAllEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setScanBuildableQueueItemStates()
                                .setScanAllQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithNodeEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verify(monitor, times(1)).checkNodeForRunningBuilds(eq(node));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanTakeWithNodeEnabledAndCheckBuildableEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verify(monitor, times(1)).checkNodeForRunningBuilds(eq(node));
        verify(monitor, times(1)).checkNodeForBuildableQueueEntries(eq(item), eq(node));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanTakeWithNodeEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setScanAllQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verify(monitor, times(1)).checkNodeForRunningBuilds(eq(node));
        verify(monitor, times(1)).checkNodeForQueueEntries(eq(item), eq(node));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanTakeWithNodeEnabledAndCheckBuildableEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnNodeLevel()
                                .setScanAllQueueItemStates()
                                .setScanBuildableQueueItemStates()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verify(monitor, times(1)).checkNodeForRunningBuilds(eq(node));
        verify(monitor, times(1)).checkNodeForQueueEntries(eq(item), eq(node));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledAndNodeEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker()
                                .setBlockOnGlobalLevel()
                                .setBlockOnNodeLevel()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledAndNodeEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(new BuildBlockerPropertyBuilder()
                        .setUseBuildBlocker()
                        .setBlockOnGlobalLevel()
                        .setBlockOnNodeLevel()
                        .setScanAllQueueItemStates()
                        .setBlockingJobs("someJob")
                        .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledAndNodeEnabledAndCheckBuildableEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(new BuildBlockerPropertyBuilder()
                        .setUseBuildBlocker()
                        .setBlockOnGlobalLevel()
                        .setBlockOnNodeLevel()
                        .setScanBuildableQueueItemStates()
                        .setBlockingJobs("someJob")
                        .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    @Test
    void testCanTakeWithGlobalEnabledAndNodeEnabledAndCheckBuildableEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(new BuildBlockerPropertyBuilder()
                        .setUseBuildBlocker()
                        .setBlockOnGlobalLevel()
                        .setBlockOnNodeLevel()
                        .setScanBuildableQueueItemStates()
                        .setScanAllQueueItemStates()
                        .setBlockingJobs("someJob")
                        .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyNoInteractions(monitor);
    }

    private static class FieldReturningMonitorFactory implements MonitorFactory {

        private final BlockingJobsMonitor monitor;

        @Override
        public BlockingJobsMonitor build(String blockingJobs) {
            return monitor;
        }

        public FieldReturningMonitorFactory(BlockingJobsMonitor monitor) {
            this.monitor = monitor;
        }
    }
}

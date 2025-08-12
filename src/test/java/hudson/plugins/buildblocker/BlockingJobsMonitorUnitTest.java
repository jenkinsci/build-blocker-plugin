package hudson.plugins.buildblocker;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.OneOffExecutor;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.SubTask;
import hudson.model.queue.WorkUnit;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlockingJobsMonitorUnitTest {

    @Mock
    private Node node;
    @Mock
    private Computer computer;
    @Mock
    private Queue queue;
    @Mock
    private Executor idleExecutor;
    @Mock
    private Executor executor;
    @Mock
    private OneOffExecutor idleOneOffExecutor;
    @Mock
    private OneOffExecutor oneOffExecutor;
    @Mock
    private Label blockingLabel;
    @Mock
    private Label nonBlockingLabel;
    @Mock
    private SubTask subTask;
    @Mock
    private MatrixConfiguration configuration;

    private Project project;
    private Project nonBlockingProject;
    private MatrixProject nonBlockingMatrixProject;
    private MatrixProject matrixProject;
    private BuildableItem buildableItem;
    private BuildableItem buildableItemOnDifferentNode;
    private BuildableItem nonBlockingBuildableItem;
    private Queue.BlockedItem blockedItem;
    private Queue.BlockedItem blockedItemOnDifferentNode;
    private Queue.BlockedItem nonBlockingBlockedItem;
    private Queue.WaitingItem waitingItem;
    private Queue.WaitingItem waitingItemOnDifferentNode;
    private Queue.WaitingItem nonBlockingWaitingItem;
    private WorkUnit workUnit;

    private BlockingJobsMonitor monitor;
    private MockedStatic<Jenkins> mockedJenkins;

    @BeforeEach
    void setUp() throws Exception {
        monitor = new BlockingJobsMonitor("blockingProject\nblockingMatrixProject");

        trainProjects();
        trainBuildableItems();
        trainBlockedItems();
        trainWaitingItems();
        trainLabels();
        trainJenkins();
        trainNodes();
        trainWorkUnit();
        trainExecutors();
    }

    @AfterEach
    void tearDown() {
        if (mockedJenkins != null) {
            mockedJenkins.close();
        }
    }

    private void trainWorkUnit() throws Exception {
        workUnit = mock(WorkUnit.class);

        Field work = WorkUnit.class.getField("work");
        work.setAccessible(true);
        work.set(workUnit, subTask);
    }

    private void trainLabels() {
        when(blockingLabel.contains(eq(node))).thenReturn(true);
        when(nonBlockingLabel.contains(eq(node))).thenReturn(false);
    }

    private void trainWaitingItems() throws Exception {
        waitingItem = mock(Queue.WaitingItem.class);
        nonBlockingWaitingItem = mock(Queue.WaitingItem.class);
        waitingItemOnDifferentNode = mock(Queue.WaitingItem.class);

        Field task = Queue.BlockedItem.class.getField("task");
        task.setAccessible(true);

        task.set(waitingItem, project);
        task.set(waitingItemOnDifferentNode, project);
        task.set(nonBlockingWaitingItem, nonBlockingProject);

        when(waitingItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(waitingItemOnDifferentNode.getAssignedLabel()).thenReturn(nonBlockingLabel);
        when(nonBlockingWaitingItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainBlockedItems() throws Exception {
        blockedItem = mock(Queue.BlockedItem.class);
        nonBlockingBlockedItem = mock(Queue.BlockedItem.class);
        blockedItemOnDifferentNode = mock(Queue.BlockedItem.class);

        Field task = Queue.BlockedItem.class.getField("task");
        task.setAccessible(true);

        task.set(blockedItem, project);
        task.set(nonBlockingBlockedItem, nonBlockingProject);
        task.set(blockedItemOnDifferentNode, project);

        when(blockedItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(blockedItemOnDifferentNode.getAssignedLabel()).thenReturn(nonBlockingLabel);
        when(nonBlockingBlockedItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainBuildableItems() throws Exception {
        buildableItem = mock(BuildableItem.class);
        buildableItemOnDifferentNode = mock(BuildableItem.class);
        nonBlockingBuildableItem = mock(BuildableItem.class);

        Field task = BuildableItem.class.getField("task");
        task.setAccessible(true);

        task.set(buildableItem, project);
        task.set(buildableItemOnDifferentNode, project);
        task.set(nonBlockingBuildableItem, nonBlockingProject);

        when(buildableItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(buildableItemOnDifferentNode.getAssignedLabel()).thenReturn(blockingLabel);
        when(nonBlockingBuildableItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainExecutors() {
        when(idleExecutor.isBusy()).thenReturn(false);
        when(executor.isBusy()).thenReturn(true);
        when(executor.getCurrentWorkUnit()).thenReturn(workUnit);

        when(idleOneOffExecutor.isBusy()).thenReturn(false);
        when(oneOffExecutor.isBusy()).thenReturn(true);
        when(oneOffExecutor.getCurrentWorkUnit()).thenReturn(workUnit);

    }

    private void trainNodes() {
        when(node.toComputer()).thenReturn(computer);
    }

    private void trainJenkins() {
        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getQueue()).thenReturn(queue);
        when(jenkins.getComputers()).thenReturn(new Computer[]{computer});
        mockedJenkins = mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);
    }

    private void trainProjects() {
        project = mock(Project.class);
        nonBlockingProject = mock(Project.class);
        matrixProject = mock(MatrixProject.class);
        nonBlockingMatrixProject = mock(MatrixProject.class);
        when(project.getFullName()).thenReturn("blockingProject");
        when(nonBlockingProject.getFullName()).thenReturn("harmlessProject");
        when(matrixProject.getFullName()).thenReturn("blockingMatrixProject");
        when(nonBlockingMatrixProject.getFullName()).thenReturn("harmlessMatrixProject");
    }

    @Test
    void testCheckNodeForBuildableQueueEntriesItemDoesNotSelfBlock() {
        when(queue.getBuildableItems(Mockito.any(Computer.class))).thenReturn(singletonList(buildableItem));

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));

        //the do not selfblock condition is hit => no interactions with the project
        verifyNoInteractions(project);
    }

    @Test
    void testCheckNodeForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems(Mockito.any(Computer.class))).thenReturn(Collections.emptyList());

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    void testCheckNodeForBuildableQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkNodeForBuildableQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    void testCheckNodeForBuildableQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));
        Node differentNode = mock(Node.class);
        Computer differentComputer = mock(Computer.class);
        when(differentNode.toComputer()).thenReturn(differentComputer);
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));
        when(queue.getBuildableItems(eq(differentComputer))).thenReturn(Collections.emptyList());

        assertThat(monitor.checkNodeForBuildableQueueEntries(mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    void testCheckNodeForQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{});

        assertThat(monitor.checkNodeForQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    void testCheckNodeForQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    void testCheckNodeForQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItemOnDifferentNode});
        Node differentNode = mock(Node.class, withSettings().strictness(Strictness.LENIENT));
        Computer differentComputer = mock(Computer.class);
        when(differentNode.toComputer()).thenReturn(differentComputer);

        assertThat(monitor.checkNodeForQueueEntries(mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    void testCheckNodeForQueueEntriesReturnsBlockedTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    void testCheckNodeForQueueEntriesReturnsNullForDifferentNodeCaseBlocked() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItemOnDifferentNode});
        Node differentNode = mock(Node.class, withSettings().strictness(Strictness.LENIENT));
        Computer differentComputer = mock(Computer.class);
        when(differentNode.toComputer()).thenReturn(differentComputer);

        assertThat(monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    void testCheckNodeForQueueEntriesReturnsWaitingTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    void testCheckNodeForQueueEntriesReturnsNullForDifferentNodeCaseWaiting() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItemOnDifferentNode});
        Node differentNode = mock(Node.class, withSettings().strictness(Strictness.LENIENT));
        Computer differentComputer = mock(Computer.class);
        when(differentNode.toComputer()).thenReturn(differentComputer);

        assertThat(monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    void testCheckForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems()).thenReturn(Collections.emptyList());

        assertThat(monitor.checkForBuildableQueueEntries(buildableItem), is(nullValue()));
    }

    @Test
    void testCheckForBuildableQueueEntriesItemDoesNotSelfBlock() {
        when(queue.getBuildableItems()).thenReturn(singletonList(buildableItem));

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));

        //the do not selfblock condition is hit => no interactions with the project
        verifyNoInteractions(project);
    }

    @Test
    void testCheckForBuildableQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getBuildableItems()).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkForBuildableQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForBuildableQueueEntriesReturnsProjectForDifferentNode() {
        when(queue.getBuildableItems()).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkForBuildableQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{});

        assertThat(monitor.checkForQueueEntries(buildableItem), is(nullValue()));
    }

    @Test
    void testCheckForQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItem});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForQueueEntriesReturnsProjectForDifferentNode() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForQueueEntriesReturnsBlockedTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItem});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForQueueEntriesReturnsProjectForDifferentNodeCaseBlocked() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForQueueEntriesReturnsWaitingTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItem});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForQueueEntriesReturnsProjectForDifferentNodeCaseWaiting() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    void testCheckForQueueEntriesReturnsNullForNonBlockingItems() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, nonBlockingBuildableItem, nonBlockingBlockedItem});

        assertThat(monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(nullValue()));

        //verify that the different project was actually checked (three items are checked for two job names each)
        verify(nonBlockingProject, times(6)).getFullName();
    }


    @Test
    void testCheckNodeForRunningBuildNeedsNode() {
        assertThat(monitor.checkNodeForRunningBuilds(null), is(nullValue()));
    }

    @Test
    void testCheckNodeForRunningBuildReturnsNullForNonBusyExecutor() {
        when(computer.getExecutors()).thenReturn(singletonList(idleExecutor));
        when(computer.getOneOffExecutors()).thenReturn(new ArrayList<>());

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        verify(idleExecutor, only()).isBusy();
    }

    @Test
    void testCheckNodeForRunningBuildReturnsNullForNonBusyOneOffExecutor() {
        when(computer.getExecutors()).thenReturn(new ArrayList<>());
        when(computer.getOneOffExecutors()).thenReturn(singletonList(idleOneOffExecutor));

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        verify(idleOneOffExecutor, only()).isBusy();
    }

    @Test
    void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunning() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkNodeForRunningBuilds(node), is(equalTo(project)));
    }

    @Test
    void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProjectOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunningOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkNodeForRunningBuilds(node), is(equalTo(project)));
    }

    @Test
    void testCheckNodeForRunningBuildReturnsNullForDifferentRunningMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(nonBlockingMatrixProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingMatrixProject, times(2)).getFullName();
    }

    @Test
    void testCheckNodeForRunningBuildReturnsBlockedMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(matrixProject);

        assertThat((MatrixProject) monitor.checkNodeForRunningBuilds(node), is(equalTo(matrixProject)));
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsNullForNonBusyExecutor() {
        when(computer.getExecutors()).thenReturn(singletonList(idleExecutor));
        when(computer.getOneOffExecutors()).thenReturn(new ArrayList<>());

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        verify(idleExecutor, only()).isBusy();
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsNullForNonBusyOneOffExecutor() {
        when(computer.getExecutors()).thenReturn(new ArrayList<>());
        when(computer.getOneOffExecutors()).thenReturn(singletonList(idleOneOffExecutor));
        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        verify(idleOneOffExecutor, only()).isBusy();
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsBlockedProjectIfItIsRunning() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkAllNodesForRunningBuilds(), is(equalTo(project)));
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningProjectOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsBlockedProjectIfItIsRunningOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkAllNodesForRunningBuilds(), is(equalTo(project)));
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(nonBlockingMatrixProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingMatrixProject, times(2)).getFullName();
    }

    @Test
    void testCheckAllNodesForRunningBuildReturnsBlockedMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(matrixProject);

        assertThat((MatrixProject) monitor.checkAllNodesForRunningBuilds(), is(equalTo(matrixProject)));
    }

}

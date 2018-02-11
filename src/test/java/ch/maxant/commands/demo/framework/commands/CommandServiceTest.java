package ch.maxant.commands.demo.framework.commands;

import ch.maxant.commands.demo.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import javax.ejb.SessionContext;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandServiceTest extends DbTest {

    private CommandService commandService;

    private Timer retryTimer;

    private Timer unlockTimer;

    private ExecutableCommand executableCommand;

    private boolean throwExceptionDuringCommandExecution = false;

    private AtomicInteger executionCount = new AtomicInteger(0);

    @Before
    public void setup(){
        super.setup();

        System.setProperty("commandservice.timeout", "0"); //so that unlocking causes immediate execution

        commandService = new CommandService();
        commandService.logger = mock(Logger.class);

        commandService.commandRepo = new CommandRepository();
        commandService.commandRepo.em = em;
        commandService.commandRepo.logger = commandService.logger;

        commandService.timerService = mock(TimerService.class);
        commandService.context = mock(SessionContext.class);
        commandService.event = mock(Event.class);
        commandService.executors = mock(Instance.class);

        unlockTimer = mock(Timer.class);
        retryTimer = mock(Timer.class);
        when(retryTimer.getInfo()).thenReturn(CommandService.RETRY_COMMANDS);
        when(unlockTimer.getInfo()).thenReturn(CommandService.UNLOCK_TIMEDOUT_COMMANDS);
        when(commandService.timerService.createIntervalTimer(anyLong(), anyLong(), any(TimerConfig.class)))
                .then((i) -> {
            TimerConfig c = (TimerConfig) i.getArguments()[2];
            if(c.getInfo().equals(CommandService.RETRY_COMMANDS)){
                return retryTimer;
            }else{
                return unlockTimer;
            }
        });
        when(commandService.context.getBusinessObject(eq(CommandService.class))).thenReturn(commandService);

        executableCommand = (idempotencyId, context) -> {
            executionCount.incrementAndGet();
            if(throwExceptionDuringCommandExecution){
                throw new RuntimeException();
            }
        };
        when(commandService.executors.iterator()).then((i) ->
            Collections.singletonList(executableCommand).iterator()
        );
    }

    @Test
    public void testAll() throws Exception {

        commandService.init();

        // ///////////////////////////////////////
        // timeout #1 - retryTimer - no commands persisted yet
        // ///////////////////////////////////////
        em.getTransaction().begin();
        commandService.timeout(retryTimer);
        em.getTransaction().commit();
        assertEquals(0, executionCount.get());

        // ///////////////////////////////////////
        // event - all ok
        // ///////////////////////////////////////
        final String commandContext = "{}";
        Command cmd = new Command(executableCommand.getClass(), commandContext);

        em.getTransaction().begin();
        em.persist(cmd);
        em.getTransaction().commit();

        em.getTransaction().begin();
        commandService.observe(cmd);
        em.getTransaction().commit();
        assertEquals(1, executionCount.get());

        //check db is empty
        em.getTransaction().begin();
        List<Command> cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(0, cmds.size());

        // ///////////////////////////////////////
        // timeout #2 - retryTimer - nothing to do, because no commands in DB
        // ///////////////////////////////////////

        em.getTransaction().begin();
        commandService.timeout(retryTimer);
        em.getTransaction().commit();
        assertEquals(1, executionCount.get());


        // ///////////////////////////////////////
        // event - with failure
        // ///////////////////////////////////////
        throwExceptionDuringCommandExecution = true;

        cmd = new Command(executableCommand.getClass(), commandContext);

        em.getTransaction().begin();
        em.persist(cmd);
        em.getTransaction().commit();

        em.getTransaction().begin();
        commandService.observe(cmd);
        em.getTransaction().commit();
        assertEquals(2, executionCount.get());

        //check db is not empty
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(1, cmds.size());
        assertEquals(1, cmds.get(0).getAttempts());
        assertNull(cmds.get(0).getLocked()); //it gets reset on failure

        // ///////////////////////////////////////
        // timeout #3 - unlockTimer - doesnt do anything, because command already unlocked
        // ///////////////////////////////////////
        em.getTransaction().begin();
        commandService.timeout(unlockTimer);
        em.getTransaction().commit();
        assertEquals(2, executionCount.get());

        //check db is not empty
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(1, cmds.size());
        assertEquals(1, cmds.get(0).getAttempts());
        assertNull(cmds.get(0).getLocked());

        // ///////////////////////////////////////
        // timeout #4 - retryTimer - should now execute the failed command
        // ///////////////////////////////////////
        em.getTransaction().begin();
        commandService.timeout(retryTimer);
        em.getTransaction().commit();
        assertEquals(3, executionCount.get());

        //check db is not empty
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(1, cmds.size());
        assertEquals(2, cmds.get(0).getAttempts());
        assertNull(cmds.get(0).getLocked());

        // ///////////////////////////////////////
        // timeout #5 - retryTimer - let it thru now
        // ///////////////////////////////////////
        throwExceptionDuringCommandExecution = false;

        em.getTransaction().begin();
        commandService.timeout(retryTimer);
        em.getTransaction().commit();
        assertEquals(4, executionCount.get());

        //check db is not empty
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(0, cmds.size());


        // ///////////////////////////////////////
        // event - with failure, and never gets resolved => check it remains in the DB
        // ///////////////////////////////////////
        throwExceptionDuringCommandExecution = true;

        cmd = new Command(executableCommand.getClass(), commandContext);

        em.getTransaction().begin();
        em.persist(cmd);
        em.getTransaction().commit();

        em.getTransaction().begin();
        commandService.observe(cmd);
        em.getTransaction().commit();
        assertEquals(5, executionCount.get());

        //check db is not empty
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(1, cmds.size());
        assertEquals(1, cmds.get(0).getAttempts());
        assertNull(cmds.get(0).getLocked());

        // ///////////////////////////////////////
        // timeout #6-9 - retryTimer - keeps failing and then stops executing!
        // ///////////////////////////////////////
        for(int i = 0; i < 4; i++){
            em.getTransaction().begin();
            commandService.timeout(retryTimer);
            em.getTransaction().commit();
            assertEquals(6+i, executionCount.get());

            //check db is not empty
            em.getTransaction().begin();
            cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
            em.getTransaction().commit();
            assertEquals(1, cmds.size());
            assertEquals(i+2, cmds.get(0).getAttempts());
            assertNull(cmds.get(0).getLocked());
        }

        // ///////////////////////////////////////
        // timeout #10 - retryTimer - no longer executes because number of attempts is too high
        // ///////////////////////////////////////
        em.getTransaction().begin();
        commandService.timeout(retryTimer);
        em.getTransaction().commit();
        assertEquals(9, executionCount.get());

        //check db is not empty
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(1, cmds.size());
        assertEquals(5, cmds.get(0).getAttempts());
        assertNull(cmds.get(0).getLocked());

        // ///////////////////////////////////////
        // timeout #11 - retryTimer - ditto
        // ///////////////////////////////////////
        em.getTransaction().begin();
        commandService.timeout(retryTimer);
        em.getTransaction().commit();
        assertEquals(9, executionCount.get());

        //check db is not empty
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(1, cmds.size());
        assertEquals(5, cmds.get(0).getAttempts());
        assertNull(cmds.get(0).getLocked());



        // ///////////////////////////////////////
        // simulate failure - put locked command in DB then call unlock timeout and ensure its executed immediately
        // ///////////////////////////////////////
        throwExceptionDuringCommandExecution = false;

        cmd = new Command(executableCommand.getClass(), commandContext);

        em.getTransaction().begin();
        em.persist(cmd);
        em.getTransaction().commit();

        Thread.sleep(50L); //ensure we are at least one ms later, when we unlock it!

        em.getTransaction().begin();
        commandService.timeout(unlockTimer);
        em.getTransaction().commit();
        assertEquals(10, executionCount.get());

        //check db only contains failed task from previous tests
        em.getTransaction().begin();
        cmds = em.createQuery(CommandTest.SELECT_ALL_COMMANDS, Command.class).getResultList();
        em.getTransaction().commit();
        assertEquals(1, cmds.size());
        assertEquals(5, cmds.get(0).getAttempts());
        assertNull(cmds.get(0).getLocked());
    }
}

package ch.maxant.commands.demo.framework.commands;

import ch.maxant.commands.demo.DbTest;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CommandTest extends DbTest {

    public static final String SELECT_ALL_COMMANDS = "select c from Command c order by c.id";

    private CommandRepository commandRepo;

    @Before
    public void setup(){
        super.setup();

        commandRepo = new CommandRepository();
        commandRepo.em = em;
    }

    @Test
    public void testAll() throws Exception {
        em.getTransaction().begin();
        try {
            // //////////////////////////////
            // create
            // //////////////////////////////
            for(int i = 0; i < 11; i++){
                commandRepo.create(new Command("name", "{\"asdf" + i + "\": 123}"));
            }
            em.getTransaction().commit();

            em.getTransaction().begin();
            List<Command> cmds = em.createQuery(SELECT_ALL_COMMANDS, Command.class).getResultList();
            assertEquals(11, cmds.size());
            Command cmd;
            for(int i = 0; i < 11; i++){
                cmd = cmds.get(i);
                assertEquals("name", cmd.getCommand());
                assertEquals("{\"asdf" + i + "\": 123}", cmd.getContext());
                assertEquals(0, cmd.getAttempts());
                assertNotNull(cmd.getIdempotencyId());
                assertNotNull(cmd.getLocked());

                cmd.resetLocked(); //so it gets selected in the next part of the test
            }
            em.getTransaction().commit();

            // //////////////////////////////
            // check locking works by starting a second thread and measuring how long its blocked for. it cannot
            // run until the main thread commits!
            // //////////////////////////////
            em.getTransaction().begin();
            cmds = commandRepo.lockCommands(10);
            long finishOfMain = System.currentTimeMillis();

            MutableLong finishOfThread = new MutableLong(Long.MAX_VALUE);
            Thread t = new Thread(() -> {
                CommandRepository cr2 = new CommandRepository();
                cr2.em = emf.createEntityManager();
                cr2.em.getTransaction().begin();
                List<Command> cmds2 = cr2.lockCommands(10);
                cr2.em.getTransaction().commit();
                assertEquals(1, cmds2.size());
                assertEquals("{\"asdf10\": 123}", cmds2.get(0).getContext());
                cr2.em.close();
                finishOfThread.setValue(System.currentTimeMillis());
            });
            t.start();

            long timeToSleep = 1000L;
            Thread.sleep(timeToSleep); //give the other thread plenty of time to run => except it wont finish, because its blocked by this one
            em.getTransaction().commit(); //this releases the lock on the commands table

            t.join(); //ensure the thread is also finished before doing check that it was forced to wait until we committed

            assertTrue(finishOfThread.longValue() > finishOfMain + timeToSleep);
            System.out.println("Diff between threads: " + (finishOfThread.longValue() - finishOfMain) + "ms");

            // //////////////////////////////
            // assertions from first locking
            // //////////////////////////////
            assertEquals(10, cmds.size());
            for(int i = 0; i < 10; i++){
                cmd = cmds.get(i);
                assertEquals("name", cmd.getCommand());
                assertEquals("{\"asdf" + i + "\": 123}", cmd.getContext());
                assertEquals(0, cmd.getAttempts());
                assertNotNull(cmd.getIdempotencyId());
                assertNotNull(cmd.getLocked());
            }

            //remove them all except for two -> one had an error, one never got confirmed :-)
            em.getTransaction().begin();
            commandRepo.delete(cmds.get(0));
            commandRepo.resetLockAfterFailure(cmds.get(1));
            for(int i = 2; i < 9; i++){ //9, ie leave the tenth one unconfirmed
                commandRepo.delete(cmds.get(i));
            }
            em.getTransaction().commit();

            // //////////////////////////////
            // check num attempts was increased on the one which had an error (index 1)
            // //////////////////////////////
            cmd = cmds.get(1);
            cmd = em.find(Command.class, cmd.getId());
            assertEquals(1, cmd.getAttempts());
            assertNull(cmd.getLocked());

            // //////////////////////////////
            // check that the one that never got confirmed is unchanged (index 9)
            // //////////////////////////////
            cmd = cmds.get(9);
            cmd = em.find(Command.class, cmd.getId());
            assertEquals(0, cmd.getAttempts());
            assertNotNull(cmd.getLocked());

            // //////////////////////////////
            // unlock all locked commands
            // //////////////////////////////
            em.getTransaction().begin();
            int numUnlocked = commandRepo.unlockCommands(0); //anything older than now
            em.getTransaction().commit();

            assertEquals(2, numUnlocked); //the "lost" one above and the one from the other thread

            // //////////////////////////////
            //now check all remaining commands
            // //////////////////////////////
            em.getTransaction().begin();
            cmds = em.createQuery(SELECT_ALL_COMMANDS, Command.class).getResultList();
            em.getTransaction().commit();
            assertEquals(3, cmds.size()); //the "lost" one above and the one from the other thread, and the one which was reset

            //the reset one
            cmd = cmds.get(0);
            assertEquals("name", cmd.getCommand());
            assertEquals("{\"asdf1\": 123}", cmd.getContext());
            assertEquals(1, cmd.getAttempts());
            assertNotNull(cmd.getIdempotencyId());
            assertNull(cmd.getLocked());

            //non-confirmed one
            cmd = cmds.get(1);
            assertEquals("name", cmd.getCommand());
            assertEquals("{\"asdf9\": 123}", cmd.getContext());
            assertEquals(0, cmd.getAttempts());
            assertNotNull(cmd.getIdempotencyId());
            assertNull(cmd.getLocked());

            //from other thread
            cmd = cmds.get(2);
            assertEquals("name", cmd.getCommand());
            assertEquals("{\"asdf10\": 123}", cmd.getContext());
            assertEquals(0, cmd.getAttempts());
            assertNotNull(cmd.getIdempotencyId());
            assertNull(cmd.getLocked());

        } finally {
            //Close the manager
            em.close();
        }
    }
}

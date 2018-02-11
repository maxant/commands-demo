package ch.maxant.commands.demo;

import ch.maxant.commands.demo.framework.commands.Command;
import ch.maxant.commands.demo.framework.commands.CommandService;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class TaskService {

    @Inject
    CommandService commandService;

    /** will create a command which causes a task to be created in ARO, asynchronously, but robustly. */
    public void createTask(long caseNr, String textForTask) {
        String context = createContext(caseNr, textForTask);

        Command command = new Command(CreateTaskCommand.class, context);

        commandService.persistCommand(command);
    }

    private String createContext(long nr, String textForTask) {
        //TODO use object mapper rather than build string ourselves...
        return "{\"caseNr\": " + nr + ", \"textForTask\": \"" + textForTask + "\"}";
    }

}

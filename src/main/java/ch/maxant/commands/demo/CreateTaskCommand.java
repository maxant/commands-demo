package ch.maxant.commands.demo;

import ch.maxant.commands.demo.framework.commands.ExecutableCommand;
import com.fasterxml.jackson.databind.JsonNode;

import javax.ejb.Stateless;

@Stateless
public class CreateTaskCommand implements ExecutableCommand {

    public static final String NAME = "CreateTask";

    @Override
    public void execute(String idempotencyId, JsonNode context) {
        long caseNr = context.get("caseNr").longValue();
        System.out.println("TODO CALL MICROSERVICE HERE: " + caseNr);
    }

    @Override
    public String getName() {
        return NAME;
    }
}

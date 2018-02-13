package ch.maxant.commands.demo.framework.commands;

import com.fasterxml.jackson.databind.JsonNode;

public interface ExecutableCommand {

    void execute(String idempotencyId, JsonNode context);

    String getName();
}

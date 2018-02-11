package ch.maxant.commands.demo.framework.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonConfig implements ContextResolver<ObjectMapper> {
    private ObjectMapper objectMapper;


    public JacksonConfig() throws Exception {
        this.objectMapper = new ObjectMapper();

        this.objectMapper.registerModule(new Jdk8Module());
        this.objectMapper.registerModule(new JavaTimeModule());

        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }


    public ObjectMapper getContext(Class<?> objectType) {
        return objectMapper;
    }
}  
 

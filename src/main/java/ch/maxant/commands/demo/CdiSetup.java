package ch.maxant.commands.demo;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

@RequestScoped
public class CdiSetup {

    public static final String PRIMARY = "primary";

    @PersistenceContext(name = PRIMARY)
    EntityManager em;

    @Produces
    public EntityManager getEm(){
        return em;
    }

    @ARO
    @Produces
    public WebTarget getClient(){
        String url = System.getProperty("aro.url");

        Client client = ClientBuilder.newClient();
        return client.target(url);
    }

}
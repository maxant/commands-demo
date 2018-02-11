package ch.maxant.commands.demo;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@RequestScoped
public class CdiSetup {

    public static final String PRIMARY = "primary";

    @PersistenceContext(name = PRIMARY)
    EntityManager em;

    @Produces
    public EntityManager getEm(){
        return em;
    }

}
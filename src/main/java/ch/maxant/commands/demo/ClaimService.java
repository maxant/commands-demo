package ch.maxant.commands.demo;

import ch.maxant.commands.demo.data.Case;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

@Stateless
public class ClaimService {

    @Inject
    EntityManager em;

    @Inject
    TaskService taskService;

    public Case getCase(Long nr) {
        try{
            return em.createNamedQuery(Case.NQFindByNumber.NAME, Case.class)
                    .setParameter(Case.NQFindByNumber.PARAM_NR, nr)
                    .getSingleResult();
        }catch (NoResultException e){
            return null;
        }
    }

    public void mergeCase(Case insuranceCase) {
        em.merge(insuranceCase);

        taskService.createTask(insuranceCase.getNr(), "Some text telling the user what to do...");
    }

}

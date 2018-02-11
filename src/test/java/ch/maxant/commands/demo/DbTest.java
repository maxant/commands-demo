package ch.maxant.commands.demo;

import org.flywaydb.core.Flyway;
import org.junit.After;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.fail;

public abstract class DbTest {

    protected EntityManager em;

    protected EntityManagerFactory emf;

    private Flyway flyway;

    public void setup() {
        boolean useMysql = Boolean.getBoolean("test.use.mysql");

        //do EM first, then flyway, otherwise the in memory DB is closed before the EM starts up, and the em version has no tables!
        emf = Persistence.createEntityManagerFactory(useMysql ? "primary_mysql" : CdiSetup.PRIMARY);
        em = emf.createEntityManager();

        flyway = new Flyway();
        if(useMysql){
            System.out.println("\r\n\r\nUSING MYSQL!!\r\n");
            flyway.setDataSource("jdbc:mysql://localhost:3306/command_demo?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC", "root", "password");
        }else{
            flyway.setDataSource("jdbc:h2:mem:test", "sa", "");
        }

        flyway.migrate();

        deleteData(flyway);
    }

    private void deleteData(Flyway flyway) {

        String[] tables = {"T_COMMAND"};

        try{
            Connection conn = flyway.getDataSource().getConnection();
            conn.setAutoCommit(false);
            for(String table : tables){
                System.out.println("DELETING FROM " + table);
                Statement stmt = conn.createStatement();
                stmt.execute("delete from " + table);
                stmt.close();
            }
            conn.commit();
            conn.close();
        }catch (SQLException e){
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @After
    public void teardown(){
        emf.close();
    }

}

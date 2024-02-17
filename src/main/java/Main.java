import Entities.User;
import orm.EntityManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static orm.MyConnector.createConnection;
import static orm.MyConnector.getConnection;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException {
        createConnection("root","*****","custom-orm");
        Connection connection = getConnection();
        EntityManager<User> userEntityManager = new EntityManager<>(connection);
        User user = new User("Boris",22, LocalDate.now());
        userEntityManager.doCreate(User.class);
        userEntityManager.persist(user);
    }
}

import Entities.User;
import orm.EntityManager;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import static orm.MyConnector.createConnection;
import static orm.MyConnector.getConnection;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        createConnection("root","******","custom-orm");
        Connection connection = getConnection();
        EntityManager<User> userEntityManager = new EntityManager<>(connection);
        User user = new User("Boris",22, LocalDate.now());
        User user1 = new User("Boris1",22, LocalDate.now());
        User user2 = new User("Boris2",22, LocalDate.now());
        User user3 = new User("Boris3",22, LocalDate.now());


        //userEntityManager.doCreate(User.class);
        //userEntityManager.doAlter(User.class);
        userEntityManager.persist(user);
        userEntityManager.persist(user1);
        userEntityManager.persist(user2);
        userEntityManager.persist(user3);


        Iterable<User> first = userEntityManager.find(User.class);
        System.out.println(first.toString());

        User toDelete = userEntityManager.findFirst(User.class, "id =4");

        userEntityManager.delete(toDelete);

        Iterable<User> second = userEntityManager.find(User.class);
        System.out.println(second.toString());

        connection.close();
    }
}

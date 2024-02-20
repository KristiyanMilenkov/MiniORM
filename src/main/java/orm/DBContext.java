package orm;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public interface DBContext<E> {
    boolean persist(E entity) throws IllegalAccessException, SQLException;

    Iterable<E> find(Class<E> table) throws SQLException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    Iterable<E> find(Class<E> table, String where) throws SQLException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    E findFirst(Class<E> table) throws SQLException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    E findFirst(Class<E> table, String where) throws SQLException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    boolean delete(E toDelete) throws IllegalAccessException, SQLException;
}
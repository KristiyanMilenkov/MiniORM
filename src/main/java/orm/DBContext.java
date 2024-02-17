package orm;

import java.sql.SQLException;

public interface DBContext<E> {
    boolean persist(E entity) throws IllegalAccessException, SQLException;

    Iterable<E> find(Class<E> table) throws SQLException, InstantiationException, IllegalAccessException;

    Iterable<E> find(Class<E> table, String where) throws SQLException, InstantiationException, IllegalAccessException;

    E findFirst(Class<E> table) throws SQLException, InstantiationException, IllegalAccessException;

    E findFirst(Class<E> table, String where) throws SQLException, InstantiationException, IllegalAccessException;
}
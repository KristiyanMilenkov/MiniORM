package orm;

import Annotations.Column;
import Annotations.Entity;
import Annotations.Id;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManager<E> implements DBContext<E> {
    private final String INSERT_QUERY = "INSERT INTO %s(%s) VALUES(%s);";
    private final String UPDATE_QUERY = "UPDATE %s SET %s WHERE %s;";
    private final String DELETE_QUERY = "DELETE FROM %s  WHERE 1 %s LIMIT 1;";
    private final String SELECT_QUERY = "SELECT * FROM %s WHERE 1 %s %s;";
    private Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean persist(E entity) throws IllegalAccessException, SQLException {
        Field primaryKey = getIdColumn(entity.getClass());
        primaryKey.setAccessible(true);
        Object primaryKeyValue = primaryKey.get(entity);
        if (primaryKeyValue == null || (long) primaryKeyValue == 0) {
            return doInsert(entity, primaryKey);
        }
        return doUpdate(entity, primaryKey);

    }

    private boolean doUpdate(E entity, Field primaryKey) throws SQLException, IllegalAccessException {
        String tableName = this.getTableName(entity.getClass());
        Field[] fields = entity.getClass().getDeclaredFields();
        String where = "";

        List<String> values = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals(primaryKey.getName())) {
                where += field.getName() + " = " + field.get(entity).toString();
            }else if (field.isAnnotationPresent(Column.class)) {
                String str = "`" + field.getAnnotation(Column.class).name() + "` = ";
                Object value = field.get(entity);
                if (field.getType() == Date.class) {
                    str += "\'" + new SimpleDateFormat("yyyyMMdd").format(value) + "\'";
                } else if (field.getType() == int.class || field.getType() == Integer.class
                        || field.getType() == double.class || field.getType() == Double.class) {
                    str += value.toString();
                } else {
                    str += "\'" + value.toString() + "\'";
                }
                values.add(str);
            }
        }
        String query = String.format(UPDATE_QUERY, tableName, String.join(", ", values), where);
        return connection.prepareStatement(query).execute();
        }


    private boolean doInsert(E entity, Field idColumn) throws SQLException, IllegalAccessException {
        String tableName = this.getTableName(entity.getClass());
        String tableFields = getColumnsWithoutId(entity.getClass());
        String tableValues = getColumnValuesWithoutId(entity);
        String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, tableFields,tableValues);
        return connection.prepareStatement(insertQuery).execute();

    }

    private String getColumnValuesWithoutId(E entity) throws IllegalAccessException {
        Class<?> aClass = entity.getClass();
        List<Field> fields = Arrays.stream(aClass.getDeclaredFields()).
                filter(f -> !f.isAnnotationPresent(Id.class)).
                filter(f -> f.isAnnotationPresent(Column.class)).
                collect(Collectors.toList());

        List<String> values = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object o = field.get(entity);
            if (o instanceof String || o instanceof LocalDate) {
                values.add("'" + o + "'");
            }else {
                values.add(o.toString());
            }
        }
        return String.join(",", values);
    }

    private String getColumnsWithoutId(Class<?> aClass) {
        return Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotationsByType(Column.class))
                .map(a -> a[0].name())
                .collect(Collectors.joining(","));
    }

    private String getTableName(Class<?> aClass) {
        Entity[] annotation = aClass.getAnnotationsByType(Entity.class);
        if (annotation.length == 0) {
            throw new UnsupportedOperationException("Entity does not have table name");
        }
        return annotation[0].name();
    }

    @Override
    public Iterable<E> find(Class<E> table) throws SQLException, InstantiationException, IllegalAccessException {
        String query = String.format(SELECT_QUERY, this.getTableName(table), "", "");
        ResultSet rs = connection.prepareStatement(query).executeQuery();

        List<E> entities = new ArrayList<>();
        while (rs.next()) {
            E entity = table.newInstance();
            this.fillEntity(table, rs, entity);
            entities.add(entity);
        }
        return entities;
    }
    private void fillEntity(Class<E> table, ResultSet rs, E entity)
            throws SQLException, IllegalAccessException {
        Field[] fields = table.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            this.fillField(field, entity, rs, field.getAnnotation(Column.class).name());
        }
    }
    private void fillField(Field field, E entity, ResultSet rs, String fieldName)
            throws SQLException, IllegalAccessException {
        if (field.getType() == int.class || field.getType() == Integer.class) {
            field.set(entity, rs.getInt(fieldName));
        } else if (field.getType() == String.class) {
            field.set(entity, rs.getString(fieldName));
        } else if (field.getType() == Date.class) {
            field.set(entity, rs.getDate(fieldName));
        } else if (field.getType() == double.class || field.getType() == Double.class) {
            field.set(entity, rs.getDouble(fieldName));
        }
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) throws SQLException, InstantiationException, IllegalAccessException {
        String query = String.format(SELECT_QUERY,
                this.getTableName(table), (where != null ? " AND " + where : ""), "");
        ResultSet rs = connection.prepareStatement(query).executeQuery();

        List<E> entities = new ArrayList<>();
        while (rs.next()) {
            E entity = table.newInstance();
            this.fillEntity(table, rs, entity);
            entities.add(entity);
        }
        return entities;
    }

    @Override
    public E findFirst(Class<E> table) throws SQLException, InstantiationException, IllegalAccessException {
        String query = String.format(SELECT_QUERY, this.getTableName(table), "", "");
        ResultSet rs = connection.prepareStatement(query).executeQuery();
        E entity = table.newInstance();
        if (rs.next()) {
            this.fillEntity(table, rs, entity);
        }
        return entity;
    }

    @Override
    public E findFirst(Class<E> table, String where) throws SQLException, InstantiationException, IllegalAccessException {
        String query = String.format(SELECT_QUERY,
                this.getTableName(table), (where != null ? " AND " + where : ""), "LIMIT 1");
        ResultSet rs = connection.prepareStatement(query).executeQuery();
        E entity = table.newInstance();
        if (rs.next()) {
            this.fillEntity(table, rs, entity);
        }
        return entity;
    }


    private Field getIdColumn(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            boolean annotationPresent = declaredField.isAnnotationPresent(Id.class);
            if (annotationPresent) {
                return declaredField;
            }
        }
        throw new UnsupportedOperationException("Entity does not have primary key");

    }
}




package ru.samlib.client.database.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import ru.samlib.client.util.ReflectionUtils;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.*;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by 0shad on 19.03.2016.
 */
public class ForeignDao extends BaseDaoImpl {

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface Cascade {
        boolean all() default false;

        boolean delete() default false;

        boolean update() default true;

        boolean create() default true;
    }

    public ForeignDao(Class dataClass) throws SQLException {
        super(dataClass);
    }

    public ForeignDao(ConnectionSource connectionSource, Class dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public ForeignDao(ConnectionSource connectionSource, DatabaseTableConfig tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

    @Override
    public int create(Object data) throws SQLException {
        mergeForeign(data, false);
        return super.create(data);
    }

    @Override
    public int update(Object data) throws SQLException {
        mergeForeign(data, true);
        return super.update(data);
    }

    public void mergeForeign(Object data, boolean update) throws SQLException {
        Map<Field, Object> foreignObjects = getAllForeignFields(data);
        for (Map.Entry<Field, Object> foreignEntry : foreignObjects.entrySet()) {
            Field foreignField = foreignEntry.getKey();
            Object newForeign = foreignEntry.getValue();
            Cascade cascadeMode;
            if (foreignField.isAnnotationPresent(Cascade.class)) {
                cascadeMode = foreignField.getAnnotation(Cascade.class);
            } else {
                continue;
            }
            if (!isNeedToDoSomething(cascadeMode)) {
                continue;
            }
            Class daoClass = (Class) foreignField.getGenericType();
            Dao dao = DaoManager.createDao(connectionSource, daoClass);
            Object newId = null;
            if (newForeign != null) {
                newId = dao.extractId(newForeign);
            }
            if (update) {
                Object oldValue = queryForId(extractId(data));
                Object oldForeign = ReflectionUtils.getField(foreignField, oldValue);
                if (oldForeign != null && (newId == null || !dao.extractId(oldForeign).equals(newId)) && isDelete(cascadeMode)) {
                    dao.delete(oldForeign);
                }
            }
            if (newForeign != null) {
                if ((newId == null || !dao.idExists(newId)) && isCreate(cascadeMode)) {
                    dao.create(newForeign);
                } else if (newId != null && isUpdate(cascadeMode)) {
                    dao.update(newForeign);
                }
            }
        }
        Map<Field, Collection> foreignCollections = getAllForeignCollections(data);
        for (Map.Entry<Field, Collection> foreignCollectionEntry : foreignCollections.entrySet()) {
            Field foreignCollectionField = foreignCollectionEntry.getKey();
            Cascade cascadeMode;
            if (foreignCollectionField.isAnnotationPresent(Cascade.class)) {
                cascadeMode = foreignCollectionField.getAnnotation(Cascade.class);
            } else {
                continue;
            }
            if (!isNeedToDoSomething(cascadeMode)) {
                continue;
            }
            Collection collection;
            if (foreignCollectionEntry.getValue() == null) {
                collection = new LinkedList<>();
            } else {
                collection = new LinkedList<>(foreignCollectionEntry.getValue());
            }
            Class daoClass = (Class) ReflectionUtils.getParameterUpperBound(0, (ParameterizedType) foreignCollectionField.getGenericType());
            Field foreignField = null;
            DatabaseField foreignFieldMeta = null;
            for (Field field : ReflectionUtils.getAllFieldsValues(daoClass)) {
                if (field.getType() == data.getClass() && isForeignField(field)) {
                    foreignField = field;
                    foreignFieldMeta = field.getAnnotation(DatabaseField.class);
                    Dao dao = DaoManager.createDao(connectionSource, daoClass);
                    if (update) {
                        List removeList = dao.queryForEq(getForeignColumnName(foreignFieldMeta, foreignField), data);
                        Iterator collectionIt = collection.iterator();
                        while (collectionIt.hasNext()) {
                            Object newObject = collectionIt.next();
                            Object id = dao.extractId(newObject);
                            if (id != null && dao.idExists(id)) {
                                Iterator removeIt = removeList.iterator();
                                while (removeIt.hasNext()) {
                                    Object removeObj = removeIt.next();
                                    if (isUpdate(cascadeMode) && id.equals(dao.extractId(removeObj))) {
                                        removeIt.remove();
                                        dao.update(newObject);
                                        collectionIt.remove();
                                    }
                                }
                            }
                        }
                        if (isDelete(cascadeMode)) {
                            for (Object remove : removeList) {
                                dao.delete(remove);
                            }
                        } else if (isUpdate(cascadeMode)) {
                            for (Object remove : removeList) {
                                ReflectionUtils.setField(null, foreignField, remove);
                                dao.update(remove);
                            }
                        }
                    }
                    if (isCreate(cascadeMode)) {
                        for (Object newObject : collection) {
                            ReflectionUtils.setField(data, foreignField, newObject);
                            dao.createOrUpdate(newObject);
                        }
                    }
                }
            }
        }
    }

    public static String getForeignColumnName(DatabaseField databaseField, Field foreignField) {
        if (databaseField.foreignColumnName().isEmpty()) {
            return foreignField.getName() + FieldType.FOREIGN_ID_FIELD_SUFFIX;
        } else {
            return foreignField.getName() + "_" + databaseField.foreignColumnName();
        }
    }

    public static boolean isUpdate(Cascade cascade) {
        return cascade.update() || cascade.all();
    }

    public static boolean isCreate(Cascade cascade) {
        return cascade.update() || cascade.create();
    }

    public static boolean isDelete(Cascade cascade) {
        return cascade.update() || cascade.delete();
    }

    public static boolean isNeedToDoSomething(Cascade cascade) {
        return cascade.create() || cascade.update() || cascade.delete() || cascade.all();
    }

    public static boolean isForeignField(Field field) {
        return field.isAnnotationPresent(DatabaseField.class)
                && field.getAnnotation(DatabaseField.class).foreign();
    }

    public static Map<Field, Object> getAllForeignFields(Object data) {
        Map<Field, Object> collections = new HashMap<>();
        for (Field field : ReflectionUtils.getAllFieldsValues(data.getClass())) {
            if (isForeignField(field)) {
                collections.put(field, ReflectionUtils.getField(field, data));
            }
        }
        return collections;
    }

    public static Map<Field, Collection> getAllForeignCollections(Object data) {
        Map<Field, Collection> collections = new HashMap<>();
        for (Field field : ReflectionUtils.getAllFieldsValues(data.getClass())) {
            if (field.getType() == Collection.class && field.isAnnotationPresent(ForeignCollectionField.class)) {
                collections.put(field, (Collection) ReflectionUtils.getField(field, data));
            }
        }
        return collections;
    }

}

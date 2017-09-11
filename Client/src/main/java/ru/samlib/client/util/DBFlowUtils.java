package ru.samlib.client.util;

import android.annotation.SuppressLint;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.From;
import com.raizlabs.android.dbflow.sql.language.Operator;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Where;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.DefaultTransactionManager;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction;
import net.vrallev.android.cat.Cat;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by Admin on 11.09.2017.
 */
public class DBFlowUtils {

    public static <C> List<C> dbFlowOneTwoManyUtilMethod(List<C> list, Class<C> clazz, Operator in) {
        if (list == null) {
            list = dbFlowQueryList(clazz, in);
        }
        return list;
    }

    public static <C> C dbFlowFindFirst(Class<C> clazz, Operator in) {
        return SQLite.select()
                .from(clazz)
                .where(in)
                .querySingle();
    }

    public static long dbFlowDelete(Class clazz, Operator in) {
        From from = SQLite.select()
                .from(clazz);
        long affected = 0;
        if(in != null) {
            affected =from.where(in).executeUpdateDelete();
        } else {
            affected =from.executeUpdateDelete();
        }
        if(affected == 0) {
            Cat.w("Potential error in DB operation: Delete. Zero rows affected! See log for more info. Class:" + clazz);
        }
        return affected;
    }

    public static <C> List<C> dbFlowQueryList(Class<C> clazz, Operator in, int skip, int limit) {
        From<C> from = SQLite.select()
                .from(clazz);
        Where<C> where = null;
        if(in != null) {
            where =  from.where(in);
        }
        if(where != null) {
            if(skip >= 0) {
                where = where.offset(skip);
            }
        } else {
            if(skip >= 0) {
                where = from.offset(skip);
            }
        }
        if(where != null) {
            if(limit >= 0) {
                where = where.limit(limit);
            }
        } else {
            if(limit >= 0) {
                where = from.limit(limit);
            }
        }
        if(where != null) {
            return where.queryList();
        } else {
            return from.queryList();
        }
    }

    public static <C> List<C> dbFlowQueryList(Class<C> clazz, Operator in) {
        return dbFlowQueryList(clazz, in, -1, -1);
    }
}

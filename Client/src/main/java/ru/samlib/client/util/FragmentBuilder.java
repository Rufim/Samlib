package ru.samlib.client.util;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import com.google.gson.internal.Primitives;
import ru.samlib.client.fragments.ErrorFragment;

import java.io.Serializable;
import java.util.*;

/**
 * Created by 0shad on 13.07.2015.
 */
public class FragmentBuilder {

    private static final String TAG = FragmentBuilder.class.getSimpleName();

    public enum ClassType {

        ARRAYLIST(ArrayList.class),
        STRING(String.class),
        ARRAY(Object[].class),
        CHAR(Character.class),
        BYTE(Byte.class),
        BOOLEAN(Boolean.class),
        SHORT(Short.class),
        INTEGER(Integer.class),
        LONG(Long.class),
        FLOAT(Float.class),
        DOUBLE(Double.class),
        BUNDLE(Bundle.class),
        CHARSEQUENCE(CharSequence.class),
        PARCELABLE(Parcelable.class),
        SERIALIZABLE(Serializable.class),
        UNSUPPORTED(Void.class);

        private Class<?> clazz;
        private Class<?> primitive;

        private ClassType(Class<?> Clazz) {
            this.clazz = Clazz;
            if(Primitives.isWrapperType(Clazz)) {
                this.primitive = Primitives.unwrap(Clazz);
            }
        }

        public static ClassType cast(Class<?> cl) {
            if (null == cl) cl = Void.class;
            for (ClassType type : values()) {
                if(Primitives.isPrimitive(cl) && type.primitive == cl) {
                    return type;
                } else if (type.clazz == cl) {
                    return type;
                } else {
                    for (Class<?> intClass : cl.getInterfaces()) {
                        if(type.clazz == intClass) {
                            return type;
                        }
                    }
                }
            }
            return UNSUPPORTED;
        }

        public static ClassType cast(Object obj) {
            if (null == obj) cast(Void.class);
            return cast(obj.getClass());
        }

    }


    private FragmentManager manager;
    private Bundle bundle = new Bundle();
    private Map<String, Object> args = new HashMap<>();
    private boolean toBackStack = false;

    public FragmentBuilder(FragmentManager manager) {
        this.manager = manager;
    }


    public static <F extends Fragment> F newInstance(Class<F> fragmentClass, Bundle args) {
        F fragment = newInstance(fragmentClass);
        fragment.setArguments(args);
        return fragment;
    }

    public static <F extends Fragment> F newInstance(Class<F> fragmentClass) {
        Fragment fragment;
        try {
            fragment = fragmentClass.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "Fragment don't have default constructor", e);
            return (F) new ErrorFragment();
        }
        return (F) fragment;
    }

    public FragmentBuilder putArg(String key, Object value) {
        ClassType type = ClassType.cast(value);
        ClassType baseType = type;
        boolean arrayFlag = false;
        boolean listFlag = false;
        if (type == ClassType.ARRAY) {
            arrayFlag = true;
            type = ClassType.cast(value.getClass().getComponentType());
        }
        if(type == ClassType.ARRAYLIST) {
            listFlag = true;
            ArrayList list = (ArrayList) value;
            type = ClassType.cast(list.toArray().getClass().getComponentType());
            if(type != ClassType.STRING || type != ClassType.CHARSEQUENCE) {
                type = ClassType.UNSUPPORTED;
            }
        }
        switch (type) {
            case PARCELABLE:
                if (arrayFlag) break;
                bundle.putParcelable(key, (Parcelable) value);
                return this;
            case CHARSEQUENCE:
                if(arrayFlag) bundle.putCharSequenceArray(key, (CharSequence[]) value);
                else if(listFlag) bundle.putCharSequenceArrayList(key, (ArrayList<CharSequence>) value);
                else bundle.putCharSequence(key, (CharSequence) value);
                return this;
            case BUNDLE:
                if (arrayFlag) break;
                bundle.putBundle(key, (Bundle) value);
                return this;
            case STRING:
                if (arrayFlag) bundle.putStringArray(key, (String[]) value);
                else if (listFlag) bundle.putStringArrayList(key, (ArrayList<String>) value);
                else bundle.putString(key, (String) value);
                return this;
            case CHAR:
                if(arrayFlag) bundle.putCharArray(key, (char[]) value);
                else bundle.putChar(key, (char) value);
                return this;
            case BYTE:
                if(arrayFlag) bundle.putByteArray(key, (byte[]) value);
                else bundle.putByte(key, (byte) value);
                return this;
            case BOOLEAN:
                if(arrayFlag) bundle.putBooleanArray(key, (boolean[]) value);
                else bundle.putBoolean(key, (boolean) value);
                return this;
            case SHORT:
                if(arrayFlag) bundle.putShortArray(key, (short[]) value);
                else bundle.putShort(key, (short) value);
                return this;
            case INTEGER:
                if(arrayFlag) bundle.putIntArray(key, (int[]) value);
                else bundle.putInt(key, (int) value);
                return this;
            case LONG:
                if(arrayFlag) bundle.putLongArray(key, (long[]) value);
                else bundle.putLong(key, (long) value);
                return this;
            case FLOAT:
                if(arrayFlag) bundle.putFloatArray(key, (float[]) value);
                else bundle.putFloat(key, (float) value);
                return this;
            case DOUBLE:
                if(arrayFlag) bundle.putDoubleArray(key, (double[]) value);
                else bundle.putDouble(key, (double) value);
                return this;
        }
        if(baseType == ClassType.SERIALIZABLE) {
            bundle.putSerializable(key, (Serializable) value);
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass().getSimpleName());
        }
        return this;
    }

    public FragmentBuilder putArgs(Map args) {
        args.putAll(args);
        return this;
    }

    public FragmentBuilder putArgs(Bundle args) {
        bundle.putAll(args);
        return this;
    }

    public FragmentBuilder addToBackStack() {
        toBackStack = true;
        return this;
    }

    public <F extends Fragment> F replaceFragment(@IdRes int container, Class<F> fragmentClass) {
        Fragment fr = manager.findFragmentByTag(fragmentClass.getSimpleName());
        FragmentTransaction transaction = manager.beginTransaction();
        if (fr == null) {
            fr = newFragment(fragmentClass);
            transaction.replace(container, fr, fragmentClass.getSimpleName());
        } else {
            fr.getArguments().putAll(bundle);
            transaction.replace(container, fr);
        }
        if(toBackStack) transaction.addToBackStack(fragmentClass.getSimpleName());
        transaction.commitAllowingStateLoss();
        return (F) fr;
    }

    public <F extends Fragment> F replaceFragment(@IdRes int container, Fragment fragment) {
        FragmentTransaction transaction = manager.beginTransaction();
        fragment.getArguments().putAll(bundle);
        transaction.replace(container, fragment);
        if (toBackStack) transaction.addToBackStack(fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();
        return (F) fragment;
    }

    public <F extends Fragment> F replaceFragment(Fragment fragment, Class<F> fragmentClass) {
        return replaceFragment(fragment.getId(), fragmentClass);
    }

    public <F extends Fragment> F replaceFragment(Fragment fragment, Fragment newFragment) {
        return replaceFragment(fragment.getId(), newFragment);
    }

    //TODO: Caution!!!!!  work only one time
    public <F extends Fragment> F replaceFragment(View placeHolder, Class<F> fragmentClass) {
        //TODO: generate unique id
        if (placeHolder.getId() == View.NO_ID) {
            placeHolder.setId(fragmentClass.getSimpleName().hashCode() + placeHolder.hashCode());
        }
        return replaceFragment(placeHolder.getId(), fragmentClass);
    }

    public <F extends Fragment> F newFragment(Class<F> fragmentClass) {
        return newInstance(fragmentClass, bundle);
    }
}

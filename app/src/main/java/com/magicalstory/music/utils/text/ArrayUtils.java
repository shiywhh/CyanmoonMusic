package com.magicalstory.music.utils.text;

import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 数组工具类
 * @author Magic_Grass
 * @create 2022/1/16 17:35
 */
public class ArrayUtils {
    static final String TAG = "ArrayUtils";

    //使用Equals判断SparseArray中Value的下标
    static public <E> int indexOfValue(SparseArray<E> sparseArray, E value) {
        if (sparseArray != null) {
            int size = sparseArray.size();
            for (int i = 0; i < size; i++) {
                if (Objects.equals(sparseArray.valueAt(i), value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    //判断两个集合是否相等
    static public boolean listEquals(List<?> list1, List<?> list2) {
        if (isEmpty(list1) && isEmpty(list2)) {
            return true;
        } else if (!isEmpty(list1) && !isEmpty(list2)) {
            Collections.sort(list1, (o1, o2) -> o1.hashCode() - o2.hashCode());
            Collections.sort(list2, (o1, o2) -> o1.hashCode() - o2.hashCode());
            return list1.toString().equals(list2.toString());
        } else {
            return false;
        }
    }

    //region 判断数组是否为空

    static public boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    static public boolean isEmpty(double[] array) {
        return array == null || array.length == 0;
    }

    static public boolean isEmpty(float[] array) {
        return array == null || array.length == 0;
    }

    static public boolean isEmpty(byte[] array) {
        return array == null || array.length == 0;
    }

    static public boolean isEmpty(boolean[] array) {
        return array == null || array.length == 0;
    }

    static public boolean isEmpty(long[] array) {
        return array == null || array.length == 0;
    }

    static public boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    //endregion

    //region 确保数组长度

    static public boolean ensureMinLength(int[] array, int minLength) {
        return array != null && array.length >= minLength;
    }

    static public boolean ensureMinLength(double[] array, int minLength) {
        return array != null && array.length >= minLength;
    }

    static public boolean ensureMinLength(float[] array, int minLength) {
        return array != null && array.length >= minLength;
    }

    static public boolean ensureMinLength(byte[] array, int minLength) {
        return array != null && array.length >= minLength;
    }

    static public boolean ensureMinLength(boolean[] array, int minLength) {
        return array != null && array.length >= minLength;
    }

    static public boolean ensureMinLength(long[] array, int minLength) {
        return array != null && array.length >= minLength;
    }

    static public boolean ensureMinLength(Object[] array, int minLength) {
        return array != null && array.length >= minLength;
    }

    //endregion

    //判断集合是否为空
    static public boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    //获取集合长度
    static public int sizeOf(Collection collection) {
        return collection == null ? 0 : collection.size();
    }

    //移除空元素
    static public boolean removeNull(Collection collection) {
        if (isEmpty(collection)) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return collection.removeIf(o -> o == null);
        } else {
            boolean result = false;
            Iterator iterator = collection.iterator();
            while (iterator.hasNext()) {
                if (iterator.next() == null) {
                    iterator.remove();
                    result = true;
                }
            }
            return result;
        }
    }

    //String数组转String带分隔符division
    static public String StringArrayToStringWithDiv(String[] array, String division) {
        return StringArrayToStringWithDiv(array, division, false);
    }

    //String数组转String带分隔符division
    static public String StringArrayToStringWithDiv(String[] array, String division, boolean skipEmptyStr) {
        return StringArrayToStringWithDiv(array, division, null, skipEmptyStr);
    }

    //String数组转String带分隔符division
    static public String StringArrayToStringWithDiv(String[] array, String division, String wrapStr, boolean skipEmptyStr) {
        StringBuilder temp = new StringBuilder();
        for (String str : array) {
            if (str == null) {
                continue;
            }
            if (TextUtils.isEmpty(str) && skipEmptyStr) {
                continue;
            }
            if (TextUtils.isEmpty(wrapStr)) {
                temp.append(str).append(division);
            } else {
                temp.append(wrapStr).append(str).append(wrapStr).append(division);
            }

        }
        //去掉最后的分隔符
        if (temp.length() > 0) {
            temp.delete(temp.length() - division.length(), temp.length());
        }
        return temp.toString();
    }

    //boolean数组转String带分隔符 division
    static public String BooleanArrayToStringWithDiv(boolean[] array, String division) {
        StringBuilder temp = new StringBuilder();
        for (boolean aBoolean : array) {
            temp.append(String.valueOf(aBoolean)).append(division);
        }
        //去掉最后的分隔符
        if (temp.length() > 0) {
            temp.delete(temp.length() - division.length(), temp.length());
        }
        return temp.toString();
    }

    //带分隔符String转boolean数组
    static public boolean[] StringToBooleanArray(String string, String division) {
        String[] strings = string.split(division);
        boolean[] arr = new boolean[strings.length];
        for (int i = 0; i < strings.length; i++) {
            arr[i] = Boolean.parseBoolean(strings[i]);
        }
        return arr;
    }

    //int数组转String带分隔符division
    static public String IntArrayToStringWithDiv(int[] array, String division) {
        StringBuilder temp = new StringBuilder();
        for (int aInt : array) {
            temp.append(aInt).append(division);
        }
        //去掉最后的分隔符
        if (temp.length() > 0) {
            temp.delete(temp.length() - division.length(), temp.length());
        }
        return temp.toString();
    }

    //Integer数组转String带分隔符division
    static public String IntegerArrayToStringWithDiv(Integer[] array, String division) {
        StringBuilder temp = new StringBuilder();
        for (Integer integer : array) {
            temp.append(integer.intValue()).append(division);
        }
        //去掉最后的分隔符
        if (temp.length() > 0) {
            temp.delete(temp.length() - division.length(), temp.length());
        }
        return temp.toString();
    }

    //int数组转String带分隔符division
    static public String IntListToStringWithDiv(List<Integer> list, String division) {
        StringBuilder temp = new StringBuilder();
        for (int aInt : list) {
            temp.append(aInt).append(division);
        }
        //去掉最后的分隔符
        if (temp.length() > 0) {
            temp.delete(temp.length() - division.length(), temp.length());
        }
        return temp.toString();
    }

    //带分隔符String转int数组
    static public int[] StringToIntArray(String string, String division) {
        if (TextUtils.isEmpty(string)) {
            return new int[0];
        }
        String[] strings = string.split(division);
        int[] arr = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            arr[i] = Integer.parseInt(strings[i]);
        }
        return arr;
    }

    //带分隔符String转int集合
    static public List<Integer> StringToIntList(String string, String division) {
        if (TextUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        String[] strings = string.split(division);
        List<Integer> list = new ArrayList<>(strings.length);
        for (String s : strings) {
            list.add(Integer.parseInt(s));
        }
        return list;
    }

    //long数组转String带分隔符division
    static public String LongArrayToStringWithDiv(long[] array, String division) {
        StringBuilder temp = new StringBuilder();
        for (long l : array) {
            temp.append(l).append(division);
        }
        //去掉最后的分隔符
        if (temp.length() > 0) {
            temp.delete(temp.length() - division.length(), temp.length());
        }
        return temp.toString();
    }

    //long列表转String带分隔符division
    static public String LongListToStringWithDiv(List<Long> list, String division, boolean skipNull) {
        StringBuilder temp = new StringBuilder();
        if (!ArrayUtils.isEmpty(list)) {
            for (Long l : list) {
                if (l == null && skipNull) {
                    continue;
                }
                temp.append(l).append(division);
            }
            //去掉最后的分隔符
            if (temp.length() > 0) {
                temp.delete(temp.length() - division.length(), temp.length());
            }
        }
        return temp.toString();
    }

    //带分隔符String转long数组
    static public long[] StringToLongArray(String string, String division) {
        if (TextUtils.isEmpty(string)) {
            return new long[0];
        }
        String[] strings = string.split(division);
        long[] arr = new long[strings.length];
        for (int i = 0; i < strings.length; i++) {
            arr[i] = Long.parseLong(strings[i]);
        }
        return arr;
    }

    //带分隔符String转long集合
    static public List<Long> StringToLongList(String string, String division) {
        if (TextUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        String[] strings = string.split(division);
        List<Long> list = new ArrayList<>();
        for (String s : strings) {
            list.add(Long.parseLong(s));
        }
        return list;
    }

    //Calendar集合转String带分隔符division
    static public String CalendarListToLongString(List<Calendar> list, String division) {
        if (isEmpty(list)) {
            return "";
        }
        StringBuilder temp = new StringBuilder();
        for (Calendar calendar : list) {
            temp.append(calendar.getTimeInMillis()).append(division);
        }
        //去掉最后的分隔符
        if (temp.length() > 0) {
            temp.delete(temp.length() - division.length(), temp.length());
        }
        return temp.toString();
    }

    //带分隔符的String转Calendar集合
    static public List<Calendar> LongStringToCalendarList(String string, String division) {
        String[] strings = string.split(division);
        List<Calendar> list = new ArrayList<>();
        for (String str : strings) {
            list.add(DateUtil.getCalendar(Long.parseLong(str)));
        }
        return list;
    }

    //String数组转Color数组
    @ColorInt
    static public int[] StringArrayToColorArray(String[] colors) {
        if (colors == null) {
            return null;
        }
        int[] result = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            result[i] = Color.parseColor(colors[i]);
        }

        return result;
    }

    //Color数组转String数组
    static public String[] ColorArrayToStringArray(@ColorInt int[] colors) {
        if (colors == null) {
            return null;
        }
        String[] result = new String[colors.length];
        for (int i = 0; i < colors.length; i++) {
            result[i] = String.format("#%08x", colors[i]);
        }

        return result;
    }

    //判断两个集合是否相同
    static public boolean equals(Collection collection1, Collection collection2) {
        if (collection1 != null && collection2 != null) {
            if (collection1.size() != collection2.size()) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return collection1.stream().sorted().collect(Collectors.joining())
                        .equals(collection2.stream().sorted().collect(Collectors.joining()));
            } else {
                return collection1.containsAll(collection2) && collection2.containsAll(collection1);
            }
        } else if (collection1 == null && collection2 == null) {
            return true;
        } else {
            return false;
        }
    }

    //Collection<Integer>转int数组
    static public int[] collIntegerToIntArr(Collection<Integer> collection) {
        if (collection != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return collection.stream().mapToInt(Integer::intValue).toArray();
            } else {
                int[] result = new int[sizeOf(collection)];
                int i = 0;
                for (Integer integer : collection) {
                    result[i++] = integer;
                }
                return result;
            }
        }
        return new int[0];
    }


}

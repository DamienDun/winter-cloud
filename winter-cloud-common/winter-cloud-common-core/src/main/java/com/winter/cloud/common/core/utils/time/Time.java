package com.winter.cloud.common.core.utils.time;


import com.winter.cloud.common.core.exception.FormatException;
import com.winter.cloud.common.core.exception.argument.ArgumentOverflowException;
import com.winter.cloud.common.core.utils.StringUtils;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 13:21
 */
public final class Time implements Comparable<Time>, Serializable {

    private static final long serialVersionUID = 9027751130704556149L;

    /**
     * 1秒的毫秒数
     */
    public static final long SECOND_SMILLISECOND = 1000L;
    /**
     * 1分钟的毫秒数
     */
    public static final long MINUTE_MILLISECOND = 60000L;
    /**
     * 1个小时的毫秒数
     */
    public static final long HOUR_MILLISECOND = 3600000L;
    /**
     * 1天的毫秒数
     */
    public static final long DAY_MILLISECOND = 86400000L;
    private static final int HOUR_MAX = 23;
    private static final int MINUTE_MAX = 59;
    private static final int SECOND_MAX = 59;
    private static final int MILLISECOND_MAX = 999;

    /**
     * Time 的最大值
     */
    public static final Time MAX_VALUE = new Time(DAY_MILLISECOND - 1L);
    /**
     * Time 的最小值
     */
    public static final Time MIN_VALUE = new Time(0L);

    private int hour = 0;
    private int minute = 0;
    private int second = 0;
    private int milliSecond = 0;
    private long totalMilliseconds = 0L;

    /**
     * 默认实例化
     */
    public Time() {
        this(0, 0, 0, 0);
    }

    /**
     * 实例化
     *
     * @param hour   小时
     * @param minute 分钟
     * @param second 秒钟
     */
    public Time(int hour, int minute, int second) {
        this(hour, minute, second, 0);
    }

    /**
     * 实例化
     *
     * @param hour        小时
     * @param minute      分钟
     * @param second      秒钟
     * @param milliSecond 毫秒
     */
    public Time(int hour, int minute, int second, int milliSecond) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setMilliSecond(milliSecond);
        calcTotalMilliseconds();
    }

    /**
     * 实例化
     *
     * @param totalMilliseconds 总毫秒
     */
    public Time(long totalMilliseconds) {
        if (totalMilliseconds < 0L || totalMilliseconds >= DAY_MILLISECOND) {
            throw new FormatException("totalMillis 必须介于 0 至 " + (DAY_MILLISECOND - 1) + " 之间。");
        }
        if (totalMilliseconds > 0) {
            long millis = totalMilliseconds;
            long v = HOUR_MILLISECOND;
            if (millis >= v) {
                long h = millis / v;
                setHour((int) h);
                millis -= h * v;
            }
            v = MINUTE_MILLISECOND;
            if (millis >= v) {
                long m = millis / v;
                setMinute((int) m);
                millis -= m * v;
            }
            v = SECOND_SMILLISECOND;
            if (millis >= v) {
                long s = millis / v;
                setSecond((int) s);
                millis -= s * v;
            }
            if (millis > 0) {
                setMilliSecond((int) millis);
            }
        }
    }

    private int getValue(int value, String name, int max, int min) {
        if (value > max) {
            throw new ArgumentOverflowException("name", name + " 大于 " + max);
        }
        if (value < min) {
            throw new ArgumentOverflowException("name", name + " 小于 " + min);
        }
        return value;
    }

    /**
     * 获取小时
     *
     * @return
     * @author
     */
    public int getHour() {
        return this.hour;
    }

    /**
     * 设置小时
     *
     * @param hour
     * @author
     */
    public void setHour(int hour) {
        this.hour = getValue(hour, "hour", HOUR_MAX, 0);
        calcTotalMilliseconds();
    }

    /**
     * 获取分钟
     *
     * @return
     * @author
     */
    public int getMinute() {
        return this.minute;
    }

    /**
     * 设置分钟
     *
     * @param minute
     * @author
     */
    public void setMinute(int minute) {
        this.minute = getValue(minute, "minute", MINUTE_MAX, 0);
        calcTotalMilliseconds();
    }

    /**
     * 获取秒钟
     *
     * @return
     * @author
     */
    public int getSecond() {
        return this.second;
    }

    /**
     * 设置秒钟
     *
     * @param second
     * @author
     */
    public void setSecond(int second) {
        this.second = getValue(second, "second", SECOND_MAX, 0);
        calcTotalMilliseconds();
    }

    /**
     * 获取毫秒
     *
     * @return
     * @author
     */
    public int getMilliSecond() {
        return this.milliSecond;
    }

    /**
     * 设置毫秒
     *
     * @param milliSecond
     * @author
     */
    public void setMilliSecond(int milliSecond) {
        this.milliSecond = getValue(milliSecond, "milliSecond", MILLISECOND_MAX, 0);
        calcTotalMilliseconds();
    }

    private void calcTotalMilliseconds() {
        this.totalMilliseconds = getMilliSecond() + (getSecond() * SECOND_SMILLISECOND)
                + (getMinute() * MINUTE_MILLISECOND) + (getHour() * HOUR_MILLISECOND);
    }

    /**
     * 获取总毫秒数
     *
     * @return
     * @author
     */
    public long getTotalMilliseconds() {
        return this.totalMilliseconds;
    }

    /**
     * 比较小大
     */
    @Override
    public int compareTo(Time other) {
        return Long.compare(getMillisOf(this), getMillisOf(other));
    }

    private long getMillisOf(Time time) {
        if (time == null) {
            return MIN_VALUE.getTotalMilliseconds();
        }
        return time.getTotalMilliseconds();
    }

    /**
     * @param value
     * @return
     * @author
     */
    private String getString(int value) {
        return StringUtils.padLeft(Long.toString(value), 2, '0');
    }

    /**
     * 希哈代码
     */
    @Override
    public int hashCode() {
        long ht = getTotalMilliseconds();
        return (int) (ht ^ (int) (ht >> 32));
    }

    /**
     * 输出
     */
    @Override
    public final String toString() {
        StringBuilder format = new StringBuilder();
        format.append(getString(getHour()));
        format.append(":");
        format.append(getString(getMinute()));
        format.append(":");
        format.append(getString(getSecond()));
        if (getMilliSecond() > 0) {
            format.append(".");
            format.append(getMilliSecond());
        }
        return format.toString();
    }

    /**
     * 比较是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || (!(obj instanceof Time))) {
            return false;
        }
        Time time = (Time) obj;
        return getTotalMilliseconds() == time.getTotalMilliseconds();
    }

    /**
     * 获取当前时间
     *
     * @return
     * @author
     */
    public static Time currentTime() {
        Calendar cal = Calendar.getInstance();
        Time time = new Time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND));
        return time;
    }

    /**
     * @param containMillis
     * @return
     * @author
     */
    public static Time currentTime(boolean containMillis) {
        Calendar cal = Calendar.getInstance();
        int millis;
        if (containMillis) {
            millis = cal.get(Calendar.MILLISECOND);
        } else {
            millis = 0;
        }
        return new Time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), millis);
    }

    /**
     * 读取当前日期
     *
     * @param date
     * @return
     * @author
     */
    public static Time readTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return new Time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND));
    }

    private final static int TIME_FORMAT_LENGHT = 2;

    /**
     * 解析
     *
     * @param timeString 时间字符
     * @return
     * @author
     */
    public static Time parse(String timeString) {
        try {
            if (timeString == null) {
                return null;
            }
            String[] a = timeString.split("\\.");
            if ((a.length == 0) || (a.length > TIME_FORMAT_LENGHT)) {
                throw new FormatException(timeString + " 不是有效的时间格式");
            }
            String[] hms = a[0].split(":");
            if (hms.length != TIME_FORMAT_LENGHT + 1) {
                throw new FormatException(timeString + " 不是有效的时间格式");
            }
            int hour = Integer.parseInt(hms[0].trim());
            int minute = Integer.parseInt(hms[1].trim());
            int second = Integer.parseInt(hms[2].trim());
            int milliSecond;
            if (a.length == TIME_FORMAT_LENGHT + 1) {
                milliSecond = Integer.parseInt(a[1].trim());
            } else {
                milliSecond = 0;
            }
            return new Time(hour, minute, second, milliSecond);
        } catch (NumberFormatException e) {
            throw new FormatException(timeString + " 不是有效的时间格式");
        }
    }

    /**
     * 是否是日期时间格式
     *
     * @param timeString
     * @return
     * @author
     */
    public static boolean isTimeFormat(String timeString) {
        try {
            return parse(timeString) != null;
        } catch (Exception e) {
        }
        return false;
    }
}

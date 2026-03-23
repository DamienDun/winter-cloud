package com.winter.cloud.common.core.utils.time;

import com.winter.cloud.common.core.exception.FormatException;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.tuple.TupleTwo;

import java.io.Serializable;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 13:45
 */
public final class TimeSpan implements Comparable<TimeSpan>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5130763819438946426L;

    /**
     * TimeSpan 的最大值
     */
    public static final TimeSpan MAX_VALUE = new TimeSpan(Long.MAX_VALUE);
    /**
     * TimeSpan 的最小值
     */
    public static final TimeSpan MIN_VALUE = new TimeSpan(Long.MIN_VALUE + 1);

    private long days;
    private int hours;
    private int minutes;
    private int seconds;
    private int milliSeconds;
    private final long totalMilliseconds;

    private TupleTwo<Long, Long> calcSection(long millis, long section, int symbol) {
        if (millis >= section) {
            long value = millis / section;
            return new TupleTwo<>(value * symbol, millis - (value * section));
        }
        return new TupleTwo<>(0L, millis);
    }

    /**
     * 实例化
     */
    public TimeSpan() {
        this(0L);
    }

    /**
     * 实例化
     *
     * @param totalMilliseconds 总毫秒数
     */
    public TimeSpan(long totalMilliseconds) {
        this.totalMilliseconds = totalMilliseconds;
        int symbol = 1;
        if (totalMilliseconds < 0) {
            symbol = -1;
        }
        long millis = Math.abs(totalMilliseconds);
        TupleTwo<Long, Long> tuple;

        tuple = calcSection(millis, Time.DAY_MILLISECOND, symbol);
        this.days = tuple.getItem1();
        millis = tuple.getItem2();

        tuple = calcSection(millis, Time.HOUR_MILLISECOND, symbol);
        this.hours = tuple.getItem1().intValue();
        millis = tuple.getItem2();

        tuple = calcSection(millis, Time.MINUTE_MILLISECOND, symbol);
        this.minutes = tuple.getItem1().intValue();
        millis = tuple.getItem2();

        tuple = calcSection(millis, Time.SECOND_SMILLISECOND, symbol);
        this.seconds = tuple.getItem1().intValue();
        millis = tuple.getItem2();

        this.milliSeconds = (int) millis * symbol;

    }

    /**
     * 实例化
     *
     * @param days         天
     * @param hours        小时
     * @param minutes      分钟
     * @param seconds      秒钟
     * @param milliseconds 毫秒
     */
    public TimeSpan(int days, int hours, int minutes, int seconds, int milliseconds) {
        this((days * Time.DAY_MILLISECOND) + (hours * Time.HOUR_MILLISECOND) + (minutes * Time.MINUTE_MILLISECOND)
                + (seconds + Time.SECOND_SMILLISECOND) + milliseconds);
    }

    /**
     * 实例化
     *
     * @param days    天
     * @param hours   小时
     * @param minutes 分钟
     * @param seconds 秒钟
     */
    public TimeSpan(int days, int hours, int minutes, int seconds) {
        this(days, hours, minutes, seconds, 0);
    }

    /**
     * 实例
     *
     * @param hours   小时
     * @param minutes 分钟
     * @param seconds 秒钟
     */
    public TimeSpan(int hours, int minutes, int seconds) {
        this(0, hours, minutes, seconds, 0);
    }

    private double calcTotal(long section) {
        return (double) this.totalMilliseconds / (double) section;
    }

    private Double totalDays = null;

    /**
     * 获取按天计算的总天数
     *
     * @return
     * @author
     */
    public double getTotalDays() {
        if (totalDays == null) {
            totalDays = calcTotal(Time.DAY_MILLISECOND);
        }
        return totalDays;
    }

    private Double totalHours = null;

    /**
     * 获取按小时计算的总小时数
     *
     * @return
     * @author
     */
    public double getTotalHours() {
        if (totalHours == null) {
            totalHours = calcTotal(Time.HOUR_MILLISECOND);
        }
        return totalHours;
    }

    private Double totalMinutes = null;

    /**
     * 获取按分钟计算的总分钟数
     *
     * @return
     * @author
     */
    public double getTotalMinutes() {
        if (totalMinutes == null) {
            totalMinutes = calcTotal(Time.MINUTE_MILLISECOND);
        }
        return totalMinutes;
    }

    private Double totalSeconds = null;

    /**
     * 获取按秒钟计算的总秒钟数
     *
     * @return
     * @author
     */
    public double getTotalSeconds() {
        if (totalSeconds == null) {
            totalSeconds = calcTotal(Time.SECOND_SMILLISECOND);
        }
        return totalSeconds;
    }

    /**
     * 获取天数部份
     *
     * @return
     * @author
     */
    public long getDays() {
        return days;
    }

    /**
     * 获取小时数部份
     *
     * @return
     * @author
     */
    public int getHours() {
        return hours;
    }

    /**
     * 获取分钟数部份
     *
     * @return
     * @author
     */
    public int getMinutes() {
        return minutes;
    }

    /**
     * 获取秒数部份
     *
     * @return
     * @author
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * 获取毫秒数部份
     *
     * @return
     * @author
     */
    public int getMilliSeconds() {
        return milliSeconds;
    }

    /**
     * 获取总毫秒数
     *
     * @return
     * @author
     */
    public long getTotalMilliseconds() {
        return totalMilliseconds;
    }

    /**
     * @param other
     * @return
     * @author
     */
    private long getMillisOf(TimeSpan other) {
        if (other == null) {
            return MIN_VALUE.getTotalMilliseconds();
        }
        return other.getTotalMilliseconds();
    }

    @Override
    public int compareTo(TimeSpan other) {
        return Long.compare(getMillisOf(this), getMillisOf(other));
    }

    /**
     * @param value
     * @return
     * @author
     */
    private String getString(int value) {
        return StringUtils.padLeft(Long.toString(value), 2, '0');
    }

    private final static int TIME_DAY_LENGTH = 2;

    /**
     * 解析
     *
     * @param timeSpanString 时间字符
     * @return
     * @author
     */
    public static TimeSpan parse(String timeSpanString) {
        try {
            if (timeSpanString == null) {
                return null;
            }
            String[] days = timeSpanString.split("\\-");
            if ((days.length == 0) || (days.length > TIME_DAY_LENGTH)) {
                throw new FormatException(timeSpanString + " 不是有效的TimeSpan格式");
            }
            int day;
            String timeString;
            if (days.length == 1) {
                day = 0;
                timeString = days[0];
            } else {
                day = Integer.parseInt(days[0].trim());
                timeString = days[1];
            }
            String[] a = timeString.split("\\.");
            if ((a.length == 0) || (a.length > TIME_DAY_LENGTH)) {
                throw new FormatException(timeSpanString + " 不是有效的TimeSpan格式");
            }
            String[] hms = a[0].split(":");
            if (hms.length != TIME_DAY_LENGTH + 1) {
                throw new FormatException(timeSpanString + " 不是有效的TimeSpan格式");
            }
            int hour = Integer.parseInt(hms[0].trim());
            int minute = Integer.parseInt(hms[1].trim());
            int second = Integer.parseInt(hms[2].trim());
            int milliSecond;
            if (a.length == TIME_DAY_LENGTH) {
                milliSecond = Integer.parseInt(a[1].trim());
            } else {
                milliSecond = 0;
            }
            return new TimeSpan(day, hour, minute, second, milliSecond);
        } catch (NumberFormatException e) {
            throw new FormatException(timeSpanString + " 不是有效的TimeSpan格式");
        }
    }

    /**
     * 输出
     */
    @Override
    public final String toString() {
        StringBuilder format = new StringBuilder();
        if (getDays() != 0) {
            format.append(getDays());
            format.append("-");
        }
        format.append(getString(getHours()));
        format.append(":");
        format.append(getString(getMinutes()));
        format.append(":");
        format.append(getString(getSeconds()));
        if (getMilliSeconds() != 0) {
            format.append(".");
            format.append(getMilliSeconds());
        }
        return format.toString();
    }
}

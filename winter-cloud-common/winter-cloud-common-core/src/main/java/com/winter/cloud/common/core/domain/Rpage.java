package com.winter.cloud.common.core.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.winter.cloud.common.core.constant.HttpStatus;

import java.io.Serializable;
import java.util.List;

/**
 * General page response.
 */
public class Rpage<T> implements Serializable {

    private static final long serialVersionUID = -2974591626148191163L;

    private int total;
    private List<T> rows;
    private int code;
    private String msg;

    public Rpage() {
    }

    public Rpage(List<T> rows, int total, int code, String msg) {
        this.total = total;
        this.rows = rows;
        this.code = code;
        this.msg = msg;
    }

    public static <T> Rpage<T> ok(IPage<T> page) {
        return new Rpage<>(page.getRecords(), Long.valueOf(page.getTotal()).intValue(), HttpStatus.SUCCESS, MSG_OK);
    }

    public static <T> Rpage<T> ok(List<T> rows, int total) {
        return new Rpage<>(rows, total, HttpStatus.SUCCESS, MSG_OK);
    }

    private static final String MSG_OK = "操作成功";

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<T> getRows() { return rows; }
    public void setRows(List<T> rows) { this.rows = rows; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
}

package edu.unc.borst.finalproject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kyleborst on 10/23/17.
 */

@SuppressWarnings("serial")
public class ToDoItem implements Serializable {

    private int id;
    private int priority;
    private String description;
    private String address;
    private LatLng latLng;
    private Date started;
    private Date due;
    private int radius;
    private int color;
    private String borderColor;
    private boolean completed;

    public ToDoItem(int id, int priority, String description, String address, LatLng latLng,
                    Date started, Date due, Integer completed, SQLiteDatabase db) {
        this.id = id;
        this.priority = priority;
        this.description = description;
        this.address = address;
        this.latLng = latLng;
        this.started = started;
        this.due = due;

        if (completed == 0) {
            this.completed = false;
        } else if (completed == 1) this.completed = true;
        if (priority < 5) {
            updatePriority(db);
        } else if (priority >= 5) {
            setPriorityItems(5);
        }
    }

    public int getColor() {
        return color;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public Date getDue() {
        return due;
    }

    public int getRadius() {
        return radius;
    }

    public Date getStarted() {
        return started;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public boolean getStatus() {
        return completed;
    }

    public int getId() {
        return id;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    private void setPriorityItems(int priority) {
        if (priority == 1) {
            radius = 200;
            color = 0x3300FF00;
            borderColor = "#00FF00";
        } else if (priority == 2) {
            radius = 955;
            color = 0x3399ff00;
            borderColor = "#99ff00";
        } else if (priority == 3) {
            radius = 1710;
            color = 0x33FFFF00;
            borderColor = "#FFFF00";
        } else if (priority == 4) {
            radius = 2464;
            color = 0x33ff6600;
            borderColor = "#ff6600";
        } else if (priority == 5) {
            radius = 3219;
            color = 0x33FF0000;
            borderColor = "#FF0000";
        }
    }

    private void updatePriority(SQLiteDatabase db) {
        Date curDate;
        int intervals = 6 - priority;
        Cursor c = db.rawQuery("SELECT date('now','localtime')", null);
        c.moveToFirst();
        String curStr = c.getString(0);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            curDate = formatter.parse(curStr);
            if (curDate.getTime() > due.getTime()) {
                priority = 5;
            } else if ((curDate.getTime() - started.getTime()) > ((due.getTime() - started.getTime()) / intervals)) {
                ++priority;
            }
        } catch (ParseException p) {
        }
        db.execSQL("UPDATE TasksTable SET Priority = " + priority + " WHERE ID = " + id);
        setPriorityItems(priority);
    }
}


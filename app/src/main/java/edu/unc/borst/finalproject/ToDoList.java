package edu.unc.borst.finalproject;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;

import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ToDoList extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    SQLiteDatabase db;
    ArrayList<ToDoItem> toDoList = new ArrayList<>();
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_do_list);
        db = openOrCreateDatabase("Tasksdb", MODE_PRIVATE, null);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.view_task_list);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.view_map:
                        Intent intent1 = new Intent(getApplicationContext(), MapsActivity.class);
                        startActivity(intent1);
                        break;

                    case R.id.add_task:
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        break;

                    case R.id.view_task_list:
                        break;
                }
                return true;
            }
        });

        Cursor c = db.rawQuery("SELECT * FROM TasksTable", null);

        c.moveToFirst();
        Date started = null;
        Date due = null;
        LatLng latLng;
        if ((c != null) && (c.getCount() > 0)) {
            try {
                started = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(6));
                due = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(7));
            } catch (ParseException p) {
            }
            latLng = new LatLng(c.getDouble(4), c.getDouble(5));

            ToDoItem tdi = new ToDoItem(c.getInt(0), c.getInt(1), c.getString(2), c.getString(3), latLng, started, due, c.getInt(8), db);
            toDoList.add(tdi);

            while (c.moveToNext()) {
                try {
                    started = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(6));
                    due = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(7));
                } catch (ParseException p) {
                }

                latLng = new LatLng(c.getDouble(4), c.getDouble(5));

                tdi = new ToDoItem(c.getInt(0), c.getInt(1), c.getString(2), c.getString(3), latLng,
                        started, due, c.getInt(8), db);
                toDoList.add(tdi);
            }
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ToDoAdapter adapter = new ToDoAdapter(this, R.layout.to_do_item, toDoList, db);
        recyclerView.setAdapter(adapter);
    }
}

package edu.unc.borst.finalproject;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    SQLiteDatabase db;
    int priority;
    int year;
    int month;
    int day;
    int dueYear;
    int dueMonth;
    int dueDay;
    String description;
    String due;
    String started;
    String address;
    double lat;
    double lng;
    private Calendar calendar;
    private TextView dateView;
    private final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 100;
    EditText textField;
    RadioGroup radioGroup;
    Place place;
    PlaceAutocompleteFragment autocompleteFragment;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = openOrCreateDatabase("Tasksdb", MODE_PRIVATE, null);

        dateView = (TextView) findViewById(R.id.due_date_view);
        textField = (EditText) findViewById(R.id.editText);
        radioGroup = (RadioGroup) findViewById(R.id.RadioGroup);
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        showDate(year, month + 1, day);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.add_task);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.view_map:
                        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                        break;

                    case R.id.add_task:
                        textField.setText("");
                        autocompleteFragment.setText("");
                        dateView.setText("");
                        radioGroup.check(R.id.one);
                        break;

                    case R.id.view_task_list:
                        startActivity(new Intent(getApplicationContext(), ToDoList.class));
                        break;
                }
                return true;
            }
        });

        //db.execSQL("DROP TABLE IF EXISTS TasksTable");
        db.execSQL("CREATE TABLE IF NOT EXISTS TasksTable (ID INTEGER PRIMARY KEY, Priority INTEGER, " +
                "Description TEXT, Address TEXT, Lat REAL, Lng REAL, Started TEXT, Due TEXT, Completed INTEGER);");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place placeSelected) {
                place = placeSelected;
                address = place.getAddress().toString();
                lat = place.getLatLng().latitude;
                lng = place.getLatLng().longitude;

            }

            @Override
            public void onError(Status status) {
                Log.v("TAG", "An error occurred: " + status);
            }
        });

        autocompleteFragment.setBoundsBias(new LatLngBounds(
                new LatLng(35.879, -79.1),
                new LatLng(36.02911, -78.8618)));

    }

    public void setDate(View view) {
        showDialog(999);
    }

    private void showDate(int year, int month, int day) {
        dateView.setText(new StringBuilder().append(month).append("/")
                .append(day).append("/").append(year));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 999) {
            return new DatePickerDialog(this, dateListener, year, month, day);
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener dateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker arg0, int year, int month, int day) {
            showDate(year, month + 1, day);
            dueYear = year;
            dueMonth = month + 1;
            dueDay = day;
        }
    };


    public void Add(View v) {
        description = textField.getText().toString().trim();
        due = dueYear + "-" + dueMonth + "-" + dueDay;

        Cursor c = db.rawQuery("SELECT date('now','localtime')", null);
        c.moveToFirst();
        started = c.getString(0);
        try {
            if (new SimpleDateFormat("yyyy-MM-dd").parse(started).getTime() > new SimpleDateFormat("yyyy-MM-dd").parse(due).getTime()) {
                Toast.makeText(this, "Due Date Must Be After Today's Date", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException p) {
        }
        if (address == null && !description.equals("")) {
            Toast.makeText(this, "Please Enter an Address", Toast.LENGTH_SHORT).show();
        } else if (address != null && description.equals("")) {
            Toast.makeText(this, "Please Enter a Description", Toast.LENGTH_SHORT).show();
        } else if (address == null && description.equals("")) {
            Toast.makeText(this, "Please Enter an Address and Description", Toast.LENGTH_SHORT).show();
        } else {
            int selected = radioGroup.getCheckedRadioButtonId();
            if (selected == R.id.one) {
                priority = 1;
            }
            if (selected == R.id.two) {
                priority = 2;
            }
            if (selected == R.id.three) {
                priority = 3;
            }
            if (selected == R.id.four) {
                priority = 4;
            }
            if (selected == R.id.five) {
                priority = 5;
            }


            db.execSQL("INSERT INTO TasksTable(Priority, Description, Address, Lat, Lng, " +
                    "Started, Due, Completed) VALUES ( "
                    + priority + ", '"
                    + description + "', '"
                    + address + "', '"
                    + lat + "', '"
                    + lng + "', '"
                    + started + "', '"
                    + due + "', "
                    + 0 + ")");
            textField.setText("");
            autocompleteFragment.setText("");
            showDate(year, month + 1, day);
            radioGroup.check(R.id.one);

            Toast.makeText(this, "Task Added", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onClick(View view) {
    }


    private void callPlaceAutocompleteActivityIntent() {
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
        }
    }
}
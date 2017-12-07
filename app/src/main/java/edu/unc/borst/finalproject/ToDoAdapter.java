package edu.unc.borst.finalproject;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.ToDoHolder> {

    private final List<ToDoItem> toDoList;
    private Context context;
    private int itemResource;
    SQLiteDatabase db;

    public ToDoAdapter(Context context, int itemResource, List<ToDoItem> toDoList, SQLiteDatabase db) {
        this.db = db;
        this.context = context;
        this.itemResource = itemResource;
        this.toDoList = toDoList;
    }

    @Override
    public ToDoHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(this.itemResource, parent, false);
        return new ToDoHolder(this.context, view);
    }

    @Override
    public void onBindViewHolder(ToDoHolder holder, int position) {
        ToDoItem toDoItem = this.toDoList.get(position);

        holder.bindToDoItem(toDoItem);

    }

    @Override
    public int getItemCount() {
        return this.toDoList.size();
    }

    public class ToDoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ImageView statusIconView;
        private final TextView addressView;
        private final TextView descriptionView;

        private ToDoItem toDoItem;
        private Context context;

        public ToDoHolder(Context context, View view) {
            super(view);
            this.context = context;

            this.descriptionView =
                    (TextView) view.findViewById(R.id.to_do_description);

            this.addressView =
                    (TextView) view.findViewById(R.id.to_do_address);

            this.statusIconView =
                    (ImageView) view.findViewById(R.id.status_icon);

            view.setOnClickListener(this);
        }

        public void bindToDoItem(ToDoItem toDoItem) {
            this.toDoItem = toDoItem;
            descriptionView.setText(toDoItem.getDescription());
            addressView.setText(toDoItem.getAddress());
            if (toDoItem.getStatus()) {
                this.itemView.setBackgroundColor(Color.parseColor("#e6effc"));
                this.statusIconView.setVisibility(View.VISIBLE);
            } else {
                this.itemView.setBackgroundColor(toDoItem.getColor());
            }

            }

        @Override
        public void onClick(View v) {
            if(this.toDoItem != null) {
                if (!this.toDoItem.getStatus()) {
                    ToDoItem tdi = this.toDoItem;
                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                    alert.setMessage(
                            tdi.getDescription()
                            + "\n" + tdi.getAddress()//.substring(0, tdi.getAddress().length() - 5)
                            + "\n"
                            + "\n" + "Date Added: " + df.format(tdi.getStarted())
                            + "\n" + "Date Due: " + df.format(tdi.getDue()));
                    alert.setCancelable(true);

                    alert.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });

                    AlertDialog alertDialog = alert.create();
                    alertDialog.show();
                } else {
                    removeAt(getAdapterPosition());
                    db.execSQL("DELETE FROM TasksTable WHERE ID = " + toDoItem.getId());
                }
            }
        }

        public void removeAt(int position) {
            toDoList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, toDoList.size());
        }
    }
}


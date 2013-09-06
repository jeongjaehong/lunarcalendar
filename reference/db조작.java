package neo.one;

 

import android.app.Activity;

import android.os.Bundle;

import android.view.View;

import android.widget.EditText;

import android.widget.Button;

import android.view.View.OnClickListener;

import android.content.Context;

import android.database.sqlite.SQLiteDatabase;

import android.database.SQLException;

import android.util.Log;

 

public class Test extends Activity implements OnClickListener {

 

       private Button insert;

       private Button update;

       private Button delete;

       private EditText editId;

       private EditText editName;

 

       private SQLiteDatabase db;

 

       private int DB_MODE = Context.MODE_PRIVATE;

       private String DB_NAME = "testdb";

       private String TABLE_NAME = "student";

 

       @Override

       protected void onCreate(Bundle icicle) {

             super.onCreate(icicle);

             setContentView(R.layout.main);

             db = null;

             insert = (Button) findViewById(R.id.Button01);

             update = (Button) findViewById(R.id.Button02);

             delete = (Button) findViewById(R.id.Button03);

             editId = (EditText)findViewById(R.id.EditText01);

             editName = (EditText)findViewById(R.id.EditText02);

             insert.setOnClickListener(this);

             update.setOnClickListener(this);

             delete.setOnClickListener(this);

 

             openDatabase();

       }

 

       private void openDatabase() {

             db = openOrCreateDatabase(DB_NAME, DB_MODE, null);

             createTable();

       }

 

       private void createTable() {

             String sql = "create table " + TABLE_NAME + " ("

                           + "id integer primary key autoincrement, "

                           + "name text not null);";

 

             try {

                    db.execSQL(sql);

             } catch (SQLException e) {

                    Log.e("ERROR", e.toString());

             }

       }

 

       public void onClick(View v) {

             String id = editId.getText().toString();

             String name = editName.getText().toString();

 

             if (v == insert) {

                    String sql = "insert into " + TABLE_NAME + " (name)"

                                 + " values('" + name + "');";

 

                    try {

                           db.execSQL(sql);

                    } catch (SQLException e) {

                           Log.e("ERROR", e.toString());

                    }

             } else if (v == update) {

                      String sql = "update " + TABLE_NAME + " set " + "name='" + name

                    + "' where id=" + id + ";";

 

                    try {

                           db.execSQL(sql);

                    } catch (SQLException e) {

                           Log.e("ERROR", e.toString());

                    }

             } else if (v == delete) {

                    String sql = "delete from " + TABLE_NAME + 

" where(id=" + id + ");";

 

                    try {

                           db.execSQL(sql);

                    } catch (SQLException e) {

                           Log.e("ERROR", e.toString());

                    }

             }

 

             editId.setText("");

             editName.setText("");

       }

}

 
 


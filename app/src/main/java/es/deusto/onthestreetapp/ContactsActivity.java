package es.deusto.onthestreetapp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import static es.deusto.onthestreetapp.ShowPlaceActivity.CONTACTS_PLACE_OBJ;
import static es.deusto.onthestreetapp.ShowPlaceActivity.MODIFIED;

public class ContactsActivity extends Activity {

    private boolean modified = false;
    private int numberContacts = 0;

    private static ArrayList<Contact> mContacts;
    private CustomAdapterContacts mContactsAdapter;
    private ListView listContacts;
    private SQLitePlaceHelper database;

    private Place place;

    private String lastContactname;
    private String lastContactnumber;

    private ActionMode mActionMode = null;
    private int longClickedPos = -1;

    public static String NUMBER_CONTACTS = "Number_contacts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        place = (Place) getIntent().getSerializableExtra(CONTACTS_PLACE_OBJ);
        numberContacts = getIntent().getIntExtra(NUMBER_CONTACTS, 0);

        listContacts=(ListView)findViewById(R.id.list_contacts);
        listContacts.setEmptyView(findViewById( R.id.empty_list_view ));

        database = new SQLitePlaceHelper(getApplicationContext());
        mContacts = database.getContacts(place);

        mContactsAdapter = new CustomAdapterContacts(mContacts, this);
        listContacts.setAdapter(mContactsAdapter);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Important: to select single mode
        listContacts.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listContacts.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            // Called when the user long-clicks an item on the list
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View row, int position, long rowid) {
                if (mActionMode != null) {
                    return false;
                }

                // Important: to marked the editing row as activated
                Log.i("Clickado", position+"");
                listContacts.setItemChecked(position, true);
                longClickedPos = position;
                row.setBackgroundColor(Color.rgb(118, 172, 0));

                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = listContacts.startActionMode(mActionModeCallback);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Cojo el identificador de la opción de menu
        int id = item.getItemId();
        if (id == R.id.add_contact) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, 1);
            return true;
        }
        else if(id == android.R.id.home){
            Intent intentResult = new Intent();
            intentResult.putExtra(MODIFIED, modified);
            intentResult.putExtra(NUMBER_CONTACTS, numberContacts);
            setResult(Activity.RESULT_OK, intentResult);
            finish();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Cursor cursor = getContentResolver().query(data.getData(), new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
                if(cursor.moveToNext()){
                    lastContactname = cursor.getString(0);
                    lastContactnumber = cursor.getString(1);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.add_comment);

                    final EditText input = new EditText(this);

                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);

                    builder.setPositiveButton(R.string.positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String desc = input.getText().toString();
                            desc = "Comment: "+ desc;
                            Contact temp = new Contact(lastContactname, place.getId(), desc, lastContactnumber);
                            mContacts.add(temp);
                            mContactsAdapter.notifyDataSetChanged();
                            database.addContact(temp);
                            modified = true;
                            numberContacts++;
                        }
                    });
                    builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String desc = input.getText().toString();
                            desc = "";
                            Contact temp = new Contact(lastContactname, place.getId(), desc, lastContactnumber);
                            mContacts.add(temp);
                            mContactsAdapter.notifyDataSetChanged();
                            database.addContact(temp);
                            modified = true;
                            numberContacts++;
                            dialog.cancel();
                        }
                    });
                    builder.show();
                    return;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contacts_action, menu);
            return true;
        }

        // Called when the user enters the action mode
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Disable the list to avoid selecting other elements while editing one
            listContacts.setEnabled(false);
            return true; // Return false if nothing is done
        }


        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.mnu_contacts_delete:
                    mode.finish();
                    new AlertDialog.Builder(ContactsActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.deleting_contact_title) + mContacts.get(longClickedPos).getName())
                            .setMessage(getString(R.string.deleting_contact_msg) +" "+  mContacts.get(longClickedPos).getName() + "?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Contact temp = mContacts.remove(longClickedPos);
                                    database.deleteContact(temp);
                                    numberContacts--;
                                    modified = true;
                                    mContactsAdapter.notifyDataSetChanged();
                                }

                            })
                            .setNegativeButton("No", null)
                            .show();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Re-enable the list after edition
            mActionMode = null;
            listContacts.getChildAt(longClickedPos).setBackgroundColor(Color.rgb(255,255,255));
            listContacts.setEnabled(true);
        }
    };
}

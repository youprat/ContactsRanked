package com.example.prat.contactsranked;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    final static int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    final static int MY_PERMISSIONS_REQUEST_WRITE_CONTACTS = 2;

    public static int page = 0;
    public static final int perPage = 20;
    LinkedHashMap<String, Integer> contactsRanked = new LinkedHashMap();
    LinkedHashMap<String, Integer> contacts = new LinkedHashMap();
    HashMap<String, String> contactNames = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handlePermissions();

        //deleteAllContacts();

        if(null == (contacts = getContacts())) {
            createContacts();
            contacts = getContacts();
        }
        retrieveFromPersistantStorage();
        updateView();
    }

    @Override
    protected void onStop(){
        super.onStop();

        storeToPersistantStorage();
    }

    protected void updateView(){
        contacts = sortByValue(contacts);
        contactsRanked = getNextNEntries(contacts,page,perPage);

        LinearLayout list = (LinearLayout) findViewById(R.id.container);
        list.removeAllViews();

        for(Object key: contactsRanked.keySet()){

            TextView textView = new TextView(this);
            textView.setBackgroundResource(R.drawable.rect_style_blue);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(10,10,10,10);
            textView.setLayoutParams(lp);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);

            final String ID = (String) key;
            String name = contactNames.get(ID);
            final int rank = contactsRanked.get(key);
            textView.append(name+" ("+rank+")");
            textView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    updateView(ID, rank);
                }
            });

            list.addView(textView);
        }
    }

    protected void updateView(String ID, int rank){
        contacts.put(ID, rank+1);

        Toast.makeText(getApplication().getBaseContext(), "Rank increased!", Toast.LENGTH_SHORT).show();

        updateView();
    }

    protected LinkedHashMap getContacts(){
        LinkedHashMap allContacts = null;

        Cursor cursor = MainActivity.this.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, //null,null,null,null);
                new String[] {ContactsContract.CommonDataKinds.Phone._ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI},
                ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '"
                        + ("1") + "'",
                null, null);

        if(cursor!=null) {
            if (cursor.getCount() > 0) {
                allContacts = new LinkedHashMap();
                while (cursor.moveToNext()) {
                    String id = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    allContacts.put(id, 0);
                    contactNames.put(id, name);
                }
            }

            cursor.close();
        }
        return allContacts;
    }

    static LinkedHashMap sortByValue(Map<String, Integer> map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue())
                        .compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        LinkedHashMap result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static LinkedHashMap getNextNEntries(LinkedHashMap<String, Integer> source, int page, int perPage) {
        int i = 0;
        LinkedHashMap target = new LinkedHashMap();
        for (Map.Entry<String, Integer> entry: source.entrySet()) {
            if(i < page*perPage) continue;
            if (i >= perPage) break;

            target.put(entry.getKey(), entry.getValue());
            i++;
        }
        return target;
    }

    protected void createContacts(){
        String[] displayName = new String[]{"Harry","Hermione","Ron","Ginny","George",
                                            "Fred","Lily","James","Arthur","Molly",
                                            "Albus","Severus","Hagrid","Snowy","Dobby",
                                            "Fleur","Luna", "Dudley","Petunia","Vernon"};

        String mobileNumber = "987654321";

        for(int i=0; i<displayName.length; i++) {
            String name = displayName[i];

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            if (name != null) {
                ops.add(ContentProviderOperation.newInsert(
                        ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(
                                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                name).build());
            }

            if (mobileNumber != null) {
                ops.add(ContentProviderOperation.
                        newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mobileNumber)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build());
            }

            try {
                getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void deleteAllContacts(){
        ContentResolver contentResolver = MainActivity.this.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
            contentResolver.delete(uri, null, null);
        }
    }

    @TargetApi(23)
    protected void handlePermissions(){
        if ((int) Build.VERSION.SDK_INT >= 23) {

            String permission = Manifest.permission.READ_CONTACTS;
            if (!(ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED)){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }

            if (!(ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED)){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_CONTACTS},
                        MY_PERMISSIONS_REQUEST_WRITE_CONTACTS);
            }
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(getApplication().getBaseContext(), "No permission to read contacts!", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
            case MY_PERMISSIONS_REQUEST_WRITE_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(getApplication().getBaseContext(), "No permission to write contacts!", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    protected void storeToPersistantStorage(){
        SharedPreferences storage = MainActivity.this.getSharedPreferences("MyPreferences",MODE_PRIVATE);
        SharedPreferences.Editor editor = storage.edit();

        for(Map.Entry<String, Integer> contact: contacts.entrySet()) {
            editor.putInt(contact.getKey(), contact.getValue());
        }

        editor.commit();
    }

    protected void retrieveFromPersistantStorage(){
        SharedPreferences storage = MainActivity.this.getSharedPreferences("MyPreferences",MODE_PRIVATE);

        for(Map.Entry<String, Integer> contact: contacts.entrySet()) {
            int rank = storage.getInt(contact.getKey(), 0);
            contacts.put(contact.getKey(),rank);
        }
    }
}

package com.layer.atlas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.messenger.App101;
import com.layer.atlas.messenger.App101.Contact;
import com.layer.atlas.messenger.R;
import com.layer.sdk.internal.utils.Log;

/**
 * @author Oleg Orlov
 * @since 27 Apr 2015
 */
public class ParticipantPicker {
    
    private static final String TAG = ParticipantPicker.class.getSimpleName();
    private static final boolean debug = true;
    
    // participants picker
    private View rootView;
    private EditText participantsFilter;
    private ListView participantsListView;
    private ViewGroup participantsNames;
     
    private ArrayList<Contact> selectedContacts = new ArrayList<App101.Contact>();
    private TreeSet<String> skipUserIds = new TreeSet<String>();
    
    public ParticipantPicker(Context context, View rootView, final App101 app, String[] userIdToSkip) {
        
        if (userIdToSkip != null) skipUserIds.addAll(Arrays.asList(userIdToSkip));
        
        final Contact[] allContacts = app.contactsMap.values().toArray(new Contact[app.contactsMap.size()]);
        Arrays.sort(allContacts, Contact.FIRST_LAST_EMAIL_ASCENDING);
        final ArrayList<Contact> contacts = new ArrayList<App101.Contact>();
        for (Contact contact : allContacts) {
            if (skipUserIds.contains(contact.userId)) continue;
            contacts.add(contact);
        }
         
        // START OF -------------------- Participant Picker ----------------------------------------
        this.rootView = rootView;
        participantsFilter = (EditText) rootView.findViewById(R.id.atlas_view_participants_picker_text);
        participantsListView = (ListView) rootView.findViewById(R.id.atlas_view_participants_picker_list);
        participantsNames = (ViewGroup) rootView.findViewById(R.id.atlas_view_participants_picker_names);

        if (rootView.getVisibility() == View.VISIBLE) {
            participantsFilter.requestFocus();
        }
        
        // log focuses
        final View scroller = rootView.findViewById(R.id.atlas_view_participants_picker_scroll);
        scroller.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "scroller.onFocusChange() hasFocus: " + hasFocus);
            }
        });
        participantsNames.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "names.onFocusChange()    hasFocus: " + hasFocus);
            }
        });
        
        // If filter.requestFocus is called from .onClickListener - filter receives focus, but
        // NamesLayout receives it immediately after that. So filter lose it.
        // XXX: scroller also receives focus 
        participantsNames.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (debug) Log.w(TAG, "names.onTouch() event: " + event);
                if (event.getAction() == MotionEvent.ACTION_DOWN)           // ACTION_UP never comes if  
                    participantsFilter.requestFocus();                      //   there is no .onClickListener
                return false;
            }
        });
        
        participantsFilter.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                View focused = participantsNames.hasFocus() ? participantsNames : participantsNames.findFocus();
                if (debug) Log.w(TAG, "filter.onFocusChange()   hasFocus: " + hasFocus + ", focused: " + focused);
                if (hasFocus) {
                    participantsListView.setVisibility(View.VISIBLE);
                }
                v.post(new Runnable() { // check focus runnable
                    @Override
                    public void run() {
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()   filter.focus: " +  participantsFilter.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()    names.focus: " +  participantsNames.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run() scroller.focus: " +  scroller.hasFocus());
                        
                        // check focus is on any descendants and hide list otherwise  
                        View focused = participantsNames.hasFocus() ? participantsNames : participantsNames.findFocus();
                        if (focused == null) {
                            participantsListView.setVisibility(View.GONE);
                            participantsFilter.setText("");
                        }
                    }
                });
            }
        });
        
        final BaseAdapter contactsAdapter;
        participantsListView.setAdapter(contactsAdapter = new BaseAdapter() {
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_participants_picker_convert, parent, false);
                }
                
                TextView name = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_name);
                TextView avatarText = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_ava);
                Contact contact = contacts.get(position);
                
                name.setText(App101.getContactFirstAndLast(contact));
                avatarText.setText(App101.getContactInitials(contact));
                return convertView;
            }
            
            public long getItemId(int position) {
                return contacts.get(position).userId.hashCode();
            }
            public Object getItem(int position) {
                return contacts.get(position);
            }
            public int getCount() {
                return contacts.size();
            }
        });
        
        participantsListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = contacts.get(position);
                selectedContacts.add(contact);
                refreshParticipants(selectedContacts);
                participantsFilter.setText("");
                participantsFilter.requestFocus();
            }

        });
        
        // track text and filter contact list
        participantsFilter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (debug) Log.w(TAG, "beforeTextChanged() s: " + s + " start: " + start+ " count: " + count+ " after: " + after);
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debug) Log.w(TAG, "onTextChanged()     s: " + s + " start: " + start+ " before: " + before+ " count: " + count);
                
                final String filter = s.toString().toLowerCase();
                contacts.clear();
                for (Contact contact : allContacts) {
                    if (selectedContacts.contains(contacts)) continue; 
                    if (skipUserIds.contains(contact.userId)) continue;
                    
                    if (contact.firstName != null && contact.firstName.toLowerCase().contains(filter)) {
                        contacts.add(contact); continue;
                    }
                    if (contact.lastName != null && contact.lastName.toLowerCase().contains(filter)) {
                        contacts.add(contact); continue;
                    }
                    if (contact.email != null && contact.email.toLowerCase().contains(filter)) {
                        contacts.add(contact); continue;
                    }
                }
                Collections.sort(contacts, new Contact.FilteringComparator(filter));
                contactsAdapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable s) {
                if (debug) Log.w(TAG, "afterTextChanged()  s: " + s);
            }
        });
        
        // select last added participant when press "Backspace/Del"
        participantsFilter.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (debug) Log.w(TAG, "onKey() keyCode: " + keyCode + ", event: " + event);
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && participantsFilter.getText().length() == 0
                        && selectedContacts.size() > 0) {
                    
                    selectedContacts.remove(selectedContacts.size() - 1);
                    refreshParticipants(selectedContacts);
                    participantsFilter.requestFocus();
                }
                return false;
            }
        });
        // END OF ---------------------- Participant Picker ---------------------------------------- 

    }
    
    public void refreshParticipants(final ArrayList<Contact> selectedContacts) {
        
        // remove name_converts first. Better to keep editText in place rather than add/remove that force keyboard to blink
        for (int i = participantsNames.getChildCount() - 1; i >= 0; i--) {
            View child = participantsNames.getChildAt(i);
            if (child != participantsFilter) {
                participantsNames.removeView(child);
            }
        }
        if (debug) Log.w(TAG, "refreshParticipants() childs left: " + participantsNames.getChildCount());
        for (Contact contactToAdd : selectedContacts) {
            View contactView = LayoutInflater.from(participantsNames.getContext())
                    .inflate(R.layout.atlas_view_participants_picker_name_convert, participantsNames, false);
            
            TextView avaText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_ava);
            avaText.setText(App101.getContactInitials(contactToAdd));
            TextView nameText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_name);
            nameText.setText(App101.getContactFirstAndLast(contactToAdd));
            contactView.setTag(contactToAdd);
            
            participantsNames.addView(contactView, participantsNames.getChildCount() - 1);
            if (debug) Log.w(TAG, "refreshParticipants() child added: " + contactView + ", for: " + contactToAdd);
        }
        participantsNames.requestLayout();
    }
    
    public String[] getSelectedUserIds() {
        String[] userIds = new String[selectedContacts.size()];
        for (int i = 0; i < selectedContacts.size(); i++) {
            userIds[i] = selectedContacts.get(i).userId;
        }
        return userIds;
    }
    
    public void setVisibility(int visibility) {
        rootView.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            participantsFilter.requestFocus();
        }
    }
    
}

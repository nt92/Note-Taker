package com.nikhilthota.notetaker;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * The main activity which allows the user to save a note
 */
// TODO Layout for the main activity
// TODO Add cancel button to go straight to the list of notes
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 1;

    private EditText noteText;
    private Button saveButton;
    private Button viewNotesButton;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteText = (EditText) findViewById(R.id.note_text_content);
        saveButton = (Button) findViewById(R.id.save_note_button);
        viewNotesButton = (Button) findViewById(R.id.view_all_notes_button);

        Bundle extras = getIntent().getExtras();
        String email = "";
        if (extras != null) {
            email = extras.getString("email");
        }

        final String finalEmail = email;
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Object> note = new HashMap<>();
                note.put("user", finalEmail);
                note.put("content", noteText.getText().toString());

                // TODO Add a toast when saving
                // Add a new document with a generated ID
                db.collection("notes")
                        .add(note)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                            }
                        });
            }
        });

        viewNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNotesViewActivity();
            }
        });
    }

    private void startNotesViewActivity() {
        Intent mainIntent = new Intent(this, NotesViewActivity.class);
        startActivity(mainIntent);
    }
}

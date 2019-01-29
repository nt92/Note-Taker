package com.nikhilthota.notetaker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import javax.annotation.Nullable;

// Activity that holds the list of notes from the backend
public class NotesViewActivity extends AppCompatActivity {
    private static final String TAG = NotesViewActivity.class.getSimpleName();

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_view);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        CollectionReference collectionReference = db.collection("notes");
        Query query = collectionReference.whereEqualTo("user", account.getEmail());
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                //queryDocumentSnapshots is the list of notes with the current user
                Log.v(TAG, ""+queryDocumentSnapshots.size());
            }
        });
    }
}

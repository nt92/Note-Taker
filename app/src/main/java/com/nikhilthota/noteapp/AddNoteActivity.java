/*
* Copyright (C) 2016 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.nikhilthota.noteapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.util.Date;


public class AddNoteActivity extends AppCompatActivity {
    public static final String EXTRA_NOTE_ID = "extraNoteId";
    static final String TAG = AddNoteActivity.class.getSimpleName();
    String mNoteId = "";

    EditText mEditText;
    Button mSaveButton;
    Button mUploadButton;
    ImageView mImageView;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        initViews();

        if(getIntent() != null && getIntent().hasExtra(EXTRA_NOTE_ID)){
            mSaveButton.setText(R.string.update_button);
            mNoteId = getIntent().getExtras().getString(EXTRA_NOTE_ID);

            // Get data from the existing document
            DocumentReference documentReference = db.collection("notebook").document(mNoteId);
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            mEditText.setText(String.valueOf(document.get("description")));
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });

            // Get image from the existing document
            storage.getReference().child("images/" + mNoteId)
                    .getBytes(1024*1024)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    mImageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.v(TAG, "Failure: " + e);
                }
            });
        }
    }

    private void initViews() {
        mEditText = findViewById(R.id.editTextTaskDescription);
        mSaveButton = findViewById(R.id.saveButton);
        mUploadButton = findViewById(R.id.addImageButton);
        mImageView = findViewById(R.id.imageView);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveButtonClicked();
            }
        });
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUploadButtonClicked();
            }
        });
    }

    private void onSaveButtonClicked() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        String description = mEditText.getText().toString();
        Date date = new Date();

        if(mNoteId.equals("")) {
            final Note note = new Note(account.getEmail(), description, date);
            CollectionReference notebookRef = db.collection("notebook");
            notebookRef.add(note);
            Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show();
        } else {
            DocumentReference noteRef = db.collection("notebook").document(mNoteId);

            //TODO update date
            noteRef
                    .update("description", description)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });
        }
        finish();
    }

    private void onUploadButtonClicked() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                mImageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                Log.v(TAG, e.toString());
                e.printStackTrace();
            }

            // Upload image to FireBase
            StorageReference imageRef = storage.getReference().child("images/" + mNoteId);
            UploadTask uploadTask = imageRef.putFile(imageUri);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.v(TAG, "Failure: " + exception);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.v(TAG, "Success: " + taskSnapshot.getMetadata());
                }
            });
        }
    }
}

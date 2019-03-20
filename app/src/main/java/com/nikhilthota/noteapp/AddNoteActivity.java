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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;


public class AddNoteActivity extends AppCompatActivity {
    public static final String EXTRA_NOTE_ID = "extraNoteId";
    static final String TAG = AddNoteActivity.class.getSimpleName();
    String mNoteId = "";

    EditText mTitleText;
    EditText mDescriptionText;
    Button mSaveButton;
    Button mUploadButton;
    Button mAudioUploadButton;
    ImageView mImageView;

    // Variables for audio recording
    RecordButton recordButton;
    MediaRecorder recorder;
    PlayButton playButton;
    MediaPlayer player;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String audioFileName;

    // Firebase variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        initViews();

        if(getIntent() != null && getIntent().hasExtra(EXTRA_NOTE_ID)){
            mSaveButton.setText(R.string.update_button);
            mNoteId = getIntent().getExtras().getString(EXTRA_NOTE_ID);

            mUploadButton.setEnabled(true);
            mAudioUploadButton.setEnabled(true);

            // Get data from the existing document
            DocumentReference documentReference = db.collection("notebook").document(mNoteId);
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            mDescriptionText.setText(String.valueOf(document.get("description")));
                            mTitleText.setText(String.valueOf(document.get("title")));
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

            // Get voice memo from existing document if not already cached
            final File audioFile = new File(audioFileName);
            if(!audioFile.exists()) {
                storage.getReference().child("recordings/" + mNoteId)
                        .getBytes(1024 * 1024)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                try {
                                    audioFile.createNewFile();
                                    writeBytesToFile(bytes, audioFile);
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.v(TAG, "Failure: " + e);
                    }
                });
            }
        }
    }

    private void initViews() {
        mDescriptionText = findViewById(R.id.editTextNoteDescription);
        mTitleText = findViewById(R.id.editTextNoteTitle);
        mSaveButton = findViewById(R.id.saveButton);
        mUploadButton = findViewById(R.id.addImageButton);
        mAudioUploadButton = findViewById(R.id.uploadAudioButton);
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
        mAudioUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAudioUploadButtonClicked();
            }
        });

        // Set up audio recording buttons programatically
        audioFileName = getExternalCacheDir().getAbsolutePath();
        audioFileName += "/" + mNoteId + ".3gp";
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        LinearLayout recordAndPlayButtons = findViewById(R.id.recordAndPlayButtons);
        recordButton = new RecordButton(this);
        recordAndPlayButtons.addView(recordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        playButton = new PlayButton(this);
        recordAndPlayButtons.addView(playButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
    }

    private void onSaveButtonClicked() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        String description = mDescriptionText.getText().toString();
        String title = mTitleText.getText().toString();
        Date date = new Date();

        if(mNoteId.equals("")) {
            final Note note = new Note(account.getEmail(), title, description, date);
            CollectionReference notebookRef = db.collection("notebook");
            notebookRef.add(note);
            Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show();
        } else {
            DocumentReference noteRef = db.collection("notebook").document(mNoteId);

            // Update all fields on save button press
            noteRef.update("description", description)
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

            noteRef.update("title", title)
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

            noteRef.update("updatedAt", date)
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

    private void onAudioUploadButtonClicked() {
        Uri recordingUri = Uri.fromFile(new File(audioFileName));

        StorageReference recordingsRef = storage.getReference().child("recordings/" + mNoteId);
        UploadTask uploadTask = recordingsRef.putFile(recordingUri);

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

    // Audio Recording Code
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void writeBytesToFile(byte[] bFile, File fileDest) {
        try {
            FileOutputStream fOut = new FileOutputStream(fileDest);
            fOut.write(bFile);
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(audioFileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(audioFileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    class RecordButton extends android.support.v7.widget.AppCompatButton {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
            setWidth(450);
        }
    }

    class PlayButton extends android.support.v7.widget.AppCompatButton {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
            setWidth(500);
        }
    }
}

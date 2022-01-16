package com.dheeraj.soothingradio;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dheeraj.soothingradio.Services.OnClearFromRecentService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements Playable {
    private MediaPlayer mediaPlayer ;

    private FirebaseDatabase firebaseDatabase ;
    private DatabaseReference songsDatabaseReference ;
    private DatabaseReference totalSongsDatabaseReference ;
    private DatabaseReference listenersDatabaseReference ;
    private FirebaseStorage firebaseStorage ;
    private StorageReference songsStorageReference ;
    private ChildEventListener childEventListener ;
    private int currentSongId  ;
    private int lastPosition ;
    private int totalSongs ;
    private int listeners ;
    private ImageView play_pauseButton ;
    private TextView artistName  ;
    private TextView songName ;
    private View loadingView ;
    private int timesOpened ;
    private ImageView background ;
    private ImageView prevButton ;
    private ImageView nextButton ;
    static int imageId ;
    private int totalImages = 9 ;

    NotificationManager notificationManager ;
    Track track ;
    BroadcastReceiver broadcastReceiver ;
    private boolean isFirstSong = true ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SoothingRadio);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hideStatusAndNavigationBar();
        // Storing data into SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);

        // Creating an Editor object to edit(write to the file)
        SharedPreferences.Editor myEdit = sharedPreferences.edit();

        // Retrieving the value using its keys the file name
        // must be same in both saving and retrieving the data
        SharedPreferences sh = getSharedPreferences("MySharedPref", 0);

        // The value will be default as empty string because for
        // the very first time when the app is opened, there is nothing to show
        totalSongs = sh.getInt("Total Songs", Integer.MAX_VALUE);
        currentSongId = sh.getInt("Current Song Id", 1);
        lastPosition = sh.getInt("Last position" , 0 );
        timesOpened = sh.getInt("Times Opened" , 0);
        Log.e("mainactivity" , "this song id = " + currentSongId);
        Log.e("Mainactivity" , "Total Songs = " + totalSongs);
        Log.e("Mainactivity", "Last position = " + lastPosition);
//        for(int i = 1 ; i <= totalSongs ; i++ ){
//            Log.e("Stored content" , "kkey = " + i + " url = " + sh.getString(""+ i , null));
//        }

        // We can then use the data

        firebaseStorage = FirebaseStorage.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        songsDatabaseReference = firebaseDatabase.getReference().child("songs");
        totalSongsDatabaseReference = firebaseDatabase.getReference().child("total_songs");
        listenersDatabaseReference = firebaseDatabase.getReference().child("Listeners");


        play_pauseButton = findViewById(R.id.play_pause);
        prevButton = findViewById(R.id.prev_Button) ;
        nextButton = findViewById(R.id.next_Button) ;
        artistName = findViewById(R.id.artist_Name);
        songName = findViewById(R.id.song_Name);
        loadingView = findViewById(R.id.loading_View);
        background = findViewById(R.id.background_View);
        background.setImageResource(getImageId());
        timesOpened ++ ;
//        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
//
//        // Creating an Editor object to edit(write to the file)
//        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putInt("Times Opened" , timesOpened);
        myEdit.commit() ;
        attachDatabaseReadListener();

        PlaySongsTask playSongsTask = new PlaySongsTask();
        playSongsTask.execute();


        play_pauseButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!mediaPlayer.isPlaying()) {
                    //Toast.makeText(MainActivity.this, "Playing", Toast.LENGTH_SHORT).show();
                    onTrackPlay();
                }
                else{
                    //Toast.makeText(MainActivity.this, "Pause", Toast.LENGTH_SHORT).show();
                    onTrackPause();
                }
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTrackPrev();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTrackNext();
            }
        });

        View fullLayout = findViewById(R.id.full_layout);
        fullLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideStatusAndNavigationBar();
            }
        });

        View shareButton = findViewById(R.id.share) ;
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
                i.putExtra(Intent.EXTRA_TEXT, "Want some soothing music to listen to?\nSoothing Radio is still going strong!\n" +
                        "\n" +
                        "https://play.google.com/store/apps/details?id=com.dheeraj.soothingradio");
                startActivity(Intent.createChooser(i, "Share URL"));
            }
        });
        View rateButton = findViewById(R.id.rate) ;
        rateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.dheeraj.soothingradio"));
                startActivity(browserIntent);
            }
        });
        //////
        TextView Listeners = findViewById(R.id.listeners);
        listenersDatabaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()) {
                    String result = task.getResult().getValue().toString();
                    Log.e("MainActivity" , "Listeners = " + listeners);
                    listeners = Integer.parseInt(result);
                    Log.e("MainActivity" , "Listeners = " + listeners);
                    listeners++;
                    Log.e("MainActivity" , "Listeners = " + listeners);
                    //Listeners.setText("Listeners : " + listeners);
                    String listenersToBeUpdated = "" + listeners ;
                    listenersDatabaseReference.setValue(listenersToBeUpdated).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                                Log.e("Mainactivity" , " No of listeners updated");
                            else
                                Log.e("Mainactivity" , " Couldn't update no of listeners");
                        }
                    });
                }else
                    Log.e("Mainactivity" , "Couldn't get the no of listeners");
            }
        });

        listenersDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String result = snapshot.getValue().toString();
                listeners = Integer.parseInt(result);
                Listeners.setText("Listeners : "+ listeners);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //////

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getExtras().getString("actionname");

                switch (action){

                    case CreateNotification.ACTION_PLAY:
                        if (mediaPlayer.isPlaying()){
                            onTrackPause();
                        } else {
                            onTrackPlay();
                        }
                        Log.e("Mainactivity", "Broadcastreceiver called");
                        break;

                    case CreateNotification.ACTION_CANCEL :
                        if(mediaPlayer.isPlaying())
                            onTrackPause();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                            notificationManager.cancelAll();
                        }
                        break ;

                    case CreateNotification.ACTION_PREV :
                        onTrackPrev() ;
                        break ;

                    case CreateNotification.ACTION_NEXT :
                        onTrackNext();
                        break ;
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel();
            registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACKS"));
            startService(new Intent(getBaseContext(), OnClearFromRecentService.class));
        }


    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CreateNotification.CHANNEL_ID,
                    "Dheeraj Patel", NotificationManager.IMPORTANCE_LOW);

            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null){
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onTrackPrev(){
        int time = mediaPlayer.getDuration() ;
        currentSongId -= 2 ;
        mediaPlayer.seekTo(time) ;
    }

    @Override
    public void onTrackNext(){
        int time = mediaPlayer.getDuration() ;
        mediaPlayer.seekTo(time);
    }


    @Override
    public void onTrackPlay() {
        mediaPlayer.start();
        play_pauseButton.setImageResource(R.drawable.ic_pause_circle);
        CreateNotificationTask createNotificationTask  = new CreateNotificationTask();
        createNotificationTask.execute("Play Notification");

    }

    @Override
    public void onTrackPause() {

        play_pauseButton.setImageResource(R.drawable.ic_play_circle);
        mediaPlayer.pause();
        CreateNotificationTask createNotificationTask  = new CreateNotificationTask();
        createNotificationTask.execute("Pause Notification");
    }

    private class CreateNotificationTask extends AsyncTask<String, Void , Void> {

        @Override
        protected Void doInBackground(String... strings) {
            if(strings[0] == "Pause Notification") {
                CreateNotification.createNotification(MainActivity.this, track ,
                        R.drawable.ic_play_button);
            }
            else if(strings[0] == "Play Notification"){
                CreateNotification.createNotification(MainActivity.this , track ,
                        R.drawable.ic_pause_button);
            }
            return null;
        }
    }

    private class PlaySongsTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            getTotalSongs();
            autoplay();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String data[] = values[0].split(" - ");
            String songName = data[1] ;
            String artistName = data[0];
            if(track == null) {
                track = new Track(songName, artistName);
            }else{
                track.setSongName(songName);
                track.setArtist(artistName);
            }
            onTrackPlay();

            loadingView.setVisibility(View.GONE);
            changeArtistNameTextview(artistName);
            changeSongNameTextview(songName);
            super.onProgressUpdate(values);
        }

        private void getTotalSongs(){
            totalSongsDatabaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if(task.isSuccessful()){
                        String result = task.getResult().getValue().toString();
                        totalSongs = Integer.parseInt(result);
                        // Storing data into SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);

                        // Creating an Editor object to edit(write to the file)
                        SharedPreferences.Editor myEdit = sharedPreferences.edit();

                        // Storing the key and its value as the data fetched from edittext
                        myEdit.putInt("Total Songs", totalSongs);

                        // Once the changes have been made,
                        // we need to commit to apply those changes made,
                        // otherwise, it will throw an error
                        myEdit.commit();
                        Log.e("PlaySongsTask", "Total songs = " + totalSongs);
                    }
                }
            });
        }

        private void autoplay(){
            if(currentSongId < 1)
                currentSongId = totalSongs ;
            if(currentSongId > totalSongs)
                currentSongId = 1 ;
            Log.e("autoplay ", "autoplay called ");
            Log.e("autoplay" , "current song id = " + currentSongId);
            try {
                SharedPreferences sh = getSharedPreferences("MySharedPref", 0);
                String url = sh.getString("" + currentSongId , null);
                Log.e("autoplay", "Url = " + url);
                Log.e("autoplay" , "timeees opened = " + timesOpened );
                if(url == null)
                    url = "https://firebasestorage.googleapis.com/v0/b/soothing-radio-f55b4.appspot.com/o/Ketsa%20-%20Steady%20Ships.mp3?alt=media&token=f12c792f-48f3-4101-8651-2d0f3c6c65d9";
                Log.e("autoplay", "Url = " + url);
                StringBuilder fullMetadata = new StringBuilder();
                String[] words = url.split("%20");
                int lastIndex = words.length -1 ;
                for(int i = 0  ;i <= lastIndex ; i++){
                    if(i ==0)
                        words[0] = words[0].replace("https://firebasestorage.googleapis.com/v0/b/soothing-radio-f55b4.appspot.com/o/", "");

                    else if(i == lastIndex){
                        StringBuilder m = new StringBuilder();
                        String t[] = words[lastIndex].split(".mp3?");
                        words[lastIndex] = t[0];
                    }
                    Log.e("autoplay", words[i]);
                    fullMetadata.append(words[i]).append(" ");
                }

                Log.e("autoplay" , " full String = " + fullMetadata.toString());
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync() ;
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if(isFirstSong == true) {
                            mediaPlayer.seekTo(lastPosition);
                            isFirstSong = false ;
                        }

                        publishProgress(fullMetadata.toString());
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                currentSongId++ ;
                                mediaPlayer.reset();
                                //Toast.makeText(MainActivity.this, "Now playing another song", Toast.LENGTH_SHORT).show();
                                autoplay();
                            }
                        });
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                mediaPlayer.reset();
                currentSongId++ ;
                autoplay();
            }
        }
    }

    private  void releaseMediaPlayer(){
        // If the media player is not null , then it may be currently playing a sound.
        if (mediaPlayer != null){
            mediaPlayer.release();

            // mediaPlayer = null means mediaPlayer is not configured to play an audio file at the moment.
            mediaPlayer = null ;
        }
    }

    private void attachDatabaseReadListener(){
        if(childEventListener == null) {
            childEventListener = new ChildEventListener() {
                // Storing data into SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);

                // Creating an Editor object to edit(write to the file)
                SharedPreferences.Editor myEdit = sharedPreferences.edit();

                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Log.e("Read Listener", "key = " + snapshot.getKey() + " value = " +snapshot.getValue().toString());
                    // Storing the key and its value as the data fetched from edittext
                    myEdit.putString(""+ snapshot.getKey(), snapshot.getValue().toString());

                    // Once the changes have been made,
                    // we need to commit to apply those changes made,
                    // otherwise, it will throw an error
                    myEdit.commit();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Log.e("child changed", "key = " + snapshot.getKey() + " value = " +snapshot.getValue().toString());
                    // Storing the key and its value as the data fetched from edittext
                    myEdit.putString(""+ snapshot.getKey(), snapshot.getValue().toString());

                    // Once the changes have been made,
                    // we need to commit to apply those changes made,
                    // otherwise, it will throw an error
                    myEdit.commit();
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

            songsDatabaseReference.addChildEventListener(childEventListener);
        }
    }


    private void detachDatabaseReadListener(){
        if(childEventListener != null) {
            songsDatabaseReference.removeEventListener(childEventListener);
            childEventListener = null ;
        }
    }

//    @Override
//    protected void onStop() {
//        listeners-- ;
//        String listenersToBeUpdated = "" + listeners ;
//        Log.e("MainActivity" , " On Stop Listeners = " + listeners);
//        listenersDatabaseReference.setValue(listenersToBeUpdated).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if(task.isSuccessful())
//                    Log.e("Mainactivity" , " No of listeners updated");
//                else
//                    Log.e("Mainactivity" , " Couldn't update no of listeners");
//            }
//        });
//        super.onStop();
//    }

    @Override
    protected void onDestroy() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);

        Log.e("On destroy" , "called");
        // Creating an Editor object to edit(write to the file)
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putInt("Current Song Id" , currentSongId);
        Log.e("OnDestroy" , "song Id updated" + currentSongId);
        myEdit.putInt("Last position" , mediaPlayer.getCurrentPosition());
        Log.e("OnDestroy", "position updated " + mediaPlayer.getCurrentPosition());
        myEdit.commit();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationManager.cancelAll();
            Log.e("On destroy " , "Notifications removed");
        }
        listeners-- ;
        String listenersToBeUpdated = "" + listeners ;
        Log.e("MainActivity" , " On Stop Listeners = " + listeners);
        listenersDatabaseReference.setValue(listenersToBeUpdated).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                    Log.e("Mainactivity" , " No of listeners updated");
                else
                    Log.e("Mainactivity" , " Couldn't update no of listeners");
            }
        });
        releaseMediaPlayer();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        background.setImageResource(getImageId());
        timesOpened ++ ;
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);

        // Creating an Editor object to edit(write to the file)
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putInt("Times Opened" , timesOpened);
        Log.e("on Resume " , "Times opened : " + timesOpened);
        myEdit.commit() ;

        //////
//        listeners++ ;
//        String listenersToBeUpdated = "" + listeners ;
//        Log.e("MainActivity" , " On Resume Listeners = " + listeners);
//        listenersDatabaseReference.setValue(listenersToBeUpdated).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if(task.isSuccessful())
//                    Log.e("Mainactivity" , " No of listeners updated");
//                else
//                    Log.e("Mainactivity" , " Couldn't update no of listeners");
//            }
//        });
        /////
        super.onResume();
    }


    void changeSongNameTextview(String SongName){
        songName.setText(SongName);
    }

    void changeArtistNameTextview(String ArtistName){
        artistName.setText(ArtistName);
    }

    private void hideStatusAndNavigationBar(){
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private int getImageId (){
        int imageNo = timesOpened % totalImages ;
        imageId  = 0  ;
        switch (imageNo){
            case 0 :
                imageId = R.drawable.image0 ;
                break ;
            case 1 :
                imageId = R.drawable.image1 ;
                break ;
            case 2 :
                imageId = R.drawable.image2 ;
                break ;
            case 3 :
                imageId = R.drawable.image3 ;
                break ;
            case 4 :
                imageId = R.drawable.image4 ;
                break ;
            case 5 :
                imageId = R.drawable.image5 ;
                break ;
            case 6 :
                imageId = R.drawable.image6 ;
                break ;
            case 7 :
                imageId = R.drawable.image7 ;
                break ;
            case 8 :
                imageId = R.drawable.image8 ;
                break ;
        }
        return imageId ;
    }
}
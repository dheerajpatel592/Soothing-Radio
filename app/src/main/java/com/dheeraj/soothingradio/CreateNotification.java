package com.dheeraj.soothingradio;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.dheeraj.soothingradio.Services.NotificationActionService;

import static com.dheeraj.soothingradio.MainActivity.imageId;

public class CreateNotification {

    public static final String CHANNEL_ID = "channel1";
    public static final String ACTION_PLAY = "actionplay";
    public static final String ACTION_CANCEL = "actioncancel";
    public static final String ACTION_PREV = "actionprev" ;
    public static final String ACTION_NEXT = "actionnext" ;
    public static Notification notification;

    public static void createNotification(Context context, Track track, int playButton){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            MediaSessionCompat mediaSessionCompat = new MediaSessionCompat( context, "tag");

            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), imageId);


            Intent intentPlay = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_PLAY);
            PendingIntent pendingIntentPlay = PendingIntent.getBroadcast(context, 0,
                    intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentPrev = new Intent(context , NotificationActionService.class)
                    .setAction(ACTION_PREV) ;
            PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(context, 0 ,
                    intentPrev , PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentNext = new Intent(context , NotificationActionService.class)
                    .setAction(ACTION_NEXT) ;
            PendingIntent pendingIntentNext = PendingIntent.getBroadcast(context , 0 ,
                    intentNext , PendingIntent.FLAG_UPDATE_CURRENT) ;

            Intent intentCancel = new Intent(context , NotificationActionService.class)
                    .setAction(ACTION_CANCEL);
            PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context , 0,
                    intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

            //create notification
            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle(track.getSongName())
                    .setContentText(track.getArtist())
                    .setLargeIcon(icon)
                    .setOnlyAlertOnce(true)//show notification for only first time
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_prev_button, "Prev" , pendingIntentPrev)
                    .addAction(playButton, "Play", pendingIntentPlay)
                    .addAction(R.drawable.ic_next_button , "Next" , pendingIntentNext)
                    .addAction(R.drawable.ic_cancel, "Cancel", pendingIntentCancel)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1)
                            .setMediaSession(mediaSessionCompat.getSessionToken()))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();

            notificationManagerCompat.notify(1, notification);

        }
    }
}


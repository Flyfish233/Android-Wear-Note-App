package mnf.android.wearnote;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.view.MenuItem;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;

import mnf.android.wearnote.Model.BaseModel;
import mnf.android.wearnote.Model.Note;
import mnf.android.wearnote.tools.MobilePreferenceHandler;
import mnf.android.wearnote.tools.WearPreferenceHandler;


/**
 * Created by muneef on 26/01/17.
 */

public class ApplicationClass extends MultiDexApplication implements NavigationView.OnNavigationItemSelectedListener,DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int CACHE_SIZE = 1;
    private static final String DB_NAME = "note";
    private static final int DB_VERSION = 1;
    static Context c;
    private static GoogleApiClient mGoogleApiClient;

    private static FirebaseStorage mFirebaseStorage;
    private static StorageReference mStorageReferance;
    private FirebaseAuth mFirebaseAuth;
    MobilePreferenceHandler pref;
    public static String DB_PATH = "/data/data/mnf.android.wearnote/databases/note.db";
   static  boolean oldContain = false;
    static List<Note> oldnNoteItems;
    @Override
    public void onCreate() {
        super.onCreate();
        c = this;
/*
        Ollie.with(this)
                .setName(DB_NAME)
                .setVersion(DB_VERSION)
                .setLogLevel(Ollie.LogLevel.FULL)
                .setCacheSize(CACHE_SIZE)
                .init();*/

        ActiveAndroid.initialize(this);
        pref = new MobilePreferenceHandler(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user!=null){
          //  backupDsToFirebase();

            if(pref.getFirstTimeOpen()){
                restoreBackupDb();
            }
            Log.e("TAG","Application class user logged in ");
        }else{
            Log.e("TAG","Application class user logged out ");
        }
    }

    public Context getInstance(){
        this.c = this;
        return c;
    }


    public static void restoreBackupDb(){
        Log.e("TAG","Application class restoreBackupDb");
        oldnNoteItems = Config.getNoteList();
        if(oldnNoteItems.size()>0){
            oldContain = true;
        }


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null) {
            String id = user.getUid();
            mFirebaseStorage = FirebaseStorage.getInstance();
            mStorageReferance = mFirebaseStorage.getReference().child("Database");
             mStorageReferance.child(id).getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Log.e("TAG","Application db download success  ");
                    writeBytesToFile(bytes,DB_PATH);
                }
            }).addOnFailureListener(new OnFailureListener() {
                 @Override
                 public void onFailure(@NonNull Exception e) {
                     Log.e("TAG","Application db download failure  e = "+e);

                 }
             });
        }
    }

    private static void writeBytesToFile(byte[] bFile, String fileDest) {
        Log.e("TAG","Application db writeBytesToFile  ");

        try (FileOutputStream fileOuputStream = new FileOutputStream(fileDest)) {
            fileOuputStream.write(bFile);
            if(oldContain){
                populateOldNotes();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void populateOldNotes(){

        if(oldnNoteItems!=null){
            if(Config.getNoteList()!=null) {
                List<Note> newItems = Config.getNoteList();
                int newCount = newItems.size();
                Log.e("TAG","Application populateOldNotes  newCount = "+newCount);
                int key =0;
                for (Note oldNoteItem : oldnNoteItems) {
                    key =0;
                    for (Note newNoteItem: newItems) {
                        if(newNoteItem.getIdn()!=oldNoteItem.getIdn()){
                            key++;
                        }
                    }
                    Log.e("TAG","Application populateOldNotes  inner loop over key  = "+key);
                    if(key == newCount){
                        Note noteAdd = new Note();
                        if(oldNoteItem.getDate()!=null)
                            noteAdd.setDate(oldNoteItem.getDate());
                        if(oldNoteItem.getBody()!=null)
                            noteAdd.setBody(oldNoteItem.getBody());
                        if(oldNoteItem.getTitle()!=null)
                            noteAdd.setTitle(oldNoteItem.getTitle());
                        if(oldNoteItem.getIdn()!=null)
                            noteAdd.setIdn(oldNoteItem.getIdn());
                        noteAdd.save();
                        Log.e("TAG","Application populateOldNotes  new note added idn = "+oldNoteItem.getIdn());
                    }

                }
            }
        }

    }


    private static void replaceDb() {
        Log.e("TAG","Application db download success  ");

    }


    public static void backupDsToFirebase(){
        Log.e("TAG","Application setUpStorageRefs ");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null) {
            if(user.getUid()!=null){
                String id = user.getUid();
                mFirebaseStorage = FirebaseStorage.getInstance();
                mStorageReferance = mFirebaseStorage.getReference().child("Database");

                StorageReference refUser = mStorageReferance.child(id);
                refUser.putFile(Uri.fromFile(getDbFile())).addOnSuccessListener((Activity) c, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.e("TAG","Application db upload success  ");

                    }
                });
                Log.e("TAG","Application db uri =  "+Uri.fromFile(getDbFile()));

            }

        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("TAG","ApplicationClass onConnected");
        syncDatatoWear();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("TAG","ApplicationClass onConnectionSuspended");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("TAG","ApplicationClass onConnectionFailed");

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.e("TAG","ApplicationClass onDataChanged");

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e("TAG","ApplicationClass onTerminate");
        mGoogleApiClient.disconnect();

    }

    public static void syncDatatoWear(){
        Log.e("TAG","ApplicationClass syncing data to wear");
        List<Note> notes =new Select()
                .all()
                .from(Note.class)
                .execute();
        BaseModel model = new BaseModel();
        model.setNote(notes);

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC);
        Gson gson = builder.create();
        String json = gson.toJson(model);
        Log.e("TAG","jsonString = "+json);
        final PutDataMapRequest putRequest = PutDataMapRequest.create("/notes");
        final DataMap map = putRequest.getDataMap();
        //  map.putInt("color", Color.RED);
        map.putString("database", json);
        Wearable.DataApi.putDataItem(mGoogleApiClient,  putRequest.asPutDataRequest());
    }

    public static void syncPrefToWear(WearPreferenceHandler prefVar){
        WearPreferenceHandler pref =prefVar;
        Log.e("TAG"," ApplicationClass syncing Preference to wear f_size = "+pref.getFontSize()+" theme = "+pref.getTheme());

        final PutDataMapRequest putRequestPref = PutDataMapRequest.create("/pref");
        final DataMap mapPref = putRequestPref.getDataMap();
        mapPref.putString("font_size", pref.getFontSize());
        mapPref.putBoolean("theme",pref.getTheme());
        mapPref.putInt("font_color",pref.getFontColor());
        mapPref.putString("font_style",pref.getFontStyle());
        Wearable.DataApi.putDataItem(mGoogleApiClient,  putRequestPref.asPutDataRequest());
    }


    public static File getDbFile(){
        return  new File("/data/data/mnf.android.wearnote/databases/note.db");
    }

    public static void syncDbFromFirebase(){

    }

}

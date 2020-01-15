package com.example.unipismartalert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class Statistics extends AppCompatActivity {

    //Firebase Instance
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("Warnings");

    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
    }


    public void emergencyButtonMethod(View view){
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int total_num=0; int numSOS=0; int numEarthquakes=0; int numFalls=0; int numAbort=0;
                    float percSOS=0; float percEarthquakes=0; float percFalls=0; float percAbort=0;

                    //Calculate the percentage of SOS type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("sos")){
                            numSOS++;
                        }
                        else if(warning.getEmergency().equals("earthquake")){
                            numEarthquakes++;
                        }
                        else if(warning.getEmergency().equals("fall")){
                            numFalls++;
                        }
                        else if(warning.getEmergency().equals("abort")){
                            numAbort++;
                        }
                        //Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                        total_num++;
                    }

                   /* //Calculate the percentage of Earthquake type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("earthquake")){
                            numEarthquakes++;
                        }
                        Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                    }

                    //Calculate the percentage of Fall type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("fall")){
                            numFalls++;
                        }
                        Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                    }

                    //Calculate the percentage of Abort type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("abort")){
                            numAbort++;
                        }
                        Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                    }*/

                    percSOS = (numSOS * 100.0f) / total_num;
                    percEarthquakes = (numEarthquakes * 100.0f) / total_num;
                    percFalls = (numFalls * 100.0f) / total_num;
                    percAbort = (numAbort * 100.0f) / total_num;

                    //Toast.makeText(Statistics.this, "Percentage: " + percSOS , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()

                    showMessage("Type of Emergency:", "Total records: "+total_num + "\n"
                            +"SOS emergencies: " + percSOS +"%" + "\n"
                            +"Earthquake emergencies: " + percEarthquakes +"%" + "\n"
                            +"Fall emergencies: " + percFalls +"%" + "\n"
                            +"Abort emergencies: " + percAbort +"%");


                }
                catch(Exception e){
                    Toast.makeText(Statistics.this,e.getMessage() , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    public void userButtonMethod(View view){
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {


                    ArrayList<String> firebaseRecords = new ArrayList<>(); //Stores the users of each record from Firebase.

                    //Calculate the percentage of SOS type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);

                        firebaseRecords.add(warning.getUser());
                    }

                    ArrayList<String> firebaseUsers = new ArrayList<>();    //Stores the users that have records in Firebase.

                    for(String user:firebaseRecords){

                        if(!firebaseUsers.contains(user))
                            firebaseUsers.add(user);

                    }

                    ArrayList<Integer> firebaseUsersStatistics = new ArrayList<>();

                    int count=0;
                    for(String user:firebaseUsers){
                        for(String record:firebaseRecords) {
                            if (user.equals(record)) {
                                ++count;
                            }
                        }
                        firebaseUsersStatistics.add(count);
                        count=0;
                    }

                    /*  Final Statistics    */
                    StringBuffer buffer4 = new StringBuffer();
                    float percent = 0.0f;
                    for(int i=0;i<firebaseUsers.size();i++){
                        percent = ((float)firebaseUsersStatistics.get(i) / (float)firebaseRecords.size()) * 100f ;
                        buffer4.append(firebaseUsers.get(i)+" : "+ percent + "%\n");
                    }
                    showMessage("Users",buffer4.toString());
                    /*  Final Statistics    */

                }
                catch(Exception e){
                    Toast.makeText(Statistics.this,e.getMessage() , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }


        });

    }




    public void timestampButtonMethod(View view){
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int total_num=0; int numSOS=0; int numEarthquakes=0; int numFalls=0; int numAbort=0;
                    float percSOS=0; float percEarthquakes=0; float percFalls=0; float percAbort=0;

                    //Calculate the percentage of SOS type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("sos")){
                            numSOS++;
                        }
                        Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                        total_num++;
                    }

                    //Calculate the percentage of Earthquake type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("earthquake")){
                            numEarthquakes++;
                        }
                        Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                    }

                    //Calculate the percentage of Fall type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("fall")){
                            numFalls++;
                        }
                        Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                    }

                    //Calculate the percentage of Abort type entries.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        if(warning.getEmergency().equals("abort")){
                            numAbort++;
                        }
                        Toast.makeText(Statistics.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                    }

                    percSOS = (numSOS * 100.0f) / total_num;
                    percEarthquakes = (numEarthquakes * 100.0f) / total_num;
                    percFalls = (numFalls * 100.0f) / total_num;
                    percAbort = (numAbort * 100.0f) / total_num;

                    Toast.makeText(Statistics.this, "Percentage: " + percSOS , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()

                    showMessage("Type of Emergency:", "Total records: "+total_num + "\n"
                            +"SOS emergencies: " + percSOS +"%" + "\n"
                            +"Earthquake emergencies: " + percEarthquakes +"%" + "\n"
                            +"Fall emergencies: " + percFalls +"%" + "\n"
                            +"Abort emergencies: " + percAbort +"%");


                }
                catch(Exception e){
                    Toast.makeText(Statistics.this,e.getMessage() , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    public void showMessage(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.show();
    }
}

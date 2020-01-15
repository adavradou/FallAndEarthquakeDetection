package com.example.unipismartalert;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {


    //Sqlite Database
    SQLiteDatabase db;


    //GUI
    private TextView sqrtTextView, timerTextView, usbTextView;
    private Button abortButton, sosButton;


    //GPS
    LocationManager locationManager;
    int REQUESTCODE = 394;

    //Acceleration sensor
    double acceleration_g;
    private Sensor accSensor;
    private SensorManager accSM;
    // Indexes for x, y, and z values
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    long startTime = 0; //Keeps the time the free fall started. e.g. 18:36:41

    volatile boolean running = true;
    boolean freeFallFlag = false; //Checks if a free fall has happened.
    boolean landingFlag = false; //Checks if a landing has happened within a second after the free fall.


    //Gyroscope sensor
    private SensorManager sensorManager;
    private Sensor gyroSensor;

    //For not continuous executing onSensorChanged & onLocationChanged.
    boolean locationFlag = true;
    double latitudeTemp = 0.0;
    double longitudeTemp = 0.0;

    /*  Battery & Power connection receivers    */
    BroadcastReceiver powerConnectionReceiver;
    BroadcastReceiver batteryReceiver;
    //Flag for USB charging
    boolean batteryUsbPluggedFlag;


    //Firebase Instance
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("Warnings");

    //Username of application for updating firebase.
    final String username = "afroksilanthi";

    /*  SMS   */
    int MY_PERMISSIONS_REQUEST_SEND_SMS = 502;
    SmsManager smgr = SmsManager.getDefault();

    //TTS
    TextToSpeech tts;
    int lastSecond; //Keeps the last second, so that the countdown timer is updated once per second.

    //SOS button
    static boolean layingOnGroundFlag = false;

    //Voice command - statistics.
    private static final int REQUEST_SPEECH = 0;

    //Earthquake firebase flag.
    boolean firebaseEarthquakeFound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign Textviews and Buttons
        sqrtTextView = (TextView) findViewById(R.id.sqrtTextView);
        timerTextView = (TextView) findViewById(R.id.timerTextView);
        usbTextView = (TextView) findViewById(R.id.usbTextView);
        sosButton = findViewById(R.id.button);
        abortButton = findViewById(R.id.abortButton);
        abortButton.setVisibility(View.INVISIBLE);
        timerTextView.setVisibility(View.INVISIBLE);
        sqrtTextView.setVisibility(View.INVISIBLE);


        //Database Creation
        db = openOrCreateDatabase("smartalert", MODE_PRIVATE, null);

        db.execSQL("CREATE TABLE IF NOT EXISTS 'contacts' (" +
                "'name' TEXT  , " +
                "'phone' TEXT  )");

        Cursor cursor = db.rawQuery("SELECT * FROM contacts ", null);
        if (cursor.getCount() == 0) {

            ContentValues insertValues = new ContentValues();
            insertValues.put("name", "gerasimos");
            insertValues.put("phone", "6976740029");
            db.insert("contacts", null, insertValues);
            insertValues.clear();
            insertValues.put("name", "agapi");
            insertValues.put("phone", "6986989217");
            db.insert("contacts", null, insertValues);
        }

        /* *//*Check Database records and show them *//*
        StringBuffer buffer = new StringBuffer();
        Cursor cursor2 = db.rawQuery("SELECT * FROM contacts", null);
        if (cursor2.getCount() != 0) {
            while (cursor2.moveToNext()) {
                buffer.append("name: " + cursor2.getString(0) + " ");
                buffer.append("phone: " + cursor2.getString(1) + "\n");

            }
            //Toast.makeText(this, buffer.toString() , Toast.LENGTH_LONG).show();
            showMessage("Contacts Table: ", buffer.toString());
        } else {
            Toast.makeText(this, "No records found !", Toast.LENGTH_LONG).show();
        }
        *//*End check*/

        //GPS
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                giveperm();

                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
        }
        Location location = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
        onLocationChanged(location);


        //SMS
        giveSMSpermission();


        //ACCELEROMETER
        accSM = (SensorManager)getSystemService(SENSOR_SERVICE); //Create our Sensor Manager
        accSensor = accSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //Accelerometer Sensor
        accSM.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL); //Register sensor Listener

        //GYROSCOPE
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //Register Gyroscope Listener .
        if(gyroSensor != null){
            sensorManager.registerListener(this,gyroSensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
        else{
            String gyroscopeToastMessage = MainActivity.this.getResources().getString(R.string.gyroscopeToastMessage);
            Toast.makeText(MainActivity.this, gyroscopeToastMessage, Toast.LENGTH_SHORT).show();
        }

        //USB charging
        checkUSBPowerConnection();

        //Test Firebase. Add a new record.
       /* Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        //Toast.makeText(this,currentTimestamp.toString(), Toast.LENGTH_LONG).show();
        addWarning("timmy","earthquake",37.984221,23.728041, currentTimestamp.toString());*/
        /*readWarnings(); //Read all warnings from Firebase.*/

        //TTS
        tts=new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });


        //SOS button is clicked.
        //For SOS button: http://angrytools.com/android/button/
        sosButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                abortButton.setVisibility(View.VISIBLE);

                //Write to firebase.
                Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
                addWarning(username, "sos", latitudeTemp, longitudeTemp, currentTimestamp.toString());


                String sosToastMessage1 = MainActivity.this.getResources().getString(R.string.sosToastMessage1);
                String sosToastMessage2 = MainActivity.this.getResources().getString(R.string.sosToastMessage2);
                String sosFinalMessage = sosToastMessage1 + latitudeTemp + ", " + longitudeTemp +sosToastMessage2;

                //String sosToastMessage = MainActivity.this.getResources().getString(R.string.sosToastMessage);
                Toast.makeText(MainActivity.this, sosFinalMessage, Toast.LENGTH_SHORT).show();


                /*Read Database records and send SMS to all. */
                StringBuffer buffer = new StringBuffer();
                Cursor cursor2 = db.rawQuery("SELECT * FROM contacts",null);
                if (cursor2.getCount()!=0){
                    while (cursor2.moveToNext()){

                        //Send SMS to all numbers in the contact list.
                        sendSMS(cursor2.getString(1), sosFinalMessage);

                    }
                    //Toast.makeText(this, buffer.toString() , Toast.LENGTH_LONG).show();
                    //showMessage("Contacts Table: ",buffer.toString());
                }
                else{
                    Toast.makeText(MainActivity.this, "No records found !" , Toast.LENGTH_LONG).show();
                }



                //The following message is heard 5 times.
                String text = "SOS! Please, I need help.";
                for (int i = 0; i < 5; i++)
                {
                    tts.speak(text, TextToSpeech.QUEUE_ADD , null);
                }

            }
        });


        //ABORT button is clicked.
        abortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String abortToastMessage = MainActivity.this.getResources().getString(R.string.abortToastMessage);
                Toast.makeText(MainActivity.this, abortToastMessage, Toast.LENGTH_SHORT).show();

                resetVariables();

                Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
                addWarning(username, "abort", latitudeTemp, longitudeTemp, currentTimestamp.toString());
            }
        });
    }


    @Override
    public void onLocationChanged(Location location) {



        if(locationFlag) {
            locationFlag = false;
            latitudeTemp = location.getLatitude();
            longitudeTemp = location.getLongitude();
            //showMessage("","latitudeTemp : " + latitudeTemp.toString() + "  longitudeTemp : " + longitudeTemp); //Testing !
            /*Toast.makeText(this,"latitudeTemp: " + latitudeTemp + " longitudeTemp:" + longitudeTemp, Toast.LENGTH_SHORT).show();*/
        }
        else if(!locationFlag && ((latitudeTemp != location.getLatitude()) || (longitudeTemp != location.getLongitude()))){
            locationFlag = true;
            //Else if ,is  executed every time location is changed.
            //Toast.makeText(this,"latitudeTemp: "+ latitudeTemp +"  longitudeTemp:"+
            //longitudeTemp +" " , Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        //The phone is detecting either earthquakes or fall.
        if (batteryUsbPluggedFlag == true) {

            //Make the USB textView green if the USB is connected.
            usbTextView.setTextColor(Color.GREEN);

            //Detect earthquakes only when the phone is connected to the the computer with USB
            if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                detectEarthquake(event.values[X], event.values[Y], event.values[Z]);
            }

        }
        else{
            //Make the USB textView red if the USB is disconnected.
            usbTextView.setTextColor(Color.RED);
            //Assumption is made that no fall will happen while the phone is connected with USB to the computer.
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                detectFall(event.values[X], event.values[Y], event.values[Z]);
            }
        }
    }


    public void detectEarthquake(double x, double y, double z){
/*        //Calculate the root-sum of squares of the signals of acceleration.
        double acceleration=Math.sqrt(Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2));

        //Convert it to g measurement.
        acceleration_g = acceleration / 9.80665;
        //sqrtTextView.setText(Double.toString(acceleration_g));

        if(acceleration_g < 0.99 || acceleration_g > 1.05)//Possible earthquake.
        {
            Toast.makeText(this, "EartHquAAkeeeeEEeeeEeeEe", Toast.LENGTH_SHORT).show();
        }*/

        //Toast.makeText(MainActivity.this,"detectEarthquake(...)", Toast.LENGTH_SHORT).show();

        if(x > 0.2f || y > 0.2f || z > 0.2f){   /* if(x > 0.5f || y > 0.5f || z > 0.5f)  */
            checkEarthquakeWarnings();
            //showMessage("#firebaseEarthquakeFound 2  " ," firebaseEarthquakeFound =" + firebaseEarthquakeFound);
            if (firebaseEarthquakeFound){
                //Toast.makeText(MainActivity.this,"EARTHQUAKE detected!!!", Toast.LENGTH_SHORT).show();

                String earthquakeDetectedToastMessage = MainActivity.this.getResources().getString(R.string.earthquakeDetectedToastMessage);
                Toast.makeText(MainActivity.this, earthquakeDetectedToastMessage, Toast.LENGTH_SHORT).show();


/*                String earthquakeToast = "EARTHQUAKE detected!!!--"+"1) Gyroscope was triggered\n 2) Firebase Warning with different username\n " +
                        "3) Firebase Warning with emergency type = earthquake\n 4) Firebase Warning with Timestamp within 60 seconds\n" +
                        "(Rad/Sec) X = "+ x +" Y = "+ y +" Z = "+ z;
                Toast.makeText(MainActivity.this, earthquakeToast, Toast.LENGTH_SHORT).show();*/


                Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
                addWarning(username, "earthquake", latitudeTemp, longitudeTemp, currentTimestamp.toString());
                firebaseEarthquakeFound = false;
            }
        }

    }

    public void detectFall(double x, double y, double z){

        //Calculate the root-sum of squares of the signals of acceleration.
        double acceleration=Math.sqrt(Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2));

        //Convert it to g measurement.
        acceleration_g = acceleration / 9.80665;
        //sqrtTextView.setText(Double.toString(acceleration_g));

        //PHASE ONE
        //In a free fall the x,y,z values of the accelerometer are near zero.
        if(acceleration_g < 0.3) //Free fall
        {

            freeFallFlag = true;
            if (running == false){
                running = true;
            }
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);

        }

    }




    //Runs without a timer by reposting this handler at the end of the runnable.
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            if (!running) return;
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            seconds = seconds % 60;

            //PHASE TWO
            //A fall generally occurs in a short period of 0.4 - 0.8s.
            //A landing must happen within a second after a free fall has been detected.
            //If the vector sum raises to a value over 30m/s, the phone/person has landed.
            if (seconds <= 1 && acceleration_g > 2) //CONVERT TO 1
            {
                landingFlag = true;
            }
            //PHASE THREE
            //If the phone is not moving for two seconds after the landing, the user/phone is laying on the ground.
            if (seconds <= 3 && landingFlag == true && acceleration_g >= 0.9 && acceleration_g <= 1.1){
                layingOnGroundFlag = true; //Flag to start the reverse counting.
            }
            if (layingOnGroundFlag == true) {
                sosButton.setVisibility(View.INVISIBLE);
                if (seconds != lastSecond){
                    showReverseTimer(seconds); //Start countdown.
                    lastSecond = seconds;
                }
            }

            //If no landing takes place within a second that the free fall was detected, stop the timer.
            else if (seconds > 3 && landingFlag == false)
            {
                //reset variables??
                running = false;
                timerTextView.setText(String.format("-----"));
                freeFallFlag = false;
            }

            timerHandler.postDelayed(this, 500);
        }
    };


    public void showReverseTimer(int layingOnGroundSeconds)
    {
        timerTextView.setVisibility(View.VISIBLE);
        abortButton.setVisibility(View.VISIBLE);
        sqrtTextView.setVisibility(View.VISIBLE);

        int remainingTime = 30 - layingOnGroundSeconds;

        if (remainingTime == 0) {
            timerTextView.setVisibility(View.INVISIBLE);
            //Toast.makeText(this, "SOS sent!!", Toast.LENGTH_SHORT).show();
            running = false; //stop the timer.

            //Write to firebase.
            Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
            addWarning(username, "fall", latitudeTemp, longitudeTemp, currentTimestamp.toString());

            String sosToastMessage1 = MainActivity.this.getResources().getString(R.string.sosToastMessage1);
            String sosToastMessage2 = MainActivity.this.getResources().getString(R.string.sosToastMessage2);
            String sosFinalMessage = sosToastMessage1 + latitudeTemp + ", " + longitudeTemp +sosToastMessage2;

            //String sosToastMessage = MainActivity.this.getResources().getString(R.string.sosToastMessage);
            Toast.makeText(MainActivity.this, sosFinalMessage, Toast.LENGTH_SHORT).show();


            /*Read Database records and send SMS to all. */
            StringBuffer buffer = new StringBuffer();
            Cursor cursor2 = db.rawQuery("SELECT * FROM contacts",null);
            if (cursor2.getCount()!=0){
                while (cursor2.moveToNext()){

                    //Send SMS to all numbers in the contact list.
                    sendSMS(cursor2.getString(1), sosFinalMessage);

                }

            }
            else{
                Toast.makeText(MainActivity.this, "No records found !" , Toast.LENGTH_LONG).show();
            }
            resetVariables();

        }
        else {
            timerTextView.setText(String.format("%02d", remainingTime));
            tts.speak(String.format(Integer.toString(remainingTime)), TextToSpeech.QUEUE_FLUSH , null);
        }
    }



    public void resetVariables(){
        abortButton.setVisibility(View.INVISIBLE);
        timerTextView.setVisibility(View.INVISIBLE);
        sqrtTextView.setVisibility(View.INVISIBLE);
        sosButton.setVisibility(View.VISIBLE);
        freeFallFlag = false;
        landingFlag = false;
        running = false;
        layingOnGroundFlag = false;
        tts.stop();
    }


    public void checkUSBPowerConnection(){

        powerConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                    // Do something when power disconnected
                    batteryUsbPluggedFlag = false;
                }
            }
        };

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                if(action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    if (status == BatteryManager.BATTERY_PLUGGED_USB){
                        batteryUsbPluggedFlag = true;
                    }
                }

            }
        };

        /*  Bind BroadcastReceivers with Intents    */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerConnectionReceiver, filter);  //Register power receiver.
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver , filter); //register battery receiver.

    }



    public boolean checkEarthquakeWarnings(){
        //Global variable
        //firebaseEarthquakeFound = false;
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {

                    Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
                    long currentMillis = currentTimestamp.getTime();

                    Warning warning;
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        warning =  warningSnapshot.getValue(Warning.class);
                        //Convert String to Timestamp and Timestamp to milliseconds.
                        Timestamp timestampTemp = Timestamp.valueOf(warning.getTimestamp());
                        long millisTemp = timestampTemp.getTime();

                        //buffer.append("currentMillis = "+currentMillis + " millisTemp = " + millisTemp);    //Testing !

                        if(!warning.getUser().equals(username) && warning.getEmergency().equals("earthquake") &&   //Check if user not equal to my username
                                ((currentMillis - millisTemp) <= 120000000) ){                              //and type of emergency=earthquake and
                            firebaseEarthquakeFound = true;                                            //timestamp of firebase record, not older than 120 sec.

                        }


                    }
                }
                catch(Exception e){
                    Toast.makeText(MainActivity.this,"checkEarthquakeWarnings() : "+e.getMessage() ,
                            Toast.LENGTH_LONG).show();
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return firebaseEarthquakeFound;
    }


    public void mapButtonMethod(View view){ //Map button is clicked.

        mapsActivity(); //Go to EarthquakesMap activity.
    }

    public void mapsActivity() {
        Intent intent = new Intent(getApplicationContext(), EarthquakesMap.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        tts.shutdown();
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(powerConnectionReceiver);

    }
    public void changeLanguage(View view) {

        showChangeLanguageDialog();

    }

    private void showChangeLanguageDialog() {
        final String[] listItems = {"Ελληνικά", "English", "Deutsch"};

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle(getString(R.string.changeLanguage));
        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (i == 0) {

                    setLocale("el");
                    restartActivity();
                }
                else if (i == 1) {

                    setLocale("en");
                    restartActivity();
                }
                else if (i == 2) {

                    setLocale("de");
                    restartActivity();
                }
            }

        });

        AlertDialog mDialog = mBuilder.create();
        mDialog.show();

    }

    public void restartActivity(){
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void setLocale(String language) {

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        SharedPreferences.Editor editor = getSharedPreferences("Settings",MODE_PRIVATE).edit();
        editor.putString("My_Lang", language);
        editor .apply();

    }


    //Request permissions for Location updates.
    public void giveperm(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUESTCODE);
        }
        else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,
                    this);
        }
    }

    //Callback for the result from requesting permissions.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this,"GPS is already on.",Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                    this);
        } else
            Toast.makeText(this,"Permission needed before starting",Toast.LENGTH_SHORT).show();

        if(requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                String smsSendToastMessage = MainActivity.this.getResources().getString(R.string.smsSendToastMessage);
                Toast.makeText(MainActivity.this, smsSendToastMessage, Toast.LENGTH_SHORT).show();
            }
            else{
                String smsNotSendToastMessage = MainActivity.this.getResources().getString(R.string.smsNotSendToastMessage);
                Toast.makeText(MainActivity.this, smsNotSendToastMessage, Toast.LENGTH_SHORT).show();
                return;
            }
        }

    }


    //Send Emergency SMS to all my contacts.
    public void giveSMSpermission(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                //SMS permission ok
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }

    }

    public void sendSMS(String number, String text){
        try {
            smgr.sendTextMessage(number, null, text, null, null);
            // showMessage("SMS OK", "SMS sent successfully");
        }
        catch(Exception e){
            // showMessage("SMS FAILED","Failed to send SMS \n"+ e.getMessage());
        }

    }

    public void addWarning(String user, String emergency, Double latitude, Double longitude, String timestamp){

        String id = myRef.push().getKey();
        Warning warning = new Warning(id,user,emergency,latitude,longitude, timestamp);
        myRef.child(id).setValue(warning);  //Save warning record in firebase.
        //Toast.makeText(this,"Warning added.",Toast.LENGTH_LONG).show();

    }

    //Read all record from Firebase
    public void readWarnings(){
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    //Add data from Firebase to the List.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        Toast.makeText(MainActivity.this, warning.getId() +" " +warning.getUser()+" "+ warning.getTimestamp() , Toast.LENGTH_SHORT).show(); //+" "+ warning.getTimestamp()
                    }
                }
                catch(Exception e){
                    Toast.makeText(MainActivity.this,e.getMessage() , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void statisticsVoiceCommandButtonMethod(View view){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Choose Activity");
        startActivityForResult(intent, REQUEST_SPEECH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_SPEECH){
            if (resultCode == RESULT_OK){
                ArrayList<String> matches = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (matches.size() == 0) {
                    // didn't hear anything
                } else {
                    String mostLikelyThingHeard = matches.get(0);
                    // toUpperCase() used to make string comparison equal
                    if(mostLikelyThingHeard.toUpperCase().equals("GO")){
                        startActivity(new Intent(this, Statistics.class));
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void showMessage(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.show();
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}

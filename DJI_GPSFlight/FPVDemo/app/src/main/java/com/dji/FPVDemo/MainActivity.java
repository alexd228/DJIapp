package com.dji.FPVDemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;

import dji.common.error.DJIError;
import dji.common.gimbal.DJIGimbalAngleRotation;
import dji.common.gimbal.DJIGimbalRotateAngleMode;
import dji.common.gimbal.DJIGimbalRotateDirection;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.missionstep.DJIAircraftYawStep;
import dji.sdk.missionmanager.missionstep.DJIGimbalAttitudeStep;
import dji.sdk.missionmanager.missionstep.DJIGoHomeStep;
import dji.sdk.missionmanager.missionstep.DJIGoToStep;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJIShootPhotoStep;
import dji.sdk.missionmanager.missionstep.DJIStartRecordVideoStep;
import dji.sdk.missionmanager.missionstep.DJIStopRecordVideoStep;
import dji.sdk.missionmanager.missionstep.DJITakeoffStep;

public class MainActivity extends AppCompatActivity {


    private Button btnClear, btnAddCoord, endCoordEntry, prepare_start;
    private TextView coordView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ArrayList<GPScoord> GPSCoordinates;

    protected DJIMission mDJIMission;
    private DJIMissionManager mMissionManager;
    private DJIFlightController mFlightController;
    private Animation mFadeoutAnimation;
    protected ProgressBar mPB;

    private Handler mUIHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAddCoord = (Button) findViewById(R.id.addCoord);
        btnClear = (Button) findViewById(R.id.clearCoords);
        endCoordEntry = (Button) findViewById(R.id.endCoordEntry);
        prepare_start = (Button) findViewById(R.id.prepare_start);
        coordView = (TextView) findViewById(R.id.CoordView);
        GPSCoordinates = new ArrayList<GPScoord>();

        Toast.makeText(MainActivity.this,
                "Test",
                Toast.LENGTH_SHORT);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                GPScoord coord = new GPScoord(location.getLatitude(), location.getLongitude());
                GPSCoordinates.add(coord);
                coordView.append("\n " + coord.latitudeAsString() + "," + coord.longitudeAsString());

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        //permission check for gps use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET,
                }, 10);
                return;
            } else {
                configureButton();
            }
        } else {
            configureButton();
        }

    }

    private void configureButton() {
        btnAddCoord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
                Toast.makeText(MainActivity.this,
                        "Coordinate Added:",
                        Toast.LENGTH_SHORT).show();
            }
        });
        btnClear.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                coordView.setText("Coordinates:");
                GPSCoordinates.clear();
                Toast.makeText(MainActivity.this,
                        "Coordinate Cleared:",
                        Toast.LENGTH_SHORT).show();
            }
        });
        endCoordEntry.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                Toast.makeText(MainActivity.this,
                        "Finished!",
                        Toast.LENGTH_SHORT).show();
            try{
                GPSCoordinates.add(GPSCoordinates.get(0));
            }catch(Exception e)
            {
                e.printStackTrace();
             }

            }
        });
        prepare_start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                Toast.makeText(MainActivity.this,
                        "Starting",
                        Toast.LENGTH_SHORT).show();
                try{
                    mMissionManager = DJIMissionManager.getInstance();
                            mDJIMission = initMission();
                            if (mDJIMission == null) {
                                Toast.makeText(MainActivity.this, "choose a mission type", Toast.LENGTH_SHORT).show();
                            }
                            mMissionManager.prepareMission(mDJIMission, new DJIMission.DJIMissionProgressHandler() {

                                        @Override
                                        public void onProgress(DJIMission.DJIProgressType type, float progress) {
                                            setProgressBar((int) (progress * 100f));
                                        }

                                    }
                                    , new DJICommonCallbacks.DJICompletionCallback() {
                                        @Override
                                        public void onResult(DJIError error) {
                                            if (error == null) {
                                                Toast.makeText(MainActivity.this, "success", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(MainActivity.this, "prepare", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                            // break;
                            if (mDJIMission != null) {
                                mMissionManager.setMissionExecutionFinishedCallback(new DJICommonCallbacks.DJICompletionCallback() {

                                    @Override
                                    public void onResult(DJIError error) {
                                        Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            //  else{Toast.makeText(MainActivity.this, "DJIMission is NULL D:", Toast.LENGTH_SHORT).show();}
                            //For the panorama mission, there will be no callback in some cases, we will fix it in next version.
                            mMissionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {

                                @Override
                                public void onResult(DJIError mError) {
                                    //Toast.makeText(MainActivity.this, "Start: " + mError.getDescription(), Toast.LENGTH_SHORT).show();

                                }
                            });

                }catch(Exception e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    configureButton();
                return;
        }
    }
    protected DJIMission initMission() {

        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        DJITakeoffStep takingoff = new DJITakeoffStep(new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                Toast.makeText(MainActivity.this, "Takeoff", Toast.LENGTH_SHORT).show();
            }
        });
        DJIGimbalAttitudeStep attitudeStep = new DJIGimbalAttitudeStep(
                DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbalAngleRotation(true, 0f, DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        //  Utils.setResultToToast(mContext, "Set gimbal attitude step: " + (error == null ? "Success" : error.getDescription()));
                        Toast.makeText(MainActivity.this, "Attitude ABSOLUTE :O! step: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

                    }
                });

        //Step 3: Go 10 meters from home point
        DJIGoToStep SpeedAndGo = new DJIGoToStep(26.2051762, -98.2838191, 2, new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                //    Utils.setResultToToast(mContext, "Goto step: " + (error == null ? "Success" : error.getDescription()));
                Toast.makeText(MainActivity.this, "Speed 1 step: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

            }
        });
        SpeedAndGo.setFlightSpeed(1 / 2);

        DJIGoToStep SpeedAndGo2 = new DJIGoToStep(26.2051762, -98.2839248, 2, new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                //  Utils.setResultToToast(mContext, "Goto step: " + (error == null ? "Success" : error.getDescription()));
                Toast.makeText(MainActivity.this, "Speed 2 step: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

            }
        });
        SpeedAndGo2.setFlightSpeed(1 / 2);
        DJIGimbalAttitudeStep CameraAngle = new DJIGimbalAttitudeStep(
                DJIGimbalRotateAngleMode.RelativeAngle,
                new DJIGimbalAngleRotation(true, 20, DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        // Utils.setResultToToast(mContext, "Set gimbal attitude step: " + (error == null ? "Success" : error.getDescription()));
                        Toast.makeText(MainActivity.this, "Gimbal 1 step: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

                    }
                });
        DJIGimbalAttitudeStep CameraAngle2 = new DJIGimbalAttitudeStep(
                DJIGimbalRotateAngleMode.RelativeAngle,
                new DJIGimbalAngleRotation(true, 45, DJIGimbalRotateDirection.CounterClockwise),
                null,
                null,
                new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        //  Utils.setResultToToast(mContext, "Set gimbal attitude step: " + (error == null ? "Success" : error.getDescription()));
                        Toast.makeText(MainActivity.this, "Gimbal 2 step: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

                    }
                });
        DJIShootPhotoStep Photo1 = new DJIShootPhotoStep(new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                //Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                Toast.makeText(MainActivity.this, "Photo step: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

            }
        });
        DJIAircraftYawStep YawMove = new DJIAircraftYawStep(90, 20, new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                // Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                Toast.makeText(MainActivity.this, "Yawstep: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

            }
        });
        DJIAircraftYawStep YawMove2 = new DJIAircraftYawStep(-180, 20, new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //  Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                Toast.makeText(MainActivity.this, "Yaw Step 2: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();
            }
        });


        DJIGoHomeStep goinghome = new DJIGoHomeStep(new DJICommonCallbacks.DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {
                Toast.makeText(MainActivity.this, "Landing", Toast.LENGTH_SHORT).show();
            }
        });
        DJIStartRecordVideoStep videoStarts = new DJIStartRecordVideoStep(new DJICommonCallbacks.DJICompletionCallback(){
            public void onResult(DJIError error){
                Toast.makeText(MainActivity.this, "RecordingVideo", Toast.LENGTH_SHORT).show();
            }
        });
        DJIStopRecordVideoStep videoStops = new DJIStopRecordVideoStep(new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Toast.makeText(MainActivity.this, "Stoping Video", Toast.LENGTH_SHORT).show();
            }
        });
        steps.add(takingoff);

       /* steps.add(attitudeStep);
        steps.add(SpeedAndGo);
        SpeedAndGo.setFlightSpeed(1/2);
        steps.add(SpeedAndGo2);
        SpeedAndGo2.setFlightSpeed(1/2);

        steps.add(YawMove);
        steps.add(CameraAngle);
        steps.add(Photo1);

        steps.add(YawMove2);
        steps.add(CameraAngle2);
        steps.add(Photo1);
        steps.add(SpeedAndGo);
       */
     //   steps.add(videoStarts);
      //  steps.add(videoStops);
        for(int i=0; i<GPSCoordinates.size();i++) {
            steps.add(new DJIGoToStep(GPSCoordinates.get(i).latitudeAsDouble(), GPSCoordinates.get(i).longitudeAsDouble(), 2, new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    //    Utils.setResultToToast(mContext, "Goto step: " + (error == null ? "Success" : error.getDescription()));
                    Toast.makeText(MainActivity.this, "Speed 1 step: " + (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();

                }
            }));
        }

        steps.add(goinghome);


        DJICustomMission damission = new DJICustomMission(steps);

        return damission;
    }


    private void setProgressBar(final int progress) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (progress >= 100) {
                    mPB.setVisibility(View.VISIBLE);
                    mPB.setProgress(100);
                    mFadeoutAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mPB.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mPB.startAnimation(mFadeoutAnimation);
                        }
                    });

                } else if (progress < 0) {
                    mPB.setVisibility(View.INVISIBLE);
                    mPB.setProgress(0);
                } else {
                    mPB.setVisibility(View.VISIBLE);
                    mPB.setProgress(0);
                }
            }
        });
    }

}

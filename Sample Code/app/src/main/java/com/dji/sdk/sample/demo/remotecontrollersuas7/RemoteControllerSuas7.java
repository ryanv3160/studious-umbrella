/**
 * Controller for view 3
 * "Main screen"
 **/

// Package the controller belongs to
package com.dji.sdk.sample.demo.remotecontrollersuas7;

// Required imports
import android.app.Service;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;


import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.view.PresentableView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;



/*********************************************************************************************************
 * Name: RemoteControllerSuas7 Class
 * Purpose: This is the controller class for the main screen of the mobile application, view 3
 *********************************************************************************************************/
public class RemoteControllerSuas7 extends RelativeLayout
        implements View.OnClickListener,PresentableView
{
    // Button to make drone take off
    private Button btnTakeOff;
    // Button to make drone land
    private Button autoLand;

    // For pop-up window
    private TextView textView;

    // Timer for queue of the drone flight commands
    private Timer sendVirtualStickDataTimer;
    // Object to send orientation commands to drone
    private SendVirtualStickDataTask sendVirtualStickDataTask;

    private boolean commandComplete;

    long takeOffTime;

    //private Compass compass;

    /*********************************************************************************************************
     * Name: flight_controls Class
     * Purpose: This is the class, models a struct, for flight control orientation values to move the drone
     *********************************************************************************************************/
    public class FlightControls
    {
        float PITCH;
        float ROLL;
        float YAW;
        float THROTTLE;
        long milliSecondDelay;

        FlightControls(float PITCH, float ROLL, float YAW, float THROTTLE, long milliSecondDelay)
        {
            this.PITCH = PITCH;
            this.ROLL = ROLL;
            this.YAW = YAW;
            this.THROTTLE = THROTTLE;
            this.milliSecondDelay = milliSecondDelay;
        }
    }

    /**-------- TURNS : YAW --------**/
    FlightControls turn_left = new FlightControls(0.0f,0.0f,90.0f, 0.0f, 1000);
    FlightControls turn_right = new FlightControls(0.0f,0.0f,-90.0f, 0.0f, 1000);

    /**-------- SLIDE : ROLL --------**/
    FlightControls slide_left = new FlightControls(0.0f,0.f,0.0f, 0.0f, 3000);
    FlightControls slide_right = new FlightControls(0.0f,0.0f,0.0f, 0.0f, 3000);

    /**-------- FWD/AFT : PITCH --------**/
    FlightControls fly_forward = new FlightControls(10.0f,0.0f,0.0f, 0.0f, 10);
    FlightControls fly_backward = new FlightControls(0.0f,0.0f,0.0f, 0.0f, 3000);

    /**-------- UP/DWN : THROTTLE --------**/
    FlightControls fly_up = new FlightControls(0.0f,0.0f,0.0f, 0.0f, 3000);
    FlightControls fly_down = new FlightControls(0.0f,0.0f,0.0f, 0.0f, 3000);

    /**-------- HOVER --------**/
    FlightControls hover = new FlightControls(0.0f,0.0f,0.0f, 0.0f, 3000);


    /*********************************************************************************************************
     * Name: RemoteControllerSuas7()
     * Purpose: Class constructor
     * Input: Context ?
     * Returns: Nothing
     * Notes: TODO May need to add alot more here ...
     *********************************************************************************************************/
    public RemoteControllerSuas7(Context context)
    {
        super(context);
        init(context);
    }


    /*********************************************************************************************************
     * Name: getHint()
     * Purpose: Shows what java class this is in message window, top right
     * Input: None
     * Returns: Name of class to caller
     * Notes: Keep for now bc good example for messages later
     *********************************************************************************************************/
    @NonNull
    @Override
    public String getHint()
    {
        return this.getClass().getSimpleName() + ".java";
    }


    /*********************************************************************************************************
     * Name: onAttachedToWindow()
     * Purpose: When we transition to this page
     * Input: None
     * Returns: Nothing
     * Notes: Commented out portion reading the forward looking sensor data. This was the intial attempt to read
     * values from the forward looking sensors, however blocking is occuring here after the intitial value read.
     * every value checked afterwards is stale. It is only stale when we start sending the drone commands to move.
     * prior to sending any commands to the drone this will update the screen with accurate distance sensor data.
     * TODO : Figure out blocking issue, Figure out trigger based on 2 meter threshold.
     *********************************************************************************************************/
    @Override
    protected void onAttachedToWindow()
    {
        // Super class constructor
        super.onAttachedToWindow();
/*
        if (ModuleVerificationUtil.isFlightControllerAvailable())
        {
            FlightController flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
            FlightAssistant intelligentFlightAssistant = flightController.getFlightAssistant();

            if (intelligentFlightAssistant != null)
            {
                intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(new VisionDetectionState.Callback()
                {
                    @Override
                    public void onUpdate(@NonNull VisionDetectionState visionDetectionState)
                    {
                        StringBuilder stringBuilder = new StringBuilder();
                        ObstacleDetectionSector[] visionDetectionSectorArray = visionDetectionState.getDetectionSectors();
                        for (ObstacleDetectionSector visionDetectionSector : visionDetectionSectorArray)
                        {
                            visionDetectionSector.getObstacleDistanceInMeters();
                            visionDetectionSector.getWarningLevel();
                            stringBuilder.append("Obstacle distance: ").append(visionDetectionSector.getObstacleDistanceInMeters()).append("\n");
                            stringBuilder.append("Distance warning: ").append(visionDetectionSector.getWarningLevel()).append("\n");
                        }

                        stringBuilder.append("WarningLevel: ").append(visionDetectionState.getSystemWarning().name()).append("\n");
                        stringBuilder.append("Sensor state: ").append(visionDetectionState.isSensorBeingUsed()).append("\n");
                        textView.setText(stringBuilder.toString());
                    }
                });
            }
        }
        else {
            Log.i(DJISampleApplication.TAG, "onAttachedToWindow FC NOT Available");
        }

*/

    }


    /*********************************************************************************************************
     * Name: onDetachedFromWindow()
     * Purpose: Clean up task queue for virtual stick when we leave this view
     * Input: None
     * Returns: Nothing
     * Notes: TODO May need to add alot more here ...
     *********************************************************************************************************/
    @Override
    protected void onDetachedFromWindow()
    {
        if (null != sendVirtualStickDataTimer)
        {
            if (sendVirtualStickDataTask != null)
            {
                sendVirtualStickDataTask.cancel();
            }
            sendVirtualStickDataTimer.cancel();
            sendVirtualStickDataTimer.purge();
            sendVirtualStickDataTimer = null;
            sendVirtualStickDataTask = null;
        }

        super.onDetachedFromWindow();

        // This works with the distance sensors.
/*

        if (ModuleVerificationUtil.isFlightControllerAvailable())
        {
            FlightAssistant intelligentFlightAssistant = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController().getFlightAssistant();
            if(intelligentFlightAssistant != null) {
                intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(null);
            }
        }
 */
    }


    /*********************************************************************************************************
     * Name: init()
     * Purpose: Intitialize the page view
     * Input: Context?
     * Returns: Nothing
     * Notes: Understand this more TODO
     *********************************************************************************************************/
    private void init(Context context)
    {
        setClickable(true);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_mobile_rc, this, true);
        initUI();
    }


    /*********************************************************************************************************
     * Name: initUI()
     * Purpose: Intitialize the GUI buttons, so that on action they run appropriate signal handler in switch
     * Input: None
     * Returns: Nothing
     * Notes: 2 buttons and message window
     *********************************************************************************************************/
    private void initUI()
    {
        btnTakeOff = (Button) findViewById(R.id.btn_take_off);
        btnTakeOff.setOnClickListener(this);
        autoLand = (Button) findViewById(R.id.btn_auto_land);
        autoLand.setOnClickListener(this);
        textView = (TextView) findViewById(R.id.textview_simulator);
    }


    /*********************************************************************************************************
     * Name: onClick()
     * Purpose: Listeners for the button press of Take-Off and Land
     * Input: Current view
     * Returns: Nothing
     * Notes: None
     *********************************************************************************************************/
    @Override
    public void onClick(View v)
    {
        // Create flight control object
        FlightController flightController = ModuleVerificationUtil.getFlightController();

        // Make sure it is not null
        if (flightController == null)
        {
            return;
        }

        // Configure the flight controls
        configFlightControls(flightController);

        // Switch off of the button pressed
        switch (v.getId())
        {
            // Take off button was pressed
            // Also considered the start mission.
            case R.id.btn_take_off:

                /**---------------------------------------**/
                /** Here start calling test cases created **/
                /**---------------------------------------**/
                //test1(flightController); //Pass
                //test2(flightController); //Pass
                //test3(flightController); //Pass
                //test4(flightController); //Pass
                test5(flightController);   //Pass


                // Land button was pressed
            case R.id.btn_auto_land:
                land(flightController);
                break;

            // Default case currently does nothing
            default:
                break;
        }
    }


    /*********************************************************************************************************
     * Name: getDescription()
     * Purpose: I think this sets up the page layout
     * Input: None
     * Returns: ?
     * Notes: TODO Figure out what this does, something to do with the layout of the page
     *********************************************************************************************************/
    @Override
    public int getDescription()
    {
        return R.string.component_listview_mobile_remote_controller;
    }


    /*********************************************************************************************************
     * ------------------------------------------ T E S T 1 ------------------------------------------------ *
     * Take off , hover 5 seconds, land
     *********************************************************************************************************/
    public void test1(FlightController flightController)
    {
        /** Start Take-off Sequence **/
        takeOff(flightController);

        /** Set Pause for take-off **/
        // Get the time when motors start
        takeOffTime = System.currentTimeMillis();
        // Loop and wait until at stable 4 foot hover, roughly 6 seconds, but be safe with 10 seconds
        while(System.currentTimeMillis() - takeOffTime < 7000)
        {
        }

        /** End Test Land the drone **/
        land(flightController);
    }

    /*********************************************************************************************************
     * ------------------------------------------ T E S T 2 ------------------------------------------------ *
     * Take off , hover 5 seconds, turn left 90, hover 5 seconds, land
     *********************************************************************************************************/
    public void test2(FlightController flightController)
    {
        /** Start Take-off Sequence **/
        takeOff(flightController);

        /** Set Pause for take-off **/
        // Get the time when motors start
        takeOffTime = System.currentTimeMillis();
        // Loop and wait until at stable 4 foot hover, roughly 6 seconds, but be safe with 10 seconds
        while(System.currentTimeMillis() - takeOffTime < 7000)
        {
        }

        /** Setup up for left turn **/
        // Initial set command complete for next command
        commandComplete = false;
        // Send command
        wrapFlightTask(turn_left);
        while(commandComplete == true)
        {
        }

        /** Pause Inbetween Commands**/
        // 5 second pause
        long pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 5000)
        {
        }

        /** End Test Land the drone **/
        land(flightController);
    }

    /*********************************************************************************************************
     * ------------------------------------------ T E S T 3 ------------------------------------------------ *
     * Take off , hover 5 seconds, turn right 90, hover 5 seconds, land
     *********************************************************************************************************/
    public void test3(FlightController flightController)
    {
        /** Start Take-off Sequence **/
        takeOff(flightController);

        /** Set Pause for take-off **/
        // Get the time when motors start
        takeOffTime = System.currentTimeMillis();
        // Loop and wait until at stable 4 foot hover, roughly 6 seconds, but be safe with 10 seconds
        while(System.currentTimeMillis() - takeOffTime < 7000)
        {
        }

        /** Setup up for right turn **/
        // Initial set command complete for next command
        commandComplete = false;
        // Send command
        wrapFlightTask(turn_right);
        while(commandComplete == true)
        {
        }

        /** Pause Inbetween Commands**/
        // 5 second pause
        long pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 5000)
        {
        }

        /** End Test Land the drone **/
        land(flightController);
    }


    /*********************************************************************************************************
     * ------------------------------------------ T E S T 4 ------------------------------------------------ *
     * Take off , hover 5 seconds, turn left 90, hover 5 seconds, turn right 90, hover 5 seconds, land
     *********************************************************************************************************/
    public void test4(FlightController flightController)
    {
        /** Start Take-off Sequence **/
        takeOff(flightController);

        /** Set Pause for take-off **/
        // Get the time when motors start
        takeOffTime = System.currentTimeMillis();
        // Loop and wait until at stable 4 foot hover, roughly 6 seconds, but be safe with 10 seconds
        while(System.currentTimeMillis() - takeOffTime < 7000)
        {
        }

        /** Setup up for left turn **/
        // Initial set command complete for next command
        commandComplete = false;
        // Send command
        wrapFlightTask(turn_left);
        while(commandComplete == true)
        {
        }
        /** Pause Inbetween Commands**/
        long pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 5000)
        {
        }

        /** Setup for right turn **/
        commandComplete = false;
        // Send command
        wrapFlightTask(turn_right);
        while(commandComplete == true)
        {
        }
        /** Pause Inbetween Commands**/
        pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 5000)
        {
        }

        /** Setup for fly forward **/
        // Initial set command complete for next command
        commandComplete = false;
        // Send command
        wrapFlightTask(fly_forward);
        while(commandComplete == true)
        {
        }
        /** Pause Inbetween Commands**/
        pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 2000)
        {
        }

        /** End Test Land the drone **/
        land(flightController);
    }

    /*********************************************************************************************************
     * ------------------------------------------ T E S T 5 ------------------------------------------------ *
     * Take off , hover 5 seconds, turn left 90, hover 5 seconds, turn right 90, hover 5 seconds,
     * fly fwd 5 meters, hover 5 seconds, land
     *********************************************************************************************************/
    public void test5(FlightController flightController)
    {
        /** Start Take-off Sequence **/
        takeOff(flightController);

        /** Set Pause for take-off **/
        // Get the time when motors start
        takeOffTime = System.currentTimeMillis();
        // Loop and wait until at stable 4 foot hover, roughly 6 seconds, but be safe with 10 seconds
        while(System.currentTimeMillis() - takeOffTime < 7000)
        {
        }

        /** Setup up for left turn **/
        // Initial set command complete for next command
        commandComplete = false;
        // Send command
        wrapFlightTask(turn_left);
        while(commandComplete == true)
        {
        }
        /** Pause Inbetween Commands**/
        long pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 5000)
        {
        }

        /** Setup for right turn **/
        commandComplete = false;
        // Send command
        wrapFlightTask(turn_right);
        while(commandComplete == true)
        {
        }
        /** Pause Inbetween Commands**/
        pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 5000)
        {
        }

        /** Setup for fly forward **/
        // Initial set command complete for next command
        commandComplete = false;
        // Send command
        wrapFlightTask(fly_forward);
        while(commandComplete == true)
        {
        }
        /** Pause Inbetween Commands**/
        pauseTime = System.currentTimeMillis();
        while(System.currentTimeMillis() - pauseTime < 2000)
        {
        }

        /** End Test Land the drone **/
        land(flightController);
    }




    /*********************************************************************************************************
     * Name: configControls Function
     * Purpose: Function to configure pitch, roll, yaw, thrust to Angular mode.
     * Input: FlightController object
     * Returns: Nothing
     * Notes: We want degree inputs not velocity inputs yet. Velocity will come later for timeliness.
     *********************************************************************************************************/
    public void configFlightControls(FlightController flightController)
    {
        //TODO add max flight height
        //TODO add other flight limitations as we discover the need for them.
        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback()
        {
            @Override
            public void onResult(DJIError djiError)
            {
                DialogUtils.showDialogBasedOnError(getContext(), djiError);
            }
        });

        //Sets the virtual stick vertical control values to be an altitude.
        //Maximum position is defined as 500 m. Minimum position is defined as 0 m.
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);

        //Sets the yaw values to be an angle relative to the north.
        //Positive and negative yaw angle is for the aircraft rotating clockwise
        //and counterclockwise, respectively. Maximum yaw angle is defined as 180 degrees.
        //Minimum yaw angle is defined as -180 degrees.
        flightController.setYawControlMode(YawControlMode.ANGLE);

        //Sets the roll and pitch values to be an angle relative to a level aircraft.
        //In the body coordinate system, positive and negative pitch angle is for the
        //aircraft rotating about the y-axis in the positive direction or negative direction,
        //respectively. Positive and negative roll angle is the positive direction or negative
        //direction rotation angle about the x-axis, respectively. However in the ground
        //coordinate system, positive and negative pitch angle is the angle value for the aircraft
        // moving south and north, respectively. Positive and negative roll angle is the angle when
        //the aircraft is moving east and west, respectively. Maximum angle is defined as 30 degrees.
        //Minimum angle is defined as -30 degrees.
        flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE); //velocity

        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.GROUND); //ground

    }//Func


    /*********************************************************************************************************
     * Name: takeoff Function
     * Purpose: Class is to call the flight controller take off function
     * Input: FlightController object
     * Returns: Nothing
     * Notes: None
     *********************************************************************************************************/
    public void takeOff(FlightController flightController)
    {
        flightController.startTakeoff(new CommonCallbacks.CompletionCallback()
        {
            @Override
            public void onResult(DJIError djiError)
            {
                DialogUtils.showDialogBasedOnError(getContext(), djiError);
            }
        });
    }


    /*********************************************************************************************************
     * Name: land Function
     * Purpose: Class is to call the flight controller auto land function
     * Input: FlightController object
     * Returns: Nothing
     * Notes: None
     *********************************************************************************************************/
    public void land(FlightController flightController)
    {
        flightController.startLanding(new CommonCallbacks.CompletionCallback()
        {
            @Override
            public void onResult(DJIError djiError)
            {
                DialogUtils.showDialogBasedOnError(getContext(), djiError);
            }
        });
    }


    /*********************************************************************************************************
     * Name: wrapFlightTask Function
     * Purpose: Class is to prepare a flight command to be scheduled
     * Input: FlightControls object
     * Returns: Nothing
     * Notes: None
     *********************************************************************************************************/
    public void wrapFlightTask(FlightControls flight_controls)
    {
        if (null == sendVirtualStickDataTimer)
        {
            sendVirtualStickDataTask = new SendVirtualStickDataTask(flight_controls);
            sendVirtualStickDataTimer = new Timer();
            sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 0, 200);
        }
    }






    /*********************************************************************************************************
     * Name: SendVirtualStickDataTask Class
     * Purpose: This is the function that will schedule the pitch, roll, thrust, yaw commands to be sent to
     * the firmware on the drone from the mobile application.
     * Input: 
     * Returns: Nothing
     * Notes:
     *********************************************************************************************************/
    private class SendVirtualStickDataTask extends TimerTask
    {
        private FlightControls flight_controls;
        private long startTime;

        // Class constructor
        SendVirtualStickDataTask(FlightControls flight_controls)
        {
            this.flight_controls = flight_controls;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run()
        {
            // If delay has expired
            if (System.currentTimeMillis() - this.startTime > flight_controls.milliSecondDelay)
            {
                if (ModuleVerificationUtil.isFlightControllerAvailable())
                {
                    DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(
                            new FlightControlData(0.0f, 0.0f, 0.0f, 0.0f),
                            new CommonCallbacks.CompletionCallback()
                            {@Override public void onResult(DJIError djiError) {} });
                }//If
                commandComplete = true;
            }
            // Continue direction of flight
            else
            {
                if (ModuleVerificationUtil.isFlightControllerAvailable())
                {
                    DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(
                            new FlightControlData(flight_controls.PITCH, flight_controls.ROLL, flight_controls.YAW, flight_controls.THROTTLE),
                            new CommonCallbacks.CompletionCallback()
                            {@Override public void onResult(DJIError djiError) {} });
                }//If
            }//Else
        }//Func
    }//Class

}//Class


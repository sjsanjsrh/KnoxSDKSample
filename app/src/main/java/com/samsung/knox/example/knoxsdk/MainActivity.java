/**
 * DISCLAIMER: PLEASE TAKE NOTE THAT THE SAMPLE APPLICATION AND
 * SOURCE CODE DESCRIBED HEREIN IS PROVIDED FOR TESTING PURPOSES ONLY.
 * <p>
 * Samsung expressly disclaims any and all warranties of any kind,
 * whether express or implied, including but not limited to the implied warranties and conditions
 * of merchantability, fitness for com.samsung.knoxsdksample particular purpose and non-infringement.
 * Further, Samsung does not represent or warrant that any portion of the sample application and
 * source code is free of inaccuracies, errors, bugs or interruptions, or is reliable,
 * accurate, complete, or otherwise valid. The sample application and source code is provided
 * "as is" and "as available", without any warranty of any kind from Samsung.
 * <p>
 * Your use of the sample application and source code is at its own discretion and risk,
 * and licensee will be solely responsible for any damage that results from the use of the sample
 * application and source code including, but not limited to, any damage to your computer system or
 * platform. For the purpose of clarity, the sample code is licensed “as is” and
 * licenses bears the risk of using it.
 * <p>
 * Samsung shall not be liable for any direct, indirect or consequential damages or
 * costs of any type arising out of any action taken by you or others related to the sample application
 * and source code.
 */
package com.samsung.knox.example.knoxsdk;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.custom.CustomDeviceManager;
import com.samsung.android.knox.custom.SystemManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;
import com.samsung.android.knox.restriction.RestrictionPolicy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This activity displays the main UI of the application. This is a simple application to restrict
 * use of the device camera using the Samsung Knox SDK.
 * Read more about the Knox SDK here:
 * https://seap.samsung.com/sdk
 * <p>
 * Please insert a valid development Knox Platform for Enterprise (KPE - Development) key to {@link Constants}.
 * </p>
 *
 * @author Samsung R&D Canada Technical Support
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final int DEVICE_ADMIN_ADD_RESULT_ENABLE = 1;
    private Button mToggleAdminBtn;
    private DevicePolicyManager mDPM;
    private ComponentName mDeviceAdmin;
    private Utils mUtils;
    private EditText mSoundDelayEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }
        }

        //...called when the activity is starting. This is where most initialization should go.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView LogView = (TextView) findViewById(R.id.logview_id);
        LogView.setMovementMethod(new ScrollingMovementMethod());
        mUtils = new Utils(LogView, TAG);

        // Check if device supports Knox SDK
        mUtils.checkApiLevel(24, this);

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdmin = new ComponentName(MainActivity.this, SampleAdminReceiver.class);

        mToggleAdminBtn = (Button) findViewById(R.id.ToggleAdmin);
        mToggleAdminBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {toggleAdmin();
            }
        });
        Button ActivateLicencebtn = (Button) findViewById(R.id.ActivateLicencebtn);
        ActivateLicencebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateLicence();
            }
        });
        Button DeactivateLicencebtn = (Button) findViewById(R.id.DeactivateLicensebtn);
        DeactivateLicencebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deactivateLicense();
            }
        });
        Button ChangeBootlogobtn = (Button) findViewById(R.id.ChangeBootlogobtn);
        ChangeBootlogobtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                changeBootlogo();
            }
        });

        mSoundDelayEdit = (EditText) findViewById(R.id.soundDelayEdit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshButtons();
    }

    /** Handle result of device administrator activation request */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEVICE_ADMIN_ADD_RESULT_ENABLE) {
            switch (resultCode) {
                // End user cancels the request
                case Activity.RESULT_CANCELED:
                    mUtils.log(getString(R.string.admin_cancelled));
                    break;
                // End user accepts the request
                case Activity.RESULT_OK:
                    mUtils.log(getString(R.string.admin_activated));
                    refreshButtons();
                    break;
            }
        }
    }

    /** Present a dialog to activate device administrator for this application */
    private void toggleAdmin() {
        boolean adminState = mDPM.isAdminActive(mDeviceAdmin);
        if (adminState) {
            mUtils.log(getString(R.string.deactivating_admin));
            // Deactivate application as device administrator
            mDPM.removeActiveAdmin(new ComponentName(this, SampleAdminReceiver.class));
            mToggleAdminBtn.setText(getString(R.string.activate_admin));
        } else {
            try {
                mUtils.log(getString(R.string.activating_admin));
                // Ask the user to add a new device administrator to the system
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                // Start the add device administrator activity
                startActivityForResult(intent, DEVICE_ADMIN_ADD_RESULT_ENABLE);

            } catch (Exception e) {
                mUtils.processException(e, TAG);
            }
        }
    }

    /**
     * Note that embedding your license key in code is unsafe and is done here for
     * demonstration purposes only.
     * Please visit https://seap.samsung.com/license-keys/about. for more details about license
     * keys.
     */
    private void activateLicence() {

        // Instantiate the KnoxEnterpriseLicenseManager class to use the activateLicense method
        KnoxEnterpriseLicenseManager licenseManager = KnoxEnterpriseLicenseManager.getInstance(this);

        try {
            // License Activation TODO Add license key to Constants.java
            licenseManager.activateLicense(Constants.KPE_LICENSE_KEY);
            mUtils.log(getResources().getString(R.string.license_progress));

        } catch (Exception e) {
            mUtils.processException(e,TAG);
        }
    }

    /**
     *  Deactivate license key.
     */
    private void deactivateLicense() {

        // Instantiate the KnoxEnterpriseLicenseManager class to use the deactivateLicense method
        KnoxEnterpriseLicenseManager licenseManager = KnoxEnterpriseLicenseManager.getInstance(this);

        try {
            // License deactivation
            licenseManager.deActivateLicense(Constants.KPE_LICENSE_KEY);
            mUtils.log(getResources().getString(R.string.license_deactivation));

        } catch (Exception e) {
            mUtils.processException(e, TAG);
        }
    }

    /** Toggle the restriction of the device camera on/off. When set to disabled, the end user will
    * be unable to use the device cameras.
    */
    private void toggleCameraState() {

        // Instantiate the EnterpriseDeviceManager class
        EnterpriseDeviceManager enterpriseDeviceManager = EnterpriseDeviceManager.getInstance(this);
        // Get the RestrictionPolicy class where the setCameraState method lives
        RestrictionPolicy restrictionPolicy = enterpriseDeviceManager.getRestrictionPolicy();

        boolean cameraEnabled = restrictionPolicy.isCameraEnabled(false);

        try {
            // Toggle the camera state on/off
            restrictionPolicy.setCameraState(!cameraEnabled); //true
            mUtils.log(getResources().getString(R.string.camera_state, !cameraEnabled));

        } catch (Exception e) {
            mUtils.processException(e,TAG);
        }


    }

    public void copyFile(String inFilePath, String outFilePath) throws IOException {
        try {
            mUtils.log("Copying \"" + inFilePath + "\"...");
            FileInputStream inputStream = new FileInputStream(inFilePath);
            FileOutputStream outputStream = new FileOutputStream(outFilePath);

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer, 0, 1024)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }


            outputStream.close();
            inputStream.close();
        }catch (Exception e){
            mUtils.log("copyError :\"" + inFilePath + "\" to \"" + outFilePath + "\"");
            throw e;
        }

    }

    /** Change boot animation
     * be changed boot animation.
     */
    private void changeBootlogo() {

        int SoundDelay = 0; // milliseconds
        SystemManager kcsm;

        try {
            SoundDelay = Integer.parseInt(mSoundDelayEdit.getText().toString());
        } catch (Exception e) {}
        try {
            CustomDeviceManager cdm = CustomDeviceManager.getInstance();
            kcsm = cdm.getSystemManager();
        } catch (Exception e) {
            mUtils.processException(e,TAG);
            return;
        }

        try {
            mUtils.log("Booting Animation Changeing...");

            String lootDir = Environment.getExternalStoragePublicDirectory("BOOT_LOGO").getPath();

            String pbootupFile = lootDir + "/bootup.qmg";
            String pbootloopFile = lootDir + "/bootloop.qmg";
            String psoundFile = lootDir + "/bootup.ogg";

            String bootupFile = getFilesDir() + "/bootup.qmg";
            String bootloopFile = getFilesDir() + "/bootloop.qmg";
            String soundFile = getFilesDir() + "/bootup.ogg";

            copyFile(pbootupFile, bootupFile);
            copyFile(pbootloopFile, bootloopFile);
            copyFile(psoundFile, soundFile);

            File fileBootup = new File(bootupFile);
            ParcelFileDescriptor bootupFD = ParcelFileDescriptor.open(new File(bootupFile), ParcelFileDescriptor.MODE_READ_ONLY);

            File fileLoop = new File(bootloopFile);
            ParcelFileDescriptor bootloopFD = ParcelFileDescriptor.open(new File(bootloopFile), ParcelFileDescriptor.MODE_READ_ONLY);

            File fileSound = new File(soundFile);
            ParcelFileDescriptor soundFD = ParcelFileDescriptor.open(new File(soundFile), ParcelFileDescriptor.MODE_READ_ONLY);

            int state = kcsm.setBootingAnimation(bootupFD, bootloopFD, soundFD, SoundDelay);

            if(state == CustomDeviceManager.SUCCESS)
                mUtils.log("Booting Animation Changed SUCCESS");
            else
                mUtils.log("Booting Animation Changed Error(" + String.valueOf(state) + ")");
        } catch (Exception e) {
            mUtils.processException(e,TAG);
        }
        try {
            mUtils.log("Shutdown Animation Changeing...");

            String lootDir = Environment.getExternalStoragePublicDirectory("BOOT_LOGO").getPath();

            String pshutdownAnim = lootDir + "/shutdown.qmg";
            String psoundFile = lootDir + "/shutdown.ogg";

            String shutdownAnim = getFilesDir() + "/shutdown.qmg";
            String soundFile = getFilesDir() + "/shutdown.ogg";

            copyFile(pshutdownAnim, shutdownAnim);
            copyFile(psoundFile, soundFile);

            File fileShutdown = new File(shutdownAnim);
            fileShutdown.setReadable(true, false);
            ParcelFileDescriptor shutdownFD = ParcelFileDescriptor.open(new File(shutdownAnim), ParcelFileDescriptor.MODE_READ_ONLY);

            File fileSound = new File(soundFile);
            fileSound.setReadable(true, false);
            ParcelFileDescriptor soundFD = ParcelFileDescriptor.open(new File(soundFile), ParcelFileDescriptor.MODE_READ_ONLY);

            int state = kcsm.setShuttingDownAnimation(shutdownFD, soundFD);

            if(state == CustomDeviceManager.SUCCESS)
                mUtils.log("Shutdown Animation Changed SUCCESS");
            else
                mUtils.log("Shutdown Animation Changed Error(" + String.valueOf(state) + ")");
        } catch (Exception e) {
            mUtils.processException(e,TAG);
        }

    }

    /** Update button state */
    private void refreshButtons() {
        boolean adminState = mDPM.isAdminActive(mDeviceAdmin);

        if (!adminState) {
            mToggleAdminBtn.setText(getString(R.string.activate_admin));

        } else {
            mToggleAdminBtn.setText(getString(R.string.deactivate_admin));
        }
    }
}
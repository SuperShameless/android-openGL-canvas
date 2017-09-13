/*
 * Copyright (C) 2008 ZXing authors
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

package com.chillingvan.canvasglsample.textureView.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import com.chillingvan.canvasglsample.textureView.camera.open.OpenCamera;
import com.chillingvan.canvasglsample.textureView.camera.open.OpenCameraInterface;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
@SuppressWarnings("deprecation, unused") // camera APIs
public final class CameraManager implements ICameraManager {

    private static final String TAG = "CameraManager";

    private final CameraConfigurationManager configManager;
    private OpenCamera camera;
    private AutoFocusManager autoFocusManager;
    private boolean previewing;
    private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
    private int requestedCameraFacing = OpenCameraInterface.CAMERA_FACING_BACK;

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PreviewCallback previewCallback;
    /**
     * Pictures are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PictureCallback pictureCallback;

    public CameraManager(Context context) {
        this.configManager = new CameraConfigurationManager(context);
        previewCallback = new PreviewCallback(configManager);
        pictureCallback = new PictureCallback(configManager);
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param surfaceHolder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    @Override
    public synchronized void openDriver(SurfaceHolder surfaceHolder) throws IOException {
        Camera cameraObject = openInner();
        cameraObject.setPreviewDisplay(surfaceHolder);
    }

    @Override
    public synchronized void openDriver(SurfaceTexture surfaceTexture) throws IOException {
        Camera cameraObject = openInner();
        cameraObject.setPreviewTexture(surfaceTexture);
    }

    @Override
    public synchronized boolean isOpen() {
        return camera != null;
    }

    /**
     * Closes the camera driver if still in use.
     */
    @Override
    public synchronized void closeDriver() {
        Log.d(TAG, "closeDriver");
        if (camera != null) {
            camera.getCamera().release();
            camera = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    @Override
    public synchronized void startPreview() {
        Log.d(TAG, "startPreview");
        OpenCamera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.getCamera().startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(theCamera.getCamera());
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    @Override
    public synchronized void stopPreview() {
        Log.d(TAG, "stopPreview");
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.getCamera().stopPreview();
            previewCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    @Override
    public int getNeedRotation() {
        return configManager.getCWNeededRotation();
    }

    /**
     * Convenience method to switch light
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    @Override
    public synchronized void setTorch(boolean newSetting) {
        Log.d(TAG, "setTorch " + newSetting);
        OpenCamera theCamera = camera;
        if (theCamera != null) {
            if (newSetting != configManager.getTorchState(theCamera.getCamera())) {
                boolean wasAutoFocusManager = autoFocusManager != null;
                if (wasAutoFocusManager) {
                    autoFocusManager.stop();
                    autoFocusManager = null;
                }
                configManager.setTorch(theCamera.getCamera(), newSetting);
                if (wasAutoFocusManager) {
                    autoFocusManager = new AutoFocusManager(theCamera.getCamera());
                    autoFocusManager.start();
                }
            }
        }
    }

    @Override
    public synchronized boolean isTorchOn() {
        boolean isOn = false;
        OpenCamera theCamera = camera;
        if (theCamera != null) {
            isOn = configManager.getTorchState(theCamera.getCamera());
        }
        return isOn;
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    @Override
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        OpenCamera theCamera = camera;
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
        }
    }

    /**
     * A picture will be returned to the handler supplied. The jpg data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    @Override
    public synchronized void takePicture(Handler handler, int message) {
        Log.d(TAG, "takePicture");
        OpenCamera theCamera = camera;
        if (theCamera != null && previewing) {
            pictureCallback.setHandler(handler, message);
            theCamera.getCamera().takePicture(null, null, pictureCallback);
        }
    }

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    @Override
    public synchronized void setManualCameraId(int cameraId) {
        Log.d(TAG, "setManualCameraId " + cameraId);
        requestedCameraId = cameraId;
    }

    /**
     * Allows third party apps to specify the camera facing, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param facing camera facing of the camera to use.
     *               Default value {@link OpenCameraInterface#CAMERA_FACING_UNSPECIFIED}.
     */
    @Override
    public synchronized void setManualCameraFacing(int facing) {
        Log.d(TAG, "setManualCameraFacing " + facing);
        requestedCameraFacing = facing;
    }

    public synchronized boolean isSupportZoom() {
        boolean support = false;
        if (camera != null) {
            Camera theCamera = camera.getCamera();
            if (theCamera != null) {
                Camera.Parameters parameters = theCamera.getParameters();
                support = parameters.isZoomSupported() && parameters.isSmoothZoomSupported();
            }
        }
        return support;
    }

    public synchronized void setZoom(double targetZoomRatio) {
        Log.d(TAG, "setZoom " + targetZoomRatio);
        if (camera != null) {
            Camera theCamera = camera.getCamera();
            if (theCamera != null) {
                Camera.Parameters parameters = theCamera.getParameters();
                CameraConfigurationUtils.setZoom(parameters, targetZoomRatio);
            }
        }
    }

    public synchronized double getMaxZoomRatio() {
        double maxZoomRatio = 1.0;
        if (camera != null) {
            Camera theCamera = camera.getCamera();
            if (theCamera != null) {
                Camera.Parameters parameters = theCamera.getParameters();
                maxZoomRatio = parameters.getMaxZoom() * 1.0 / 100;
            }
        }
        return maxZoomRatio;
    }

    public synchronized double getCurrentZoomRatio() {
        double curZoomRatio = 1.0;
        if (camera != null) {
            Camera theCamera = camera.getCamera();
            if (theCamera != null) {
                Camera.Parameters parameters = theCamera.getParameters();
                int curZoom = parameters.getZoom();
                curZoomRatio = (curZoom == 0 ? 1 : curZoom) * 1.0 / 100;
            }
        }
        return curZoomRatio;
    }

    // ----------------------------------------------------

    private Camera openInner() throws IOException {
        Log.d(TAG, "openDriver");

        OpenCamera theCamera = camera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(requestedCameraId, requestedCameraFacing);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            camera = theCamera;
        }

        configManager.initFromCameraParameters(theCamera);

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();

        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }

        return cameraObject;
    }
}

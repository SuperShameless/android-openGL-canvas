package com.chillingvan.canvasglsample.textureView.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public interface ICameraManager {
    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param surfaceHolder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    void openDriver(SurfaceHolder surfaceHolder) throws IOException;

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param surfaceTexture The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    void openDriver(SurfaceTexture surfaceTexture) throws IOException;

    /**
     * Is the camera driver opened
     *
     * @return true if opened
     */
    boolean isOpen();

    /**
     * Closes the camera driver if still in use.
     */
    void closeDriver();

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    void startPreview();

    /**
     * Tells the camera to stop drawing preview frames.
     */
    void stopPreview();

    int getNeedRotation();

    /**
     * Convenience method to switch light
     *
     * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
     */
    void setTorch(boolean newSetting);

    /**
     * Is torch opened
     *
     * @return true if opened
     */
    boolean isTorchOn();

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    void requestPreviewFrame(Handler handler, int message);

    /**
     * A picture will be returned to the handler supplied. The jpg data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    void takePicture(Handler handler, int message);

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    void setManualCameraId(int cameraId);

    /**
     * Allows third party apps to specify the camera facing, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param facing camera facing of the camera to use.
     *               Default value {@link com.chillingvan.canvasglsample.textureView.camera.open.OpenCameraInterface#CAMERA_FACING_UNSPECIFIED}.
     */
    void setManualCameraFacing(int facing);

    /**
     * Is support zoom in this camera
     *
     * @return true if support
     */
    boolean isSupportZoom();

    /**
     * Set zoom value
     *
     * @param targetZoomRatio 1.0 ~ max zoom ratio
     */
    void setZoom(double targetZoomRatio);

    /**
     * Get max zoom ratio
     *
     * @return max zoom ratio, 0 means not support zoom
     */
    double getMaxZoomRatio();

    /**
     * Get current zoom ratio
     *
     * @return current zoom ratio
     */
    double getCurrentZoomRatio();
}

/*
 *
 *  *
 *  *  * Copyright (C) 2016 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.chillingvan.canvasglsample.textureView;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.GLView;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.canvasgl.textureFilter.PixelationFilter;
import com.chillingvan.canvasglsample.R;
import com.chillingvan.canvasglsample.textureView.camera.CameraManager;
import com.chillingvan.canvasglsample.textureView.camera.open.OpenCameraInterface;

import java.io.IOException;

public class TextureCameraActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "TextureCameraActivity";

    private int cameraFacing = OpenCameraInterface.CAMERA_FACING_BACK;
    private CameraManager cameraManager;
    private CameraPreviewTextureView cameraTextureView;
    private PreviewConsumerTextureView previewConsumerTextureView;
    private Button toggle;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_canvas);

        toggle = (Button) findViewById(R.id.toggle);
        imageView = (ImageView) findViewById(R.id.image_v);

        toggle.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toggle: {
                cameraFacing = cameraFacing != OpenCameraInterface.CAMERA_FACING_FRONT ?
                        OpenCameraInterface.CAMERA_FACING_FRONT :
                        OpenCameraInterface.CAMERA_FACING_BACK;
                closeCamera();
                openCamera();
                break;
            }
        }
    }

    private void initCameraTexture() {
        cameraTextureView = (CameraPreviewTextureView) findViewById(R.id.camera_texture);
        previewConsumerTextureView = (PreviewConsumerTextureView) findViewById(R.id.camera_texture2);
        cameraTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraTextureView.getDrawingBitmap(new Rect(0, 0, v.getWidth(), v.getHeight()), new GLView.GetDrawingCacheCallback() {
                    @Override
                    public void onFetch(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                });

            }
        });
        previewConsumerTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewConsumerTextureView.getDrawingBitmap(new Rect(0, 0, v.getWidth(), v.getHeight()), new GLView.GetDrawingCacheCallback() {
                    @Override
                    public void onFetch(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });

        previewConsumerTextureView.setTextureFilter(new PixelationFilter(15));
        cameraTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                previewConsumerTextureView.setSharedEglContext(eglContext);
            }
        });
        cameraTextureView.setOnSurfaceTextureSet(new GLSurfaceTextureProducerView.OnSurfaceTextureSet() {
            @Override
            public void onSet(SurfaceTexture surfaceTexture, RawTexture surfaceTextureRelatedTexture) {
                Loggers.d(TAG, String.format("onSet: "));
                previewConsumerTextureView.setSharedTexture(surfaceTextureRelatedTexture, surfaceTexture);
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraTextureView.requestRenderAndWait();
                        previewConsumerTextureView.requestRenderAndWait();
                    }
                });

                openCamera();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        Loggers.d("TextureCameraActivity", String.format("onResume: "));
        initCameraTexture();
        openCamera();
        cameraTextureView.onResume();
        previewConsumerTextureView.onResume();
    }

    private void openCamera() {
        SurfaceTexture surfaceTexture = cameraTextureView.getSurfaceTexture();
        if (surfaceTexture == null) {
            Loggers.w(TAG, "No surfaceTexture provided");
            return;
        }

        if (cameraManager == null) {
            cameraManager = new CameraManager(getApplicationContext());
        }

        if (cameraManager.isOpen()) {
            Loggers.w(TAG, "openCamera() while already open -- late SurfaceView callback?");
            return;
        }

        try {
            cameraManager.setManualCameraFacing(cameraFacing);
            cameraManager.openDriver(surfaceTexture);
            cameraManager.startPreview();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        cameraManager.stopPreview();
        cameraManager.closeDriver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Loggers.d("TextureCameraActivity", String.format("onPause: "));
        closeCamera();
        cameraTextureView.onPause();
        previewConsumerTextureView.onPause();
    }
}

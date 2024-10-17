/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tensorflow.lite.examples.objectdetection.fragments

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"
    private lateinit var windowManager: WindowManager
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val animatorSet: AnimatorSet = AnimatorSet()
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService


    private var lastRecordTime = 0L // 上次记录的时间戳
    private val handler = Handler(Looper.getMainLooper())


    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager


//        playRight()
        // 显示"请靠近"文案
        handler.postDelayed({
            // 在这里更新UI显示"请靠近"文案
            showTipsText("请靠近")
        }, 3000)

        // 显示"向左移动相机"文案
        handler.postDelayed({
            playLeft()
            // 在这里更新UI显示"向左移动相机"文案
            showTipsText("向左移动相机")
        }, 8000)

        // 显示"识别成功"文案
        handler.postDelayed({
            playLeftStop()
            // 在这里更新UI显示"识别成功"文案
            showTipsText("识别成功")
        }, 10000)
        return fragmentCameraBinding.root
    }

    private fun playLeft() {
//        val animatorSet = AnimatorSet()
        val imageView = fragmentCameraBinding.arrowTop
        imageView.visibility = View.VISIBLE
        val translationAnim = ObjectAnimator.ofFloat(imageView, "translationY", 200f, 50f)
        translationAnim.repeatCount = 3 // 设置重复次数为无限
        translationAnim.duration = 1000 // 设置动画持续时间
        translationAnim.interpolator = LinearInterpolator() // 设置插值器，可以使动画匀速播放
        val alphaAnim = ObjectAnimator.ofFloat(imageView, "alpha", 1.0f, 0.0f)
        alphaAnim.repeatCount = ValueAnimator.INFINITE
        alphaAnim.duration = 1000
        animatorSet.playTogether(translationAnim, alphaAnim)
        // 设置目标View,播放动画
        animatorSet.start()
    }

    private fun playLeftStop() {
        val imageView = fragmentCameraBinding.arrowTop
        imageView.visibility = View.INVISIBLE
        // 设置目标View,播放动画
        animatorSet.cancel()
    }

//    private fun playRight() {
//        val animatorSet = AnimatorSet()
//        val imageView = fragmentCameraBinding.arrowBottom
//        imageView.visibility = View.VISIBLE
//        val translationAnim = ObjectAnimator.ofFloat(imageView, "translationY", 680f, 830f)
//        translationAnim.repeatCount = ValueAnimator.INFINITE // 设置重复次数为无限
//        translationAnim.duration = 1000 // 设置动画持续时间
//        translationAnim.interpolator = LinearInterpolator() // 设置插值器，可以使动画匀速播放
//        val alphaAnim = ObjectAnimator.ofFloat(imageView, "alpha", 1.0f, 0.0f)
//        alphaAnim.repeatCount = ValueAnimator.INFINITE
//        alphaAnim.duration = 1000
//        animatorSet.playTogether(translationAnim, alphaAnim)
//        // 设置目标View,播放动画
//        animatorSet.start()
//    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }


        // Attach listeners to UI control widgets
//        initBottomSheetControls()
    }

//    private fun initBottomSheetControls() {
//        // When clicked, lower detection score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
//            if (objectDetectorHelper.threshold >= 0.1) {
//                objectDetectorHelper.threshold -= 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise detection score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
//            if (objectDetectorHelper.threshold <= 0.8) {
//                objectDetectorHelper.threshold += 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, reduce the number of objects that can be detected at a time
//        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
//            if (objectDetectorHelper.maxResults > 1) {
//                objectDetectorHelper.maxResults--
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, increase the number of objects that can be detected at a time
//        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
//            if (objectDetectorHelper.maxResults < 5) {
//                objectDetectorHelper.maxResults++
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, decrease the number of threads used for detection
//        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
//            if (objectDetectorHelper.numThreads > 1) {
//                objectDetectorHelper.numThreads--
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, increase the number of threads used for detection
//        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
//            if (objectDetectorHelper.numThreads < 4) {
//                objectDetectorHelper.numThreads++
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, change the underlying hardware used for inference. Current options are CPU
//        // GPU, and NNAPI
//        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
//        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    objectDetectorHelper.currentDelegate = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
//
//        // When clicked, change the underlying model used for object detection
//        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
//        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    objectDetectorHelper.currentModel = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
//    }

    // Update the values displayed in the bottom sheet. Reset detector.
//    private fun updateControlsUi() {
//        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
//            objectDetectorHelper.maxResults.toString()
//        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
//            String.format("%.2f", objectDetectorHelper.threshold)
//        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
//            objectDetectorHelper.numThreads.toString()
//
//        // Needs to be cleared instead of reinitialized because the GPU
//        // delegate needs to be initialized on the thread using it when applicable
//        objectDetectorHelper.clearObjectDetector()
//        fragmentCameraBinding.overlay.clear()
//    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // 获取当前屏幕方向
    fun isLandscape(): Boolean {
        val orientation = resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }

// 在 Activity 中调用 isLandscape() 函数判断屏幕方向

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
        // 获取屏幕旋转角度
        val rotation = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        Log.d("相机", "屏幕旋转的角度：${rotation}")


        //   分析相机捕获的图像帧 ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    //接收到图像调用识别helper 进行识别
    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
//            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
//                            String.format("%d ms", inferenceTime)
            fragmentCameraBinding.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

            //处理识别结果
            val currentTime = System.currentTimeMillis()
            // 每秒更新一次提示
            if (currentTime - lastRecordTime >= 1000) {
                // 处理图像并记录结果
                recordAnalysisResult(results, "" + lastRecordTime)
                lastRecordTime = currentTime
            }

            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    // 定时器，每秒触发一次
    private val timerRunnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 1000)
        }
    }


    /**
     * 设置提示文案
     */
    fun showTipsText(text: String) {
        fragmentCameraBinding.detectTip.text = text
    }


    private fun recordAnalysisResult(
        results: MutableList<Detection>?,
        recordAnalysisResult: String
    ) {
        // 处理图像并记录结果的逻辑
        Log.d("", "记录时间戳 $recordAnalysisResult")
        var drawableText = ""
        for (result in results ?: LinkedList<Detection>()) {
            val boundingBox = result.boundingBox
            var scaleFactor: Float = 1f
            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

//            drawableText =
//                result.categories[0].label + " " + String.format(
//                    "%.2f",
//                    result.categories[0].score
//                ) +
//                        "top :$top bottom: $bottom left: $left right: $right"
            drawableText = result.categories[0].label + " "
        }
        fragmentCameraBinding.detectData.text = drawableText

    }


//    override fun onResume() {
//        super.onResume()
//        handler.post(timerRunnable)
//    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}

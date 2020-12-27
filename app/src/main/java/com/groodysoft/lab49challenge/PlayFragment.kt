package com.groodysoft.lab49challenge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.groodysoft.lab49challenge.databinding.FragmentPlayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.format
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val ONE_SECOND_MS = 1000L
const val GAME_DURATION_MS = 120 * ONE_SECOND_MS

class PlayFragment: Fragment(), CameraItemListener {

    private lateinit var binding: FragmentPlayBinding

    private val tiles = mutableListOf<CameraItemView>()
    private var currentTileIndex = -1

    private var gameIsStarted = false
    private var gameIsOver = false

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewFinder.isVisible) {
                    hideViewFinder()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        // change the activity/window status bar color
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.bg_gray)

        binding = FragmentPlayBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        outputDirectory = getOutputDirectory()

        val items = Lab49Repository.currentItemsToSnap
        tiles.add(binding.tileA)
        tiles.add(binding.tileB)
        tiles.add(binding.tileC)
        tiles.add(binding.tileD)

        for ((index, tile) in tiles.withIndex()) {
            tile.configure(index, items[index], this)
        }

        val startPrompts = resources.getStringArray(R.array.start_prompts)
        if (startPrompts.size == 3) {
            GlobalScope.launch(Dispatchers.Main) {
                binding.timer.text = startPrompts[0]    // Ready?
                delay(ONE_SECOND_MS)
                binding.timer.text = startPrompts[1]    // Set...
                delay(ONE_SECOND_MS)
                binding.timer.text = startPrompts[2]    // Go!
                delay(ONE_SECOND_MS)
                startGame()
            }
        }

        binding.shutterButton.setOnClickListener {
            takePhoto()
        }
    }

    private lateinit var timer: CountDownTimer
    private fun startGame() {
        gameIsStarted = true
        updateTimeDisplay(GAME_DURATION_MS)
        timer = object : CountDownTimer(GAME_DURATION_MS + ONE_SECOND_MS, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val done = updateTimeDisplay(millisUntilFinished)
                if (done) {
                    endGame(false)
                }
            }
            override fun onFinish() {
            }
        }.start()
    }

    private fun updateTimeDisplay(millisUntilFinished: Long): Boolean {

        val minutes: Long = millisUntilFinished / 1000 / 60
        val seconds = (millisUntilFinished / 1000 % 60)
        binding.timer.text = format("%d:%02d", minutes, seconds)
        return minutes == 0L && seconds == 0L
    }

    private fun endGame(won: Boolean) {

        gameIsOver = true
        timer.cancel()

        val color = if (won) Color.GREEN else Color.RED
        binding.timerBackground.setBackgroundColor(color)

        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.game_over)
        builder.setMessage(if (won) R.string.winner_message else R.string.loser_message)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            requireActivity().onBackPressed()
        }
        builder.show()
    }

    private val Int.tileAtIndex: CameraItemView?
        get() {
            return when (this) {
                0 -> binding.tileA
                1 -> binding.tileB
                2 -> binding.tileC
                3 -> binding.tileD
                else -> null
            }
        }

    override fun onTapped(index: Int) {

        if (gameIsStarted && !gameIsOver) {
            index.tileAtIndex?.let { tile ->
                currentTileIndex = index
                if (!tile.isMatched) {
                    showViewFinder()
                }
            }
        }
    }

    override fun onResultChanged(index: Int) {

        val successCt = tiles.filter { it.isMatched }.size
        if (successCt == 4) {
            endGame(true)
        }
    }

    private fun onImageCaptured( bitmap: Bitmap) {

        currentTileIndex.tileAtIndex?.let { tile ->

            tile.setCapturedImage(bitmap)
            tile.setResultState(TileResultState.VERIFY)

            // send image to server
            val payload = ImagePayload(tile.item.name, "junkfdjsklfewio")

            GlobalScope.launch(Dispatchers.IO + requireActivity().exceptionHandler) {

                // get phony image match result
                val result = Lab49Repository.postItem(payload)
                tile.setResultState(
                        when (result.matched) {
                            true -> TileResultState.SUCCESS
                            false -> TileResultState.INCORRECT
                        }
                )
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.createSurfaceProvider())
                    }

            imageCapture = ImageCapture.Builder()
                    .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {

        // Get a stable reference of the modifiable image capture use case
        binding.viewFinder.isVisible = true
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
                outputDirectory,
                SimpleDateFormat(FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                hideViewFinder()
                val msg = "Photo capture failed: ${exc.message}"
                Log.e(TAG, msg, exc)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                val savedUri = Uri.fromFile(photoFile)
                hideViewFinder()
                currentTileIndex.tileAtIndex?.let { tile ->

                    val tileWidth = tile.width

                    val rawCapturedBitmap = BitmapFactory.decodeFile(savedUri.getPath())
                    val resizedBitmap = BitmapScaler.scaleToFitWidth(rawCapturedBitmap, tileWidth)
                    resizedBitmap.rotate(90f)?.let { rotatedBitmap ->

                        onImageCaptured(rotatedBitmap)
                    }
                }
            }
        })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }

    private fun showViewFinder() {

        binding.viewFinder.isVisible = true
        binding.shutterButton.isVisible = true
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()

    }

    private fun hideViewFinder() {

        binding.viewFinder.isVisible = false
        binding.shutterButton.isVisible = false
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}
package com.groodysoft.lab49challenge

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.gson.reflect.TypeToken
import com.groodysoft.lab49challenge.databinding.FragmentPlayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.format
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Camera permissions are enforced in MainActivity before we can get to this fragment
// App does not currently support permissions being maliciously denied after that point

class PlayFragment: Fragment(), TileListener {

    companion object {
        private const val ONE_SECOND_MS = 1000L
        private const val GAME_DURATION_MS = 120 * ONE_SECOND_MS
        private const val REQUEST_IMAGE_CAPTURE = 1

        // flip this flag to switch between two supported camera paths
        private const val USE_CAMERAX = true
    }

    private val args: PlayFragmentArgs by navArgs()

    private lateinit var binding: FragmentPlayBinding

    private lateinit var currentTile: TileView

    private var gameIsStarted = false
    private var gameIsOver = false

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraXExecutor: ExecutorService

    private var tempPhotoDir: File? = null
    private lateinit var tempPhotoFile: File
    private lateinit var tempPhotoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewFinder.isVisible) {
                    hideCameraX()
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

        try {
            // we just use one temp file for all captures, and delete any temp files in onDestroy()
            tempPhotoDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            tempPhotoFile = File.createTempFile("temp_", ".jpg", tempPhotoDir)
            tempPhotoUri = FileProvider.getUriForFile(requireContext(), "com.groodysoft.lab49challenge.file.provider", tempPhotoFile)
        } catch (e: Exception) {
            requireActivity().showFatalAlert(R.string.error_temp_photo)
        }

        // in a more state-of-the-art implementation, typeface could be specifed in XML
        binding.title.typeface = MainApplication.fontKarlaBold
        binding.timer.typeface = MainApplication.fontKarlaRegular

        // current list of server items is passed as a navigation argument
        val listType: Type = object : TypeToken<List<Lab49ServerItem?>?>() {}.type
        val items: List<Lab49ServerItem> = MainApplication.gson.fromJson(args.jsonItemArray, listType)

        // opted for 4 discrete tiles instead of the overhead of a recycler view and adapter/viewholder pattern
        binding.tileA.configureItem(items[0])
        binding.tileB.configureItem(items[1])
        binding.tileC.configureItem(items[2])
        binding.tileD.configureItem(items[3])

        countdownToStart()

        binding.shutterButton.setOnClickListener {
            takePhotoWithCameraX()
        }
    }

    private fun countdownToStart() {

        // isAdded checks avoid an exception if the fragment is exited during the delay
        GlobalScope.launch(Dispatchers.Main) {
            if (isAdded) binding.timer.text = getString(R.string.ready)
            delay(ONE_SECOND_MS)
            if (isAdded) binding.timer.text = getString(R.string.set)
            delay(ONE_SECOND_MS)
            if (isAdded) binding.timer.text = getString(R.string.go)
            delay(500)
            startGame()
        }
    }

    private lateinit var timer: CountDownTimer
    private fun startGame() {

        // configure the tap listeners
        binding.tileA.configureListener(this)
        binding.tileB.configureListener(this)
        binding.tileC.configureListener(this)
        binding.tileD.configureListener(this)

        gameIsStarted = true
        updateTimeDisplay(GAME_DURATION_MS)
        timer = object : CountDownTimer(GAME_DURATION_MS + ONE_SECOND_MS, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (isAdded) {
                    val done = updateTimeDisplay(millisUntilFinished)
                    if (done) {
                        endGame(false)
                    }
                } else {
                    this.cancel()
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

    override fun onTapped(tile: TileView) {

        if (gameIsStarted && !gameIsOver) {

            currentTile = tile
            if (!tile.isMatched) {

                if (USE_CAMERAX) {
                    showCameraX()
                } else {
                    dispatchTakePictureIntent()
                }
            }
        }
    }

    override fun onResultChanged(tile: TileView) {
        if (binding.tileA.isMatched && binding.tileB.isMatched && binding.tileC.isMatched && binding.tileD.isMatched) {
            endGame(true)
        }
    }

    private fun startCameraX() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
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
                requireActivity().showFatalAlert(R.string.error_starting_camera)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhotoWithCameraX() {

        // Get a stable reference of the modifiable image capture use case
        binding.viewFinder.isVisible = true
        val imageCapture = imageCapture ?: return

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempPhotoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                hideCameraX()
                Toast.makeText(requireContext(), R.string.error_taking_photo, Toast.LENGTH_LONG).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                hideCameraX()
                currentTile.processCapturedBitmap(requireActivity(), tempPhotoFile.absolutePath)
            }
        })
    }

    private fun showCameraX() {

        binding.viewFinder.isVisible = true
        binding.shutterButton.isVisible = true
        cameraXExecutor = Executors.newSingleThreadExecutor()
        startCameraX()

    }

    private fun hideCameraX() {

        binding.viewFinder.isVisible = false
        binding.shutterButton.isVisible = false
        cameraXExecutor.shutdown()
    }

    private fun dispatchTakePictureIntent() {

        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.resolveActivity(requireActivity().packageManager)

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)

        } catch (e: ActivityNotFoundException) {
            requireActivity().showFatalAlert(R.string.error_no_camera_app)
        } catch (e: SecurityException) {
            requireActivity().showFatalAlert(R.string.error_no_camera_permisisons)
        } catch (ex: IOException) {
            requireActivity().showFatalAlert(R.string.error_saving_image)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            currentTile.processCapturedBitmap(requireActivity(), tempPhotoFile.absolutePath)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tempPhotoDir?.deleteTempFiles()
    }

    private fun File.deleteTempFiles(): Boolean {

        if (isDirectory) {
            val files = listFiles()
            if (files != null) {
                for (f in files) {
                    // recursion not necessary
                    f.delete()
                }
            }
        }
        // delete the folder itself
        return delete()
    }
}
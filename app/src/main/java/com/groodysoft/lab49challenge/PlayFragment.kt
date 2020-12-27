package com.groodysoft.lab49challenge

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

// Camera permissions are enforced in MainActivity before we can get to this fragment
// App does not currently support permissions being maliciously denied after that point

class PlayFragment: Fragment(), CameraItemListener {

    companion object {
        private const val ONE_SECOND_MS = 1000L
        private const val GAME_DURATION_MS = 120 * ONE_SECOND_MS
        private const val REQUEST_IMAGE_CAPTURE = 1

        private const val USE_CAMERAX = true
    }

    private val args: PlayFragmentArgs by navArgs()

    private lateinit var binding: FragmentPlayBinding

    private val tiles = mutableListOf<ItemTileView>()
    private var currentTileIndex = -1

    private var gameIsStarted = false
    private var gameIsOver = false

    private var imageCapture: ImageCapture? = null
    lateinit var capturedRawImagePath: String

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

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

        outputDirectory = getOutputDirectory()

        // in a more state-of-the-art implementation, typeface could be specifed in XML
        binding.title.typeface = MainApplication.fontKarlaBold
        binding.timer.typeface = MainApplication.fontKarlaRegular

        // current list of server items is passed as a navigation argument
        val listType: Type = object : TypeToken<List<Lab49ServerItem?>?>() {}.type
        val items: List<Lab49ServerItem> = MainApplication.gson.fromJson(args.jsonItemArray, listType)

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
            takePhotoWithCameraX()
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

    private val Int.tileAtIndex: ItemTileView?
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

                    if (USE_CAMERAX) {
                        showCameraX()
                    } else {
                        dispatchTakePictureIntent()
                    }
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

    private fun handleFinalBitmap(bitmap: Bitmap) {

        currentTileIndex.tileAtIndex?.let { tile ->

            // display bitmap in verifying state
            tile.setCapturedImage(bitmap)
            tile.setResultState(TileResultState.VERIFY)

            // encode image
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Encoding = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // send image to server
            val payload = ImagePayload(tile.item.name, base64Encoding)
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

        // Create time-stamped output file to hold the image
        val photoFile = File(outputDirectory, "temp.jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

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
                val savedUri = Uri.fromFile(photoFile)
                savedUri.path?.let {
                    capturedRawImagePath = it
                    processCapturedBitmap()
                } ?: run {
                    requireActivity().showFatalAlert(R.string.error_saving_image)
                }
            }
        })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }

    private fun showCameraX() {

        binding.viewFinder.isVisible = true
        binding.shutterButton.isVisible = true
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCameraX()

    }

    private fun hideCameraX() {

        binding.viewFinder.isVisible = false
        binding.shutterButton.isVisible = false
        cameraExecutor.shutdown()
    }

    private fun dispatchTakePictureIntent() {

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureIntent.resolveActivity(requireActivity().packageManager).also {

                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    requireActivity().showFatalAlert(R.string.error_saving_image)
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            "com.groodysoft.lab49challenge.file.provider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        } catch (e: ActivityNotFoundException) {
            requireActivity().showFatalAlert(R.string.error_no_camera_app)
        } catch (e: SecurityException) {
            requireActivity().showFatalAlert(R.string.error_no_camera_permisisons)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            processCapturedBitmap()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("temp_", ".jpg", storageDir).apply {
            capturedRawImagePath = absolutePath
        }
    }

    private fun processCapturedBitmap() {

        currentTileIndex.tileAtIndex?.let { tile ->

            BitmapFactory.Options().apply {

                // decode just to get the dimensions
                inJustDecodeBounds = true
                BitmapFactory.decodeFile(capturedRawImagePath, this)
                inJustDecodeBounds = false
                // reverse the width and height because it's landscape view instead of the portrait we want
                inSampleSize = max(1, min(outWidth / tile.imageHeight, outHeight / tile.imageWidth))

                // decode to resample
                BitmapFactory.decodeFile(capturedRawImagePath, this)?.also { resizedBitmap ->

                    // rotate
                    resizedBitmap.rotate(90f)?.let { rotatedBitmap ->
                        handleFinalBitmap(rotatedBitmap)
                    }
                }
            }
        }
    }
}
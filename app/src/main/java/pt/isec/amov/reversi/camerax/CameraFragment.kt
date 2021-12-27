package pt.isec.amov.reversi.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import pt.isec.amov.reversi.activities.MainActivity


class CameraFragment : Fragment() {

    private var imagePath: String? = null
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var auth: FirebaseAuth
    private lateinit var outputDirectory: File

    //private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: FragmentCameraBinding


    /* When in this fragment hide actionbar */
    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.supportActionBar!!.hide()
    }

    /* When we left this fragment show the action bar*/
    override fun onStop() {
        super.onStop()
        (activity as MainActivity?)!!.supportActionBar!!.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCameraBinding.inflate(inflater)
        /* Check if camera permissions are granted */
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }


        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }

        auth = Firebase.auth

        outputDirectory = getOutputDirectory()
        //cameraExecutor = Executors.newSingleThreadExecutor()

        return binding.root
    }

    private fun takePhoto() {
        // Get a stable reference of the
        // modifiable image capture use case
        val imageCaptureAux = imageCapture ?: return


        /* Create output file to hold the with user uid image */
        val photoFile = File(
            outputDirectory,
            "${auth.currentUser!!.uid}.jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imagePath = photoFile.absolutePath

        // Set up image capture listener
        imageCaptureAux.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    /* Save navigation view */
                    val navigationView =
                        requireActivity().findViewById<com.google.android.material.navigation.NavigationView>(
                            R.id.nav_view
                        )
                    /* Change navigation header with the photo taken*/
                    val headerView =
                        navigationView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.userImage)

                    /* HeaderView won't update if saved twice with the same URI*/
                    headerView.setImageURI(null)
                    headerView.setImageURI(savedUri)

                    Toast.makeText(
                        requireContext(),
                        "Photo capture succeeded: $savedUri",
                        Toast.LENGTH_LONG
                    ).show()
                    stopCamera()
                    findNavController().navigate(R.id.action_cameraFragment_to_profileFragment)
                }
            })
    }

    private fun stopCamera() {
        cameraProvider.unbindAll()
    }

    private fun startCamera() {
        /* Get control when camera is iniciated*/
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().setTargetResolution(Size(85, 85)).build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    requireActivity(), cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /* Confirms the permissions are granted*/
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            /* If User give permissions start camera*/
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                /* User don't give permissions go back to profile view */
                Toast.makeText(requireContext(), resources.getString(R.string.permissionsNotGranted), Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigate(R.id.action_cameraFragment_to_profileFragment)
            }
        }
    }


    companion object {
        private const val TAG = "CameraXGFG"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /*override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }*/
}
package com.example.cartooo.fragments

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.navigation.fragment.findNavController
import com.example.cartooo.R

@Suppress("DEPRECATION")
class PermissionsFragment :Fragment(){
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUEST_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        fun allPermissionsGranted(context: Context) = REQUEST_PERMISSIONS.all {
            checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (allPermissionsGranted(
                requireContext()
            )){
            findNavController().navigate(R.id.action_permissions_to_camera)
        }else {
            requestPermissions(
                REQUEST_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted(
                    requireContext()
                )){
                findNavController().navigate(R.id.action_permissions_to_camera)
            }else {
                Toast.makeText(
                    context,
                    "Permission not granted by the user",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
package com.ahmedonibiyo.projemanag.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ahmedonibiyo.projemanag.R
import com.ahmedonibiyo.projemanag.firebase.FireStoreClass
import com.ahmedonibiyo.projemanag.models.User
import com.ahmedonibiyo.projemanag.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_my_profile.*
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    companion object {
        private const val READ_STORAGE_PERMISSION_CODE = 1
        private const val PICK_IMAGE_REQUEST_CODE = 2
    }

    private var selectedImageFileUri: Uri? = null
    private var profileImageUrl = ""
    private lateinit var userDetails: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setupActionBar()

        FireStoreClass().loadUserData(this)

        iv_profile_user_image.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                showImageChooser()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        btn_update.setOnClickListener {
            if (selectedImageFileUri != null) {
                uploadUserImage()
            } else {
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageChooser()
            }
        } else {
            Toast.makeText(
                this,
                "Oops you just denied permissions for storage. You can allow it in the app settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showImageChooser() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            selectedImageFileUri = data.data

            try {
                Glide
                    .with(this@MyProfileActivity)
                    .load(selectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(iv_profile_user_image)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_my_profile_activity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_back)
            actionBar.title = resources.getString(R.string.my_profile_title)
        }

        toolbar_my_profile_activity.setNavigationOnClickListener { onBackPressed() }
    }

    fun setUserDataInUI(user: User) {

        userDetails = user
        Glide
            .with(this@MyProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(iv_profile_user_image)

        et_name.setText(user.name)
        et_email.setText(user.email)
        if (user.mobile != 0L) {
            et_mobile.setText(user.mobile.toString())
        }
    }

    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()

        if (profileImageUrl.isNotEmpty() && profileImageUrl != userDetails.image) {
            userHashMap[Constants.IMAGE] = profileImageUrl
        }

        if (et_name.text.toString() != userDetails.name) {
            userHashMap[Constants.NAME] = et_name.text.toString()
        }

        if (et_mobile.text.toString() != userDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = et_mobile.text.toString().toLong()
        }

        FireStoreClass().updateUserProfileData(this, userHashMap)
    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        if (selectedImageFileUri != null) {
            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." + getFileExtension(
                    selectedImageFileUri
                )
            )

            sRef.putFile(selectedImageFileUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    hideProgressBar()
                    Log.i(
                        "Firebase Image URL",
                        taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                    )

                    taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                        Log.i("Downloadable Image URL", uri.toString())
                        profileImageUrl = uri.toString()

                        updateUserProfileData()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        exception.message,
                        Toast.LENGTH_LONG
                    ).show()

                    hideProgressBar()
                }
        }
    }

    private fun getFileExtension(uri: Uri?): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    fun profileUpdateSuccess() {
        hideProgressBar()
        finish()
    }
}
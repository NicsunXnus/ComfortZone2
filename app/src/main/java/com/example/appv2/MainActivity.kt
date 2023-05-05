package com.example.appv2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext

import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.appv2.databinding.ActivityMainBinding
import com.example.appv2.functionality.OnDoubleClickListener

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var narrateIndicator: LottieAnimationView
    private lateinit var typingIndicator: LottieAnimationView
    private lateinit var handsIndicator: LottieAnimationView
    private lateinit var currentSession :TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        narrateIndicator = findViewById<LottieAnimationView>(R.id.narrateIndicator)
        typingIndicator =  findViewById<LottieAnimationView>(R.id.typingIndicator2)
        handsIndicator = findViewById<LottieAnimationView>(R.id.handsTyping)
        currentSession = findViewById<TextView>(R.id.session_placeholder)

        narrateIndicator.visibility = View.GONE
        typingIndicator.visibility = View.GONE
        handsIndicator.visibility = View.GONE
        currentSession.visibility = View.GONE
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery,R.id.chat_history
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        loadProfilePictureSelection()
        // Load the saved image URI from preferences
        //checkStoragePermission()
        val savedImageUri = loadImageUriFromPreferences()
        val imageView = binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.imageView)
        // Set the profile picture ImageView with the saved image URI
        if (savedImageUri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(savedImageUri, takeFlags)
            imageView.setImageURI(savedImageUri)
        }
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        val session_name = findViewById<TextView>(R.id.session_placeholder)
        session_name.setOnLongClickListener {
            Toast.makeText(this, sharedViewModel.currentSession.value?.name ?: "No session name", Toast.LENGTH_SHORT).show()
            true
        }
        val relax = binding.navView.getHeaderView(0).findViewById<LottieAnimationView>(R.id.relax)
        relax.setOnClickListener(OnDoubleClickListener(
            singleClickAction = {
                relax.cancelAnimation()
                relax.speed = 1.0f
            },
            doubleClickAction = {
                relax.resumeAnimation()
            }
        ))

        relax.setOnLongClickListener {
            relax.speed = 2.0f
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun onChangeProfilePictureClick(view: View) {
        // Show a dialog to let the user choose a new profile picture
        showChangeProfilePictureDialog()
    }

    private fun saveProfilePictureSelection(pictureName: String) {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("profile_picture", pictureName)
        editor.apply()
    }

    private fun loadProfilePictureSelection() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val pictureName = sharedPreferences.getString("profile_picture", null)
        if (pictureName != null) {
            val imageView = binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.imageView)
            val resId = resources.getIdentifier(pictureName, "drawable", packageName)
            imageView.setImageResource(resId)
        }
    }


    private fun showChooseDefaultProfilePictureDialog() {
        val profilePictures = arrayOf(
            "boy", "boy_two", "boy_three",
            "girl", "girl_two", "girl_three"
        )

        AlertDialog.Builder(this)
            .setTitle("Choose a profile picture")
            .setItems(profilePictures) { _, which ->
                val selectedPicture = profilePictures[which]
                val imageView = binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.imageView)
                val resId = resources.getIdentifier(selectedPicture, "drawable", packageName)
                imageView.setImageResource(resId)

                // Save the user's profile picture selection
                saveProfilePictureSelection(selectedPicture)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangeProfilePictureDialog() {
        val options = arrayOf("Choose from gallery", "Choose from default pictures")

        AlertDialog.Builder(this)
            .setTitle("Change profile picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        pickImageFromGallery()
                    }
                    1 -> {
                        showChooseDefaultProfilePictureDialog()
                        val sharedPreferences = getSharedPreferences("gallery_photo", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.remove("profile_image_uri")
                        editor.apply()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 2
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }

    private fun saveImageUriToPreferences(imageUri: Uri) {
        val sharedPreferences = getSharedPreferences("gallery_photo", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("profile_image_uri", imageUri.toString())
            apply()
        }
    }

    private fun loadImageUriFromPreferences(): Uri? {
        val sharedPreferences = getSharedPreferences("gallery_photo", Context.MODE_PRIVATE)
        val imageUriString = sharedPreferences.getString("profile_image_uri", null)
        return if (imageUriString != null) {
            Uri.parse(imageUriString)
        } else {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                val imageView = binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.imageView)
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(selectedImageUri, takeFlags)
                imageView.setImageURI(selectedImageUri)
                saveImageUriToPreferences(selectedImageUri)
            }
        }
    }

}
package com.example.cardify.ui.group

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.cardify.R
import com.example.cardify.data.CreateGroupRequest
import com.example.cardify.databinding.ActivityCreateGroupBinding
import com.example.cardify.viewmodel.CreateGroupViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Allows users to create a new group via the backend API.
 */
class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private val viewModel: CreateGroupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonSubmit.setOnClickListener { submitForm() }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isSubmitting.observe(this) { isSubmitting ->
            binding.progressBar.isVisible = isSubmitting
            binding.buttonSubmit.isEnabled = !isSubmitting
        }
        viewModel.creationSuccess.observe(this) {
            Toast.makeText(this, R.string.message_group_created, Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
        viewModel.errorMessage.observe(this) { message ->
            message ?: return@observe
            val displayMessage = if (message.isBlank()) {
                getString(R.string.error_generic)
            } else {
                message
            }
            Snackbar.make(binding.root, displayMessage, Snackbar.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    private fun submitForm() {
        val title = binding.inputTitle.text?.toString().orEmpty()
        val description = binding.inputDescription.text?.toString().orEmpty()
        val dateTime = binding.inputDateTime.text?.toString().orEmpty()
        val location = binding.inputLocation.text?.toString().orEmpty()
        val maxPeopleText = binding.inputMaxPeople.text?.toString().orEmpty()

        val errorResId = viewModel.validateInput(title, description, dateTime, location, maxPeopleText)
        if (errorResId != null) {
            Snackbar.make(binding.root, getString(errorResId), Snackbar.LENGTH_LONG).show()
            return
        }

        val request = CreateGroupRequest(
            title = title,
            description = description,
            dateTime = dateTime,
            location = location,
            maxPeople = maxPeopleText.toInt()
        )
        viewModel.submitGroup(request)
    }
}

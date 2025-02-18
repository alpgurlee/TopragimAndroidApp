package com.alperengurle.EmlakApp.ui.profile

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.data.model.User
import com.alperengurle.EmlakApp.databinding.DialogEditProfileBinding

class EditProfileDialog(
    context: Context,
    private val user: User?,
    private val onSave: (User) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dialog'u ekranın %90'ı genişliğinde göster
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        user?.let { user ->
            binding.firstNameEditText.setText(user.firstName)
            binding.lastNameEditText.setText(user.lastName)
            binding.phoneEditText.setText(user.phoneNumber)
            binding.professionEditText.setText(user.profession)
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            val firstName = binding.firstNameEditText.text.toString()
            val lastName = binding.lastNameEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()
            val profession = binding.professionEditText.text.toString()

            if (validateInputs(firstName, lastName, phone)) {
                val updatedUser = User(
                    id = user?.id ?: "",
                    firstName = firstName,
                    lastName = lastName,
                    email = user?.email ?: "",
                    phoneNumber = phone,
                    profession = profession.ifEmpty { null },
                    fcmToken = user?.fcmToken
                )
                onSave(updatedUser)
                dismiss()
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun validateInputs(firstName: String, lastName: String, phone: String): Boolean {
        var isValid = true

        if (firstName.isEmpty()) {
            binding.firstNameEditText.error = "İsim boş olamaz"
            isValid = false
        }

        if (lastName.isEmpty()) {
            binding.lastNameEditText.error = "Soyisim boş olamaz"
            isValid = false
        }

        if (phone.isEmpty()) {
            binding.phoneEditText.error = "Telefon numarası boş olamaz"
            isValid = false
        } else if (!phone.matches(Regex("^[0-9]{10,11}$"))) {
            binding.phoneEditText.error = "Geçersiz telefon numarası"
            isValid = false
        }

        return isValid
    }
}
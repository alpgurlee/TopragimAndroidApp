package com.alperengurle.EmlakApp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.alperengurle.EmlakApp.databinding.FragmentAccountDetailsBinding
import com.alperengurle.EmlakApp.viewmodel.AccountDetailsViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountDetailsFragment : Fragment() {
    private var _binding: FragmentAccountDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountDetailsViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var dateFormat: SimpleDateFormat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        setupUI()
        loadUserData()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            birthdayEditText.setOnClickListener {
                showDatePicker()
            }

            saveButton.setOnClickListener {
                updateUserDetails()
            }
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                binding.apply {
                    firstNameEditText.setText(document.getString("firstName"))
                    lastNameEditText.setText(document.getString("lastName"))
                    phoneEditText.setText(document.getString("phoneNumber"))
                    professionEditText.setText(document.getString("profession"))
                    birthdayEditText.setText(document.getString("birthDate"))
                }
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "Bilgiler yüklenirken hata oluştu", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Doğum Tarihi")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = dateFormat.format(Date(selection))
            binding.birthdayEditText.setText(date)
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun updateUserDetails() {
        val userId = auth.currentUser?.uid ?: return
        binding.apply {
            val updates = hashMapOf<String, Any>().apply {
                put("firstName", firstNameEditText.text.toString())
                put("lastName", lastNameEditText.text.toString())
                put("phoneNumber", phoneEditText.text.toString())
                put("profession", professionEditText.text.toString())
                put("birthDate", birthdayEditText.text.toString())
            }

            progressBar.visibility = View.VISIBLE
            saveButton.isEnabled = false

            db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled = true
                    Snackbar.make(root, "Bilgiler güncellendi", Snackbar.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled = true
                    Snackbar.make(root, "Güncelleme başarısız oldu", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
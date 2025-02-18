package com.alperengurle.EmlakApp.ui.auth


import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.databinding.FragmentVerificationDialogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions



class VerificationDialogFragment : DialogFragment() {
    private var _binding: FragmentVerificationDialogBinding? = null
    private val binding get() = _binding!!
    private var verificationId: String = ""
    private var onVerificationComplete: ((Boolean) -> Unit)? = null
    private var remainingTime = 120 // 2 dakika
    private var timer: CountDownTimer? = null
    private val editTexts = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
        verificationId = arguments?.getString(ARG_VERIFICATION_ID) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVerificationDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        startTimer()
    }

    private fun setupViews() {
        editTexts.apply {
            add(binding.code1)
            add(binding.code2)
            add(binding.code3)
            add(binding.code4)
            add(binding.code5)
            add(binding.code6)
        }

        setupCodeInputs()

        binding.verifyButton.setOnClickListener {
            verifyCode()
        }

        binding.resendButton.setOnClickListener {
            resendCode()
        }
    }

    private fun setupCodeInputs() {
        editTexts.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < editTexts.size - 1) {
                        editTexts[index + 1].requestFocus()
                    }
                }
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    editText.text.isEmpty() &&
                    index > 0) {
                    editTexts[index - 1].apply {
                        requestFocus()
                        text.clear()
                    }
                    true
                } else false
            }
        }
    }

    private fun verifyCode() {
        val code = editTexts.joinToString("") { it.text.toString() }

        if (code.length != 6) {
            showError("Lütfen 6 haneli kodu giriniz")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.verifyButton.isEnabled = false

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    onVerificationComplete?.invoke(true)
                    dismiss()
                } else {
                    binding.verifyButton.isEnabled = true
                    showError("Doğrulama başarısız: ${task.exception?.message}")
                }
            }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(remainingTime * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = (millisUntilFinished / 1000).toInt()
                binding.timerText.text = "$remainingTime saniye sonra tekrar gönderebilirsiniz"
            }

            override fun onFinish() {
                binding.timerText.text = ""
                binding.resendButton.isEnabled = true
            }
        }.start()
    }

    private fun resendCode() {
        // Implement resend logic here
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }

    companion object {
        private const val ARG_VERIFICATION_ID = "verification_id"

        fun newInstance(verificationId: String, onComplete: (Boolean) -> Unit) =
            VerificationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VERIFICATION_ID, verificationId)
                }
                onVerificationComplete = onComplete
            }
    }
}
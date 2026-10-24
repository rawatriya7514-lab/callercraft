package com.craftcaller.app.auth

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.util.Prefs
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/** Firebase Phone-Auth flow (degrades gracefully when unconfigured). */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    var busy by mutableStateOf(false)
        private set
    var verifyId by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val firebaseReady: Boolean get() = auth != null

    /** Offline exit visible whenever the bundled config is the placeholder. */
    val demoMode: Boolean get() = FirebaseRepository.placeholderConfig

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signIn(credential)
        }

        override fun onVerificationFailed(exception: FirebaseException) {
            busy = false
            error = when {
                FirebaseRepository.placeholderConfig ->
                    "Demo build: Firebase config placeholder hai, isliye Google ne OTP request reject ki. " +
                        "Neeche \"Continue as local user (offline mode)” use karo — ya real google-services.json add karo (README §Firebase)."
                else -> exception.message ?: "Verification failed"
            }
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            busy = false
            verifyId = verificationId
        }
    }

    fun sendOtp(activity: Activity, phone: String) {
        val au = auth
        if (au == null) {
            error = "Firebase is not configured — replace app/google-services.json (see README)."
            return
        }
        busy = true
        error = null
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(au)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    fun verify(code: String, onSuccess: () -> Unit) {
        val au = auth
        val vid = verifyId
        if (au == null) {
            error = "Firebase is not configured — see README."
            return
        }
        if (vid == null) {
            error = "Request an OTP first."
            return
        }
        busy = true
        signIn(PhoneAuthProvider.getCredential(vid, code), onSuccess)
    }

    private fun signIn(credential: PhoneAuthCredential, onSuccess: () -> Unit = {}) {
        val au = auth ?: return
        au.signInWithCredential(credential).addOnCompleteListener { task ->
            busy = false
            if (task.isSuccessful) {
                Prefs(getApplication()).uid = task.result.user?.uid.orEmpty()
                onSuccess()
            } else {
                error = task.exception?.message ?: "Sign-in failed"
            }
        }
    }
}

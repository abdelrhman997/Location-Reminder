package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity


class AuthenticationActivity : AppCompatActivity() {
    companion object {
        const val SIGN_IN = 1
    }

    lateinit var login_btn:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            navigateToReminderActivity()
            return
        }

        setContentView(R.layout.activity_authentication)
        login_btn =findViewById(R.id.login_button)
        login_btn.setOnClickListener { onLoginButtonClicked() }
    }

    private fun navigateToReminderActivity() {
        startActivity(Intent(this, RemindersActivity::class.java))
        finish()
    }

    private fun onLoginButtonClicked() {
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(
                    listOf(
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.EmailBuilder().build()
                    )
                )
                .setAuthMethodPickerLayout(
                    AuthMethodPickerLayout
                        .Builder(R.layout.auth_picker_lay)
                        .setEmailButtonId(R.id.email_sign_in_btn)
                        .setGoogleButtonId(R.id.google_sign_in_btn)
                        .build()
                )
                .setTheme(R.style.AppTheme)
                .build(), SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != SIGN_IN) {
            return
        }

        if (resultCode == RESULT_OK) {
            navigateToReminderActivity()
            return
        }
    }
}

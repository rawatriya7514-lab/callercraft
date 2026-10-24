package com.craftcaller.app.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.model.CraftUser
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.repository.ContactsRepository
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.work.ContactSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** First-launch profile: name, email, consent-based community contact sync. */
@Composable
fun ProfileSetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf(prefs.userName) }
    var email by rememberSaveable { mutableStateOf(prefs.userEmail) }
    var syncContacts by rememberSaveable { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Set up your profile", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Your name appears to other CraftCaller users when they search your number.",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = syncContacts, onCheckedChange = { syncContacts = it })
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    "Sync contacts to CraftCaller",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "Uploads contact names + numbers to the community directory.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
        Spacer(Modifier.height(26.dp))

        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                scope.launch(Dispatchers.IO) {
                    saveProfile(context, prefs, name.trim(), email.trim(), syncContacts)
                    withContext(Dispatchers.Main) {
                        busy = false
                        onDone()
                    }
                }
            },
            enabled = !busy && name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Finish setup", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private suspend fun saveProfile(
    context: Context,
    prefs: Prefs,
    name: String,
    email: String,
    syncContacts: Boolean
) {
    prefs.userName = name
    prefs.userEmail = email

    val contentUid = runCatching {
        com.google.firebase.auth.FirebaseAuth.getInstance().uid
    }.getOrNull()
    val uid = prefs.uid.ifEmpty { contentUid ?: "local-${UUID.randomUUID()}" }
    prefs.uid = uid

    // Publish profile + own-number into the community directories (best effort).
    runCatching {
        FirebaseRepository.registerUser(
            CraftUser(
                uid = uid,
                name = name,
                email = email,
                phone = prefs.userPhone,
                createdAt = System.currentTimeMillis()
            )
        )
        if (prefs.userPhone.isNotBlank()) {
            FirebaseRepository.registerOwnNumber(prefs.userPhone, name)
        }
    }

    if (syncContacts) {
        runCatching {
            val contacts = ContactsRepository(context).loadDeviceContacts()
            if (contacts.isNotEmpty()) FirebaseRepository.uploadCommunityContacts(contacts)
        }
        ContactSyncWorker.enqueue(context)
    }

    prefs.onboardingDone = true
}

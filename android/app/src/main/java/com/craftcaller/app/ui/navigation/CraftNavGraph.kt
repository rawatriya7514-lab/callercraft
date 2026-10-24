package com.craftcaller.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.craftcaller.app.auth.OtpScreen
import com.craftcaller.app.auth.PhoneLoginScreen
import com.craftcaller.app.auth.ProfileSetupScreen
import com.craftcaller.app.home.HomeScreen
import com.craftcaller.app.onboarding.OnboardingScreen
import com.craftcaller.app.search.SearchScreen
import com.craftcaller.app.settings.AudioSettingsScreen
import com.craftcaller.app.settings.CallerIdSettingsScreen
import com.craftcaller.app.settings.DataPrivacyScreen
import com.craftcaller.app.settings.ListsScreen
import com.craftcaller.app.settings.PermissionsScreen
import com.craftcaller.app.settings.ProfileEditorScreen
import com.craftcaller.app.settings.SettingsHubScreen
import com.craftcaller.app.settings.TroubleshootScreen
import com.craftcaller.app.settings.VoiceEngineScreen
import com.craftcaller.app.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val OTP = "otp/{phone}"
    const val PROFILE = "profile"
    const val HOME = "home"
    const val SEARCH = "search"
    const val ONBOARDING = "onboarding"
    const val PROFILE_EDIT = "settings/profile"
    const val AUDIO_EDIT = "settings/audio"
    const val SETTINGS_HUB = "settings/hub"
    const val VOICE_ENGINE = "settings/voice"
    const val CALLER_ID = "settings/callerid"
    const val LISTS = "settings/lists"
    const val PERMISSIONS = "settings/permissions"
    const val DATA_PRIVACY = "settings/data"
    const val TROUBLESHOOT = "settings/troubleshoot"

    fun otp(phone: String) = "otp/$phone"
}

@Composable
fun CraftNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onGoToAuth = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onGoToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onGoToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    // Personal profile saved; continue into the (existing) auth flow.
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            PhoneLoginScreen(
                onOtpSent = { phone -> navController.navigate(Routes.otp(phone)) },
                onOfflineContinue = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(
            Routes.OTP,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { entry ->
            OtpScreen(
                phone = entry.arguments?.getString("phone").orEmpty(),
                onVerified = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileSetupScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenAudio = { navController.navigate(Routes.AUDIO_EDIT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS_HUB) }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.PROFILE_EDIT) {
            ProfileEditorScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.AUDIO_EDIT) {
            AudioSettingsScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_HUB) {
            SettingsHubScreen(
                onClose = { navController.popBackStack() },
                onOpenProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenAudio = { navController.navigate(Routes.AUDIO_EDIT) },
                onOpenVoice = { navController.navigate(Routes.VOICE_ENGINE) },
                onOpenCallerId = { navController.navigate(Routes.CALLER_ID) },
                onOpenLists = { navController.navigate(Routes.LISTS) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenData = { navController.navigate(Routes.DATA_PRIVACY) },
                onOpenTroubleshoot = { navController.navigate(Routes.TROUBLESHOOT) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) }
            )
        }

        composable(Routes.VOICE_ENGINE) {
            VoiceEngineScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.CALLER_ID) {
            CallerIdSettingsScreen(
                onClose = { navController.popBackStack() },
                onOpenLists = { navController.navigate(Routes.LISTS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE_EDIT) }
            )
        }

        composable(Routes.LISTS) {
            ListsScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.DATA_PRIVACY) {
            DataPrivacyScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.TROUBLESHOOT) {
            TroubleshootScreen(onClose = { navController.popBackStack() })
        }
    }
}

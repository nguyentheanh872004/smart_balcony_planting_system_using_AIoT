package com.example.hihi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.hihi.login.LoginScreen
import com.example.hihi.main.MainScreen
import com.example.hihi.ui.theme.HihiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HihiTheme {

                // 🔐 STATE LOGIN DUY NHẤT
                var isLoggedIn by remember { mutableStateOf(false) }

                if (isLoggedIn) {
                    // 👉 ĐÃ LOGIN → VÀO DASHBOARD
                    MainScreen(
                        onLogout = {
                            isLoggedIn = false
                        }
                    )
                } else {
                    // 👉 CHƯA LOGIN → LOGIN SCREEN
                    LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                        }
                    )
                }
            }
        }
    }
}

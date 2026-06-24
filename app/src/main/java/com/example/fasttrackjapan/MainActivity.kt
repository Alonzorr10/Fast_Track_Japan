package com.example.fasttrackjapan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fasttrackjapan.ui.theme.FastTrackJapanTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FastTrackJapanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fast Track Japan",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Log In Button (Filled Style)
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Log In", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { /* TODO: Handle Sign Up Navigation */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Sign Up", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    FastTrackJapanTheme {
        WelcomeScreen()
    }
}
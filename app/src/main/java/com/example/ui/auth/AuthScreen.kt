package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.LocalAttendanceColors

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val semantic = LocalAttendanceColors.current

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(semantic.headerStart, semantic.headerEnd, MaterialTheme.colorScheme.background)
        )
      )
      .systemBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Image(
          painter = painterResource(id = R.drawable.payroll_hero_banner_1794625257459),
          contentDescription = stringResource(R.string.cd_hero_banner),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp)),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = stringResource(R.string.app_header_title),
        style = MaterialTheme.typography.headlineMedium,
        color = semantic.goldAccent,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.app_header_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.88f),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.12f),
      ) {
        Text(
          text = stringResource(R.string.app_credit_badge),
          style = MaterialTheme.typography.bodySmall,
          color = semantic.goldAccent,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          FeatureHighlightRow(
            icon = Icons.Filled.EventAvailable,
            title = stringResource(R.string.auth_feature_attendance_title),
            subtitle = stringResource(R.string.auth_feature_attendance_desc),
          )
          FeatureHighlightRow(
            icon = Icons.Filled.AccountBalanceWallet,
            title = stringResource(R.string.auth_feature_salary_title),
            subtitle = stringResource(R.string.auth_feature_salary_desc),
          )
          FeatureHighlightRow(
            icon = Icons.Filled.CloudDone,
            title = stringResource(R.string.auth_feature_sync_title),
            subtitle = stringResource(R.string.auth_feature_sync_desc),
          )

          if (errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = {
                  isLoading = false
                },
              )
            },
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.sign_in_with_google),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(42.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp),
        )
      }
    }
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

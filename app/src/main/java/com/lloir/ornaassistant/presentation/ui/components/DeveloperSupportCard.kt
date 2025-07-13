package com.lloir.ornaassistant.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.PIXEL_6_PRO
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lloir.ornaassistant.presentation.theme.OrnaAssistantTheme

@Composable
fun DeveloperSupportCard(
    modifier: Modifier = Modifier,
    onDonateButtonClicked: () -> Unit
) {
    OrnaCard(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Developed by lloir. If you wish, you can support the development by donating!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OrnaButton(
                onClick = onDonateButtonClicked,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text("Donate via buymeacoffee")
            }
        }
    }
}


@Preview(device = PIXEL_6_PRO)
@Composable
fun DeveloperSupportPreview() {
    OrnaAssistantTheme {
        DeveloperSupportCard(onDonateButtonClicked = {})
    }
}

@Preview(device = PIXEL_6_PRO)
@Composable
fun DeveloperSupportPreviewWidth() {
    OrnaAssistantTheme {
        DeveloperSupportCard(modifier = Modifier.width(250.dp), onDonateButtonClicked = {})
    }
}

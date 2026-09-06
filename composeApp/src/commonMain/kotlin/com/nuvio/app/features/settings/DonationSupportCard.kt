package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.appTheme
import com.nuvio.app.core.ui.nuvio
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.support_nuvio_description
import nuvio.composeapp.generated.resources.support_nuvio_donate_kofi
import nuvio.composeapp.generated.resources.support_nuvio_title
import nuvio.composeapp.generated.resources.support_nuvio_view_supporters
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DonationSupportCard(
    modifier: Modifier = Modifier,
    onViewSupporters: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val uriHandler = LocalUriHandler.current
    val donateUrl = remember { CommunityConfig.DONATIONS_DONATE_URL.trim() }
    val accentColors = memberBrandWordmarkColors(MaterialTheme.appTheme)
    val backgroundBrush = remember(accentColors) {
        Brush.linearGradient(
            accentColors.mapIndexed { index, color ->
                color.copy(alpha = if (index == 0) 0.20f else 0.10f)
            },
        )
    }
    val borderBrush = remember(accentColors) {
        Brush.linearGradient(accentColors.map { it.copy(alpha = 0.55f) })
    }
    val shape = RoundedCornerShape(NuvioTokens.Radius.xl)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tokens.colors.surfaceCard,
        shape = shape,
        border = BorderStroke(tokens.borders.hairline, borderBrush),
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = tokens.colors.accent.copy(alpha = 0.13f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(72.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MemberBrandWordmark(height = 28.dp)
                Text(
                    text = stringResource(Res.string.support_nuvio_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.support_nuvio_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { uriHandler.openUri(donateUrl) },
                        enabled = donateUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.colors.accent,
                            contentColor = tokens.colors.onAccent,
                        ),
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.support_nuvio_donate_kofi))
                    }
                    onViewSupporters?.let { action ->
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.support_nuvio_view_supporters))
                        }
                    }
                }
            }
        }
    }
}

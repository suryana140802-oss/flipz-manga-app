package com.example.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppColors

data class DockItem<T>(
    val value: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Floating Capsule Navigation Dock yang terinspirasi langsung dari gambar referensi:
 * - Bentuk kapsul melayang di bagian bawah layar dengan margin kiri, kanan, bawah.
 * - Tab aktif berupa kapsul elevated (Dark Navy / Black di Light Mode, Sky Blue di Dark Mode) berisi Ikon + Teks.
 * - Tab non-aktif hanya menampilkan ikon minimalis dengan warna soft slate.
 */
@Composable
fun <T> FloatingNavigationDock(
    items: List<DockItem<out T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.dockBackground,
            shadowElevation = 6.dp,
            modifier = Modifier
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = item.value == selectedItem

                    val containerBg by animateColorAsState(
                        targetValue = if (isSelected) colors.dockActiveBackground else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dockContainerBg"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) colors.dockActiveContent else colors.dockInactiveContent,
                        label = "dockContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(containerBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onItemSelected(item.value)
                            }
                            .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(19.dp)
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    color = contentColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

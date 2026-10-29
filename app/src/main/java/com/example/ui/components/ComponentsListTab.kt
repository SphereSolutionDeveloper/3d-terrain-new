package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProceduralModelMesh
import com.example.ui.theme.*

@Composable
fun ComponentsListTab(mesh: ProceduralModelMesh) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "PRIMITIVES (${mesh.boxes.size} PARTS)",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
        )

        mesh.boxes.forEachIndexed { index, box ->
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(box.r, box.g, box.b, 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(box.r, box.g, box.b, box.a), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${index + 1}. ${box.name}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${box.sizeX}m×${box.sizeY}m×${box.sizeZ}m @ (${box.centerX}, ${box.centerY}, ${box.centerZ})",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = box.colorHex,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(box.r, box.g, box.b, 1.0f)
                    )
                }
            }
        }
    }
}

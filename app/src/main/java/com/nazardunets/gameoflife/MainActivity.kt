package com.nazardunets.gameoflife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nazardunets.gameoflife.ui.theme.GameOfLifeTheme
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameOfLifeTheme {
                val controller = remember { GameController() }

                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GameGrid(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        controller
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { controller.toggleRunning() }) {
                        Text(text = if (controller.isRunning) "Stop" else "Start")
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

object GOL {
    private fun countAliveNeighbors(grid: List<List<Boolean>>, row: Int, column: Int): Int {
        var count = 0

        for (i in (row - 1)..(row + 1)) {
            for (j in (column - 1)..(column + 1)) {
                if ((i == row && j == column)
                    || (i < 0 || i >= grid.size || j < 0 || j >= grid[0].size)
                ) continue

                if (grid[i][j]) count++
            }
        }

        return count
    }

    fun nextGen(grid: List<List<Boolean>>): List<List<Boolean>> {
        val result = List(grid.size) { MutableList(grid[0].size) { false } }

        for (i in grid.indices) {
            for (j in grid[0].indices) {
                val count = countAliveNeighbors(grid, i, j)

                result[i][j] = when {
                    grid[i][j] && (count < 2 || count > 3) -> false
                    !grid[i][j] && count == 3 -> true
                    else -> grid[i][j]
                }
            }
        }

        return result
    }
}

class GameController {
    var grid by mutableStateOf<List<List<Boolean>>>(generateInitialGrid())
        private set
    var isRunning by mutableStateOf(false)
        private set

    private var lastUpdateTime = 0L

    fun onToggleCell(rowIndex: Int, columnIndex: Int) {
        if (isRunning) return

        grid = grid.mapIndexed { rrowIndex, row ->
            if (rrowIndex != rowIndex) row
            else row.mapIndexed { ccolumnIndex, cell ->
                if (ccolumnIndex != columnIndex) cell
                else !cell
            }
        }
    }

    private fun generateInitialGrid() = List(70) { List(11) { false } }

    fun toggleRunning() {
        isRunning = !isRunning
    }

    fun onFrame(delta: Long) {
        if (isRunning) {
            val timeSinceLastUpdate = delta - lastUpdateTime
            if (timeSinceLastUpdate > MPU) {
                grid = GOL.nextGen(grid)
                lastUpdateTime = delta
            }
        }
    }

    companion object {
        const val MPU = 1000L / 2L
    }
}

@Composable
fun GameGrid(
    modifier: Modifier = Modifier,
    controller: GameController,
) {
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                controller.onFrame(it)
            }
        }
    }

    BoxWithConstraints(
        modifier
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        val cellSizeDp = maxWidth / controller.grid[0].size
        val gridHeight = cellSizeDp * controller.grid.size
        Canvas(
            Modifier
                .height(gridHeight)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val sizePx = cellSizeDp.toPx()
                            val row = (it.y / sizePx).toInt()
                            val column = (it.x / sizePx).toInt()
                            controller.onToggleCell(row, column)
                        }
                    )
                }
        ) {
            val cellSizePx = cellSizeDp.toPx()
            val borderWidthPx = BorderWidth.toPx()
            controller.grid.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, cell ->
                    val offset = Offset(
                        x = columnIndex * cellSizePx,
                        y = rowIndex * cellSizePx
                    )
                    val size = Size(cellSizePx, cellSizePx)

                    val bodyColor = if (cell) AliveColor else DeadColor
                    drawRect(
                        color = bodyColor,
                        topLeft = offset,
                        size = size
                    )
                    drawRect(
                        color = BorderColor,
                        topLeft = offset,
                        size = size,
                        style = Stroke(width = borderWidthPx)
                    )
                }
            }
        }
    }
}

val AliveColor = Color.Black
val DeadColor = Color.White
val BorderColor = Color.LightGray
val BorderWidth = 1.dp

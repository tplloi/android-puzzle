package com.example.slidepuzzle.ui.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Size
import android.view.View
import androidx.core.content.ContextCompat
import com.example.slidepuzzle.R
import com.example.slidepuzzle.ui.game.state.PuzzleGrid

class GameBoard(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val highlightColor = ContextCompat.getColor(context, R.color.board_active)

    private val paint = Paint()
    private var animator: ValueAnimator? = null

    private val tileSpacing = 3
    private var tileSize = Rect(0, 0, 0, 0)
    private var renderOffset = Rect(0, 0, 0, 0)
    private var animOffset = PointF(0.0f, 0.0f)

    private lateinit var activeSlide: Point

    private var grid = PuzzleGrid(
        null,
        Size(4, 4)
    )

    init {
        paint.isAntiAlias = true
        paint.typeface = Typeface.DEFAULT_BOLD

        setOnTouchListener { _, event ->
            onSlide(
                getSlideCoordinates(
                    PointF(
                        event.x,
                        event.y
                    )
                )
            )
            true
        }
    }

    private fun getSlideCoordinates(p: PointF): Point {
        return Point(
            (p.x / width * grid.size.width).toInt(),
            (p.y / height * grid.size.height).toInt()
        )
    }

    fun resize(size: Size, image: Bitmap? = null, shuffle: Boolean = true) {
        grid.regenerate(size, image, shuffle)
        requestLayout()
    }

    fun shuffle(reset: Boolean = false) {
        grid.shuffle(reset)
        invalidate()
    }

    private fun onSlide(p: Point) {
        val direction = grid.checkSlideMoveDirection(p)
        if (animator != null || direction == null)
            return

        // reset offset
        animOffset.set(0.0f, 0.0f)
        activeSlide = p

        // start animation
        animator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
            duration = 250

            addUpdateListener {
                animOffset.set(
                    direction.offsetX * tileSize.width() * (it.animatedValue as Float),
                    direction.offsetY * tileSize.height() * (it.animatedValue as Float)
                )
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                    grid.moveSlide(p)?.let { newCoordinates ->
                        activeSlide = newCoordinates
                        animOffset.set(0.0f, 0.0f)
                        invalidate()
                    }
                }
            })

            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        grid.let {
            tileSize.set(
                0,
                0,
                Math.ceil(MeasureSpec.getSize(widthMeasureSpec).toDouble() / it.size.width).toInt(),
                Math.ceil(MeasureSpec.getSize(heightMeasureSpec).toDouble() / it.size.height)
                    .toInt()
            )
        }
    }

    private fun drawSlideTitle(canvas: Canvas, offset: Rect, text: String) {
        // fill
        paint.strokeWidth = 4.0f
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        canvas.drawText(
            text,
            offset.left + 5.0f,
            offset.top + 13.0f,
            paint
        )

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawText(
            text,
            offset.left + 5.0f,
            offset.top + 13.0f,
            paint
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (j in 0 until grid.size.height) {
            for (i in 0 until grid.size.width) {
                val puzzle = grid.puzzles[j][i]
                puzzle?.let {
                    val active =
                        ::activeSlide.isInitialized && activeSlide.x == i && activeSlide.y == j

                    val x = i * tileSize.width()
                    val y = j * tileSize.height()

                    renderOffset.set(
                        x + tileSpacing / 2,
                        y + tileSpacing / 2,
                        x + tileSize.width() - tileSpacing,
                        y + tileSize.height() - tileSpacing
                    )

                    if (active) {
                        renderOffset.left += animOffset.x.toInt()
                        renderOffset.top += animOffset.y.toInt()
                        renderOffset.right += animOffset.x.toInt()
                        renderOffset.bottom += animOffset.y.toInt()
                    }

                    canvas.drawBitmap(
                        puzzle.bitmap,
                        null,
                        renderOffset,
                        null
                    )

                    // fill
                    drawSlideTitle(
                        canvas,
                        renderOffset,
                        (puzzle.index + 1).toString()
                    )

                    // Draw border around active
                    if (active) {
                        paint.strokeWidth = 4.0f
                        paint.style = Paint.Style.STROKE
                        paint.color = highlightColor

                        canvas.drawRect(
                            renderOffset,
                            paint
                        )
                    }
                }
            }
        }
    }
}
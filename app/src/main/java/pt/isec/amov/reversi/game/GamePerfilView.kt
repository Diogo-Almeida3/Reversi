package pt.isec.amov.reversi.game

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import pt.isec.amov.reversi.R
import android.graphics.BitmapFactory


import androidx.core.graphics.scale


private const val MARGIN = 24
private const val LINE_SIZE = 5
private const val MARGIN_PIECE = 8
private const val ROUND_RADIUS = 20F
class GamePerfilView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var windowHeight = 0
    private var windowWidth = 0
    private var pieceHeight = 0
    private var pieceWidth = 0
    private val orientation = resources.configuration.orientation

    private lateinit var boardGame: BoardGame

    fun setData(boardGame: BoardGame) {
        this.boardGame = boardGame

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)

        pieceHeight = (windowHeight - LINE_SIZE) / boardGame.getBoardSize()
        pieceWidth = (windowWidth - LINE_SIZE) / boardGame.getBoardSize()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {

            drawBackground(canvas)
        if(orientation != Configuration.ORIENTATION_LANDSCAPE)
            drawDataPortrait(canvas)
        else
            drawDataLandscape(canvas)

    }

    private fun drawDataLandscape(canvas: Canvas?) {
        for (i in 0 until boardGame.getPlayers()) {
            val top = (windowHeight / boardGame.getPlayers()) * i + MARGIN * 2
            val bottom = (windowHeight / boardGame.getPlayers()) * i + (windowHeight / boardGame.getPlayers()) - MARGIN
            val left = 0
            val right = windowWidth
            val middleVertical = bottom - ((bottom - top) / 2)
            val middleHorizontal = windowWidth / 2

            /* Image Related */
            val imageSize = 400 / boardGame.getPlayers()
            val imagePos = middleVertical - (imageSize / 2)
            val size = getNameSize()

            /* Text Related */
            val textPos = middleVertical - 50
            val paintName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
            }
            val nChars = boardGame.getUsername(i).length

            /* Score Related */
            val boxQuarter = (right - left) / 4
            val centerX = middleHorizontal + boxQuarter
            val centerY = middleVertical + 45
            val paint = Paint().apply { color = boardGame.getColor(i) }
            val paintScore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = getScoreSize()
            }
            val actualScore = boardGame.getTotalPieces(i)

            when (boardGame.getGameMode()) {
                0 -> {
                    if (i == 0) { //Jogador atual
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize, imageSize, false), 50f, imagePos.toFloat(), null)
                        canvas?.drawText(boardGame.getUsername(i), 0, nChars, (middleHorizontal + boxQuarter/2 - nChars * 12).toFloat(), textPos.toFloat(), paintName)
                    } else { //Anónimo
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher).scale(imageSize, imageSize, false), 50f, imagePos.toFloat(), null)
                        canvas?.drawText("Anónimo", 0, 7, (middleHorizontal + boxQuarter/2 - nChars * 12).toFloat(), textPos.toFloat(), paintName)
                    }
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 65f, paint)
                    canvas?.drawText(actualScore.toString(), middleHorizontal.toFloat(),  (middleVertical + 75).toFloat(), paintScore)
                }
                1 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize, imageSize, false), 50f, imagePos.toFloat(), null)
                    canvas?.drawText(boardGame.getUsername(i), 0, nChars, (middleHorizontal + boxQuarter/2 - nChars * 12).toFloat(), textPos.toFloat(), paintName)
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 65f, paint)
                    canvas?.drawText(actualScore.toString(), middleHorizontal.toFloat(),  (middleVertical + 75).toFloat(), paintScore)
                }
                2 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize, imageSize, false), 50f, imagePos.toFloat(), null)
                    canvas?.drawText(boardGame.getUsername(i), 0, nChars, (middleHorizontal + boxQuarter/2 - nChars * 12).toFloat(), textPos.toFloat(), paintName)
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat()-30, 50f, paint)
                    canvas?.drawText(actualScore.toString(), middleHorizontal.toFloat(),  (middleVertical + 45).toFloat(), paintScore)
                }
            }


        }
    }

    private fun drawBackground(canvas: Canvas?) {
        /* Portrait */
        for (i in 0 until boardGame.getPlayers()) {
            var left: Int
            var top : Int
            var right : Int
            var bottom : Int
            var rect : RectF

            if (orientation == Configuration.ORIENTATION_PORTRAIT ) {
                left = (windowWidth / boardGame.getPlayers()) * i + MARGIN
                top = 0
                right = (windowWidth / boardGame.getPlayers()) * i + (windowWidth / boardGame.getPlayers()) - MARGIN
                bottom = windowHeight
                rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            } else {
                left = 0
                top = (windowHeight / boardGame.getPlayers()) * i + MARGIN
                right = windowWidth
                bottom = (windowHeight / boardGame.getPlayers()) * i + (windowHeight / boardGame.getPlayers()) - MARGIN
                rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            }

            canvas?.drawRoundRect(rect, ROUND_RADIUS, ROUND_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            canvas?.drawRoundRect(rect, ROUND_RADIUS, ROUND_RADIUS,
                Paint(Paint.ANTI_ALIAS_FLAG and Paint.DITHER_FLAG).apply {
                    color = boardGame.getBoardColor(1)
                    style = Paint.Style.STROKE
                    strokeWidth = 10.0f
                })
        }
    }

    private fun drawDataPortrait(canvas: Canvas?) {
        /* Portrait */
        for (i in 0 until boardGame.getPlayers()) {
            val right = (windowWidth / boardGame.getPlayers()) * i + (windowWidth / boardGame.getPlayers()) - MARGIN
            val left = (windowWidth / boardGame.getPlayers()) * i + MARGIN

            /* Image Related */
            val imageSize = 400 / boardGame.getPlayers()
            val middle = right - ((right - left) / 2)
            val imagePos = middle - (imageSize / 2)
            val size = getNameSize()

            /* Text Related */
            val paintName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
            }
            val nChars = boardGame.getUsername(i).length

            /* Score Related */
            val boxQuarter = (right - left) / 4
            val centerX = middle + boxQuarter
            val centerY = imageSize + 240
            val paint = Paint().apply { color = boardGame.getColor(i) }
            val textPosX = middle - boxQuarter
            val textPosY = imageSize + 275
            val paintScore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = getScoreSize()
            }
            val actualScore = boardGame.getTotalPieces(i)

            when (boardGame.getGameMode()) {
                0 -> {
                    if (i == 0) { //Jogador atual
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize, imageSize, false), imagePos.toFloat(), 50f, null)
                        canvas?.drawText(boardGame.getUsername(i), 0, nChars, (middle - nChars * 15).toFloat(), (imageSize + 120).toFloat(), paintName)
                    } else { //Anónimo
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher).scale(imageSize, imageSize, false), imagePos.toFloat(), 50f, null)
                        canvas?.drawText("Anónimo", 0, 7, (middle - 7 * 15).toFloat(), (imageSize + 120).toFloat(), paintName)
                    }
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 65f, paint)
                }
                1 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize, imageSize, false), imagePos.toFloat(), 50f, null)
                    canvas?.drawText(boardGame.getUsername(i), 0, nChars, (middle - nChars * 15).toFloat(), (imageSize + 120).toFloat(), paintName)
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 65f, paint)
                }
                2 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize, imageSize, false), imagePos.toFloat(), 50f, null)
                    canvas?.drawText(boardGame.getUsername(i), 0, nChars, (middle - nChars * 8.75).toFloat(), (imageSize + 120).toFloat(), paintName)
                    canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), 50f, paint)
                }
            }

            canvas?.drawText(actualScore.toString(), textPosX.toFloat(), textPosY.toFloat(), paintScore)
        }
    }

    private fun getNameSize(): Float { //Limitar o nome a 15 chars
        if (boardGame.getGameMode() != 2)
            return 56F
        return 38F
    }

    private fun getScoreSize(): Float {
        if (boardGame.getGameMode() != 2)
            return 82F
        return 56F
    }
}
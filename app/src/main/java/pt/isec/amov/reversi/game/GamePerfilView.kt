package pt.isec.amov.reversi.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import pt.isec.amov.reversi.R
import android.graphics.BitmapFactory

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.graphics.scale
import java.io.File
import java.net.URL


private const val MARGIN = 24

class GamePerfilView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var windowHeight = 0
    private var windowWidth = 0

    private lateinit var boardGame: BoardGame

    fun setData(boardGame: BoardGame) {
        this.boardGame = boardGame

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        drawBackground(canvas)
        //drawData(canvas) //todo Iluminar ou colocar uma bola no jogador que é a jogar de modo a identificar
    }

    private fun drawBackground(canvas: Canvas?) {
        for (i in 0 until boardGame.getPlayers()) {
            val left = (windowWidth / boardGame.getPlayers()) * i + MARGIN
            val top = 0
            val right = (windowWidth /boardGame.getPlayers()) * i + (windowWidth / boardGame.getPlayers()) - MARGIN
            val bottom = windowHeight
            val rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

            canvas?.drawRoundRect(
                rect,
                20F,
                20F,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            canvas?.drawRoundRect(
                rect,
                20F,
                20F,
                Paint(Paint.ANTI_ALIAS_FLAG and Paint.DITHER_FLAG).apply {
                    color = Color.BLUE
                    style = Paint.Style.STROKE
                    strokeWidth = 10.0f
                })

            val imageSize = 400 / boardGame.getPlayers()
            val middle = right - ((right-left)/2)
            val imagePos = middle - (imageSize/2)
            val size = getNameSize()

            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = size
            }

            val nChars = boardGame.getUsername(i).length
            when(boardGame.getGameMode()){
                0 -> {
                    if(i == 0){ //Jogador atual
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                        canvas?.drawText(boardGame.getUsername(i),0,nChars, (middle - nChars*15).toFloat(),(imageSize + 120).toFloat(),text)
                    }
                    else{ //Anónimo
                        canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                        canvas?.drawText("Anónimo",0,7, (middle - 7*15).toFloat(),(imageSize + 120).toFloat(),text)
                    }
                }
                1 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                    canvas?.drawText(boardGame.getUsername(i),0,nChars, (middle - nChars*15).toFloat(),(imageSize + 120).toFloat(),text)
                }
                2 -> {
                    canvas?.drawBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo_reversi).scale(imageSize,imageSize,false),imagePos.toFloat(),50f,null)
                    canvas?.drawText(boardGame.getUsername(i),0,nChars, (middle - nChars*8.75).toFloat(),(imageSize + 120).toFloat(),text)
                }
            }


        }
    }

    private fun getNameSize() : Float{ //Limitar o nome a 15 chars
        if(boardGame.getGameMode() != 2)
            return 56F
        return 38F
    }
    private fun drawData(canvas: Canvas?) {
        for(i in 0 until boardGame.getPlayers()){
            //Definir a imagem do utilizador guardada na class Player
            //Definir o nome do utilizador guardado na class Player
            //Definir a pontuação do utilizador armazenada na class Player
        }
    }
}
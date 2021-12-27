package pt.isec.amov.reversi.game.jsonClasses.specialPieces

import java.util.ArrayList

class RequestBomb {
    var type: String? = null
    var switchToBomb : Boolean? = null

    constructor(): super() {
        this.type = "REQUEST_BOMB"
    }

    constructor(switchToBomb : Boolean): super() {
        this.type = "REQUEST_BOMB"
        this.switchToBomb = switchToBomb
    }
}